package com.novastream.app.data.adult

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str

/**
 * Pornhub built-in source.
 *
 * Listings come from the video pages (`/video`, `/video?o=mv`, `/video?o=tr`); detail + streams from
 * `view_video.php`, where the player embeds a `mediaDefinitions` JSON array containing an HLS
 * master playlist per quality.
 *
 * Two things about the current markup are easy to get wrong and both were reported as bugs:
 * the card title lives in a `<div class="title">` (the only `title=` attribute nearby belongs to
 * the "Content Partner" badge, i.e. `data-title=`), and the thumbnail is a plain `<img src>`
 * rather than the retired `data-image` / `data-mediumthumb` attributes.
 */
object PornhubSource : AdultSource {

    override val id = "pornhub"
    override val name = "Pornhub"
    private const val BASE = "https://www.pornhub.com"

    /** Cookie that lets phncdn serve signed playlists — without it `hm-h` answers 410/412. */
    private const val AGE_COOKIE = "accessAgeDisclaimerPH=1; age_verified=1"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Popular" to "$BASE/video?o=mv",
            "Trending" to "$BASE/video?o=tr",
            "Latest" to "$BASE/video",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/video/search?search=" + java.net.URLEncoder.encode(query, "UTF-8")))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories")
        return adultRx("""<a[^>]*href="(/video\?c=\d+)"[^>]*>([^<]{2,60})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val url = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(url.substringAfter("c="), label, "$BASE$url")
            }
            .distinctBy { it.id }
            .take(24)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescapeJson(html.capture(RE_TITLE))
            ?: adultUnescape(html.ogContent("og:title"))
            ?: html.capture("""<title>([^<|]*)""")
            ?: adultUnescape(html.capture("""<h1[^>]*>(.*?)</h1>"""))
        val thumb = adultUnescapeJson(html.capture(RE_IMAGE))
            ?: adultUnescapeJson(html.ogContent("og:image"))
        val seconds = html.capture(RE_DURATION)?.toLongOrNull()
        val uploader = adultUnescape(html.capture(RE_AUTHOR))
            ?: adultUnescape(html.capture("""class="uploaderLink"[^>]*>\s*([^<]{1,80})\s*</a>"""))
        val views = adultUnescape(html.capture(RE_VIEWS))
            ?: adultUnescape(html.capture("""<div class="views">([^<]+)</div>"""))

        val streams = parseStreams(html, pageUrl = url)
        // Related videos reuse the same list markup further down the page; drop the current one.
        val related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
            .getOrDefault(emptyList())

        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = adultCollapse(title) ?: "Pornhub video",
                thumbnail = thumb,
                duration = seconds?.let { adultDuration(it) },
                views = views,
                uploader = uploader,
            ),
            uploader = uploader,
            streams = streams,
            related = related,
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    /** Parse a video listing page into cards. */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (chunk in html.split("data-video-vkey=\"").drop(1)) {
            val vkey = chunk.substringBefore('"')
            if (vkey.isBlank()) continue
            val window = chunk.take(4000)
            val title = parseTitle(window) ?: continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = "$BASE/view_video.php?viewkey=$vkey",
                    title = title,
                    thumbnail = parseThumb(window),
                    duration = window.capture("""<var class="duration">([^<]+)</var>""")
                        ?: window.capture("""<span class="time">([\d:]+)</span>"""),
                    views = adultCollapse(
                        window.capture("""<span class="views">\s*<var>([^<]+)</var>""")
                            ?: window.capture("""<div class="views">([^<]+)</div>""")
                    ),
                    uploader = adultUnescape(
                        window.capture("""class="uploaderLink"[^>]*>\s*([^<]{1,80})\s*</a>""")
                    ),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    /**
     * Card title, in order of reliability: the text of the `.title` link (current markup), the
     * `title=` attribute of the viewkey anchor (legacy markup), the thumbnail `alt`, then any bare
     * `title=` attribute. The lookbehind on the last two patterns matters — `data-title="Content
     * Partner"` sits on the card and is what previously leaked through as the video name.
     */
    private fun parseTitle(window: String): String? {
        val fromTitleBlock = adultCollapse(
            window.capture("""<div class="title">\s*<a[^>]*>(.*?)</a>""")?.let { stripTags(it) }
        )
        if (fromTitleBlock != null) return fromTitleBlock

        val fromAnchor = adultUnescape(
            window.capture("""href="[^"]*view_video\.php\?viewkey=[^"]*"[^>]*title="([^"]+)""")
        )
        if (!fromAnchor.isNullOrBlank()) return adultCollapse(fromAnchor)

        val fromAlt = adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,})"""))
        if (!fromAlt.isNullOrBlank()) return adultCollapse(fromAlt)

        val fromBare = adultUnescape(window.capture("""(?<![-\w])title="([^"]{3,})"""))
        if (!fromBare.isNullOrBlank()) return adultCollapse(fromBare)
        return null
    }

    private fun parseThumb(window: String): String? =
        adultUnescapeJson(window.capture("""data-image="([^"]+)"""))
            ?: adultUnescapeJson(window.capture("""data-mediumthumb="([^"]+)"""))
            ?: window.capture("""<img[^>]*\ssrc="(https?://[^"]+)""")
            ?: adultUnescapeJson(window.capture("""data-poster="([^"]+)"""))
            ?: adultUnescapeJson(window.capture("""data-path="([^"]+)"""))

    /**
     * Extract HLS/MP4 streams from the embedded `mediaDefinitions` array. The array is parsed as
     * JSON (a regex cannot cope with `quality` sometimes being a string, sometimes `[]`, or with
     * `videoUrl` preceding/following `quality`), and every URL is tagged with the headers the
     * player needs to actually fetch it.
     */
    internal fun parseStreams(html: String, pageUrl: String? = null): List<StreamSource> {
        val headers = adultPlayHeaders(pageUrl, AGE_COOKIE)
        val byQuality = LinkedHashMap<String, StreamSource>()

        for (element in mediaDefinitions(html)) {
            val url = adultAbsoluteUrl(element.str("videoUrl")) ?: continue
            // `get_media` is a JSON endpoint (answers `{}`), not a media file — never list it.
            if (url.contains("/video/get_media")) continue
            val height = element.str("height")?.toIntOrNull()
                ?: element.str("quality")?.takeIf { q -> q.isNotBlank() && q.all { it.isDigit() } }?.toIntOrNull()
                ?: adultQualityFromUrl(url)?.removeSuffix("p")?.toIntOrNull()
            val label = height?.let { "${it}p" } ?: "Auto"
            if (byQuality.containsKey(label)) continue
            byQuality[label] = StreamSource(
                url = url,
                quality = label,
                name = name,
                title = label,
                addonId = id,
                addonName = name,
                headers = headers,
            )
        }

        if (byQuality.isEmpty()) legacyStreams(html, headers, byQuality)

        return byQuality.values.sortedByDescending {
            it.quality?.removeSuffix("p")?.toIntOrNull() ?: -1
        }
    }

    /** Regex fallback for pages whose `mediaDefinitions` cannot be parsed as JSON. */
    private fun legacyStreams(
        html: String,
        headers: Map<String, String>,
        into: LinkedHashMap<String, StreamSource>,
    ) {
        val start = html.indexOf("mediaDefinitions")
        if (start < 0) return
        val segment = html.substring(start, minOf(html.length, start + 40_000))
        for (m in MEDIA_RE.findAll(segment)) {
            val streamUrl = adultAbsoluteUrl(m.groupValues[1]) ?: continue
            if (streamUrl.contains("/video/get_media")) continue
            val quality = m.groupValues[2]
            if (quality.isBlank()) continue
            val label = "${quality}p"
            if (into.containsKey(label)) continue
            into[label] = StreamSource(
                url = streamUrl,
                quality = label,
                name = name,
                title = label,
                addonId = id,
                addonName = name,
                headers = headers,
            )
        }
    }

    /** The `mediaDefinitions` JSON array, extracted with bracket matching so strings are safe. */
    private fun mediaDefinitions(html: String): List<JsonElement> {
        val marker = html.indexOf("\"mediaDefinitions\"")
        if (marker < 0) return emptyList()
        val open = html.indexOf('[', marker)
        if (open < 0) return emptyList()
        var depth = 0
        var inString = false
        var escaped = false
        for (i in open until html.length) {
            val ch = html[i]
            if (escaped) { escaped = false; continue }
            if (ch == '\\') { escaped = true; continue }
            if (ch == '"') { inString = !inString; continue }
            if (inString) continue
            if (ch == '[') depth++
            if (ch == ']') {
                depth--
                if (depth == 0) {
                    val json = html.substring(open, i + 1)
                    return runCatching { JsonParser.parseString(json).asJsonArray.toList() }
                        .getOrDefault(emptyList())
                }
            }
        }
        return emptyList()
    }

    // Escaped strings so leading/trailing quotes are safe in the pattern.
    private const val RE_TITLE = "\"video_title\":\"(.*?)\""
    private const val RE_IMAGE = "\"image_url\":\"(.*?)\""
    private const val RE_DURATION = "\"video_duration\":(\\d+)"
    private const val RE_AUTHOR = "\"author\":\\s*\"([^\"]*)\""
    private const val RE_VIEWS = """<span class="views">\s*<var>([^<]+)</var>"""
    private val MEDIA_RE = Regex("\"videoUrl\":\"(.*?)\".{0,240}?\"quality\":\"(\\d+)\"", ADULT_DOT)
}
