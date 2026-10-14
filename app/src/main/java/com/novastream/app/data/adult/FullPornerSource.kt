package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * FullPorner built-in source (fullporner.com).
 *
 * A full-length tube: video pages are `/watch/{hash}`, the front page carries
 * the featured shelf (paged under `/home/{n}/`), categories live under
 * `/category/{slug}` and pornstars under `/pornstar/{slug}`. Streams come from
 * the watch page via [TubeMedia].
 */
object FullPornerSource : AdultSource {

    override val id = "fullporner"
    override val name = "FullPorner"
    private const val BASE = "https://fullporner.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Featured" to "$BASE/",
            "Page 2" to "$BASE/home/2",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/search/$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/search/$q/1") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = fetch("$BASE/category")
            adultRx("""<a[^>]*href="(/category/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1]
                    val slug = path.trim('/').substringAfterLast('/')
                    val label = adultUnescape(m.groupValues[2])?.trim()
                        ?: return@mapNotNull null
                    if (slug.isEmpty() || label.isEmpty() || slug == "category") null
                    else AdultCategory(slug, label, BASE + path)
                }
                .distinctBy { it.id }
                .take(24)
                .toList()
        }.getOrDefault(emptyList())
        if (live.isNotEmpty()) return live
        return STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/category/$slug") }
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "FullPorner video"
        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = adultUnescapeJson(html.ogContent("og:image")),
                duration = adultCollapse(
                    html.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)""")
                ),
            ),
            streams = TubeMedia.streams(html, this, referer = url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = "$BASE/")

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*fullporner\.com)?/watch/[A-Za-z0-9]+/?)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2200))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: href.trim('/').substringAfterLast('/').ifBlank { "FullPorner video" }
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href),
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
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

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_CATEGORIES = listOf(
        "anal" to "Anal",
        "blowjob" to "Blowjob",
        "big-tits" to "Big Tits",
        "milf" to "Milf",
        "teen" to "Teen",
        "threesome" to "Threesome",
    )
}
