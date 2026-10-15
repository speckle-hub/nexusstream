package com.novastream.app.data.sfw

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultQueryTokens
import com.novastream.app.data.adult.adultRelevanceScore
import com.novastream.app.data.adult.adultTitlesMatch
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

/**
 * Aggregates every built-in [SfwAnimeSource] into the shapes the app already uses.
 *
 * Anime *metadata* stays AniList-led, so this repository only answers the two
 * questions the add-ons used to be needed for: *which episodes exist for this
 * title* ([episodes]) and *where does a given episode actually play* ([streams]).
 * Series-level catalog search ([searchTitles]) feeds the Anime section rows.
 *
 * Sources are queried concurrently under hard bounds with per-source error
 * collection, so one dead or bot-blocked site degrades to a notice while the
 * rest keep resolving. Results are short-TTL cached.
 */
object SfwAnimeRepository {

    /** Episode list plus a human-readable error per source that failed. */
    data class Episodes(val episodes: List<SfwAnimeEpisode>, val errors: List<String> = emptyList())

    /** Built-in streams plus a human-readable error per source that failed. */
    data class Streams(val streams: List<StreamSource>, val errors: List<String> = emptyList())

    private const val TTL_MS = 10 * 60_000L
    private const val MAX_CACHE = 48
    private const val SOURCE_TIMEOUT_MS = 5_000L
    private const val TOTAL_BUDGET_MS = 8_000L
    private const val STREAM_TIMEOUT_MS = 5_000L
    private const val SEARCH_TIMEOUT_MS = 8_000L
    private const val MAX_EPISODES = 200

