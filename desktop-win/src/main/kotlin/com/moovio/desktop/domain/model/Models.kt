package com.moovio.desktop.domain.model

import kotlinx.serialization.Serializable

/** The content kinds Moovio understands. ANIME maps to Stremio's `series` type. */
@Serializable
enum class MediaType {
    MOVIE,
    SERIES,
    ANIME,
    MANGA;

    val stremioType: String
        get() = when (this) {
            MOVIE -> "movie"
            SERIES, ANIME -> "series"
            MANGA -> "manga"
        }

    val label: String
        get() = when (this) {
            MOVIE -> "Movie"
            SERIES -> "Series"
            ANIME -> "Anime"
            MANGA -> "Manga"
        }
}

/**
 * A catalog card. [id] is the id used by the metadata provider ("tt0111161", "anilist:21",
 * or an add-on specific id). [provider] records where it came from so play actions can route
 * the id to the right add-on.
 */
@Serializable
data class MediaItem(
    val id: String,
    val type: MediaType,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val releaseInfo: String? = null,
    val runtime: String? = null,
    /** Add-on that supplied the card, when it came from a catalog resource. */
    val addonId: String? = null,
    /** True only for content that may never surface outside the PIN-gated area. */
    val adult: Boolean = false,
    /** IMDb id when the provider exposed one (lets Stremio stream add-ons resolve it). */
    val imdbId: String? = null,
) {
    val key: String get() = "$type:$id"
}

@Serializable
data class Episode(
    val id: String,
    val season: Int,
    val episode: Int,
    val title: String,
    val thumbnail: String? = null,
    val released: String? = null,
    val description: String? = null,
) {
    val label: String get() = if (season > 1) "S$season E$episode" else "E$episode"
}

@Serializable
data class MetaDetail(
    val id: String,
    val type: MediaType,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val releaseInfo: String? = null,
    val rating: Double? = null,
    val runtime: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val adult: Boolean = false,
    /** MAL id, used to improve anime stream matching when present. */
    val malId: Int? = null,
    /** IMDb id from provider external links, used as the stream add-on id. */
    val imdbId: String? = null,
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        type = type,
        name = name,
        poster = poster,
        background = background,
        logo = logo,
        year = year,
        rating = rating,
        description = description,
        genres = genres,
        releaseInfo = releaseInfo,
        runtime = runtime,
        adult = adult,
        imdbId = imdbId,
    )
}

@Serializable
data class SubtitleTrack(
    val url: String,
    val lang: String,
    val label: String,
)

/** A single playable (or non-playable) stream returned by a Stremio add-on. */
@Serializable
data class StreamSource(
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val quality: String? = null,
    val seeders: Int? = null,
    val addonId: String = "",
    val addonName: String = "",
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList(),
) {
    val isDirect: Boolean get() = !url.isNullOrBlank()
    val isTorrent: Boolean get() = !infoHash.isNullOrBlank()
    val isPlayable: Boolean get() = isDirect

    /** Stable identity for de-duplication across add-ons. */
    val fingerprint: String get() = url ?: "torrent:$infoHash:$fileIdx"

    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: addonName
}

@Serializable
data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String,
    val genres: List<String> = emptyList(),
    /** True when the catalog supports `search=` extras. */
    val searchable: Boolean = false,
)

@Serializable
data class Addon(
    val manifestUrl: String,
    val transportUrl: String,
    val id: String,
    val name: String,
    val version: String = "",
    val description: String = "",
    val logo: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
    val catalogs: List<AddonCatalog> = emptyList(),
    /** NSFW add-ons are hidden everywhere except the gated area. */
    val adult: Boolean = false,
    val enabled: Boolean = true,
    val builtIn: Boolean = false,
) {
    val supportsStreams: Boolean get() = resources.any { it.equals("stream", true) }
    val supportsCatalog: Boolean get() = resources.any { it.equals("catalog", true) }
    val supportsMeta: Boolean get() = resources.any { it.equals("meta", true) }
}

@Serializable
data class WatchProgress(
    val mediaKey: String,
    val mediaId: String,
    val type: MediaType,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = 0,
    val adult: Boolean = false,
) {
    val fraction: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val finished: Boolean get() = durationMs > 0 && fraction >= 0.94f

    /**
     * Worth listing under Continue Watching: anything with real progress that has not effectively
     * finished. Deliberately only a couple of seconds, so a short-but-real viewing session still
     * shows up; accidental opens that never got past the first second stay out.
     */
    val inProgress: Boolean get() = !finished && positionMs >= MIN_PROGRESS_MS

    companion object {
        const val MIN_PROGRESS_MS = 2_000L
    }
}

/** A request handed to the player screen. */
data class PlaybackRequest(
    val media: MediaItem,
    val source: StreamSource,
    val startPositionMs: Long = 0,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
)

/** Metadata for the player to resolve streams when "auto play" was requested. */
data class PlayTarget(
    val media: MediaItem,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val startPositionMs: Long = 0,
    /** Pre-selected stream; null means "resolve and auto-pick the best". */
    val source: StreamSource? = null,
    /** Already-loaded metadata, so the player can resolve episodes/IMDb ids directly. */
    val detail: MetaDetail? = null,
)
