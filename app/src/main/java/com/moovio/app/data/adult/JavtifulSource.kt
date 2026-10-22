package com.moovio.app.data.adult

/**
 * Javtiful built-in JAV source (javtiful.com).
 *
 * A modern JAV catalogue keyed by studio code: video pages are
 * `/video/{id}/{slug}` (e.g. `/video/107099/lulu-435`), listings under
 * `/videos` with `?sort=popular_week|most_viewed|most_liked`, shelves under
 * `/censored`, `/uncensored` and `/reducing-mosaic`, and categories under
 * `/category/{slug}`. Streams come from the video page via [TubeMedia].
 */
object JavtifulSource : AdultSource {

    override val id = "javtiful"
    override val name = "Javtiful"
    private const val BASE = "https://javtiful.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Popular this week" to "$BASE/videos?sort=popular_week",
            "Most viewed" to "$BASE/videos?sort=most_viewed",
            "Uncensored" to "$BASE/uncensored",
            "Reducing mosaic" to "$BASE/reducing-mosaic",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/search?q=$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/videos?search=$q") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> {
        val live = runCatching {
            val html = fetch("$BASE/categories")
            adultRx("""<a[^>]*href="(/category/[^"/]+/?)"[^>]*>([^<]{2,40})</a>""")
                .findAll(html)
                .mapNotNull { m ->
                    val path = m.groupValues[1]
                    val slug = path.trim('/').substringAfterLast('/')
                    val label = adultUnescape(m.groupValues[2])?.trim()?.substringBeforeLast(' ')
                        ?: return@mapNotNull null
                    if (slug.isEmpty() || label.isEmpty()) null
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
            ?: "Javtiful video"
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
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*javtiful\.com)?/video/\d+[^"#?]*)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
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

    private fun slugTitle(href: String): String =
        href.trim('/').substringAfterLast('/').replace('-', ' ').replace('_', ' ')
            .uppercase().ifBlank { "Javtiful video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_CATEGORIES = listOf(
        "big-tits" to "Big Tits",
        "married-woman" to "Married Woman",
        "amateur" to "Amateur",
        "mature-woman" to "Mature Woman",
        "school-girls" to "School Girls",
    )
}
