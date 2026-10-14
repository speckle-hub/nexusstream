package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * Ohentai (ohentai.org) built-in source.
 *
 * A classic PHP hentai tube: detail pages are `detail.php?vid={base64id}` cards
 * (title + `video_data/…` poster right on the card) and the tag shelves
 * (`tagsearch.php?tag={tag}`, with `&sort=` variants) double as the search
 * endpoint. Streams resolve from the detail page (direct `<source>` or its
 * player iframe) through [HentaiTubes.resolvedStreams].
 */
object OhentaiSource : HentaiSource {

    override val id = "ohentai"
    override val name = "Ohentai"
    private const val BASE = "https://ohentai.org"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = runCatching { AdultHttp.get("$BASE/tagsearch.php?tag=$q", referer = "$BASE/") }
            .getOrNull()
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
            href.contains("detail.php?vid=")
        }.map { ep ->
            // Card posters are site-relative (`video_data/…`); absolutise so the UI
            // and offline paths never see a bare relative URL.
            val poster = ep.poster
            if (poster != null && !poster.startsWith("http")) ep.copy(poster = HentaiTubes.absolutise(poster, BASE))
            else ep
        }
}
