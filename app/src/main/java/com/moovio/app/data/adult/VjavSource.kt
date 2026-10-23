package com.moovio.app.data.adult

/**
 * VJAV built-in JAV source (vjav.com).
 *
 * A paged JAV tube with a stable, confirmed layout: listings under
 * `/videos/latest-updates/{page}/`, `/videos/most-popular/{page}/` and
 * `/videos/top-rated/{page}/`, and search under `/search/{page}/?s={query}`. Video cards
 * link to `/video…` pages (both `/video/{id}` and `/videos/{id}/{slug}` shapes are
 * accepted); listing/chrome paths (latest-updates, most-popular, top-rated, search,
 * categories, tags, models, page) are excluded. Streams come from the video page via
 * [TubeMedia].
 */
object VjavSource : AdultSource {
    override val experimental = true

    override val id = "vjav"
    override val name = "VJAV"
    private const val BASE = "https://vjav.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/videos/latest-updates/1/",
            "Most popular" to "$BASE/videos/most-popular/1/",
            "Top rated" to "$BASE/videos/top-rated/1/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/search/1/?s=$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/search/$q/") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = fetch("$BASE/categories/")
            adultRx("""<a[^>]*href="((?:https?://[^"]*vjav\.com)?/(?:categories|tags|models)/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1].substringAfter("vjav.com").ifBlank { m.groupValues[1] }
                    val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                    if (label.isEmpty()) null
                    else AdultCategory(path.trim('/'), label, BASE + path)
                }
                .distinctBy { it.id }
                .take(24)
                .toList()
        }.getOrDefault(emptyList())
        if (live.isNotEmpty()) return live
        return STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/$slug/") }
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "VJAV video"
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
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*vjav\.com)?/videos?/[^"#?]+)"[^>]*>""")
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
                    duration = window.capture("""(?:duration|time)[^>]*>\s*([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
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
            .ifBlank { "VJAV video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val CHROME_PATHS = listOf(
        "latest-updates", "most-popular", "top-rated", "/search", "/categories",
        "/tags", "/models", "/pornstars", "/page/", "/playlist/",
    )

    private val STATIC_CATEGORIES = listOf(
        "videos/latest-updates/1" to "Latest",
        "videos/most-popular/1" to "Most Popular",
        "videos/top-rated/1" to "Top Rated",
    )
}
