package com.moovio.desktop.domain.model

data class Season(
    val number: Int,
    val title: String? = null,
    val episodeCount: Int = 0,
    val poster: String? = null,
)

data class Episode(
    val id: String,
    val season: Int,
    val number: Int,
    val title: String? = null,
    val rating: Double? = null,
    val poster: String? = null,
    val url: String? = null,
)

data class MetaDetail(
    val mediaId: String,
    val type: MediaType,
    val title: String,
    val year: Int? = null,
    val rating: Double? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val genre: String? = null,
    val synopsis: String? = null,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val streams: List<StreamSource> = emptyList(),
)
