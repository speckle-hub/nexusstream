package com.novastream.app.data.repo

import com.novastream.app.data.adult.AdultRepository
import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.hentai.HentaiEpisode
import com.novastream.app.data.hentai.HentaiRepository
import com.novastream.app.data.local.AddonStore
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.remote.StremioClient
import com.novastream.app.data.remote.TmdbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

/** Streams plus per-add-on failure reasons, so the UI can say *why* a list came back empty. */
data class StreamsResult(
    val streams: List<StreamSource> = emptyList(),
    val errors: List<String> = emptyList(),
)

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

    suspend fun streamsFor(item: MediaItem, videoId: String? = null): List<StreamSource> =
        streamsForDetailed(item, videoId).streams

    /**
     * Same as [streamsFor] but also returns per-add-on failure reasons for clearer UI feedback.
     */
    suspend fun streamsForDetailed(item: MediaItem, videoId: String? = null): StreamsResult {
        // Built-in adult sources resolve their own direct streams from the video page.
        AdultSources.byId(item.addonId)?.let { source ->
            val direct = runCatching { AdultRepository.streams(item.id, source) }.getOrDefault(emptyList())
            return StreamsResult(direct)
        }

        // NSFW anime gets a built-in stream source so the section plays with zero add-ons
        // installed: the built-in results lead the list and the enabled Stremio NSFW add-ons are
        // queried alongside them as extra / fallback entries. Unchanged from before this pass.
        if (item.type == MediaType.NSFW_ANIME) {
            val direct = HentaiEpisode.fromVideoId(videoId)
            return coroutineScope {
                val builtIn = async {
                    runCatching { hentaiStreams(item, videoId, direct) }.getOrDefault(emptyList())
                }
                val addons = async {
                    // Our own episode ids are page URLs — an add-on has nothing to do with them.
                    if (direct != null) StreamsResult() else stremioStreamsDetailed(item, videoId)
                }
                val addonResult = addons.await()
                StreamsResult(
                    streams = (builtIn.await().ranked() + addonResult.streams.ranked())
                        .distinctBy { streamKey(it) },
                    errors = addonResult.errors,
                )
            }
        }
        return stremioStreamsDetailed(item, videoId)
    }

    /** Streams from the built-in hentai sources for [item] / [videoId]. */
    private suspend fun hentaiStreams(
        item: MediaItem,
        videoId: String?,
        direct: Pair<String, String>?,
    ): List<StreamSource> {
        if (direct != null) {
            val (sourceId, url) = direct
            val episode = HentaiEpisode(
                sourceId = sourceId,
                series = item.title,
                title = item.title,
                url = url,
            )
            return HentaiRepository.streams(episode).streams
        }
        return HentaiRepository.streams(item.title, episodeNumber(item, videoId)).streams
    }

    /**
     * Episode number carried by an add-on's episode id (`anime:113804:0:5` -> 5). The base id
     * itself (no episode picked yet) must not be mistaken for one, otherwise every lookup would
     * ask for a nonsense episode number.
     */
    private fun episodeNumber(item: MediaItem, videoId: String?): Int? {
        if (videoId.isNullOrBlank() || videoId == item.id) return null
        return Regex("""(\d{1,4})$""").find(videoId)?.groupValues?.get(1)?.toIntOrNull()
            ?.takeIf { it in 1..9999 }
    }

    /**
     * Streams from every enabled Stremio add-on that can serve streams, with per-add-on error
     * collection, a hard per-add-on timeout (one hung host can no longer stall the whole list) and
     * reliability-first ranking. Regular Movie/TV/Anime results are cached briefly so re-opening a
     * title or re-tapping an episode is instant.
     */
    private suspend fun stremioStreamsDetailed(item: MediaItem, videoId: String?): StreamsResult = coroutineScope {
        val cacheable = item.type == MediaType.MOVIE || item.type == MediaType.SERIES || item.type == MediaType.ANIME
        val key = "${item.addonId}|${item.id}|${videoId ?: ""}"
        if (cacheable) {
            synchronized(cacheLock) {
                cache[key]?.takeIf { System.currentTimeMillis() - it.first < STREAM_TTL_MS }?.let {
                    return@coroutineScope it.second
                }
            }
        }

        val (type, baseId) = resolveId(item)
        val id = videoId ?: baseId
        val isAnime = item.type == MediaType.ANIME || item.type == MediaType.NSFW_ANIME
        // Request forms we can derive for this item; each add-on gets the first one its idPrefixes
        // accepts (e.g. an `anilist:` add-on gets `anilist:<id>`, a `tt` add-on gets a series
        // request for a tt episode). Add-ons with no prefixes keep the historical id unchanged.
        val candidates = StremioClient.candidateRequests(type, baseId, isAnime, videoId)
        val streamCapable = addonRepo.enabledAddons()
            .filter { it.resources.isEmpty() || it.resources.contains("stream") }
        // Real 18+ streams may only come from installed NSFW add-ons; try the owning one first.
        val addons = if (item.type == MediaType.REAL) {
            streamCapable.filter { it.nsfw }.sortedByDescending { it.id == item.addonId }
        } else {
            streamCapable.sortedByDescending { it.id == item.addonId }
        }

        val outcomes = addons.map { addon ->
            async {
                val (aType, aId) = StremioClient.chooseRequest(type to id, candidates, addon.idPrefixes)
                runCatching {
                    withTimeout(STREAM_TIMEOUT_MS) { StremioClient.fetchStreams(addon, aType, aId) }
                }.fold(
                    onSuccess = { StreamOutcome(addon.name, it, null) },
                    onFailure = { StreamOutcome(addon.name, emptyList(), it.message ?: "failed") },
                )
            }
        }.map { it.await() }

        val all = outcomes.flatMap { it.streams }.map { enrich(it) }
        val errors = outcomes.mapNotNull { outcome -> outcome.error?.let { "${outcome.addon}: $it" } }
        val result = StreamsResult(
            streams = StreamRanking.rank(all, AddonStore.Defaults.preferredStreamHosts),
            errors = errors,
        )
        if (cacheable) {
            synchronized(cacheLock) {
                if (cache.size >= STREAM_CACHE_MAX) cache.clear()
                cache[key] = System.currentTimeMillis() to result
            }
        }
        result
    }

    /** Fill in the seeder count from the stream text when the add-on didn't report one. */
    private fun enrich(stream: StreamSource): StreamSource =
        if (stream.seeders != null) stream
        else stream.copy(seeders = StreamRanking.seedersOf(stream.title, stream.name, stream.description))

    suspend fun subtitlesFor(item: MediaItem, videoId: String? = null): List<SubtitleTrack> = coroutineScope {
        val (type, baseId) = resolveId(item)
        val id = videoId ?: baseId
        val isAnime = item.type == MediaType.ANIME || item.type == MediaType.NSFW_ANIME
        val candidates = StremioClient.candidateRequests(type, baseId, isAnime, videoId)
        val addons = addonRepo.enabledAddons().filter { it.resources.contains("subtitles") }
        val jobs = addons.map { addon ->
            val (aType, aId) = StremioClient.chooseRequest(type to id, candidates, addon.idPrefixes)
            async { runCatching { StremioClient.fetchSubtitles(addon, aType, aId) }.getOrDefault(emptyList()) }
        }
        jobs.flatMap { it.await() }.distinctBy { it.url }
    }

    /** De-duplicates within one group and orders it highest quality first (built-in NSFW lists). */
    private fun List<StreamSource>.ranked(): List<StreamSource> =
        distinctBy { streamKey(it) }.sortedByDescending { StreamRanking.qualityRank(it.quality) }

    private fun streamKey(stream: StreamSource): String =
        stream.url ?: stream.infoHash ?: stream.externalUrl ?: stream.ytId
        ?: "${stream.addonId ?: ""}:${stream.name ?: ""}:${stream.title ?: ""}"

    private class StreamOutcome(
        val addon: String,
        val streams: List<StreamSource>,
        val error: String?,
    )

    /** Short-lived cache of ranked regular-stream results; NSFW/Real 18+ are never cached. */
    private val cache = HashMap<String, Pair<Long, StreamsResult>>()
    private val cacheLock = Any()

    private companion object {
        /** Hard cap per add-on so a hung host can't gate the whole stream list. */
        const val STREAM_TIMEOUT_MS = 12_000L

        /** Re-opening or re-tapping the same episode within this window is served from memory. */
        const val STREAM_TTL_MS = 5L * 60 * 1000

        const val STREAM_CACHE_MAX = 64
    }
}
