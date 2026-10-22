package com.moovio.app.data.sfw

import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.Video

/**
 * A movie or series title discovered by a built-in SFW source.
 *
 * [url] is the absolute canonical page URL and doubles as the item id everywhere
 * (navigation, meta lookup, stream lookup), mirroring
 * [com.moovio.app.data.adult.AdultVideo] so the detail/stream flow stays identical
 * to the Stremio path without extra routing.
 */
data class SfwTitle(
    val sourceId: String,
    val url: String,
    val title: String,
    val kind: MediaType,
    val poster: String? = null,
    val year: String? = null,
    val rating: Double? = null,
) {
    fun toMediaItem(sourceName: String): MediaItem = MediaItem(
        id = url,
        type = kind,
        title = title,
        poster = poster,
        backdrop = poster,
        year = year,
        rating = rating,
        addonId = sourceId,
        addonName = sourceName,
    )
}

/** One home-screen row produced by a source (e.g. "FlixHQ · Trending"). */
data class SfwRow(
    val title: String,
    val sourceId: String,
    val type: MediaType,
    val titles: List<SfwTitle>,
)

/**
 * One playable episode of a series (or an anime episode) discovered by a built-in
 * SFW source. [url] is the absolute episode page URL; [toVideo] encodes it with
 * [sourceId] as the [Video] id so a later stream lookup is a single page fetch.
 */
data class SfwEpisode(
    /** Owning source id — also the [StreamSource.addonId] of the streams it resolves to. */
    val sourceId: String,
    /** Series name, used to group episodes and as a display fallback. */
    val series: String,
    /** Episode page title exactly as the site wrote it. */
    val title: String,
    /** Absolute episode page URL. */
    val url: String,
    val season: Int? = null,
    val episode: Int? = null,
    val poster: String? = null,
) {
    fun displayTitle(): String {
        val cleaned = title.replace(Regex("""\s+"""), " ").trim()
        return cleaned.ifBlank { series.ifBlank { title } }
    }

    /**
     * Episode id for [Video]: `sfw:<sourceId>:<episode page url>`. The `sfw:` prefix tells
     * [com.moovio.app.data.repo.StreamRepository] to resolve this from the built-in
     * sources instead of asking a Stremio add-on to interpret a URL as an add-on id.
     */
    fun toVideo(): Video = Video(
        id = videoId,
        title = displayTitle(),
        season = season,
        episode = episode,
        thumbnail = poster,
    )

    val videoId: String get() = "$VIDEO_ID_PREFIX$sourceId:$url"

    companion object {
        const val VIDEO_ID_PREFIX = "sfw:"

        /** Inverse of [videoId] — null when [id] was not produced by a built-in SFW source. */
        fun fromVideoId(id: String?): Pair<String, String>? {
            val raw = id ?: return null
            if (!raw.startsWith(VIDEO_ID_PREFIX)) return null
            val rest = raw.removePrefix(VIDEO_ID_PREFIX)
            val split = rest.indexOf(':')
            if (split <= 0 || split == rest.length - 1) return null
            return rest.substring(0, split) to rest.substring(split + 1)
        }
    }
}

/** Full detail for a title page: metadata, episode list and (for movies) streams. */
data class SfwDetail(
    val title: SfwTitle,
    val description: String? = null,
    val poster: String? = null,
    val episodes: List<SfwEpisode> = emptyList(),
    val streams: List<StreamSource> = emptyList(),
    val related: List<SfwTitle> = emptyList(),
)

/** Anime series title discovered by a built-in SFW anime source (AniList-led metadata). */
data class SfwAnimeTitle(
    val sourceId: String,
    val url: String,
    val title: String,
    val poster: String? = null,
) {
    fun toMediaItem(sourceName: String): MediaItem = MediaItem(
        id = url,
        type = MediaType.ANIME,
        title = title,
        poster = poster,
        backdrop = poster,
        addonId = sourceId,
        addonName = sourceName,
    )
}

/**
 * One playable anime episode. Mirrors
 * [com.moovio.app.data.hentai.HentaiEpisode] with the `sfwa:` video-id prefix
 * so anime episode pages route to the SFW anime repository.
 */
data class SfwAnimeEpisode(
    val sourceId: String,
    val series: String,
    val title: String,
    val url: String,
    val episode: Int? = null,
    val poster: String? = null,
) {
    fun displayTitle(): String {
        val cleaned = title.replace(Regex("""\s+"""), " ").trim()
        return cleaned.ifBlank { series.ifBlank { title } }
    }

    fun toVideo(): Video = Video(
        id = videoId,
        title = displayTitle(),
        season = null,
        episode = episode,
        thumbnail = poster,
    )

    val videoId: String get() = "$VIDEO_ID_PREFIX$sourceId:$url"

    companion object {
        const val VIDEO_ID_PREFIX = "sfwa:"

        fun fromVideoId(id: String?): Pair<String, String>? {
            val raw = id ?: return null
            if (!raw.startsWith(VIDEO_ID_PREFIX)) return null
            val rest = raw.removePrefix(VIDEO_ID_PREFIX)
            val split = rest.indexOf(':')
            if (split <= 0 || split == rest.length - 1) return null
            return rest.substring(0, split) to rest.substring(split + 1)
        }
    }
}

/** Raised when a specific SFW source is unreachable / blocked / returns nothing usable. */
class SfwSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
