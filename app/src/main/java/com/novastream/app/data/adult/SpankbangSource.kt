package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * SpankBang built-in source.
 *
 * SpankBang sits behind Cloudflare, so requests from many networks are challenged (403). The
 * scraper below handles the normal markup — when it is reachable, `window.initials` carries the
 * `stream_url`/`stream_url_hd` values, and the listing uses `div.video-item` cards. When Cloudflare
 * blocks us, [AdultHttp] raises a clear [AdultSourceException] and the Real 18+ tab simply notes
 * that this one source is unavailable.
 */
object SpankbangSource : AdultSource {

    override val id = "spankbang"
    override val name = "SpankBang"
    private const val BASE = "https://spankbang.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Trending" to "$BASE/trending_videos/",
            "Popular" to "$BASE/new_videos/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/s/" + java.net.URLEncoder.encode(query, "UTF-8")))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories/")
        return adultRx("""<a[^>]*href="(/[a-z0-9\-]+/)"[^>]*>([^<]{2,40})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null else AdultCategory(path.trim('/'), label, BASE + path)
            }
            .distinctBy { it.id }
            .take(20)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(
            html.capture("""["']title["']\s*:\s*["']([^"']{2,140})["']""")
                ?: html.capture("""<title>([^<|]*)""")
        )?.trim().orEmpty().ifBlank { "SpankBang video" }
        val thumb = adultUnescapeJson(html.capture("""["']thumbnail["']\s*:\s*["'](https?://[^"']+)["']"""))
        val uploader = adultUnescape(html.capture("""["']uploader["']\s*:\s*["']([^"']+)["']"""))
        val duration = html.capture("""["']duration["']\s*:\s*["']([0-9:]{3,9})["']""")

        val streams = LinkedHashSet<String>().apply {
            listOf("stream_url_hd", "stream_url").forEach { key ->
                html.capture("""["']$key["']\s*:\s*["'](https?://[^"']+)["']""")?.let { add(it) }
            }
        }.map { u ->
            val label = adultQualityFromUrl(u) ?: "Auto"
            StreamSource(
                url = u,
                quality = label,
                name = name,
                title = label,
                addonId = id,
                addonName = name,
                headers = adultPlayHeaders(url),
            )
        }

        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = thumb,
                duration = duration,
                uploader = uploader,
            ),
            uploader = uploader,
            streams = streams,
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a[^>]*href="(/[a-z0-9]+/video)""").findAll(html)) {
            val path = m.groupValues[1]
            val window = html.substring(maxOf(0, m.range.first - 400), minOf(html.length, m.range.last + 900))
            val title = adultUnescape(
                window.capture("""title="([^"]{3,160})""")
                    ?: window.capture("""alt="([^"]{3,160})""")
            ) ?: continue
            if (title.isBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = BASE + path,
                    title = title,
                    thumbnail = window.capture("""data-src="([^"]+)""")
                        ?: window.capture("""src="(https?://[^"]+)"""),
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }
}
