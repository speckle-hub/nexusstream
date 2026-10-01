package com.novastream.app.data.adult

/** XVideos built-in source. */
object XvideosSource : AdultSource {

    override val id = "xvideos"
    override val name = "XVideos"
    private const val BASE = "https://www.xvideos.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/",
            "Best" to "$BASE/best",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/?k=" + java.net.URLEncoder.encode(query, "UTF-8")))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/tags")
        return adultRx("""<a[^>]*href="(/tags/[^"/?]+)"[^>]*>([^<]{2,60})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.removePrefix("/tags/"), label, BASE + path)
            }
            .distinctBy { it.id }
            .take(24)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.capture("""html5player\.setVideoTitle\('([^']*)'\)"""))
            ?: html.capture("""<title>([^<|]*)""")?.trim().orEmpty()
        val thumb = html.capture("""html5player\.setThumbUrl\('([^']*)'\)""")
        val uploader = adultUnescape(html.capture("\"uploader\":\"([^\"]*)\""))
        val duration = html.capture("""<span class="duration">([^<]+)</span>""")

        val video = AdultVideo(
            sourceId = id,
            url = url,
            title = title.ifBlank { "XVideos video" },
            thumbnail = thumb,
            duration = duration,
            uploader = uploader,
        )
        return AdultDetail(
            video = video,
            uploader = uploader,
            streams = Xtubes.parseStreams(html, id, name, pageUrl = url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal fun parseList(html: String): List<AdultVideo> = Xtubes.parseList(html, BASE, id)
}
