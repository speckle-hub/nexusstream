package com.novastream.app.data.sfw

import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.repo.MirrorHealth
import com.novastream.app.data.repo.ScraperRace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * Aggregates every built-in [SfwSource] into the shapes the app already uses.
 *
 * TMDB stays the metadata authority: this repository only answers discovery
 * (home rows, title search) plus episode/stream resolution. Each source is
 * queried independently under hard timeouts with per-source error collection
 * and slow-mirror quarantine, so one dead site degrades to a notice while the
 * rest keep resolving. Results are short-TTL cached per source (home) and per
 * query (search).
 */
object SfwRepository {

    /** Combined home result plus a human-readable error per source that failed. */
    data class Home(val rows: List<CatalogRow>, val errors: List<String>)

    /** Search result plus a human-readable error per source that failed. */
    data class Search(val items: List<MediaItem>, val errors: List<String>)

    private const val SOURCE_TTL_MS = 10 * 60_000L
    private const val SEARCH_TTL_MS = 5 * 60_000L
    private const val SEARCH_CACHE_MAX = 32
    private const val SEARCH_TIMEOUT_MS = 8_000L
    private const val HOME_TIMEOUT_MS = 5_000L
    private const val DETAIL_TIMEOUT_MS = 8_000L
    private const val STREAM_TIMEOUT_MS = 5_000L
    private const val SEARCH_PARALLEL = 8
    private const val MIN_QUERY_CHARS = 2
    private const val RETRY_DELAY_MS = 300L
    private const val DETAIL_TTL_MS = 10 * 60_000L
    private const val DETAIL_CACHE_MAX = 48

    private val mirrorHealth = MirrorHealth()
    private val searchSemaphore = Semaphore(SEARCH_PARALLEL)

    private class SourceResult(val rows: List<CatalogRow>, val error: String?)

