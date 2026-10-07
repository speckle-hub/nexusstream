package com.novastream.app.data.adult

/**
 * Jable built-in JAV source.
 *
 * Listings come from `/latest-updates/`, `/hot/` and `/new/` (cards link to `/videos/{code}/`);
 * search from `/search/{query}/`. The player page embeds an HLS manifest (`var hlsUrl = '…'`, a
 * `<source src>`, or a bare `.m3u8`), which [TubeMedia] picks up.
 */
object JableSource : AdultSource {

    override val id = "jable"
    override val name = "Jable"
    private const val BASE = "https://jable.tv"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/latest-updates/",
            "Hot" to "$BASE/hot/",
            "New" to "$BASE/new/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/search/" + encode(query) + "/"))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories/")
        return adultRx("""<a[^>]*href="(https?://[^"]*jable\.tv/tags/[^"/]+/)"[^>]*>([^<]{2,40})</a>""")
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
            ?: adultCollapse(html.capture("""<h4[^>]*>(.*?)</h4>""")?.let { stripTags(it) })
            ?: "Jable video"
        val thumb = adultUnescapeJson(html.ogContent("og:image"))
        val duration = adultCollapse(html.capture("""<span[^>]*class="[^"]*duration[^"]*">([^<]+)</span>"""))

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
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*jable\.tv)?/videos/[^"#?]+)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val path = href.substringAfter("jable.tv").ifBlank { href }
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultCollapse(window.capture("""<h6[^>]*>(.*?)</h6>""")?.let { stripTags(it) })
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
            .ifBlank { "Jable video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
