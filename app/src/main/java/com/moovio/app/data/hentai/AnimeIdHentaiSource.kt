package com.moovio.app.data.hentai

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * AnimeIdHentai (animeidhentai.com) built-in source.
 *
 * A WordPress-style hentai index: search via `/?s={query}` with a `/search/{query}/` fallback
 * (and the `www.` mirror as a last resort, mirroring the HentaiHaven `.xxx`/`.org` approach).
 * Cards are generic post anchors, so the shared [HentaiTubes.parseCards] parser with the
 * site-chrome filter applies; streams resolve from the episode page (or its player iframe)
 * through [HentaiTubes.resolvedStreams].
 */
object AnimeIdHentaiSource : HentaiSource {
    override val experimental = true

    override val id = "animeidhentai"
    override val name = "AnimeIdHentai"
    private const val BASE = "https://animeidhentai.com"
    private const val MIRROR = "https://www.animeidhentai.com"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val candidates = listOf(
            "$BASE/?s=$q",
            "$BASE/search/$q/",
            "$MIRROR/?s=$q",
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
        HentaiTubes.parseCards(html, BASE, id, HentaiTubes::isContentLink)
}
