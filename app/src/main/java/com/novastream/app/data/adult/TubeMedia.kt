package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str

/**
 * Shared media extraction for the built-in adult scrapers.
 *
 * Tube and JAV sites bury their playable URLs in an assortment of shapes — a
 * `mediaDefinitions`/`xplayerSettings` JSON blob, a bare `sources` object, a
 * `var hlsUrl = '…'`, protocol-relative `<source src>` tags, or escaped JS strings. Rather than
 * re-deriving that guesswork in every source, each scraper hands its HTML here and gets back every
 * absolute MP4/HLS URL it can find, labelled by quality **only** when the markup makes it
 * unambiguous (URL suffix, a sibling `label`/`quality` key, or a nearby `720p` token).
 *
 * Anything that looks like an ad / placeholder file is dropped, and the result is ordered
 * highest-quality-first so the best mirror sits at the top of the stream list.
 */
internal object TubeMedia {

    /** Absolute `.mp4`/`.m3u8` URLs anywhere in [html] (absolute or protocol-relative), in order. */
    fun urls(html: String): List<String> = scan(html).map { it.first }

    /**
     * Streams for [html], tagged with the headers the player will actually send. Unlabelled
     * manifests become `HLS`, unlabelled progressive files become `Auto`.
     */
    fun streams(
        html: String,
        source: AdultSource,
        referer: String?,
        cookie: String? = null,
    ): List<StreamSource> {
        val headers = adultPlayHeaders(referer, cookie)
        return scan(html)
            .map { (url, label) ->
                val quality = label
                    ?: adultQualityFromUrl(url)
                    ?: if (url.contains(".m3u8", ignoreCase = true)) "HLS" else "Auto"
                StreamSource(
                    url = url,
                    quality = quality,
                    name = source.name,
                    title = quality,
                    addonId = source.id,
                    addonName = source.name,
                    headers = headers,
                )
            }
            .sortedByDescending { it.quality?.removeSuffix("p")?.toIntOrNull() ?: -1 }
    }

    /**
     * Streams from a `mediaDefinitions`-style JSON array (`videoUrl` + `quality`/`height`/`format`),
     * the shape shared by the Aylo-family tubes (Pornhub / YouPorn / RedTube). Missing/unknown
     * quality falls back to the URL suffix, else `Auto`.
     */
    fun definitions(json: String, source: AdultSource, pageUrl: String?): List<StreamSource> {
        val headers = adultPlayHeaders(pageUrl)
        val array = runCatching {
            val root = com.google.gson.JsonParser.parseString(json)
            when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject -> root.asJsonObject.get("mediaDefinitions")
                    ?.takeIf { it.isJsonArray }?.asJsonArray
                else -> null
            }
        }.getOrNull() ?: return emptyList()

        return array.mapNotNull { el ->
            val url = adultAbsoluteUrl(el.str("videoUrl")) ?: return@mapNotNull null
            val height = el.str("height")?.toIntOrNull()
                ?: el.str("quality")?.toIntOrNull()
                ?: adultQualityFromUrl(url)?.removeSuffix("p")?.toIntOrNull()
            val label = height?.let { "${it}p" } ?: "Auto"
            StreamSource(
                url = url,
                quality = label,
                name = source.name,
                title = label,
                addonId = source.id,
                addonName = source.name,
                headers = headers,
            )
        }.distinctBy { it.url }
            .sortedByDescending { it.quality?.removeSuffix("p")?.toIntOrNull() ?: -1 }
    }

    /**
     * First `<img>` thumbnail inside [window], preferring lazy `data-src`, resolving
     * protocol-relative (`//cdn/…`) URLs and un-escaping stopped entities.
     */
    fun thumb(window: String): String? =
        adultUnescape(window.capture("""<img[^>]*\sdata-src="(//[^"]+)"""))?.let { "https:$it" }
            ?: adultUnescape(window.capture("""<img[^>]*\ssrc="(//[^"]+)"""))?.let { "https:$it" }
            ?: adultUnescape(window.capture("""<img[^>]*\sdata-src="(https?://[^"]+)"""))
            ?: adultUnescape(window.capture("""<img[^>]*\ssrc="(https?://[^"]+)"""))

    /** `(url, quality)` pairs for every distinct, non-junk media URL in [html], in page order. */
    fun scan(html: String): List<Pair<String, String?>> {
        val out = ArrayList<Pair<String, String?>>()
        val seen = HashSet<String>()
        for (m in MEDIA_URL.findAll(html)) {
            val url = adultAbsoluteUrl(m.value) ?: continue
            if (isJunk(url) || !seen.add(url)) continue
            val context = html.substring(
                m.range.last + 1,
                (m.range.last + 80).coerceAtMost(html.length),
            )
            out.add(url to labelFor(url, context))
        }
        return out
    }

    /**
     * Quality for [url]: URL suffix (`…_720p.mp4`) beats a `/1080.mp4` path segment, which beats a
     * `"quality":"720p"` / `label: "1080"` right after the URL. Only the text immediately after the
     * URL is scanned — a wider window would steal the *neighbouring* entry's label.
     */
    private fun labelFor(url: String, contextAfter: String): String? =
        adultQualityFromUrl(url)
            ?: adultRx("""/(\d{3,4})\.(?:mp4|m3u8)""").find(url)?.groupValues?.get(1)?.plus("p")
            ?: contextAfter.capture("""(?:quality|label|res|resolution)["']?\s*[:=]\s*["']?(\d{3,4})p""")?.plus("p")
            ?: contextAfter.capture("""["']?(\d{3,4})p["']?""")

    /** Ad / placeholder files that must never be offered as a playable stream. */
    private fun isJunk(url: String): Boolean =
        JUNK.any { url.contains(it, ignoreCase = true) }

    /** Matches absolute **and** protocol-relative MP4/HLS URLs, stopping at escapes and quotes. */
    private val MEDIA_URL =
        adultRx("""(?:https?:)?//[^"'\s<>\\]+?\.(?:mp4|m3u8)(?:\?[^"'\s<>\\]*)?""")

    private val JUNK = listOf(
        "doubleclick",
        "googlesyndication",
        "adservice",
        "/ads/",
        "advert",
        "preview.mp4",
        "placeholder",
        "sample.mp4",
        "blank.mp4",
        "1x1.mp4",
    )
}
