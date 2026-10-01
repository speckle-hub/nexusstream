package com.novastream.app.data.adult

import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource

/**
 * A single adult video, sourced entirely from the provider site itself.
 *
 * [url] is the absolute canonical page URL and doubles as the item id everywhere in the app
 * (navigation, meta lookup, stream lookup), which keeps the detail/stream flow identical to the
 * Stremio path without needing any extra routing.
 */
data class AdultVideo(
    val sourceId: String,
    val url: String,
    val title: String,
    val thumbnail: String? = null,
    val duration: String? = null,
    val views: String? = null,
    val quality: String? = null,
    val uploader: String? = null,
) {
    fun toMediaItem(sourceName: String): MediaItem = MediaItem(
        id = url,
        type = MediaType.REAL,
        title = title,
        poster = thumbnail,
        backdrop = thumbnail,
        description = null,
        addonId = sourceId,
        addonName = sourceName,
    )
}

/** One home-screen row produced by a source (e.g. "Pornhub · Trending"). */
data class AdultRow(val title: String, val sourceId: String, val videos: List<AdultVideo>)

/** A category / tag on the provider site. */
data class AdultCategory(val id: String, val name: String, val url: String)

/** Full detail for a single video: metadata, playable streams and related videos. */
data class AdultDetail(
    val video: AdultVideo,
    val description: String? = null,
    val uploader: String? = null,
    val streams: List<StreamSource> = emptyList(),
    val related: List<AdultVideo> = emptyList(),
)

/** Raised when a specific source is unreachable / blocked / returns nothing usable. */
class AdultSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
