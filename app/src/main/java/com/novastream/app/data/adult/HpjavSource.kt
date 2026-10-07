package com.novastream.app.data.adult

/**
 * HPJAV built-in JAV source.
 *
 * A WordPress tube site: listings from `/lastest-updates/` and `/popular/` (numeric `/{id}/{slug}/`
 * post links), search from `/?s={query}`. The video page embeds a JW Player setup block whose
 * `file:`/`sources` entries point at an MP4 (or HLS) file, which [TubeMedia] extracts.
 */
object HpjavSource : AdultSource {

    override val id = "hpjav"
    override val name = "HPJAV"
    private const val BASE = "https://hpjav.tv"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/lastest-updates/",
            "Popular" to "$BASE/popular/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/?s=" + encode(query)))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/")
        return adultRx("""<a[^>]*href="(https?://[^"]*hpjav\.tv/(?:category|tag)/[^"/]+/)"[^>]*>([^<]{2,40})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val url = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(url.trim('/').substringAfterLast('/'), label, url)
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
            ?: "HPJAV video"
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
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*hpjav\.tv)?/\d{5,}/[^"#?]+)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val path = href.substringAfter("hpjav.tv").ifBlank { href }
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultCollapse(
                    window.capture("""class="[^"]*entry-title[^"]*"[^>]*>(.*?)</a>""")?.let { stripTags(it) }
                )
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(path)
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

    private fun slugTitle(path: String): String =
        path.trim('/').substringAfterLast('/')
            .replace('-', ' ')
            .ifBlank { "HPJAV video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
