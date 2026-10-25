package com.moovio.desktop.domain.model

/**
 * Lightweight catalog item shared between screens. Fully serializable so the library can
 * persist it to disk without pulling in network model classes.
 */
data class MediaItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val year: Int? = null,
    val rating: Double? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val genre: String? = null,
    val synopsis: String? = null,
    val source: String? = null,
    val isAdult: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun subtitle(): String =
        listOfNotNull(year?.toString(), genre).joinToString(" · ")
}
