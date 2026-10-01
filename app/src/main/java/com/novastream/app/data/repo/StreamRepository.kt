package com.novastream.app.data.repo

import com.novastream.app.data.adult.AdultRepository
import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.remote.StremioClient
import com.novastream.app.data.remote.TmdbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Resolves playable streams and subtitles from every enabled Stremio addon. */
class StreamRepository(private val addonRepo: AddonRepository) {

    /** Resolve (stremioType, id) for stream addon queries. */
    private suspend fun resolveId(item: MediaItem): Pair<String, String> {
        return when (item.addonId) {
            "tmdb" -> {
                val imdb = TmdbClient.imdbId(item.type, item.id)
                val t = if (item.type == MediaType.MOVIE) "movie" else "series"
                t to (imdb ?: item.id)
            }
            "anilist", "jikan" -> "anime" to item.id
            else -> {
                // Reuse the exact type the add-on declared for this item (critical for NSFW
                // add-ons that use non-standard types such as "Porn" or "hentai").
                val t = item.stremioType ?: when (item.type) {
                    MediaType.MOVIE, MediaType.REAL -> "movie"
                    MediaType.SERIES -> "series"
                    MediaType.ANIME, MediaType.NSFW_ANIME -> "anime"
                    MediaType.MANGA, MediaType.NSFW_MANGA -> "manga"
                    else -> "movie"
                }
                t to item.id
            }
        }
    }

    suspend fun streamsFor(item: MediaItem, videoId: String? = null): List<StreamSource> {
        // Built-in adult sources resolve their own direct streams from the video page.
        AdultSources.byId(item.addonId)?.let { source ->
            return runCatching { AdultRepository.streams(item.id, source) }.getOrDefault(emptyList())
        }
        return coroutineScope {
        val (type, baseId) = resolveId(item)
        val id = videoId ?: baseId
        val streamCapable = addonRepo.enabledAddons()
            .filter { it.resources.isEmpty() || it.resources.contains("stream") }
        // Real 18+ streams may only come from installed NSFW add-ons; try the owning one first.
        val addons = if (item.type == MediaType.REAL) {
            streamCapable.filter { it.nsfw }.sortedByDescending { it.id == item.addonId }
        } else {
            streamCapable.sortedByDescending { it.id == item.addonId }
        }
        val jobs = addons.map { addon ->
            async {
                runCatching { StremioClient.fetchStreams(addon, type, id) }.getOrDefault(emptyList())
            }
        }
        jobs.flatMap { it.await() }
            .distinctBy { it.url ?: it.infoHash ?: it.externalUrl }
            .sortedByDescending { qualityRank(it.quality) }
        }
    }

    suspend fun subtitlesFor(item: MediaItem, videoId: String? = null): List<SubtitleTrack> = coroutineScope {
        val (type, baseId) = resolveId(item)
        val id = videoId ?: baseId
        val addons = addonRepo.enabledAddons().filter { it.resources.contains("subtitles") }
        val jobs = addons.map { addon ->
            async { runCatching { StremioClient.fetchSubtitles(addon, type, id) }.getOrDefault(emptyList()) }
        }
        jobs.flatMap { it.await() }.distinctBy { it.url }
    }

    private fun qualityRank(q: String?): Int = when (q) {
        "4K" -> 5
        "1440p" -> 4
        "1080p" -> 3
        "720p" -> 2
        "480p" -> 1
        "360p" -> 0
        else -> -1
    }
}
