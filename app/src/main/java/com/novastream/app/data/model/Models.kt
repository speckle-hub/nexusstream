package com.novastream.app.data.model

import com.google.gson.JsonObject

/** Logical content type used across the app. */
enum class MediaType(val id: String, val label: String) {
    MOVIE("movie", "Movies"),
    SERIES("series", "TV Shows"),
    ANIME("anime", "Anime"),
    MANGA("manga", "Manga"),
    NSFW_ANIME("nsfw_anime", "NSFW Anime"),
    NSFW_MANGA("nsfw_manga", "NSFW Manga"),
    REAL("real", "Real 18+");

    companion object {
        fun fromId(id: String?): MediaType? = entries.firstOrNull { it.id == id }
    }
}

/** A single browsable item (poster card). */
data class MediaItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val year: String? = null,
    val rating: Double? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val addonId: String? = null,
    val catalogId: String? = null,
    val addonName: String? = null,
    /**
     * The exact Stremio "type" string this item declared in its catalog response
     * (e.g. "movie", "series", "Porn", "hentai"). Stremio add-ons — especially NSFW
     * ones — use non-standard types, and meta/stream requests MUST reuse the item's own
     * declared type, otherwise the add-on returns nothing.
     */
    val stremioType: String? = null,
) {
    /** Composite key that encodes the owning addon so meta lookups are deterministic. */
    val key: String get() = "${addonId ?: "local"}::$id"

    /**
     * True when this title is *read* rather than watched (manga, including the NSFW variant).
     *
     * Used to split the shared history list into the Home/Library "Continue Watching" and
     * "Continue Reading" sections — both write the same [com.novastream.app.data.model.WatchEntry]
     * shape, but only the reading entries should ever appear under a Play CTA.
     */
    val isReadable: Boolean get() = type == MediaType.MANGA || type == MediaType.NSFW_MANGA
}

/** Stream source returned by a Stremio stream addon. */
data class StreamSource(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    val quality: String? = null,
    val addonId: String? = null,
    val addonName: String? = null,
    val isTorrent: Boolean = false,
    /**
     * HTTP headers the player must send with every request for this stream (Referer / Cookie /
     * User-Agent). Hotlink-protected CDNs — Pornhub's `hm-h` node in particular — answer 410/412
     * without them, so a stream that "looks fine" silently fails to play.
     */
    val headers: Map<String, String> = emptyMap(),
    /** Raw Stremio "sources" entries, e.g. "tracker:udp://...", "dht:...". */
    val sources: List<String> = emptyList(),
    /**
     * Optional chapter/intro/outro markers (milliseconds). When an add-on or HLS manifest supplies
     * them the player shows real "Skip Intro"/"Skip Outro" actions; when absent it falls back to a
     * conservative heuristic window. See `PlayerActivity`.
     */
    val introStartMs: Long? = null,
    val introEndMs: Long? = null,
    val outroStartMs: Long? = null,
    /**
     * Backup mirror URLs tried in order when [url] fails mid-playback (Phase 12 multi-source
     * failover). Populated by sources that resolve the same stream through several hosts.
     */
    val alternates: List<String> = emptyList(),
    /**
     * Torrent swarms seeders reported by the add-on (Torrentio puts it in the title). Used to rank
     * the most reliable streams first so the top entry is the one most likely to play smoothly.
     */
    val seeders: Int? = null,
) {
    /** A directly playable HTTP/HTTPS URL, when the add-on provided one. */
    val playableUrl: String? get() = url ?: externalUrl

    /** True when this stream can only be played through a torrent engine (P2P). */
    val needsTorrent: Boolean get() = playableUrl == null && !infoHash.isNullOrBlank()

    /** A magnet URI built from the info hash + trackers, for the torrent engine. */
    val magnetUri: String? get() {
        val hash = infoHash?.takeIf { it.isNotBlank() } ?: return null
        val sb = StringBuilder("magnet:?xt=urn:btih:").append(hash)
        title?.takeIf { it.isNotBlank() }?.let {
            sb.append("&dn=").append(java.net.URLEncoder.encode(it, "UTF-8"))
        }
        sources.filter { it.startsWith("tracker:") }.forEach { tr ->
            val url = tr.removePrefix("tracker:")
            if (url.isNotBlank()) sb.append("&tr=").append(java.net.URLEncoder.encode(url, "UTF-8"))
        }
        return sb.toString()
    }
}

