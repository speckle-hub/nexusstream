package com.novastream.app.data.sfw

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
 * Shared embed/stream extraction for the built-in SFW sources.
 *
 * Movie/TV/anime aggregators rarely host video themselves: a title page links an
 * episode or server page, which embeds a player iframe (sometimes nested), whose
 * document finally carries the media URL — a direct `.mp4`/`.m3u8`, a
 * `"file": "…"` / `"sources": […]` JSON blob, or a Dean-Edwards-packed script.
 * Every helper here is pure except the small fetch hops in [resolveChain], so the
 * parsing is fixture-pinned by unit tests and each site object only supplies its
 * URLs and card filters.
 */
internal object SfwEmbeds {

    /**
     * Direct media URLs embedded in a page: absolute http(s) URLs ending in a media
     * extension (stops at the extension so thumbnails can't sneak in), `<source>`
     * video tags, `"file"`/`"source"` JSON string fields, and `"sources"` arrays
     * with `file`+`label` entries.
     */
    fun mediaUrls(html: String): List<String> {
        val unescaped = html.replace("\\/", "/")
        val direct = adultRx("""https?://[^"'\s<>\\]+?(?:\.mp4|\.m3u8|\.mkv|\.webm)(?=[?#"'<>\s]|$)""")
            .findAll(unescaped)
            .mapNotNull { adultUnescape(it.value) }
        val bySourceTag = adultRx("""<source\b[^>]*>""").findAll(unescaped)
            .map { it.value }
            .filter { SOURCE_VIDEO_TYPE.containsMatchIn(it) }
            .mapNotNull { adultUnescape(it.capture("""\ssrc="([^"]+)"""")) }
        val byFileField = adultRx(""""(?:file|source)"\s*:\s*"([^"]{8,2000})"""").findAll(unescaped)
            .mapNotNull { adultUnescape(it.groupValues[1]) }
            .filter { looksLikeMedia(it) }
        return (direct + bySourceTag + byFileField).distinct().toList()
    }

    /** Quality/source-label pairs from `"sources": [{file, label}]` player blobs. */
    fun labeledSources(html: String): List<Pair<String, String?>> {
        val unescaped = html.replace("\\/", "/")
        return adultRx("""\{\s*"file"\s*:\s*"([^"]{8,2000})"\s*(?:,\s*"label"\s*:\s*"([^"]{1,12})")?""")
            .findAll(unescaped)
            .mapNotNull { m ->
                val url = adultUnescape(m.groupValues[1])?.takeIf { looksLikeMedia(it) } ?: return@mapNotNull null
                url to m.groupValues[2].takeIf { it.isNotBlank() }
            }
            .distinctBy { it.first }
            .toList()
    }

    /** All player-iframe sources on a page, absolutised against [host]. */
    fun iframeSrcs(html: String, host: String): List<String> =
        adultRx("""<iframe[^>]*\ssrc="([^"]+)"""").findAll(html)
            .map { absolutise(it.groupValues[1], host) }
            .filter { it.startsWith("http") }
            .distinct()
            .toList()

    fun toStreams(
        urls: List<String>,
        sourceId: String,
        sourceName: String,
        referer: String,
    ): List<StreamSource> = urls.distinct().map { url ->
        StreamSource(
            url = url,
            name = sourceName,
            addonId = sourceId,
            addonName = sourceName,
            quality = qualityOf(url),
            headers = adultPlayHeaders(referer),
        )
    }

    /** Quality from a URL (`…_1080p.mp4`, `/720.mp4`) or a player label (`1080p`). */
    fun qualityOf(url: String): String? =
        adultQualityFromUrl(url)
            ?: Regex("""/(\d{3,4})\.(?:mp4|m3u8|mkv|webm)""").find(url)?.groupValues?.get(1)?.plus("p")

    /**
     * Normalise a site/player quality label into a [StreamRanking][com.novastream.app.data.repo.StreamRanking]-compatible
     * tier: `1080p`/`1080` → `1080p`, `HD` → `720p`, `SD` → `480p`, CAM/HDTS/TS kept
     * verbatim (unknown tiers sort below recognised ones, never crash ranking).
     */
    fun normalizeLabel(label: String?): String? {
        val raw = label?.trim().orEmpty()
        if (raw.isEmpty()) return null
        Regex("""(\d{3,4})\s*p?""").find(raw)?.let { return "${it.groupValues[1]}p" }
        return when (raw.uppercase()) {
            "HD", "HD720", "720" -> "720p"
            "SD", "480" -> "480p"
            "FULLHD", "FHD", "1080" -> "1080p"
            "4K", "UHD", "2160" -> "4K"
            else -> raw.uppercase().take(12)
        }
    }

    /**
     * Resolve streams for a page: the page's own media URLs first, then each
     * embedded player iframe (in order), then one nested level. Stops at the first
     * level that yields anything, so a working direct link never pays for hops.
     */
    suspend fun resolveChain(
        pageHtml: String,
        pageUrl: String,
        sourceId: String,
        sourceName: String,
        maxIframes: Int = 3,
    ): List<StreamSource> {
        val hostRoot = rootOf(pageUrl).ifBlank { pageUrl }
        // Packed scripts hide their URLs from the regexes — unpack first when present.
        val unpacked = jsUnpackAll(pageHtml)
        toStreams(mediaUrls(unpacked), sourceId, sourceName, hostRoot)
            .takeIf { it.isNotEmpty() }?.let { return it }
        labeledSources(unpacked)
            .takeIf { it.isNotEmpty() }
            ?.map { (url, label) ->
                StreamSource(
                    url = url,
                    name = sourceName,
                    addonId = sourceId,
                    addonName = sourceName,
                    quality = normalizeLabel(label) ?: qualityOf(url),
                    headers = adultPlayHeaders(hostRoot),
                )
            }?.let { return it }

        for (iframe in iframeSrcs(pageHtml, hostRoot).take(maxIframes)) {
            val embed = runCatching { AdultHttp.get(iframe, referer = pageUrl) }.getOrNull()
                ?: continue
            val embedUnpacked = jsUnpackAll(embed)
            toStreams(mediaUrls(embedUnpacked), sourceId, sourceName, iframe)
                .takeIf { it.isNotEmpty() }?.let { return it }
            labeledSources(embedUnpacked)
                .takeIf { it.isNotEmpty() }
                ?.map { (url, label) ->
                    StreamSource(
                        url = url,
                        name = sourceName,
                        addonId = sourceId,
                        addonName = sourceName,
                        quality = normalizeLabel(label) ?: qualityOf(url),
                        headers = adultPlayHeaders(iframe),
                    )
                }?.let { return it }
            val nested = iframeSrcs(embed, rootOf(iframe).ifBlank { iframe }).firstOrNull()
                ?: continue
            val inner = runCatching { AdultHttp.get(nested, referer = iframe) }.getOrNull()
                ?: continue
            toStreams(mediaUrls(jsUnpackAll(inner)), sourceId, sourceName, nested)
                .takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    fun absolutise(url: String, host: String): String = when {
        url.startsWith("http") -> adultAbsoluteUrl(url) ?: url
        url.startsWith("//") -> adultAbsoluteUrl(url) ?: "https:$url"
        else -> host.trimEnd('/') + if (url.startsWith("/")) url else "/$url"
    }

    fun rootOf(url: String): String =
        Regex("""^https?://[^/]+""").find(url)?.value.orEmpty()

    fun thumb(window: String): String? =
        com.novastream.app.data.hentai.HentaiTubes.thumb(window)

    /**
     * Unpacks Dean-Edwards-packed scripts (`eval(function(p,a,c,k,e,d){…})`) so the
     * media URLs hidden inside become visible to [mediaUrls]. Returns the input
     * unchanged when it carries no packed block. Pure — unit-tested.
     */
    fun jsUnpackAll(html: String): String {
        var out = html
        PACKED.findAll(html).forEach { m ->
            val unpacked = jsUnpack(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4])
            if (unpacked != null) out = out.replace(m.value, unpacked)
        }
        return out
    }

    private val PACKED =
        adultRx("""eval\(function\(p,a,c,k,e,d?\)\{.*?\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""")

    internal fun jsUnpack(payload: String, radixS: String, countS: String, symtabS: String): String? {
        val radix = radixS.toIntOrNull() ?: return null
        val count = countS.toIntOrNull() ?: return null
        if (radix !in 2..62 || count <= 0) return null
        val symtab = symtabS.split("|")
        if (symtab.size < count) return null
        return runCatching {
            val word = Regex("""\b\w+\b""")
            word.replace(payload) { m ->
                val token = m.value
                val index = unbase(token, radix)
                if (index in symtab.indices && symtab[index].isNotEmpty()) symtab[index] else token
            }
        }.getOrNull()
    }

    private fun unbase(s: String, radix: Int): Int {
        var value = 0
        for (ch in s) {
            val digit = when (ch) {
                in '0'..'9' -> ch - '0'
                in 'a'..'z' -> ch - 'a' + 10
                in 'A'..'Z' -> ch - 'A' + 36
                else -> return -1
            }
            if (digit >= radix) return -1
            value = value * radix + digit
        }
        return value
    }

    private fun looksLikeMedia(url: String): Boolean {
        val clean = url.substringBefore('?').substringBefore('#').lowercase()
        if (clean.endsWith(".mp4") || clean.endsWith(".m3u8") || clean.endsWith(".mkv") || clean.endsWith(".webm")) return true
        // Player JSON blobs often carry bare host paths without extensions — accept http(s)
        // file-ish URLs here; [AdultHttp.verifyPlayable][com.novastream.app.data.adult.AdultHttp.verifyPlayable]
        // drops the dead ones before Play lights up.
        return (url.startsWith("http://") || url.startsWith("https://")) &&
            !clean.endsWith(".js") && !clean.endsWith(".css") &&
            !clean.endsWith(".png") && !clean.endsWith(".jpg") &&
            !clean.endsWith(".jpeg") && !clean.endsWith(".webp") &&
            !clean.endsWith(".gif") && !clean.endsWith(".vtt") && !clean.endsWith(".srt")
    }

    private val SOURCE_VIDEO_TYPE =
        Regex("""type\s*=\s*["']video/""", RegexOption.IGNORE_CASE)

    /** Card title junk that is navigation chrome, never a title. */
    fun isJunk(title: String): Boolean = title.trim().lowercase() in JUNK

    private val JUNK = setOf(
        "read more", "watch now", "watch", "play", "play now", "home", "next", "prev",
        "previous", "menu", "more", "view all", "comments", "login", "register",
        "dmca", "about", "contact", "search", "trending", "latest", "popular",
    )

    /**
     * Generic card-list parser for the movie/TV/anime index pages: splits the page
     * into post blocks (`<article>` or card/item/thumb classes) so menu links can't
     * masquerade as titles, then takes each block's first anchor passing [match].
     */
    fun parseTitleCards(
        html: String,
        host: String,
        sourceId: String,
        kind: com.novastream.app.data.model.MediaType,
        match: (String) -> Boolean,
    ): List<SfwTitle> {
        val out = ArrayList<SfwTitle>()
        val seen = HashSet<String>()
        for (block in postBlocks(html)) {
            val card = cardFrom(block, host, sourceId, kind, match) ?: continue
            if (seen.add(card.url)) out.add(card)
        }
        return out
    }

    private fun cardFrom(
        block: String,
        host: String,
        sourceId: String,
        kind: com.novastream.app.data.model.MediaType,
        match: (String) -> Boolean,
    ): SfwTitle? {
        for (m in adultRx("""<a\s[^>]*href="([^"]+)"[^>]*>(.*?)</a>""").findAll(block)) {
            val href = m.groupValues[1]
            if (!match(href)) continue
            val inner = adultCollapse(adultUnescape(stripTags(m.groupValues[2])))
            val window = block.substring(
                maxOf(0, m.range.first - 400),
                minOf(block.length, m.range.last + 900),
            )
            val title = inner?.takeIf { it.length >= 2 && !isJunk(it) }
                ?: adultUnescape(m.value.capture("""\stitle="([^"]{2,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{2,200})""""))
                ?: continue
            if (title.isBlank() || isJunk(title)) continue
            val year = Regex("""\b(19\d{2}|20\d{2})\b""").find(window)?.groupValues?.get(1)
                ?: Regex("""\b(19\d{2}|20\d{2})\b""").find(title)?.groupValues?.get(1)
            return SfwTitle(
                sourceId = sourceId,
                url = absolutise(href, host),
                title = title,
                kind = kind,
                poster = thumb(window),
                year = year,
            )
        }
        return null
    }

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

    private val BLOCK_PATTERNS = listOf(
        """<article\b""",
        """<div[^>]*class="[^"]*\b(?:post|video-item|thumb-block|video-box|thumb|card|item|film|movie|serie|flw-item)\b""",
    )
}
