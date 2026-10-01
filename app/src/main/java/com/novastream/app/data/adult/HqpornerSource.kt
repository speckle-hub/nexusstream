package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * HQporner built-in source.
 *
 * Listings are straightforward (`/`, `/top`, `/top/week`, `/top/month`, `?q=`). Three traps:
 *
 *  - the card's own link text is often the literal **"play images"** (a slideshow trigger), while
 *    the real title sits in the following `<h2>`;
 *  - the detail page's *first* `<iframe>` is an ad — the player lives in a `mydaddy.cc/video/`
 *    frame further down;
 *  - the embed publishes **protocol-relative** MP4 URLs inside escaped JS strings (`\"//host/x.mp4\"`).
 */
object HqpornerSource : AdultSource {

    override val id = "hqporner"
    override val name = "HQporner"
    private const val BASE = "https://hqporner.com"

    /** Link text that is UI chrome, never a video title. */
    private val JUNK_TITLES = setOf("play images", "loading...", "playing...", "play", "watch")

    private val AD_HOSTS = listOf(
        "adtng.com", "doubleclick.net", "googlesyndication", "smartpop",
        "mnaspm", "mayzaent", "/hf.html", "adsystem", "popads",
    )

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/",
            "Top" to "$BASE/top",
            "Top this week" to "$BASE/top/week",
            "Top this month" to "$BASE/top/month",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/?q=" + java.net.URLEncoder.encode(query, "UTF-8")))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories")
        return adultRx("""<a[^>]*href="(/category/[^"]+)"[^>]*>([^<]{2,60})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.removePrefix("/category/"), label, BASE + path)
            }
            .distinctBy { it.id }
            .take(24)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultCollapse(
            html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) }
                ?: html.ogContent("og:title")
                ?: html.capture("""<title>([^<|]*)""")
        ) ?: "HQporner video"
        val thumb = firstMainThumb(html) ?: adultUnescapeJson(html.ogContent("og:image"))

        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = thumb,
                duration = parseDuration(html),
            ),
            streams = resolveStreams(html, pageUrl = url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    /**
     * Parse a listing page.
     *
     * Each card is one `<a href="/hdporn/…">`; a window is cut from that anchor to the next
     * anchor *pointing at a different video* (so a card that has both an image link and a
     * separate title link still reads as one card) and capped at 4 kB. Inside that window we
     * prefer the `<h2>`, then the anchor's own text, then a `click-trigger` link, then the image
     * `alt`, and finally the URL slug.
     */
    internal fun parseList(html: String): List<AdultVideo> {
        val anchors = adultRx("""<a[^>]*href="(/hdporn/[^"]+\.html)"""")
            .findAll(html)
            .toList()
        val out = ArrayList<AdultVideo>()
        for ((index, anchor) in anchors.withIndex()) {
            val path = anchor.groupValues[1]
            val start = anchor.range.first
            val nextDifferent = anchors
                .getOrNull(index + 1)
                ?.takeIf { it.groupValues[1] != path }
                ?.range?.first
            val end = minOf(start + 4000, nextDifferent ?: (start + 4000))
            val window = html.substring(start, minOf(end, html.length))
            val ownText = adultCollapse(
                adultRx("""<a[^>]*href="[^"]*"[^>]*>(.*?)</a>""")
                    .find(window)?.groupValues?.get(1)?.let { stripTags(it) }
            )?.takeIf { !isJunk(it) }

            val title = adultCollapse(
                window.capture("""<h2[^>]*>(.*?)</h2>""")?.let { stripTags(it) }
            )?.takeIf { !isJunk(it) }
                ?: ownText
                ?: adultCollapse(
                    window.capture("""class="click-trigger"[^>]*>(.*?)</a>""")?.let { stripTags(it) }
                )?.takeIf { !isJunk(it) }
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,})"""))
                    ?.takeIf { !isJunk(it) }
                ?: slugTitle(path)

            out.add(
                AdultVideo(
                    sourceId = id,
                    url = BASE + path,
                    title = title,
                    thumbnail = window.capture(MAIN_THUMB)?.let { "https:$it" },
                    duration = parseDuration(window),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun isJunk(title: String): Boolean = title.lowercase() in JUNK_TITLES

    private fun slugTitle(path: String): String =
        path.substringAfter("/hdporn/").substringBefore(".html")
            .substringAfterLast('-').replace('_', ' ')
            .ifBlank { "HQporner video" }

    private fun parseDuration(html: String): String? =
        adultCollapse(
            html.capture("""fa-clock-o[^>]*></i>\s*([^<]+)""")
                ?: html.capture("""<span class="time">([\d:]+)</span>""")
        )

    private fun firstMainThumb(html: String): String? =
        html.capture(MAIN_THUMB)?.let { "https:$it" }

    /**
     * Direct streams on the page, else follow the **player** iframe (never an ad frame) and look
     * there. The embed host is flaky, so one silent failure is retried before giving up.
     */
    internal suspend fun resolveStreams(html: String, pageUrl: String? = null): List<StreamSource> {
        directStreams(html, referer = pageUrl)?.takeIf { it.isNotEmpty() }?.let { return it }

        val iframe = playerIframe(html) ?: return emptyList()
        val embedUrl = adultAbsoluteUrl(iframe) ?: return emptyList()
        repeat(2) {
            val embed = runCatching {
                AdultHttp.get(embedUrl, referer = pageUrl ?: BASE)
            }.getOrNull() ?: return@repeat
            directStreams(embed, referer = embedUrl)?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    /** The video frame — `mydaddy.cc/video/…` first, else the first non-ad `<iframe>`. */
    internal fun playerIframe(html: String): String? {
        val sources = adultRx("""<iframe[^>]*\ssrc="([^"]+)""")
            .findAll(html)
            .map { it.groupValues[1] }
            .toList()
        return sources.firstOrNull { it.contains("mydaddy.cc/video/") }
            ?: sources.firstOrNull { src -> AD_HOSTS.none { src.contains(it, ignoreCase = true) } }
    }

    internal fun directStreams(html: String, referer: String?): List<StreamSource>? {
        val headers = adultPlayHeaders(referer)
        val urls = LinkedHashSet<String>()
        val labels = HashMap<String, String>()
        for (m in MEDIA_URL.findAll(html)) {
            val url = adultAbsoluteUrl(m.value) ?: continue
            if (AD_HOSTS.any { url.contains(it, ignoreCase = true) }) continue
            urls.add(url)
            // Quality may be in the URL (`_720p.mp4`), in a sibling `label:"720p"`, or as a bare
            // path segment (`/720.mp4`) — HQporner's embeds use all three shapes.
            // Only the text right after this URL — anything wider grabs the next entry's label.
            val ctxStart = m.range.last + 1
            val ctxEnd = (m.range.last + 60).coerceAtMost(html.length)
            val context = html.substring(ctxStart, ctxEnd)
            labelFor(url, context)?.let { labels[url] = it }
        }
        if (urls.isEmpty()) return null
        val streams = urls.map { url ->
            val label = labels[url]
                ?: if (url.contains(".m3u8")) "HLS" else "Auto"
            StreamSource(
                url = url,
                quality = label,
                name = name,
                title = label,
                addonId = id,
                addonName = name,
                headers = headers,
            )
        }
        return streams.sortedByDescending { it.quality?.removeSuffix("p")?.toIntOrNull() ?: -1 }
    }

    /**
     * `…_360p.mp4` → `360p`; `/1080.mp4` → `1080p`; a `label:"720p"` right after the URL → `720p`.
     * URL-derived labels win: scanning a wider window would pick up the *neighbouring* entry's
     * label and stamp it on this one.
     */
    private fun labelFor(url: String, contextAfter: String): String? =
        adultQualityFromUrl(url)
            ?: adultRx("""/(\d{3,4})\.(?:mp4|m3u8)""").find(url)?.groupValues?.get(1)?.plus("p")
            ?: contextAfter.capture("""label["']?\s*:\s*["']?(\d{3,4})p""")?.plus("p")

    private const val MAIN_THUMB = """(//[^"']*?_main\.jpg)"""

    /** Matches absolute **and** protocol-relative MP4/HLS URLs, stopping at escapes and quotes. */
    private val MEDIA_URL = adultRx("""(?:https?:)?//[^"'\s<>\\]+?\.(?:mp4|m3u8)(?:\?[^"'\s<>\\]*)?""")
}
