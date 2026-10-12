package com.novastream.app.data.cloudstream

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.Video
import kotlinx.coroutines.withTimeout

/**
 * Adapts a loaded CloudStream [MainAPI] provider to the app's own model types.
 *
 * A provider search result becomes a [MediaItem] whose `addonId` is `cloudstream:<provider>` and
 * whose `id` is the provider's own page URL, so the detail/stream paths can hand it straight back to
 * [MainAPI.load]. Episodes carry the provider "data" string in [Video.id], which is exactly what
 * [MainAPI.loadLinks] needs — so resolving an episode is a single call with no extra round-trip.
 */
object CloudStreamSources {

    const val ADDON_PREFIX = "cloudstream:"

    fun addonId(provider: MainAPI): String = "$ADDON_PREFIX${provider.name}"

    fun providerNameOf(addonId: String?): String? =
        addonId?.takeIf { it.startsWith(ADDON_PREFIX) }?.removePrefix(ADDON_PREFIX)

    private fun mapType(tvTypeName: String?): MediaType = when (tvTypeName) {
        "Anime", "OVA" -> MediaType.ANIME
        "TvSeries", "Cartoon", "Documentary", "AsianDrama", "Live" -> MediaType.SERIES
        else -> MediaType.MOVIE
    }

    /** Provider search → poster cards. Never throws; a failed provider yields an empty list. */
    suspend fun search(provider: MainAPI, query: String, timeoutMs: Long = 12_000): List<MediaItem> {
        val results = runCatching {
            withTimeout(timeoutMs) { provider.search(query, 1)?.items }
        }.getOrNull() ?: return emptyList()
        return results.map { it.toMediaItem(provider) }
    }

    private fun SearchResponse.toMediaItem(provider: MainAPI): MediaItem = MediaItem(
        id = url,
        type = mapType(type?.name),
        title = name,
        poster = posterUrl,
        year = null,
        rating = null,
        addonId = addonId(provider),
        addonName = provider.name,
        stremioType = type?.name,
    )

    /**
     * Episodes for a series/anime item (empty for a movie). The returned [Video.id] is the
     * provider data string used later by [streams].
     */
    suspend fun episodes(provider: MainAPI, item: MediaItem, timeoutMs: Long = 12_000): List<Video> {
        val load = runCatching { withTimeout(timeoutMs) { provider.load(item.id) } }.getOrNull()
            ?: return emptyList()
        return episodesFrom(load)
    }

    private fun episodesFrom(load: LoadResponse): List<Video> = when (load) {
        is TvSeriesLoadResponse -> load.episodes.map { it.toVideo() }
        is AnimeLoadResponse -> load.episodes.values.flatten().map { it.toVideo() }
        else -> emptyList()
    }

    /** Full detail-page metadata for a CloudStream item, resolved from the provider in one load. */
    suspend fun metaDetail(provider: MainAPI, item: MediaItem, timeoutMs: Long = 15_000): com.novastream.app.data.model.MetaDetail? {
        val load = runCatching { withTimeout(timeoutMs) { provider.load(item.id) } }.getOrNull()
            ?: return null
        val videos = episodesFrom(load)
        return com.novastream.app.data.model.MetaDetail(
            id = item.id,
            type = if (videos.isNotEmpty()) "series" else "movie",
            name = load.name.ifBlank { item.title },
            poster = load.posterUrl ?: item.poster,
            background = load.backgroundPosterUrl ?: load.posterUrl ?: item.backdrop ?: item.poster,
            description = load.plot,
            releaseInfo = load.year?.toString() ?: item.year,
            genres = load.tags.orEmpty(),
            addonId = item.addonId,
            videos = videos,
            totalEpisodes = videos.size.takeIf { it > 0 },
        )
    }

    private fun Episode.toVideo(): Video = Video(
        id = data,
        title = name ?: episode?.let { "Episode $it" } ?: "Episode",
        season = season,
        episode = episode,
        thumbnail = posterUrl,
    )

    /** True when this is a movie-style provider load (single playable data URL). */
    private fun LoadResponse.movieData(): String? = (this as? MovieLoadResponse)?.dataUrl

    /**
     * Resolve playable streams for [item] (a movie) or [video] (an episode, whose id is the
     * provider data string). Returns app [StreamSource]s; never throws.
     */
    suspend fun streams(
        provider: MainAPI,
        item: MediaItem,
        video: Video? = null,
        timeoutMs: Long = 20_000,
    ): List<StreamSource> {
        val data = video?.id ?: runCatching {
            withTimeout(timeoutMs) { provider.load(item.id)?.movieData() }
        }.getOrNull()
        if (data.isNullOrBlank()) return emptyList()

        val out = ArrayList<StreamSource>()
        runCatching {
            withTimeout(timeoutMs) {
                provider.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { /* subtitles are resolved separately */ },
                    callback = { link -> out.add(link.toStreamSource(provider)) },
                )
            }
        }
        return out
    }

    private fun ExtractorLink.toStreamSource(provider: MainAPI): StreamSource = StreamSource(
        url = url,
        title = name.ifBlank { source },
        name = provider.name,
        description = source,
        quality = Qualities.getStringByInt(quality),
        addonId = addonId(provider),
        addonName = provider.name,
        headers = getAllHeaders(),
    )

    /** Best-effort title match: the provider's result whose title equals [title] (case-insensitive). */
    fun bestMatch(candidates: List<MediaItem>, title: String): MediaItem? {
        val normalized = title.trim().lowercase()
        return candidates.firstOrNull { it.title.trim().lowercase() == normalized }
            ?: candidates.firstOrNull { it.title.trim().lowercase().contains(normalized) }
    }
}
