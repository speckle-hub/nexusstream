package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * HentaiHaven built-in source.
 *
 * A WordPress site: search via `/?s={query}`, post cards parsed into episodes, and streams taken
 * from the page's own `<source>`/`.mp4` or its embedded player (one/two iframe hops). Primary host
 * first, then the older `.org` mirror.
 */
object HentaiHavenSource : HentaiSource {

    override val id = "hentaihaven"
    override val name = "HentaiHaven"

    /** Primary host first, then the legacy mirror. */
    private val hosts = listOf("https://hentaihaven.xxx", "https://hentaihaven.org")

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        for (host in hosts) {
            val html = runCatching { AdultHttp.get("$host/?s=$q", referer = "$host/") }.getOrNull() ?: continue
            val cards = parseCards(html, host)
            if (cards.isNotEmpty()) return cards
        }
        return emptyList()
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val referer = HentaiTubes.rootOf(episode.url).let { if (it.isBlank()) episode.url else "$it/" }
        val page = AdultHttp.get(episode.url, referer = referer)
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String, host: String = hosts.first()): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, host, id, HentaiTubes::isContentLink)
}
