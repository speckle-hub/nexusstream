package com.moovio.app.data.repo

import com.moovio.app.data.adult.AdultRepository
import com.moovio.app.data.adult.AdultSources
import com.moovio.app.data.hentai.HentaiEpisode
import com.moovio.app.data.hentai.HentaiRepository
import com.moovio.app.data.cloudstream.CloudStreamSources
import com.moovio.app.data.sfw.SfwAnimeEpisode
import com.moovio.app.data.sfw.SfwAnimeRepository
import com.moovio.app.data.sfw.SfwAnimeSources
import com.moovio.app.data.sfw.SfwEpisode
import com.moovio.app.data.sfw.SfwRepository
import com.moovio.app.data.sfw.SfwSources
import com.moovio.app.data.sfw.sfwSeasonEpisode
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.SubtitleTrack
import com.moovio.app.data.model.Video
import com.moovio.app.data.remote.StremioClient
import com.moovio.app.data.remote.TmdbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Streams plus per-add-on failure reasons, so the UI can say *why* a list came back empty. */
data class StreamsResult(
    val streams: List<StreamSource> = emptyList(),
    val errors: List<String> = emptyList(),
)

/** Resolves playable streams and subtitles from every enabled Stremio addon. */
class StreamRepository(
    private val addonRepo: AddonRepository,
    private val cloudStreamRepo: CloudStreamRepository,
) {

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
     *
     * Phase 32: [onPartial] fires progressively as each provider lands (completion order), so
     * the UI can paint the first valid playable link instead of waiting on every slow hoster.
     */
    suspend fun streamsForDetailed(
        item: MediaItem,
        videoId: String? = null,
        onPartial: (suspend (StreamsResult) -> Unit)? = null,
    ): StreamsResult {
        // Built-in adult sources resolve their own direct streams from the video page.
        AdultSources.byId(item.addonId)?.let { source ->
            val direct = runCatching { AdultRepository.streams(item.id, source) }.getOrDefault(emptyList())
            return StreamsResult(direct)
        }

        // Built-in SFW movie/TV titles resolve their own direct streams: a movie page
        // or an `sfw:` episode page, with no add-on involved.
        SfwSources.byId(item.addonId)?.let { source ->
            val direct = runCatching {
                val ep = SfwEpisode.fromVideoId(videoId)
                if (ep != null && ep.first == source.id) {
                    SfwRepository.streamsForEpisode(
                        SfwEpisode(source.id, item.title, item.title, ep.second),
                        source,
                    )
                } else {
                    SfwRepository.streams(item.id, source)
                }
            }.getOrDefault(emptyList())
            return StreamsResult(direct)
        }

        // Built-in SFW anime episode pages (`sfwa:`) resolve directly.
        SfwAnimeEpisode.fromVideoId(videoId)?.let { (sourceId, url) ->
            val source = SfwAnimeSources.byId(sourceId)
            val direct = if (source != null) {
                runCatching {
                    SfwAnimeRepository.streams(SfwAnimeEpisode(sourceId, item.title, item.title, url)).streams
                }.getOrDefault(emptyList())
            } else emptyList()
            return StreamsResult(direct)
        }

        // CloudStream provider items own their streams: a movie uses the provider's data URL, an
        // episode carries it in [videoId] (see CloudStreamSources).
        if (item.addonId?.startsWith(CloudStreamSources.ADDON_PREFIX) == true) {
            val video = videoId?.takeIf { it.isNotBlank() }?.let { Video(id = it) }
            val direct = runCatching { cloudStreamRepo.streamsFor(item, video) }.getOrDefault(emptyList())
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
                    if (direct != null) StreamsResult() else stremioStreamsDetailed(item, videoId, onPartial)
                }
                val addonResult = addons.await()
                StreamsResult(
                    streams = (builtIn.await().ranked() + addonResult.streams.ranked())
                        .distinctBy { streamKey(it) },
                    errors = addonResult.errors,
                )
            }
        }
        // Phase 32: race the Stremio fan-out against the CloudStream title match concurrently
        // instead of awaiting one before starting the other. Built-in SFW sources race
        // alongside them, so Play works with zero add-ons installed.
        return coroutineScope {
            val baseDeferred = async { stremioStreamsDetailed(item, videoId, onPartial) }
            val extraDeferred = async {
                if (item.type == MediaType.MOVIE && !item.title.isBlank()) {
                    runCatching { cloudStreamRepo.titleStreams(item.title, item.type) }
                        .getOrDefault(emptyList())
                } else emptyList()
            }
            val sfwDeferred = async {
                if (item.type == MediaType.MOVIE || item.type == MediaType.SERIES || item.type == MediaType.ANIME) {
                    runCatching { sfwBuiltins(item, videoId) }.getOrDefault(emptyList())
                } else emptyList()
            }
            val base = baseDeferred.await()
            val extra = extraDeferred.await()
            val sfw = sfwDeferred.await()
            if (extra.isEmpty() && sfw.isEmpty()) return@coroutineScope base
            val merged = (extra.ranked() + sfw.ranked() + base.streams).distinctBy { streamKey(it) }
            StreamsResult(
                streams = StreamRanking.rank(
                    merged,
                    AddonStore.Defaults.preferredStreamHosts,
                    directFirst = true,
                ),
                errors = base.errors,
            )
        }
    }

    /**
     * Built-in SFW streams for a regular title: anime resolves by title + episode
     * number (AniList-led, like the NSFW-anime path), movies/series by title with
     * season/episode parsed from the Stremio-style video id and the TMDB id passed
     * through for the TMDB-keyed resolvers (Vidsrc family).
     */
    private suspend fun sfwBuiltins(item: MediaItem, videoId: String?): List<StreamSource> {
        if (item.type == MediaType.ANIME) {
            return SfwAnimeRepository.streams(item.title, episodeNumber(item, videoId)).streams
        }
        val (season, episode) = sfwSeasonEpisode(videoId, item.id)
        // TMDB-keyed resolvers (Vidsrc family) need an exact id: movies always carry
        // it, series only once an episode is picked (a series without S/E must never
        // resolve as a movie embed).
        val tmdbId = item.id.takeIf {
            item.addonId == "tmdb" && !it.contains(':') && it.all { c -> c.isDigit() } &&
                (item.type == MediaType.MOVIE || (season != null && episode != null))
        }
        return SfwRepository.streamsForTitle(item.title, item.year, season, episode, tmdbId)
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
     *
     * Phase 32: providers race concurrently under [STREAM_TIMEOUT_MS] and [onPartial] fires as
     * each one lands (completion order), so the player/UI paints the first valid playable link
     * instead of waiting on every slow or dead hoster.
     */
    private suspend fun stremioStreamsDetailed(
        item: MediaItem,
        videoId: String?,
        onPartial: (suspend (StreamsResult) -> Unit)? = null,
    ): StreamsResult = coroutineScope {
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

        val outcomes = ArrayList<StreamOutcome>()
        val mutex = Mutex()
        addons.map { addon ->
            launch {
                val (aType, aId) = StremioClient.chooseRequest(type to id, candidates, addon.idPrefixes)
                val outcome = ScraperRace.raceResult(STREAM_TIMEOUT_MS) {
                    StremioClient.fetchStreams(addon, aType, aId)
                }.fold(
                    onSuccess = { StreamOutcome(addon.name, it, null) },
                    onFailure = { StreamOutcome(addon.name, emptyList(), it.message ?: "failed") },
                )
                val partial = mutex.withLock {
                    outcomes += outcome
                    val all = outcomes.flatMap { it.streams }.map { enrich(it) }
                    val errors = outcomes.mapNotNull { o -> o.error?.let { "${o.addon}: $it" } }
                    StreamsResult(
                        streams = StreamRanking.rank(all, AddonStore.Defaults.preferredStreamHosts),
                        errors = errors,
                    )
                }
                // Progressive yield: emit as soon as the first valid playable link resolves.
                // Guarded so a throwing consumer can't cancel the sibling providers.
                if (partial.streams.isNotEmpty() || outcome.error != null) {
                    runCatching { onPartial?.invoke(partial) }
                }
            }
        }.joinAll()

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
            async {
                ScraperRace.raceResult(SUBTITLE_TIMEOUT_MS) { StremioClient.fetchSubtitles(addon, aType, aId) }
                    .getOrDefault(emptyList())
            }
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
        /**
         * Hard cap per add-on (Phase 32: 5 s fail-fast) so a hung host can't gate the stream list.
         * Every provider races concurrently and the first valid playable link yields immediately.
         */
        const val STREAM_TIMEOUT_MS = 5_000L

        /** Subtitle fan-out gets the same fail-fast ceiling. */
        const val SUBTITLE_TIMEOUT_MS = 5_000L

        /** Re-opening or re-tapping the same episode within this window is served from memory. */
        const val STREAM_TTL_MS = 5L * 60 * 1000

        const val STREAM_CACHE_MAX = 64
    }
}
