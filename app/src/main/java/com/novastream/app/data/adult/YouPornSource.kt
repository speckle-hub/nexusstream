package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * YouPorn built-in source.
 *
 * Listings come from the Aylo tube pages (`/most_viewed/`, `/best/`, `/recent/`); search from
 * `/search/?query=`. Streams prefer the site's own `media_definitions` JSON endpoint (the same
 * array embedded in the page: one HLS/MP4 entry per quality) and fall back to scanning the page
 * markup through [TubeMedia].
 */
object YouPornSource : AdultSource {

    override val id = "youporn"
    override val name = "YouPorn"
    private const val BASE = "https://www.youporn.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Most viewed" to "$BASE/most_viewed/",
            "Best" to "$BASE/best/",
            "Newest" to "$BASE/recent/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/search/?query=" + encode(query)))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories/")
        return adultRx("""<a[^>]*href="(/category/[^"/]+/)"[^>]*>([^<]{2,50})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.removePrefix("/category/").trim('/'), label, BASE + path)
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
            ?: "YouPorn video"
        val thumb = adultUnescapeJson(html.ogContent("og:image"))
        val duration = adultCollapse(
            html.capture("""<span class="[^"]*duration[^"]*">([^<]+)</span>""")
        )

        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = thumb,
                duration = duration,
            ),
            streams = resolveStreams(html, url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    /**
     * Streams for a watch page: the `media_definitions` endpoint first (a compact JSON array per
     * quality), then whatever the page itself embeds.
     */
    internal suspend fun resolveStreams(html: String, pageUrl: String): List<StreamSource> {
        val videoId = pageUrl.substringAfter("/watch/", "").substringBefore('/').takeIf { it.isNotBlank() }
        if (videoId != null) {
            val json = runCatching {
                AdultHttp.get("$BASE/api/video/media_definitions/$videoId/", referer = pageUrl)
            }.getOrNull()
            if (json != null) {
                val parsed = parseDefinitions(json, pageUrl)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return embeddedStreams(html, pageUrl)
    }

    /** Parse a `mediaDefinitions` JSON array into quality-labelled streams. */
    internal fun parseDefinitions(json: String, pageUrl: String?): List<StreamSource> =
        TubeMedia.definitions(json, this, pageUrl)

    /** Page-embedded fallbacks (`mp4File`, `hlsUrl`, bare `.mp4`/`.m3u8` URLs). */
    internal fun embeddedStreams(html: String, pageUrl: String?): List<StreamSource> =
        TubeMedia.streams(html, this, referer = pageUrl)

    /**
     * Parse a listing page: each card is an `a[href="/watch/{id}/{slug}/"]`; the title is the
     * anchor's own `title=`/text or the image `alt`, the thumbnail the nearby `data-src`/`src`.
     */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="(/watch/[^"#?]+)"[^>]*>""").findAll(html)) {
            val path = m.groupValues[1]
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2200))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(
                    window.capture("""class="[^"]*video-box-title[^"]*"[^>]*>([^<]{3,200})<""")
                )
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(path)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = BASE + path,
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""class="[^"]*video-duration[^"]*"[^>]*>([0-9:]{3,9})<""")
                        ?: window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun slugTitle(path: String): String =
        path.substringAfter("/watch/").substringBefore('/')
            .replace('-', ' ')
            .ifBlank { "YouPorn video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
