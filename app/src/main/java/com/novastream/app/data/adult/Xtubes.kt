package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * Shared scraping helpers for the X-style tube sites (XVideos, XNXX).
 *
 * Both use the same result markup — `<div id="video_<eid>" class="thumb-block">` containing an
 * `<a href="/video.<eid>/<slug>">` and a `.thumb-under` block — and both expose direct MP4 + HLS
 * URLs on the detail page via `html5player.setVideo…`.
 */
internal object Xtubes {

    fun parseList(html: String, base: String, sourceId: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (chunk in html.split("<div id=\"video_").drop(1)) {
            val window = chunk.take(2800)
            val path = window.capture("""href="(/video[.\-][^"]*)""") ?: continue
            val title = adultUnescape(
                window.capture("""<p class="title"><a[^>]*>(.*?)<span""")
                    ?: window.capture("""<p class="title"><a[^>]*>(.*?)</a>""")
                    ?: window.capture("""href="/video[.\-][^"]*"[^>]*>([^<]{3,})</a>""")
            ) ?: continue
            if (title.isBlank()) continue
            val metadata = stripTags(window.capture("""<p class="metadata">(.*?)</p>""").orEmpty())
            out.add(
                AdultVideo(
                    sourceId = sourceId,
                    url = base + path,
                    title = title,
                    thumbnail = window.capture("""data-src="([^"]+)"""),
                    duration = window.capture("""<span class="duration">([^<]+)</span>"""),
                    views = metadata.capture("""([\d.,]+\s*[kmKM]?)\s*Views""")?.replace(" ", ""),
                    quality = window.capture("""<span class="video-hd-mark">([^<]+)</span>"""),
                    uploader = window.capture("""<span class="name">([^<]+)</span>"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    fun parseStreams(
        html: String,
        sourceId: String,
        sourceName: String,
        pageUrl: String? = null,
    ): List<StreamSource> {
        val headers = adultPlayHeaders(pageUrl)
        val out = ArrayList<StreamSource>()
        fun add(pattern: String, fallbackLabel: String) {
            val url = html.capture(pattern) ?: return
            if (url.isBlank()) return
            val label = adultQualityFromUrl(url) ?: fallbackLabel
            out.add(
                StreamSource(
                    url = url,
                    quality = label,
                    name = sourceName,
                    title = label,
                    addonId = sourceId,
                    addonName = sourceName,
                    headers = headers,
                )
            )
        }
        add("""html5player\.setVideoHLS\('([^']*)'\)""", "HLS")
        add("""html5player\.setVideoUrlHigh\('([^']*)'\)""", "High")
        add("""html5player\.setVideoUrlLow\('([^']*)'\)""", "Low")
        return out.distinctBy { it.url }
    }
}
