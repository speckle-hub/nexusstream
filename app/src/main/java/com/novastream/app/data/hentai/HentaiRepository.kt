package com.novastream.app.data.hentai

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.adult.adultQueryTokens
import com.novastream.app.data.adult.adultRelevanceScore
import com.novastream.app.data.adult.adultTitlesMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

/**
 * Aggregates every built-in [HentaiSource] into the shapes the app already uses.
 *
 * The app gets NSFW-anime *metadata* from AniList, so this repository only answers the two
 * questions the add-ons used to be needed for:
 *
 *  - [episodes]  — which episodes exist for this title (used to populate the detail page)
 *  - [streams]   — where does a given episode actually play
 *
 * Sources are queried concurrently and every failure is collected instead of thrown, so one dead
 * or Cloudflare-blocked site degrades to a notice while the rest keep resolving. Results are
 * short-TTL cached because both the detail page and the stream panel hit the same title within a
 * few seconds of each other.
 */
object HentaiRepository {

    /** How long a resolved episode list / stream list stays good for a back-navigation. */
    private const val TTL_MS = 10 * 60_000L
    private const val MAX_CACHE = 48

    /**
     * Hard bounds. These sites sit behind Cloudflare and their fetches (Http: 20 s connect /
     * 30 s read, plus 429 retries) used to be awaited with no timeout at all, so one hung host
     * held the detail page open for over a minute. Every source now gets [SOURCE_TIMEOUT_MS],
     * the whole episode lookup a [TOTAL_BUDGET_MS] budget across all query variants, and a stream
     * resolution [STREAM_TIMEOUT_MS] — a timeout degrades to "this source has nothing", which is
     * exactly how a dead site is already treated.
     */
    private const val SOURCE_TIMEOUT_MS = 5_000L
    private const val TOTAL_BUDGET_MS = 8_000L
    private const val STREAM_TIMEOUT_MS = 12_000L

