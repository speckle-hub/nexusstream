package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultAbsoluteUrl
import com.novastream.app.data.adult.adultCollapse
import com.novastream.app.data.adult.adultPlayHeaders
import com.novastream.app.data.adult.adultQualityFromUrl
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.adult.stripTags
import com.novastream.app.data.model.StreamSource

/**
 * Shared scraping helpers for the built-in hentai sources.
 *
 * The new sources are mostly WordPress-style episode/page sites with a direct `<source>` or an
 * embedded player iframe, so the same three pieces are needed everywhere: a direct-media-URL
 * extractor, a listing parser, and a one/two-hop embed resolver. Keeping them here means each
 * site is a small object that only supplies its URLs and card filter.
 */
internal object HentaiTubes {

    /**
     * Direct media URLs embedded in a page (absolute or escaped JS strings). A URL must *stop* at
     * the extension, so preview images named `… .mp4_snapshot_… .webp` can never be mistaken for a
     * stream.
     */
    fun mediaUrls(html: String): List<String> {
        val unescaped = html.replace("\\/", "/")
        val byExtension = adultRx("""https?://[^"'\s<>\\]+?(?:\.mp4|\.m3u8)(?=[?#"'<>\s]|$)""")
            .findAll(unescaped)
            .mapNotNull { adultUnescape(it.value) }
        // Some hosts (e.g. Haho's filegasm embeds) serve extension-less URLs and declare the
        // container in a `<source type="video/…">` attribute instead, so read those tags too.
        // Only video tracks qualify, so a `<source type="text/vtt">` subtitle can't sneak in.
        val bySourceTag = adultRx("""<source\b[^>]*>""").findAll(unescaped)
            .map { it.value }
            .filter { SOURCE_VIDEO_TYPE.containsMatchIn(it) }
            .mapNotNull { adultUnescape(it.capture("""\ssrc="([^"]+)"""")) }
        return (byExtension + bySourceTag).distinct().toList()
    }

    fun toStreams(html: String, source: HentaiSource, referer: String): List<StreamSource> =
        mediaUrls(html).map { stream(it, source, referer) }

    fun stream(url: String, source: HentaiSource, referer: String): StreamSource = StreamSource(
        url = url,
        name = source.name,
        addonId = source.id,
        addonName = source.name,
        quality = qualityOf(url),
        headers = adultPlayHeaders(referer),
    )

    /** Quality from the URL: `…_720p.mp4` first, then a `/1080.mp4` path segment, else null. */
    fun qualityOf(url: String): String? =
        adultQualityFromUrl(url)
            ?: Regex("""/(\d{3,4})\.(?:mp4|m3u8)""").find(url)?.groupValues?.get(1)?.plus("p")

    /**
     * Resolve streams for an episode page: the page's own media URLs first, then one level of
     * embedded player iframe, then a second (some players nest a frame).
     */
    suspend fun resolvedStreams(
        pageHtml: String,
        source: HentaiSource,
        pageUrl: String,
    ): List<StreamSource> {
        val hostRoot = rootOf(pageUrl).ifBlank { pageUrl }
        toStreams(pageHtml, source, hostRoot).takeIf { it.isNotEmpty() }?.let { return it }

        val iframe = pageHtml.capture("""<iframe[^>]*\ssrc="([^"]+)"""")
            ?.let { absolutise(it, hostRoot) }
            ?: return emptyList()
        val embed = runCatching { AdultHttp.get(iframe, referer = pageUrl) }.getOrNull()
            ?: return emptyList()
        toStreams(embed, source, iframe).takeIf { it.isNotEmpty() }?.let { return it }

        val nested = embed.capture("""<iframe[^>]*\ssrc="([^"]+)"""")
            ?.let { absolutise(it, rootOf(iframe).ifBlank { iframe }) }
            ?: return emptyList()
        val inner = runCatching { AdultHttp.get(nested, referer = iframe) }.getOrNull()
            ?: return emptyList()
        return toStreams(inner, source, nested)
    }

    fun thumb(window: String): String? =
        adultUnescape(window.capture("""<img[^>]*\s(?:data-src|data-lazy-src|src)="([^"]+)""""))
            ?.let { adultAbsoluteUrl(it) ?: it }

    fun absolutise(url: String, host: String): String = when {
        url.startsWith("http") -> adultAbsoluteUrl(url) ?: url
        url.startsWith("//") -> adultAbsoluteUrl(url) ?: "https:$url"
        else -> host.trimEnd('/') + if (url.startsWith("/")) url else "/$url"
    }

    fun rootOf(url: String): String =
        Regex("""^https?://[^/]+""").find(url)?.value.orEmpty()

    /** Episode number from a title like "… Episode 3", "Ep. 3" or "… - 03". */
    fun episodeOf(title: String): Int? =
        Regex("""\bep(?:isode)?\.?\s*(\d+)\b""", RegexOption.IGNORE_CASE).find(title)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""(?:[-–]\s*|\b)(\d{1,3})\s*$""").find(title)
                ?.groupValues?.get(1)?.toIntOrNull()

    /** Series name = title with the episode marker and everything after it stripped. */
    fun seriesOf(title: String): String =
        title.replace(Regex("""\bep(?:isode)?\.?\s*\d+.*$""", RegexOption.IGNORE_CASE), " ").trim()
            .ifBlank { title.trim() }

    /**
     * Generic listing parser for the WordPress-style sites.
     *
     * The page is split into post blocks (`<article>` or a post/video/thumb/card class) so menu
     * links can't masquerade as videos; inside each block the first anchor whose href passes
     * [match] is the card. The title is the anchor's own text, its `title=`/`alt` attribute, or a
     * nearby heading. When no blocks are found the whole document is treated as one block.
     */
    fun parseCards(
        html: String,
        host: String,
        sourceId: String,
        match: (String) -> Boolean,
    ): List<HentaiEpisode> {
        val out = ArrayList<HentaiEpisode>()
        val seen = HashSet<String>()
        for (block in postBlocks(html)) {
            val card = cardFrom(block, host, sourceId, match) ?: continue
            if (seen.add(card.url)) out.add(card)
        }
        return out
    }

    /** True for a plausible content permalink (rules out social, query and site-chrome links). */
    fun isContentLink(href: String): Boolean =
        href.isNotBlank() &&
            !NAV.containsMatchIn(href) &&
            (href.startsWith("/") || href.startsWith("http"))

    private fun postBlocks(html: String): List<String> {
        for (pattern in BLOCK_PATTERNS) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(html)) {
                val parts = regex.split(html).drop(1)
                if (parts.size >= 2) return parts
            }
        }
        return listOf(html)
    }

    private fun cardFrom(
        block: String,
        host: String,
        sourceId: String,
        match: (String) -> Boolean,
    ): HentaiEpisode? {
        // `(.*?)</a>` (with DOT_MATCHES_ALL) so an anchor that wraps an `<img>` still matches;
        // the inner HTML is tag-stripped before it can be used as a title.
        for (m in adultRx("""<a\s[^>]*href="([^"]+)"[^>]*>(.*?)</a>""").findAll(block)) {
            val href = m.groupValues[1]
            if (!match(href)) continue
            val inner = adultCollapse(adultUnescape(stripTags(m.groupValues[2])))
            val window = block.substring(
                maxOf(0, m.range.first - 400),
                minOf(block.length, m.range.last + 900),
            )
            val title = inner?.takeIf { it.length >= 3 && !isJunk(it) }
                ?: adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: continue
            if (title.isBlank() || isJunk(title)) continue
            return HentaiEpisode(
                sourceId = sourceId,
                series = seriesOf(title),
                title = title,
                url = absolutise(href, host),
                episode = episodeOf(title),
                poster = thumb(window),
            )
        }
        return null
    }

    private fun isJunk(title: String): Boolean = title.trim().lowercase() in JUNK

    private val BLOCK_PATTERNS = listOf(
        """<article\b""",
        """<div[^>]*class="[^"]*\b(?:post|video-item|thumb-block|video-box|thumb|card|item)\b""",
    )

    private val NAV = Regex(
        """(?:#|mailto:|javascript:)|\?|/(?:category|tag|author|page|wp-|feed|comment|about|contact|dmca|privacy|terms|login|register|dmca|search)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val SOURCE_VIDEO_TYPE =
        Regex("""type\s*=\s*["']video/""", RegexOption.IGNORE_CASE)

    private val JUNK = setOf(
        "read more", "watch now", "watch", "play", "play now", "home", "next", "prev",
        "previous", "menu", "more", "view all", "comments", "leave a comment", "login",
        "register", "dmca", "about", "contact",
    )
}
