package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * HenVids (henvids.com) built-in source.
 *
 * A series-based hentai tube: every video page (`/hentai/{slug}`, e.g.
 * `/hentai/shoujo-ramune-7`) is already one episode, so search results need no
 * series expansion. Listings live under `/latest` and `/trending`, tag shelves
 * under `/tag/{slug}`; streams resolve from the video page (or its player
 * iframe) through [HentaiTubes.resolvedStreams].
 */
object HenVidsSource : HentaiSource {

    override val id = "henvids"
    override val name = "HenVids"
    private const val BASE = "https://henvids.com"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val candidates = listOf(
            "$BASE/search/$q/",
            "$BASE/search/?s=$q",
            "$BASE/search/?query=$q",
        )
        var html: String? = null
        for (url in candidates) {
            html = runCatching { AdultHttp.get(url, referer = "$BASE/") }.getOrNull()
            if (!html.isNullOrBlank()) break
        }
        return parseCards(html ?: return emptyList())
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id) { href ->
            // Episode pages are `/hentai/{slug}`; tag/brand/series shelves and the
            // search/listing pages themselves are navigation, not content.
            href.contains("/hentai/") && !href.contains("/tag/") &&
                !href.contains("/brand/") && !href.contains("/series")
        }
}
