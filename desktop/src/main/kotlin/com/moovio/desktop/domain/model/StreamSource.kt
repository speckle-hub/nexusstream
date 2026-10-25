package com.moovio.desktop.domain.model

/**
 * A single playable stream resolved from an add-on. Ranked by quality → reliability → seeders.
 */
data class StreamSource(
    val url: String,
    val quality: String? = null,
    val codec: String? = null,
    val language: String? = null,
    val source: String? = null,
    val isTorrent: Boolean = false,
    val seeders: Int? = null,
)