    private val lock = Any()
    private val episodeCache =
        object : LinkedHashMap<String, Pair<Long, Episodes>>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, Episodes>>) =
                size > MAX_CACHE
        }
    private val streamCache =
        object : LinkedHashMap<String, Pair<Long, Streams>>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, Streams>>) =
                size > MAX_CACHE
        }

    /**
     * Episodes for a title, best-scoring series first. Only groups whose series
     * recognisably matches the query survive — a loose upstream search can never
     * substitute a different series (the §2s correct-title guarantee, SFW edition).
     */
    suspend fun episodes(title: String): Episodes {
        val key = title.trim().lowercase()
        cached(key)?.let { return it }

        val errors = ArrayList<String>()
        var best: List<SfwAnimeEpisode> = emptyList()
        var bestRel = -1
        var bestDelta = Int.MAX_VALUE
        val start = System.currentTimeMillis()

        for (query in queryVariants(title)) {
            val remaining = TOTAL_BUDGET_MS - (System.currentTimeMillis() - start)
            if (remaining <= 0) break
            val tokens = adultQueryTokens(query)
            val results = coroutineScope {
                SfwAnimeSources.all
                    .map { source -> async { source to boundedSearch(source, query, remaining) } }
                    .map { it.await() }
            }
            for ((source, result) in results) {
                result.onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
            }
            val hits = results.filter { it.second.isSuccess }.flatMap { it.second.getOrDefault(emptyList()) }
            if (hits.isEmpty()) continue

            for (group in groupBySeries(hits)) {
                val head = group.first()
                val candidate = "${head.series} ${head.title}"
                if (!adultTitlesMatch(query, candidate)) continue
                val rel = adultRelevanceScore(candidate, tokens)
                if (rel <= 0) continue
                val delta = abs(head.series.length - query.length)
                if (rel > bestRel || (rel == bestRel && delta < bestDelta)) {
                    bestRel = rel
                    bestDelta = delta
                    best = group
                }
            }
            if (best.isNotEmpty()) break
        }

        val episodes = best
            .distinctBy { it.url }
            .sortedBy { it.episode ?: Int.MAX_VALUE }
            .take(MAX_EPISODES)
        val out = Episodes(episodes, errors.distinct())
        if (episodes.isNotEmpty() || errors.isEmpty()) store(key, out)
        return out
    }

    /** Streams for a title + episode number, resolved through the best matching source. */
    suspend fun streams(title: String, episodeNumber: Int?): Streams {
        val found = episodes(title)
        val target = pickEpisode(found.episodes, episodeNumber)
            ?: return Streams(emptyList(), found.errors)
        val resolved = streams(target)
        return Streams(resolved.streams, (found.errors + resolved.errors).distinct())
    }

    /** Streams for an episode page the detail page already picked (no search needed). */
    suspend fun streams(episode: SfwAnimeEpisode): Streams {
        cachedStreams(episode.url)?.let { return it }
        val source = SfwAnimeSources.byId(episode.sourceId)
            ?: return Streams(emptyList(), listOf("Unknown built-in source: ${episode.sourceId}"))

        val result = bounded(STREAM_TIMEOUT_MS) { source.streams(episode) }
        val raw = result.getOrElse { emptyList() }
        // Pre-flight the links so Play only lights up on live ones.
        val verified = if (raw.isEmpty()) raw
        else runCatching {
            withTimeout(STREAM_TIMEOUT_MS) { AdultHttp.verifyPlayable(raw) }
        }.getOrDefault(raw)
        val errors = result.exceptionOrNull()
            ?.let { listOf("${source.name}: ${it.message ?: "unavailable"}") }
            .orEmpty()
        val out = Streams(verified, errors)
        if (verified.isNotEmpty() || errors.isEmpty()) storeStreams(episode.url, out)
        return out
    }

    /**
     * Series-level search merged across all anime sources (Anime section +
     * global search rows). Bounded per source with per-source error collection.
     */
    suspend fun searchTitles(query: String): Pair<List<MediaItem>, List<String>> = coroutineScope {
        val clean = query.trim()
        if (clean.length < 2) return@coroutineScope emptyList<MediaItem>() to emptyList()
        val items = ArrayList<MediaItem>()
        val errors = ArrayList<String>()
        val mutex = Mutex()
        SfwAnimeSources.all.map { source ->
            launch {
                val result = bounded(SEARCH_TIMEOUT_MS) { source.searchTitles(clean) }
                mutex.withLock {
                    result
                        .onSuccess { found ->
                            items += found.map { it.toMediaItem(source.name) }
                        }
                        .onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
                }
            }
        }.joinAll()
        val tokens = adultQueryTokens(clean)
        val deduped = items.distinctBy { it.key }
        val relevant = deduped.filter {
            adultTitlesMatch(clean, it.title) || adultQueryTokens(it.title).any { t -> t in tokens }
        }
        val ranked = (relevant.ifEmpty { deduped })
            .sortedByDescending { adultRelevanceScore(it.title, tokens) }
        ranked to errors.distinct()
    }

    // ---- Bounded execution (same contract as HentaiRepository) ----

    private suspend fun <T> bounded(timeoutMs: Long, block: suspend () -> T): Result<T> = try {
        Result.success(withTimeout(timeoutMs.coerceAtLeast(1)) { block() })
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun boundedSearch(
        source: SfwAnimeSource,
        query: String,
        budgetMs: Long,
    ): Result<List<SfwAnimeEpisode>> = bounded(minOf(SOURCE_TIMEOUT_MS, budgetMs)) { source.search(query) }

    // ---- Selection helpers (internal so the tests can pin them) ----

    internal fun queryVariants(title: String): List<String> {
        val clean = title.trim().replace(Regex("""\s+"""), " ")
        val stripped = clean
            .replace(Regex("""[^\p{L}\p{N}\s]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val tokens = adultQueryTokens(clean).joinToString(" ")
        return listOf(clean, stripped, tokens).filter { it.isNotBlank() }.distinct().take(3)
    }

    internal fun groupBySeries(episodes: List<SfwAnimeEpisode>): List<List<SfwAnimeEpisode>> =
        episodes.groupBy { it.series.trim().lowercase() }
            .values
            .map { group -> group.sortedBy { it.episode ?: Int.MAX_VALUE } }

    internal fun pickEpisode(episodes: List<SfwAnimeEpisode>, wanted: Int?): SfwAnimeEpisode? {
        if (episodes.isEmpty()) return null
        if (wanted == null) return episodes.firstOrNull { it.episode == 1 } ?: episodes.first()
        episodes.firstOrNull { it.episode == wanted }?.let { return it }
        return episodes.singleOrNull()
    }

    // ---- Cache ----

    private fun cached(key: String): Episodes? = synchronized(lock) {
        episodeCache[key]?.takeIf { now() - it.first < TTL_MS }?.second
    }

    private fun store(key: String, value: Episodes) = synchronized(lock) {
        episodeCache[key] = now() to value
    }

    private fun cachedStreams(key: String): Streams? = synchronized(lock) {
        streamCache[key]?.takeIf { now() - it.first < TTL_MS }?.second
    }

    private fun storeStreams(key: String, value: Streams) = synchronized(lock) {
        streamCache[key] = now() to value
    }

    private fun now() = System.currentTimeMillis()
}
