package com.novastream.app.data.adult

/**
 * xHamster built-in source.
 *
 * Listings come from `/newest`, `/best` and `/trending` (each a page of `a[href="/videos/{slug}-{id}"]`
 * cards); search from `/search/{query}`. Detail pages embed the player config (`xplayerSettings` /
 * a `sources` object) somewhere in the page, so streams are resolved with the shared
 * [TubeMedia] extractor, which copes with xHamster's escaped `https:\/\/…` JSON strings.
 */
object XhamsterSource : AdultSource {

    override val id = "xhamster"
    override val name = "xHamster"
    private const val BASE = "https://xhamster.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Newest" to "$BASE/newest",
            "Best" to "$BASE/best",
            "Trending" to "$BASE/trending",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/search/" + encode(query)))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories")
        return adultRx("""<a[^>]*href="((?:https?://[^"]*xhamster\.com)?/categories/[^"]+)"[^>]*>([^<]{2,40})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1].substringAfter("xhamster.com").takeIf { it.isNotBlank() }
                    ?: m.groupValues[1].takeIf { it.startsWith("/") }
                    ?: return@mapNotNull null
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.removePrefix("/categories/").trim('/'), label, BASE + path)
            }
            .distinctBy { it.id }
            .take(24)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "xHamster video"
        val thumb = adultUnescapeJson(html.ogContent("og:image"))
        val duration = adultCollapse(
            html.capture("""<span class="duration">([^<]+)</span>""")
                ?: html.capture("""["']duration["']\s*:\s*["']?([0-9:]{3,9})""")
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

    /**
     * Parse a listing page into cards. Each card is one anchor to `/videos/…`; a bounded window is
     * cut from it to pick up the thumbnail and duration that sit alongside the link.
     */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*xhamster\.com)?/videos/[^"#?]+)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val path = href.substringAfter("xhamster.com").ifBlank { href }
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(path)
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

    private fun slugTitle(path: String): String =
        path.substringAfter("/videos/")
            .substringBeforeLast('-')
            .replace('-', ' ')
            .replace('_', ' ')
            .ifBlank { "xHamster video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
