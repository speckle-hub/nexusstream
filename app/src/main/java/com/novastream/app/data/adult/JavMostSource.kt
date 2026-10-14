package com.novastream.app.data.adult

/**
 * JavMost built-in JAV source (javmost.com).
 *
 * A JAV tube reached here behind bot protection, so the parser is deliberately
 * multi-fallback: stock WordPress search (`/?s={query}` + `/search/{query}/`),
 * generic post-permalink cards filtered against the taxonomy/chrome paths, and
 * streams from the post page via [TubeMedia]. Per-source error isolation keeps
 * a blocked JavMost from affecting the rest of the tab.
 */
object JavMostSource : AdultSource {

    override val id = "javmost"
    override val name = "JavMost"
    private const val BASE = "https://javmost.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/",
            "Popular" to "$BASE/popular/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/?s=$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/search/$q/") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = fetch("$BASE/")
            adultRx("""<a[^>]*href="((?:https?://[^"]*javmost\.com)?/(?:category|genre|tag)/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1].substringAfter("javmost.com").ifBlank { m.groupValues[1] }
                    val label = adultUnescape(m.groupValues[2])?.trim()
                        ?: return@mapNotNull null
                    if (label.isEmpty()) null
                    else AdultCategory(path.trim('/').substringAfterLast('/'), label, BASE + path)
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
            ?: "JavMost video"
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

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*javmost\.com)?/(?!category/|tag/|genre/|studio/|actor/|director/|author/|page/|search|wp-|feed|comment)[^"#?]{3,180}/?)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            if (isChrome(href)) continue
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
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun isChrome(href: String): Boolean = CHROME_PATHS.any { href.contains(it) }

    private fun absolute(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        else -> BASE + href
    }

    private fun slugTitle(href: String): String =
        href.trim('/').substringAfterLast('/').replace('-', ' ').replace('_', ' ')
            .ifBlank { "JavMost video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val CHROME_PATHS = listOf(
        "/category/", "/tag/", "/genre/", "/studio/", "/actor/", "/director/",
        "/author/", "/page/", "/search", "/wp-", "/feed", "/comment",
    )

    private val STATIC_CATEGORIES = listOf(
        "censored" to "Censored",
        "uncensored" to "Uncensored",
        "english-subbed" to "English Subbed",
        "amateur" to "Amateur",
    )
}