    private val lock = Any()
    private val episodeCache =
        object : LinkedHashMap<String, Pair<Long, HentaiEpisodes>>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, HentaiEpisodes>>) =
                size > MAX_CACHE
        }
    private val streamCache =
        object : LinkedHashMap<String, Pair<Long, HentaiStreams>>(16, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, HentaiStreams>>) =
                size > MAX_CACHE
        }

    /**
     * Episodes for a title, best-scoring series first.
     *
     * Every source is queried with each [queryVariants] entry until one variant produces a group
     * of episodes whose title actually matches the query — that retry exists because several of
     * these sites' search endpoints silently return nothing for punctuation.
     */
    suspend fun episodes(title: String): HentaiEpisodes {
        val key = title.trim().lowercase()
        cached(key)?.let { return it }

        val errors = ArrayList<String>()
        var best: List<HentaiEpisode> = emptyList()
        var bestRel = -1
        var bestDelta = Int.MAX_VALUE
        val start = System.currentTimeMillis()

        for (query in queryVariants(title)) {
            // Stop stacking query variants once the total budget is spent: the detail page must
            // never wait a minute for an episode list that may not exist.
            val remaining = TOTAL_BUDGET_MS - (System.currentTimeMillis() - start)
            if (remaining <= 0) break
            val tokens = adultQueryTokens(query)
            val results = coroutineScope {
                HentaiSources.all
                    .map { source -> async { source to boundedSearch(source, query, remaining) } }
                    .awaitAll()
            }
            for ((source, result) in results) {
                result.onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
            }
            val hits = results.filter { it.second.isSuccess }.flatMap { it.second.getOrDefault(emptyList()) }
            if (hits.isEmpty()) continue

            for (group in groupBySeries(hits)) {
                val head = group.first()
                val candidate = "${head.series} ${head.title}"
                // A hit is only *this title* when a significant word of the query appears in it.
                // Site search endpoints happily return popular/unrelated posts, and the old
                // any-token > 0 test let those through ("to" matches "Netokano"), which is how
                // the detail page could open a completely different series than the one clicked.
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
        val out = HentaiEpisodes(episodes, errors.distinct())
        // A failed lookup isn't worth pinning for ten minutes — the next open should retry it.
        if (episodes.isNotEmpty() || errors.isEmpty()) store(key, out)
        return out
    }

    /** Streams for a title + episode number, resolved through the best matching source. */
    suspend fun streams(title: String, episodeNumber: Int?): HentaiStreams {
        val found = episodes(title)
        val target = pickEpisode(found.episodes, episodeNumber)
            ?: return HentaiStreams(emptyList(), found.errors)
        val resolved = streams(target)
        return HentaiStreams(resolved.streams, (found.errors + resolved.errors).distinct())
    }

    /** Streams for an episode page the detail page already picked (no search needed). */
    suspend fun streams(episode: HentaiEpisode): HentaiStreams {
        cachedStreams(episode.url)?.let { return it }
        val source = HentaiSources.byId(episode.sourceId)
            ?: return HentaiStreams(emptyList(), listOf("Unknown built-in source: ${episode.sourceId}"))

        val result = bounded(STREAM_TIMEOUT_MS) { source.streams(episode) }
        val raw = result.getOrElse { emptyList() }
        // Pre-flight the links exactly like Real 18+ does, so Play only lights up on live ones.
        val verified = if (raw.isEmpty()) raw
        else runCatching { AdultHttp.verifyPlayable(raw) }.getOrDefault(raw)
        val errors = result.exceptionOrNull()
            ?.let { listOf("${source.name}: ${it.message ?: "unavailable"}") }
            .orEmpty()
        val out = HentaiStreams(verified, errors)
        if (verified.isNotEmpty() || errors.isEmpty()) storeStreams(episode.url, out)
        return out
    }

    // ---- Bounded execution ---------------------------------------------------

    /**
     * Runs [block] under [timeoutMs]. A timeout degrades to an ordinary failure — the source is
     * then treated exactly like a dead site — while cancellation of the *caller* (navigating
     * away, a superseding search) is rethrown. Plain `runCatching` would swallow that too, which
     * is how cancelled lookups kept running in the background (ISSUE-10).
     */
    private suspend fun <T> bounded(timeoutMs: Long, block: suspend () -> T): Result<T> = try {
        Result.success(withTimeout(timeoutMs.coerceAtLeast(1)) { block() })
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** One source's search under the per-source cap, never past the remaining lookup budget. */
    private suspend fun boundedSearch(
        source: HentaiSource,
        query: String,
        budgetMs: Long,
    ): Result<List<HentaiEpisode>> = bounded(minOf(SOURCE_TIMEOUT_MS, budgetMs)) { source.search(query) }

    // ---- Selection helpers (internal so the tests can pin them) ----

    /**
     * Query retries for a title: the title as written, then with punctuation removed (several of
     * these sites' search endpoints drop every result when a `:` or `!` is in the query), then
     * the significant keywords alone.
     */
    internal fun queryVariants(title: String): List<String> {
        val clean = title.trim().replace(Regex("""\s+"""), " ")
        val stripped = clean
            .replace(Regex("""[^\p{L}\p{N}\s]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val tokens = adultQueryTokens(clean).joinToString(" ")
        return listOf(clean, stripped, tokens).filter { it.isNotBlank() }.distinct().take(3)
    }

    /** Splits mixed hits into one group per series, ordered by episode number. */
    internal fun groupBySeries(episodes: List<HentaiEpisode>): List<List<HentaiEpisode>> =
        episodes.groupBy { it.series.trim().lowercase() }
            .values
            .map { group -> group.sortedBy { it.episode ?: Int.MAX_VALUE } }

    /**
     * The episode to play for [wanted]:
     *  - `null` -> episode 1 when the site numbered it, otherwise the first entry
     *  - exact number -> that episode
     *  - a lone episode on the site -> that one (single-episode OVAs are often mis-numbered)
     *  - otherwise nothing, because playing a different episode than the user picked is worse
     *    than falling back to the Stremio add-ons
     */
    internal fun pickEpisode(episodes: List<HentaiEpisode>, wanted: Int?): HentaiEpisode? {
        if (episodes.isEmpty()) return null
        if (wanted == null) return episodes.firstOrNull { it.episode == 1 } ?: episodes.first()
        episodes.firstOrNull { it.episode == wanted }?.let { return it }
        return episodes.singleOrNull()
    }

    // ---- Cache ----

    private fun cached(key: String): HentaiEpisodes? = synchronized(lock) {
        episodeCache[key]?.takeIf { now() - it.first < TTL_MS }?.second
    }

    private fun store(key: String, value: HentaiEpisodes) = synchronized(lock) {
        episodeCache[key] = now() to value
    }

    private fun cachedStreams(key: String): HentaiStreams? = synchronized(lock) {
        streamCache[key]?.takeIf { now() - it.first < TTL_MS }?.second
    }

    private fun storeStreams(key: String, value: HentaiStreams) = synchronized(lock) {
        streamCache[key] = now() to value
    }

    private fun now() = System.currentTimeMillis()

    /** Upper bound so a long-running series can't flood the episode list. */
    private const val MAX_EPISODES = 60
}
