package com.novastream.app.data.hentai

import com.novastream.app.data.model.StreamSource

/**
 * A modular built-in NSFW-anime stream source.
 *
 * The app already gets its metadata from AniList, so a hentai source only has to answer two
 * questions: *which episodes exist for this title* and *where does this episode actually play*.
 * That keeps the contract far smaller than [com.novastream.app.data.adult.AdultSource] (which has
 * to own catalog, detail and discovery as well) while keeping the same shape: adding or removing
 * a site is a one-line change in [HentaiSources].
 *
 * Every method may throw; [HentaiRepository] isolates failures per source so a dead site degrades
 * to a notice while the others still resolve.
 */
interface HentaiSource {

    /** Stable id, also used as the [StreamSource.addonId] of the streams it produces. */
    val id: String

    /** Human-readable name shown in the stream list. */
    val name: String

    /**
     * Episode-level results for [query], most relevant first.
     *
     * Sources that index *series* (rather than single episodes) expand the top matches into
     * episodes before returning, so callers never have to know which layout a site uses.
     */
    suspend fun search(query: String): List<HentaiEpisode>

    /** Playable streams for one episode page returned by [search]. */
    suspend fun streams(episode: HentaiEpisode): List<StreamSource>
}
