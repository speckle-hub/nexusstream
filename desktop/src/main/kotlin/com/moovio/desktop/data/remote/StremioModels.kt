package com.moovio.desktop.data.remote

import kotlinx.serialization.Serializable
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.StreamSource

@Serializable
data class StremioManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val types: List<String> = emptyList(),
    val catalogs: List<StremioCatalog> = emptyList(),
    val resources: List<StremioResource> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
    val logo: String? = null,
    val contactEmail: String? = null,
    val homepage: String? = null,
    val repository: String? = null,
)

@Serializable
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<StremioExtra> = emptyList(),
)

@Serializable
data class StremioExtra(
    val name: String,
    val options: List<StremioExtraOption> = emptyList(),
    val isRequired: Boolean = false,
)

@Serializable
data class StremioExtraOption(
    val name: String,
    val isRequired: Boolean = false,
)

@Serializable
data class StremioResource(
    val name: String,
    val types: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
)

@Serializable
data class StremioMetaResponse(val meta: StremioMeta?)

@Serializable
data class StremioMeta(
    val id: String,
    val type: String,
    val name: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val runtime: Int? = null,
    val genre: String? = null,
    val imdbRating: Double? = null,
    val videos: List<StremioVideo> = emptyList(),
    val director: String? = null,
    val cast: String? = null,
    val trailer: String? = null,
    val links: List<StremioLink> = emptyList(),
)

@Serializable
data class StremioVideo(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val releaseInfo: String? = null,
    val videoType: String? = null,
)

@Serializable
data class StremioLink(val title: String, val url: String)

@Serializable
data class StremioCatalogResponse(val metas: List<StremioCatalogMeta> = emptyList())

@Serializable
data class StremioCatalogMeta(
    val id: String,
    val type: String? = null,
    val name: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val genre: String? = null,
    val rating: Double? = null,
    val released: String? = null,
)

@Serializable
data class StremioStreamsResponse(val streams: List<StremioStream> = emptyList())

@Serializable
data class StremioStream(
    val url: String,
    val title: String? = null,
    val quality: String? = null,
    val codec: String? = null,
    val language: String? = null,
    val name: String? = null,
    val behaviorHints: StremioBehaviorHints? = null,
)

@Serializable
data class StremioBehaviorHints(
    val notWebReady: Boolean = false,
    val forceSubtitle: Boolean = false,
)

@Serializable
data class StremioSubtitleResponse(
    val subtitles: List<StremioSubtitle> = emptyList(),
)

@Serializable
data class StremioSubtitle(
    val url: String,
    val language: String? = null,
)

/** Maps a Stremio stream into the app's domain model, ranked as-is (add-ons may pre-rank). */
fun StremioStream.toDomain(source: String): StreamSource =
    StreamSource(
        url = url,
        quality = quality,
        codec = codec,
        language = language,
        source = source,
        isTorrent = url.lowercase().contains("torrent") || url.lowercase().contains("acestream"),
    )

fun MediaType.stremioType(): String = when (this) {
    MediaType.MOVIE, MediaType.TV, MediaType.ANIME -> "movie"
    MediaType.MANGA -> "movie"
}
