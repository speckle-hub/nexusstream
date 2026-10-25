package com.moovio.desktop.data.repo

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.remote.AniListClient
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.remote.TmdbClient
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataRepository(
    private val cinemeta: CinemetaClient,
    private val aniList: AniListClient,
    private val stremio: StremioClient,
    private val addonStore: AddonStore,
    private val tmdb: TmdbClient,
) {
    /**
     * Resolves full detail for a card. Falls back through (1) the provider that produced the
     * card, (2) Cinemeta/AniList by id, (3) a title search.
     */
    suspend fun detail(item: MediaItem, includeAdult: Boolean = false): MetaDetail? = withContext(Dispatchers.IO) {
        var detail: MetaDetail? = null

        detail = when {
            item.id.startsWith("anilist:") -> runCatching { aniList.detail(item.id) }.getOrNull()
            item.id.startsWith("tt") -> runCatching {
                cinemeta.meta(if (item.type == MediaType.MOVIE) MediaType.MOVIE else MediaType.SERIES, item.id)
            }.getOrNull()
            item.addonId != null -> {
                val addon = addonStore.current.firstOrNull { it.id == item.addonId }
                if (addon != null && (includeAdult || !addon.adult)) {
                    runCatching { stremio.meta(addon, item.type.stremioType, item.id) }.getOrNull()
                } else null
            }
            else -> null
        }

        if (detail == null && item.id.startsWith("tt")) {
            detail = runCatching {
                cinemeta.meta(if (item.type == MediaType.MOVIE) MediaType.MOVIE else MediaType.SERIES, item.id)
            }.getOrNull()
        }

        if (detail == null) {
            // Last resort: title search against Cinemeta, then AniList.
            detail = runCatching {
                val found = cinemeta.search(
                    if (item.type == MediaType.MOVIE) MediaType.MOVIE else MediaType.SERIES,
                    item.name,
                ).firstOrNull { it.year == item.year } ?: cinemeta.search(
                    if (item.type == MediaType.MOVIE) MediaType.MOVIE else MediaType.SERIES,
                    item.name,
                ).firstOrNull()
                found?.let {
                    cinemeta.meta(if (it.type == MediaType.MOVIE) MediaType.MOVIE else MediaType.SERIES, it.id)
                }
            }.getOrNull()
        }

        if (detail == null && (item.type == MediaType.ANIME || item.id.startsWith("anilist:"))) {
            detail = runCatching {
                aniList.search(item.name, 5).firstOrNull()?.let { aniList.detail(it.id) }
            }.getOrNull()
        }

        detail = detail?.let { mergeFallbacks(it, item) }

        // Optional polish: a TMDB key unlocks better backdrops when metadata is thin.
        if (detail != null && tmdb.isConfigured) {
            val imdb = detail.imdbId ?: item.imdbId ?: detail.id.takeIf { it.startsWith("tt") }
            val artwork = imdb?.let { runCatching { tmdb.artworkForImdb(it) }.getOrNull() }
            if (artwork != null) {
                detail = detail.copy(
                    background = detail.background ?: artwork.backdrop,
                    poster = detail.poster ?: artwork.poster,
                )
            }
        }

        detail
    }

    private fun mergeFallbacks(detail: MetaDetail, item: MediaItem): MetaDetail = detail.copy(
        poster = detail.poster ?: item.poster,
        background = detail.background ?: item.background ?: item.poster,
        description = detail.description ?: item.description,
        year = detail.year ?: item.year,
        rating = detail.rating ?: item.rating,
        genres = if (detail.genres.isNotEmpty()) detail.genres else item.genres,
        adult = detail.adult || item.adult,
        imdbId = detail.imdbId ?: item.imdbId
            ?: detail.id.takeIf { it.startsWith("tt") }
            ?: item.id.takeIf { it.startsWith("tt") },
    )

    suspend fun prefetch(item: MediaItem) {
        withContext(Dispatchers.IO) { runCatching { detail(item) } }
    }
}
