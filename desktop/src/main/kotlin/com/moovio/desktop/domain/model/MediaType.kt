package com.moovio.desktop.domain.model

enum class MediaType {
    MOVIE,
    TV,
    ANIME,
    MANGA;

    fun label(): String = when (this) {
        MOVIE -> "Movies"
        TV -> "TV"
        ANIME -> "Anime"
        MANGA -> "Manga"
    }
}