    private val sourceLock = Any()
    private val sourceCache = HashMap<String, Pair<Long, SourceResult>>()
    private val searchLock = Any()
    private val searchCache =
        object : LinkedHashMap<String, Pair<Long, Search>>(8, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, Search>>) =
                size > SEARCH_CACHE_MAX
        }
    private val detailLock = Any()
    private val detailCache =
        object : LinkedHashMap<String, Pair<Long, SfwDetail>>(8, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, SfwDetail>>) =
                size > DETAIL_CACHE_MAX
        }

    /**
     * Home rows for [section], emitted progressively in completion order via
     * [onUpdate]. Each source is short-TTL cached individually and races under
     * [HOME_TIMEOUT_MS]; quarantined mirrors are skipped.
     */
    suspend fun homeRows(
        section: MediaType,
        onUpdate: (suspend (rows: List<CatalogRow>, errors: List<String>) -> Unit)? = null,
    ): Home = coroutineScope {
        val sources = SfwSources.all.filterNot { it.moviesOnly && section == MediaType.SERIES }
        val results = LinkedHashMap<String, SourceResult>()
        val mutex = Mutex()

        for (source in sources) {
            cachedSource(source.id, section)?.let { results[source.id] = it }
        }
        val stale = sources.filter { results[it.id] == null && !mirrorHealth.isQuarantined(it.id) }
        if (stale.isEmpty()) {
            val home = snapshot(sources, results)
            onUpdate?.invoke(home.rows, home.errors)
            return@coroutineScope home
        }
        if (results.isNotEmpty()) {
            val partial = snapshot(sources, results)
            onUpdate?.invoke(partial.rows, partial.errors)
        }
        stale.map { source ->
            launch {
                val result = ScraperRace.raceResult(HOME_TIMEOUT_MS) { source.home() }
                    .fold(
                        onSuccess = { rows ->
                            mirrorHealth.recordSuccess(source.id)
                            SourceResult(
                                rows.filter { it.type == section }
                                    .map { it.toCatalogRow(section) },
                                null,
                            )
                        },
                        onFailure = {
                            mirrorHealth.recordFailure(source.id)
                            SourceResult(emptyList(), "${source.name}: ${it.message ?: "unavailable"}")
                        },
                    )
                storeSource(source.id to section.id, result)
                val partial = mutex.withLock {
                    results[source.id] = result
                    snapshot(sources, results)
                }
                runCatching { onUpdate?.invoke(partial.rows, partial.errors) }
            }
        }.joinAll()
        snapshot(sources, results)
    }

    private fun snapshot(sources: List<SfwSource>, results: Map<String, SourceResult>): Home {
        val rows = ArrayList<CatalogRow>()
        val errors = ArrayList<String>()
        for (source in sources) {
            val result = results[source.id] ?: continue
            rows += result.rows
            result.error?.let { errors += it }
        }
        return Home(rows, errors)
    }

    private fun cachedSource(id: String, section: MediaType): SourceResult? = synchronized(sourceLock) {
        sourceCache["$id:${section.id}"]?.takeIf { now() - it.first < SOURCE_TTL_MS }?.second
    }

    private fun storeSource(key: Pair<String, String>, result: SourceResult) = synchronized(sourceLock) {
        sourceCache["${key.first}:${key.second}"] = now() to result
    }

    /** Title search merged across all built-in sources (use [searchTitles] for items only). */
    suspend fun search(query: String, type: MediaType? = null): Search =
        searchProgressive(query, type) { _, _ -> }

    /**
     * Progressive title search in completion order: [onUpdate] fires after each
     * source lands so the UI paints immediately instead of waiting on the slowest
     * site. Results are relevance-ranked with an unfiltered fallback (a site can
     * legitimately return synonym-only matches).
     */
    suspend fun searchProgressive(
        query: String,
        type: MediaType? = null,
        onUpdate: suspend (items: List<MediaItem>, errors: List<String>) -> Unit,
    ): Search = coroutineScope {
        val clean = query.trim()
        if (clean.length < MIN_QUERY_CHARS) return@coroutineScope Search(emptyList(), emptyList())
        val cacheKey = "${type?.id ?: "all"}:${clean.lowercase()}"
        cachedSearch(cacheKey)?.let {
            onUpdate(it.items, it.errors)
            return@coroutineScope it
        }
        val tokens = sfwTokens(clean)
        val titles = ArrayList<MediaItem>()
        val errors = ArrayList<String>()
        val mutex = Mutex()
        SfwSources.all
            .filter { type == null || !it.moviesOnly || type == MediaType.MOVIE }
            .filterNot { mirrorHealth.isQuarantined(it.id) }
            .map { source ->
                launch {
                    val result = searchOne(source, clean)
                    val partial = mutex.withLock {
                        result
                            .onSuccess { found ->
                                titles += found
                                    .filter { type == null || it.kind == type }
                                    .map { it.toMediaItem(source.name) }
                            }
                            .onFailure {
                                mirrorHealth.recordFailure(source.id)
                                errors += "${source.name}: ${it.message ?: "unavailable"}"
                            }
                        if (result.isSuccess) mirrorHealth.recordSuccess(source.id)
                        rankSearch(titles.toList(), errors.toList(), tokens)
                    }
                    runCatching { onUpdate(partial.items, partial.errors) }
                }
            }.joinAll()
        val out = rankSearch(titles, errors, tokens)
        if (out.items.isNotEmpty()) storeSearch(cacheKey, out)
        out
    }

    private fun rankSearch(titles: List<MediaItem>, errors: List<String>, tokens: List<String>): Search {
        val deduped = titles.distinctBy { it.key }
        val relevant = deduped.filter { sfwTitleMatches(it.title, tokens) }
        val ranked = (relevant.ifEmpty { deduped })
            .sortedByDescending { sfwRelevance(it.title, tokens) }
        return Search(ranked, errors.distinct())
    }

    private suspend fun searchOne(source: SfwSource, query: String): Result<List<SfwTitle>> {
        val first = ScraperRace.raceResult(SEARCH_TIMEOUT_MS) {
            searchSemaphore.withPermit { source.search(query) }
        }
        if (first.isSuccess || first.exceptionOrNull() is TimeoutCancellationException) return first
        delay(RETRY_DELAY_MS)
        return ScraperRace.raceResult(SEARCH_TIMEOUT_MS) {
            searchSemaphore.withPermit { source.search(query) }
        }
    }

    private fun cachedSearch(key: String): Search? = synchronized(searchLock) {
        searchCache[key]?.takeIf { now() - it.first < SEARCH_TTL_MS }?.second
    }

    private fun storeSearch(key: String, value: Search) = synchronized(searchLock) {
        searchCache[key] = now() to value
    }

    /** Cached detail for a built-in title page URL. */
    suspend fun detail(url: String, source: SfwSource): SfwDetail {
        cachedDetail(url)?.let { return it }
        val fresh = withTimeout(DETAIL_TIMEOUT_MS) { source.detail(url) }
        synchronized(detailLock) { detailCache[url] = now() to fresh }
        return fresh
    }

    /**
     * Streams for a built-in title page URL (movies resolve here directly),
     * keeping only links that actually respond — so Play lights up on live ones.
     */
    suspend fun streams(url: String, source: SfwSource): List<StreamSource> {
        val detail = try {
            detail(url, source)
        } catch (e: TimeoutCancellationException) {
            return emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        return verify(detail.streams)
    }

    /** Streams for an episode page the detail page already picked. */
    suspend fun streamsForEpisode(episode: SfwEpisode, source: SfwSource): List<StreamSource> {
        val raw = try {
            withTimeout(STREAM_TIMEOUT_MS) { source.streamsForEpisode(episode) }
        } catch (e: TimeoutCancellationException) {
            return emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        return verify(raw)
    }

    /**
     * Streams for a TMDB/regular title by name: search every source, keep the
     * strict same-title match, then resolve episodes (series) or detail streams
     * (movies). TMDB-id resolvers (Vidsrc family) run first when [tmdbId] is known
     * since they need no title matching at all.
     */
    suspend fun streamsForTitle(
        title: String,
        year: String?,
        season: Int?,
        episode: Int?,
        tmdbId: String?,
    ): List<StreamSource> = coroutineScope {
        val out = ArrayList<StreamSource>()
        val errors = ArrayList<String>()
        // TMDB-keyed resolvers: exact, no search needed.
        if (!tmdbId.isNullOrBlank()) {
            SfwSources.all.map { source ->
                async {
                    ScraperRace.raceResult(STREAM_TIMEOUT_MS) {
                        source.streamsForTmdb(tmdbId, season, episode)
                    }.getOrDefault(emptyList())
                }
            }.flatMap { it.await() }.let { out += it }
            if (out.isNotEmpty()) return@coroutineScope verify(out)
        }
        // Title-keyed sources: search → strict match → episodes/detail.
        val jobs = SfwSources.all
            .filter { !it.moviesOnly || season == null }
            .filterNot { mirrorHealth.isQuarantined(it.id) }
            .map { source ->
                async {
                    runCatching {
                        withTimeout(DETAIL_TIMEOUT_MS) { resolveByTitle(source, title, year, season, episode) }
                    }
                }
            }
        for (job in jobs) {
            job.await()
                .onSuccess { out += it }
                .onFailure { errors += it.message ?: "unavailable" }
        }
        verify(out)
    }

    private suspend fun resolveByTitle(
        source: SfwSource,
        title: String,
        year: String?,
        season: Int?,
        episode: Int?,
    ): List<StreamSource> {
        val hits = searchOne(source, title).getOrDefault(emptyList())
        val best = hits
            .filter { sfwSameTitle(title, it.title) }
            .filter { year.isNullOrBlank() || it.year.isNullOrBlank() || it.year == year }
            .maxByOrNull { sfwRelevance(it.title, sfwTokens(title)) }
            ?: return emptyList()
        val detail = detail(best.url, source)
        if (season == null || episode == null) return detail.streams
        // Series: pick the matching season/episode, fall back to episode 1 shape.
        val target = detail.episodes.firstOrNull { it.season == season && it.episode == episode }
            ?: detail.episodes.firstOrNull { it.episode == episode }
            ?: detail.episodes.firstOrNull { it.episode == 1 }
            ?: return emptyList()
        return source.streamsForEpisode(target)
    }

    /** Drop only the streams that fail with a definitive server rejection; keep order. */
    internal suspend fun verify(streams: List<StreamSource>): List<StreamSource> {
        if (streams.isEmpty()) return streams
        return ScraperRace.raceResult(ScraperRace.PROVIDER_TIMEOUT_MS) {
            AdultHttp.verifyPlayable(streams)
        }.getOrDefault(streams)
    }

    private fun cachedDetail(url: String): SfwDetail? = synchronized(detailLock) {
        detailCache[url]?.takeIf { now() - it.first < DETAIL_TTL_MS }?.second
    }

    private fun now() = System.currentTimeMillis()

    private fun SfwRow.toCatalogRow(section: MediaType): CatalogRow = CatalogRow(
        title = title,
        items = titles.map {
            val source = SfwSources.byId(it.sourceId)
            it.toMediaItem(source?.name ?: it.sourceId)
        },
        addonId = sourceId,
        catalogId = null,
        type = section,
    )
}
