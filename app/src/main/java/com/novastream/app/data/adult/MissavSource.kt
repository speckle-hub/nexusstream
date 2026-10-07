package com.novastream.app.data.adult

/**
 * MissAV built-in JAV source.
 *
 * Listings come from `/en/new`, `/en/release`, `/en/trending` and `/en/today` (cards link to
 * `/en/{code}`); search from `/en/search/{query}`. MissAV serves its manifest from a `surrit.com`
 * CDN and exposes it as an `.m3u8` on the video page, so streams are resolved with [TubeMedia].
 */
object MissavSource : AdultSource {

    override val id = "missav"
    override val name = "MissAV"
    private const val BASE = "https://missav.ws"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "New" to "$BASE/en/new",
            "Release" to "$BASE/en/release",
            "Trending" to "$BASE/en/trending",
            "Today" to "$BASE/en/today",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/en/search/" + encode(query)))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/en/genres")
        return adultRx("""<a[^>]*href="(?:https?://[^"]*missav\.ws)?(/en/genres/[^"/]+)"[^>]*>([^<]{2,40})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.removePrefix("/en/genres/").trim('/'), label, BASE + path)
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
            ?: "MissAV video"
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
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*missav\.ws)?/en/[a-z0-9_\-]+)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val path = href.substringAfter("missav.ws").ifBlank { href }
            // Skip the section/navigation links (`/en/new`, `/en/genres`, …): every real video
            // code carries a digit (e.g. `/en/ssis-001`, `/en/h_1717cstj00019`), the sections do not.
            if (!path.substringAfterLast('/').any { it.isDigit() }) continue
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultCollapse(window.capture("""<a[^>]*>([^<]{3,200})</a>""")?.let { stripTags(it) })
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: codeTitle(path)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = if (href.startsWith("http")) href else BASE + path,
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""class="[^"]*duration[^"]*"[^>]*>([0-9:]{3,9})<"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun codeTitle(path: String): String =
        path.trim('/').substringAfterLast('/').replace('-', ' ')
            .ifBlank { "MissAV video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
