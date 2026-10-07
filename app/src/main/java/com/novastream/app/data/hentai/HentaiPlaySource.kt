package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultPlayHeaders
import com.novastream.app.data.adult.adultQualityFromUrl
import com.novastream.app.data.adult.adultRx
import com.novastream.app.data.adult.adultUnescape
import com.novastream.app.data.adult.capture
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * HentaiPlay built-in source — an episode-per-page layout, which is the cheapest possible
 * resolution path: one search, one page fetch, and the page already carries a direct MP4
 * `<source>` (verified live: `206 video/mp4`, valid `ftyp` box, no Referer required).
 *
 * Its WordPress search is punctuation-sensitive (`:` and `!` in a query return nothing), so
 * [com.novastream.app.data.hentai.HentaiRepository] always retries with a punctuation-stripped
 * variant of the title.
 */
object HentaiPlaySource : HentaiSource {

    override val id = "hentaiplay"
    override val name = "HentaiPlay"

    private const val BASE = "https://hentaiplay.net"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val url = "$BASE/search/" + URLEncoder.encode(query.trim(), "UTF-8")
        val html = AdultHttp.get(url, referer = "$BASE/")
        return parseResults(html)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val html = AdultHttp.get(episode.url, referer = "$BASE/")
        return directMediaUrls(html)
            .map { url ->
                StreamSource(
                    url = url,
                    name = name,
                    addonId = id,
                    addonName = name,
                    quality = adultQualityFromUrl(url),
                    headers = adultPlayHeaders("$BASE/"),
                )
            }
    }

    // ---- Parsing (internal so the tests can pin the contract without a network call) ----

    /**
     * Splits the result page on `<div id="post-` so each chunk holds exactly one card, then
     * reads the permalink, the title and the thumbnail out of it.
     */
    internal fun parseResults(html: String): List<HentaiEpisode> =
        html.split("""<div id="post-""").drop(1).mapNotNull { block ->
            val url = block.capture("""entry-title"><a href="([^"]+)"""") ?: return@mapNotNull null
            val title = adultUnescape(block.capture("""entry-title"><a[^>]*>([^<]+)</a>"""))
                ?: return@mapNotNull null
            HentaiEpisode(
                sourceId = id,
                series = seriesOf(title),
                title = title,
                url = url,
                episode = episodeOf(title),
                poster = block.capture("""<img[^>]*src="([^"]+)"""),
            )
        }

    /**
     * Series name for grouping: the title with the episode number and everything after it
     * removed (`… Animation Episode 1 English` -> `… Animation`). The `\b` keeps the match off
     * words such as "Step".
     */
    internal fun seriesOf(title: String): String =
        title.replace(Regex("""\bep(?:isode)?\.?\s*\d+.*$""", RegexOption.IGNORE_CASE), " ").trim()
            .ifBlank { title.trim() }

    internal fun episodeOf(title: String): Int? =
        Regex("""\bep(?:isode)?\.?\s*(\d+)\b""", RegexOption.IGNORE_CASE).find(title)
            ?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Direct media URLs embedded in a page — both the `<source src="…">` tag this site uses and
     * any escaped JS blob. A URL must *stop* at the extension, so preview images named
     * `… .mp4_snapshot_… .webp` can never be mistaken for a stream.
     */
    internal fun directMediaUrls(html: String): List<String> =
        adultRx("""https?://[^"'\s<>\\]+?(?:\.mp4|\.m3u8)(?=[?#"'<>\s]|$)""")
            .findAll(html.replace("\\/", "/"))
            .mapNotNull { adultUnescape(it.value) }
            .distinct()
            .toList()
}
