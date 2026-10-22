package com.moovio.app.data.adult

import com.moovio.app.data.model.StreamSource

/**
 * TXXX built-in source (txxx.com).
 *
 * An Aylo-family tube (same engine as YouPorn/RedTube): listings under
 * `/best/`, `/newest/` and `/most_viewed/`, search under `/?search=`. Streams
 * prefer the site's own `media_definitions` JSON endpoint and fall back to
 * scanning the page markup through [TubeMedia] — mirroring [YouPornSource].
 */
object TxxxSource : AdultSource {

    override val id = "txxx"
    override val name = "TXXX"
    private const val BASE = "https://www.txxx.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Best" to "$BASE/best/",
            "Newest" to "$BASE/newest/",
            "Most viewed" to "$BASE/most_viewed/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/?search=$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/search/?query=$q") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = fetch("$BASE/categories/")
            adultRx("""<a[^>]*href="(/category/[^"/]+/)"[^>]*>([^<]{2,50})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1]
                    val label = adultUnescape(m.groupValues[2])?.trim()
                        ?: return@mapNotNull null
                    if (label.isEmpty()) null
                    else AdultCategory(path.removePrefix("/category/").trim('/'), label, BASE + path)
                }
                .distinctBy { it.id }
                .take(24)
                .toList()
        }.getOrDefault(emptyList())
        if (live.isNotEmpty()) return live
        return STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/category/$slug/") }
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "TXXX video"
        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = adultUnescapeJson(html.ogContent("og:image")),
                duration = adultCollapse(
                    html.capture("""class="[^"]*duration[^"]*"[^>]*>([0-9:]{3,9})<""")
                ),
            ),
            streams = resolveStreams(html, url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal suspend fun resolveStreams(html: String, pageUrl: String): List<StreamSource> {
        val videoId = pageUrl.substringAfter("/watch/", "").substringBefore('/').takeIf { it.isNotBlank() }
        if (videoId != null) {
            val json = runCatching {
                AdultHttp.get("$BASE/api/video/media_definitions/$videoId/", referer = pageUrl)
            }.getOrNull()
            if (json != null) {
                val parsed = TubeMedia.definitions(json, this, pageUrl)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return TubeMedia.streams(html, this, referer = pageUrl)
    }

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="(/watch/[^"#?]+)"[^>]*>""").findAll(html)) {
            val path = m.groupValues[1]
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2200))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: path.substringAfter("/watch/").substringBefore('/').replace('-', ' ')
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = BASE + path,
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_CATEGORIES = listOf(
        "amateur" to "Amateur",
        "anal" to "Anal",
        "big-tits" to "Big Tits",
        "blowjob" to "Blowjob",
        "milf" to "Milf",
        "teen" to "Teen",
    )
}
