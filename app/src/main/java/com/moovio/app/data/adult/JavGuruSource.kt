package com.moovio.app.data.adult

/**
 * JavGuru built-in JAV source (jav.guru).
 *
 * A WordPress-style JAV tube: the front page carries the latest posts and stable category
 * archives exist for the translated/decensored shelves (`/category/english-subbed/`,
 * `/category/decensored/`). Search is the stock WordPress `/?s={query}` with a
 * `/search/{query}/` fallback. Cards are generic post permalinks, filtered against the
 * taxonomy/chrome paths; streams come from the post page via [TubeMedia].
 */
object JavGuruSource : AdultSource {

    override val id = "javguru"
    override val name = "JavGuru"
    private const val BASE = "https://jav.guru"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/",
            "English Subbed" to "$BASE/category/english-subbed/",
            "Decensored" to "$BASE/category/decensored/",
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
            adultRx("""<a[^>]*href="((?:https?://[^"]*jav\.guru)?/category/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1].substringAfter("jav.guru").ifBlank { m.groupValues[1] }
                    val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
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
            ?: "JavGuru video"
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

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*jav\.guru)?/(?!category/|tag/|studio/|actor/|director/|genre/|author/|page/|search|wp-)[^"#?]{3,180}/?)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            if (isChrome(href)) continue
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: adultCollapse(window.capture("""<a[^>]*>([^<]{3,200})</a>""")?.let { stripTags(it) })
                ?: slugTitle(href)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href),
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""class="[^"]*duration[^"]*"[^>]*>([0-9:]{3,9})<"""),
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
            .ifBlank { "JavGuru video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val CHROME_PATHS = listOf(
        "/category/", "/tag/", "/studio/", "/actor/", "/director/", "/genre/",
        "/author/", "/page/", "/search", "/wp-", "/feed", "/comment",
    )

    private val STATIC_CATEGORIES = listOf(
        "english-subbed" to "English Subbed",
        "decensored" to "Decensored",
        "uncensored" to "Uncensored",
        "censored" to "Censored",
        "amateur" to "Amateur",
        "vr" to "VR",
    )
}
