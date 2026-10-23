package com.moovio.app.data.adult

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.remote.str

/**
 * Eporner built-in source.
 *
 * Eporner exposes a public JSON API (v2), which makes it the cleanest general tube alongside
 * the retired Avgle API: home rows and search come from
 * `/api/v2/video/search/?query=…&per_page=…&page=…&thumbsize=big&order=…&gay=0&lq=0&format=json`
 * (`order` = `latest` / `top-week` / `top-month`), and each entry already carries its canonical
 * page URL, thumbnail and duration. Streams come from the video page itself via [TubeMedia],
 * which picks up the player's MP4/HLS sources.
 */
object EpornerSource : AdultSource {
    override val experimental = true

    override val id = "eporner"
    override val name = "Eporner"
    private const val BASE = "https://www.eporner.com"
    private const val API = "https://www.eporner.com/api/v2/video/search/"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to apiUrl(query = null, order = "latest"),
            "Top week" to apiUrl(query = null, order = "top-week"),
            "Top month" to apiUrl(query = null, order = "top-month"),
        ),
    ) { url -> parseApi(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseApi(fetch(apiUrl(query = query, order = "latest")))

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = AdultHttp.get("$BASE/cats/", referer = BASE)
            adultRx("""<a[^>]*href="((?:https?://[^"]*eporner\.com)?/cat/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1].substringAfter("eporner.com").ifBlank { m.groupValues[1] }
                    val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                    if (label.isEmpty()) null
                    else AdultCategory(path.trim('/').substringAfterLast('/'), label, BASE + path)
                }
                .distinctBy { it.id }
                .take(24)
                .toList()
        }.getOrDefault(emptyList())
        if (live.isNotEmpty()) return live
        // Curated fallback: the catalogue's stable top-level categories.
        return STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/cat/$slug/") }
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "Eporner video"
        val thumb = adultUnescapeJson(html.ogContent("og:image"))
        val duration = adultCollapse(
            html.capture("""<span[^>]*class="[^"]*duration[^"]*">([^<]+)</span>""")
        )
        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = thumb,
                duration = duration,
            ),
            streams = TubeMedia.streams(html, this, referer = BASE),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal fun apiUrl(query: String?, order: String, page: Int = 1, perPage: Int = 30): String {
        val q = if (query.isNullOrBlank()) "" else "query=" + encode(query) + "&"
        return "$API?${q}per_page=$perPage&page=$page&thumbsize=big&order=$order&gay=0&lq=0&format=json"
    }

    /** Parse an API search/list response: root `videos[]`. */
    internal fun parseApi(json: String): List<AdultVideo> {
        val videos = runCatching {
            JsonParser.parseString(json).asJsonObject
                .get("videos")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return videos
            .mapNotNull { el -> el.takeIf { it.isJsonObject }?.asJsonObject?.let { parseVideo(it) } }
            .distinctBy { it.url }
    }

    internal fun parseVideo(o: JsonObject): AdultVideo? {
        val title = adultUnescape(o.str("title")) ?: return null
        val pageUrl = adultUnescape(o.str("url"))?.takeIf { it.startsWith("http") } ?: return null
        val thumb = o.get("default_thumb")?.takeIf { it.isJsonObject }?.asJsonObject?.str("src")
            ?.let { adultUnescape(it) }
            ?: o.get("thumbs")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject?.str("src")
                ?.let { adultUnescape(it) }
        val duration = o.str("length_sec")?.toLongOrNull()?.takeIf { it > 0 }?.let { adultDuration(it) }
            ?: adultUnescape(o.str("length_min"))
        return AdultVideo(
            sourceId = id,
            url = pageUrl,
            title = title,
            thumbnail = thumb,
            duration = duration,
            views = o.str("views"),
        )
    }

    /** Parse an HTML listing/category page into cards (category rows + related). */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*eporner\.com)?/(?:video-|hd-porn/|video/)[^"#?]+)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(href)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href),
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""(?:duration|time)[^>]*>\s*([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun absolute(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        else -> BASE + href
    }

    private fun slugTitle(href: String): String =
        href.trim('/').substringAfterLast('/').replace('-', ' ').replace('_', ' ')
            .ifBlank { "Eporner video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_CATEGORIES = listOf(
        "amateur" to "Amateur",
        "anal" to "Anal",
        "asian" to "Asian",
        "big-tits" to "Big Tits",
        "blowjob" to "Blowjob",
        "brunette" to "Brunette",
        "hardcore" to "Hardcore",
        "lesbian" to "Lesbian",
        "milf" to "MILF",
        "teen" to "Teen",
        "threesome" to "Threesome",
        "vintage" to "Vintage",
    )
}
