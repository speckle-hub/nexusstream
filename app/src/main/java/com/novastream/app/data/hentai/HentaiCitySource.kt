package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * HentaiCity built-in source — a WordPress episode site (`/?s={query}` for search) with a direct
 * MP4/HLS on the page or an embedded player. Streams resolve through [HentaiTubes].
 */
object HentaiCitySource : HentaiSource {

    override val id = "hentaicity"
    override val name = "HentaiCity"
    private const val BASE = "https://hentaicity.com"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/?s=$q", referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return parseCards(html)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = AdultHttp.get(episode.url, referer = "$BASE/")
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id, HentaiTubes::isContentLink)
}
