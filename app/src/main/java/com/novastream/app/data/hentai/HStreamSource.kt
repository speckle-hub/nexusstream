package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * HStream (hstream.moe) built-in source.
 *
 * Listings link to `/hentai/{id}-{slug}`. HStream is Cloudflare-protected, so requests from many
 * networks are challenged; [AdultHttp] turns that into a clear [com.novastream.app.data.adult.AdultSourceException]
 * and the section simply notes this one source as unavailable. Streams are HLS/MP4 pulled from the
 * page (or its player frame).
 */
object HStreamSource : HentaiSource {

    override val id = "hstream"
    override val name = "HStream"
    private const val BASE = "https://hstream.moe"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/search?q=$q", referer = "$BASE/") }.getOrNull()
            ?: runCatching { AdultHttp.get("$BASE/search/$q", referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return parseCards(html)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = AdultHttp.get(episode.url, referer = "$BASE/")
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id) { href -> href.contains("/hentai/") }
}