/** Subtitle track returned by a Stremio subtitles addon. */
data class SubtitleTrack(
    val id: String,
    val url: String,
    val lang: String,
    val addonName: String? = null,
)

/** Episode / video entry from a meta addon. */
data class Video(
    val id: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnail: String? = null,
    val released: String? = null,
    val overview: String? = null,
    /** Runtime in whole minutes, when the source knows it (TMDB season episodes do). */
    val runtimeMin: Int? = null,
)

/** A season header for the detail page (expandable). */
data class SeasonInfo(
    val season: Int,
    val title: String? = null,
    val poster: String? = null,
    val episodeCount: Int? = null,
)

/** A cast / character entry with an optional portrait. */
data class CastMember(
    val name: String,
    val character: String? = null,
    val image: String? = null,
)

/** Full metadata for a detail page. */
data class MetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val videos: List<Video> = emptyList(),
    val addonId: String? = null,
    val raw: JsonObject? = null,
    // ---- Rich detail fields ----
    val altTitles: List<String> = emptyList(),
    val votes: Int? = null,
    val status: String? = null,
    val totalEpisodes: Int? = null,
    val totalChapters: Int? = null,
    val trailerUrl: String? = null,
    val castMembers: List<CastMember> = emptyList(),
    val recommendations: List<MediaItem> = emptyList(),
)

/** Catalog descriptor exposed by a Stremio addon manifest. */
data class CatalogDef(
    val type: String,
    val id: String,
    val name: String,
    val genres: List<String> = emptyList(),
    val extra: List<ExtraDef> = emptyList(),
)

data class ExtraDef(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
)

enum class AddonKind(val label: String) {
    STREMIO("Stremio"),
    CLOUDSTREAM("CloudStream"),
    ANIYOMI("Aniyomi"),
    KEIYOUSHI("Mihon / Keiyoushi"),
}

/** Installed addon / extension. */
data class Addon(
    val id: String,
    val name: String,
    val transportUrl: String,
    val description: String? = null,
    val version: String? = null,
    val logo: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    /**
     * Stremio `idPrefixes`: the id namespaces this add-on serves (`tt`, `kitsu:`, `anilist:`, …).
     * Empty means the add-on did not restrict ids (the protocol default). The union of the
     * manifest-level list and any per-resource lists. Used to pick the request id form and to
     * avoid asking an add-on for ids it cannot answer.
     */
    val idPrefixes: List<String> = emptyList(),
    val catalogs: List<CatalogDef> = emptyList(),
    val enabled: Boolean = true,
    val nsfw: Boolean = false,
    val kind: AddonKind = AddonKind.STREMIO,
)

/** CloudStream extension descriptor. */
data class CloudStreamExt(
    val name: String,
    val url: String,
    val description: String? = null,
    val icon: String? = null,
    val version: String? = null,
    val language: String? = null,
    val internalName: String? = null,
    val status: Int = 1,
    val tvTypes: List<String> = emptyList(),
)

/** Aniyomi / Mihon extension descriptor. */
data class MangaExt(
    val name: String,
    val pkg: String,
    val apk: String,
    val icon: String? = null,
    val version: String? = null,
    val lang: String? = null,
    val nsfw: Boolean = false,
    val sources: List<MangaSource> = emptyList(),
)

data class MangaSource(
    val id: Long,
    val name: String,
    val lang: String? = null,
    val baseUrl: String? = null,
)

/** A remote repository entry shown in the addon manager. */
data class RemoteRepo(
    val name: String,
    val url: String,
    val kind: AddonKind,
)

/** Row of content (a catalog result) for the home / section screens. */
data class CatalogRow(
    val title: String,
    val items: List<MediaItem>,
    val addonId: String? = null,
    val catalogId: String? = null,
    val type: MediaType = MediaType.MOVIE,
)

/** Continue-watching / history entry. */
data class WatchEntry(
    val item: MediaItem,
    val videoId: String? = null,
    val videoTitle: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
