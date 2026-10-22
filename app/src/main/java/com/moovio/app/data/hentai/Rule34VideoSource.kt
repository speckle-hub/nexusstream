package com.moovio.app.data.hentai

import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * Rule34Video (rule34video.com) built-in source.
 *
 * A tube-style archive of rule34 / parody animation: search lives under `/search/{query}/`
 * (confirmed pattern: `/search/link/`), listings under `/latest-updates/`, and video pages
 * under `/video/{id}/{slug}/`. Cards are plain anchors to `/video/…`, so the shared
 * [HentaiTubes.parseCards] listing parser applies; streams resolve from the video page (or
 * its player iframe) through [HentaiTubes.resolvedStreams].
 */
object Rule34VideoSource : HentaiSource {

    override val id = "rule34video"
    override val name = "Rule34Video"
    private const val BASE = "https://rule34video.com"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/search/$q/", referer = "$BASE/") }.getOrNull()
            ?: runCatching { AdultHttp.get("$BASE/search/$q", referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return parseCards(html)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        val page = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseCards(html: String): List<HentaiEpisode> =
        HentaiTubes.parseCards(html, BASE, id) { href ->
            // Video pages are `/video/{id}/…`; the plural `/videos/…` (when present) is a
            // listing, and search/model/tag pages are navigation, not content.
            href.contains("/video/") && !href.contains("/videos/") &&
                !href.contains("/search/") && !href.contains("/models/") && !href.contains("/tags/")
        }
}
