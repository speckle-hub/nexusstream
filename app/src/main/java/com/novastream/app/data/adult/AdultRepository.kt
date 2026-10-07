package com.novastream.app.data.adult

import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * Aggregates every built-in [AdultSource] into the app's normal content shapes.
 *
 * Each source is queried independently and failures are collected instead of thrown, so one site
 * being down (or Cloudflare-blocked) degrades to a visible notice while the rest of Real 18+ keeps
 * working. All metadata and streams here come from the adult sites themselves — no TMDB/AniList.
 *
 * Results are short-TTL cached: the home tab used to re-fetch ~1 MB of HTML per source on every
 * pull-to-refresh and back-navigation, and the detail page was fetched **twice** (once for
 * metadata, once for streams).
 */
object AdultRepository {

    /** Combined home result plus a human-readable error per source that failed. */
    data class Home(val rows: List<CatalogRow>, val errors: List<String>)

    /**
     * Search result plus a human-readable error per source that failed. Returning the errors lets
     * the UI explain *why* an empty search is empty (all sources blocked/offline) instead of
     * showing a bare "No results".
     */
    data class Search(val items: List<MediaItem>, val errors: List<String>)

    /** How long a source's freshly scraped rows stay good for a quick tab switch / re-entry. */
    private const val SOURCE_TTL_MS = 10 * 60_000L

    /** Searches are cached by query so re-typing or returning to the tab is instant. */
    private const val SEARCH_TTL_MS = 5 * 60_000L
    private const val SEARCH_CACHE_MAX = 32

    /**
     * Per-source search ceiling. These sites are intermittently rate-limited/hung; without a cap a
     * single unresponsive source would hold up the whole search (the old code waited on every job).
     */
    private const val SEARCH_TIMEOUT_MS = 8_000L

    /**
     * Cap the number of sources queried at once. Twelve simultaneous scrapes invite rate-limiting
     * on shared hosts, and the cap costs nothing now that results are emitted progressively.
     */
    private const val SEARCH_PARALLEL = 10
    private val searchSemaphore = Semaphore(SEARCH_PARALLEL)

    /** Detail pages are heavy (~1 MB each) and users back-navigate constantly. */
    private const val DETAIL_TTL_MS = 10 * 60_000L
    private const val DETAIL_CACHE_MAX = 32

    /** One source's cached home rows (or the error that replaced them). */
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
        object : LinkedHashMap<String, Pair<Long, AdultDetail>>(8, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, AdultDetail>>) =
                size > DETAIL_CACHE_MAX
        }

    /**
     * Home rows, emitted **progressively**: [onUpdate] is invoked every time another source
     * finishes, so the first rows paint immediately instead of waiting for the slowest site.
     *
     * Each source is short-TTL cached **individually**, so re-entering the tab serves cached
     * sources instantly and only refetches the stale ones. Results are always emitted in the
     * source registry's order, regardless of which site answers first.
     */
    suspend fun home(
        onUpdate: (suspend (rows: List<CatalogRow>, errors: List<String>) -> Unit)? = null,
    ): Home = coroutineScope {
        val sources = AdultSources.all
        val results = LinkedHashMap<String, SourceResult>()

        for (source in sources) {
            cachedSource(source.id)?.let { results[source.id] = it }
        }

        val stale = sources.filter { results[it.id] == null }
        if (stale.isEmpty()) {
            val home = snapshot(sources, results)
            onUpdate?.invoke(home.rows, home.errors)
            return@coroutineScope home
        }

        // Some sources were cached: show them immediately while the rest load.
        if (results.isNotEmpty()) {
            val partial = snapshot(sources, results)
            onUpdate?.invoke(partial.rows, partial.errors)
        }

        val jobs = stale.map { source -> source to async { runCatching { source.home() } } }
        for ((source, job) in jobs) {
            val result = job.await().fold(
                onSuccess = { adultRows -> SourceResult(adultRows.map { it.toCatalogRow(source) }, null) },
                onFailure = { SourceResult(emptyList(), "${source.name}: ${it.message ?: "unavailable"}") },
            )
            storeSource(source.id, result)
            results[source.id] = result
            val partial = snapshot(sources, results)
            // Progressive paint: publish after *each* source, not only once all are done.
            onUpdate?.invoke(partial.rows, partial.errors)
        }

        snapshot(sources, results)
    }

    /** Merges cached/fresh per-source results back into registry order. */
    private fun snapshot(
        sources: List<AdultSource>,
        results: Map<String, SourceResult>,
    ): Home {
        val rows = ArrayList<CatalogRow>()
        val errors = ArrayList<String>()
        for (source in sources) {
            val result = results[source.id] ?: continue
            rows += result.rows
            result.error?.let { errors += it }
        }
        return Home(rows, errors)
    }

    private fun cachedSource(id: String): SourceResult? = synchronized(sourceLock) {
        sourceCache[id]?.takeIf { now() - it.first < SOURCE_TTL_MS }?.second
    }

    private fun storeSource(id: String, result: SourceResult) = synchronized(sourceLock) {
        sourceCache[id] = now() to result
    }

    /** Convenience wrapper for callers that don't need the per-source failures. */
    suspend fun search(query: String): List<MediaItem> = searchDetailed(query).items

    /**
     * Query every built-in source concurrently and merge the results.
     *
     * A cached result for [query] is served immediately. Otherwise each source is queried with a
     * hard [SEARCH_TIMEOUT_MS] ceiling, retried **once** on a transient failure (timeout / block /
     * 5xx) and its failure is recorded rather than swallowed, so a single dead site degrades to a
     * notice while the others still return results. Results are then filtered and ranked against
     * the query keywords: only titles matching at least one keyword are shown, most-relevant first.
     * If that filter would empty an otherwise non-empty list (a site can legitimately return
     * synonym-only matches) the unfiltered set is used instead, so search never returns nothing
     * while real results exist.
     */
    suspend fun searchDetailed(query: String): Search = searchDetailedProgressive(query) { _, _ -> }

    /**
     * Progressive variant of [searchDetailed]: [onUpdate] fires after **each** source finishes, so
     * a slow/hung site no longer holds the whole search hostage — the UI paints the first results
     * immediately and fills in the rest as they land. The complete, ranked list is still returned.
     */
    suspend fun searchDetailedProgressive(
        query: String,
        onUpdate: suspend (items: List<MediaItem>, errors: List<String>) -> Unit,
    ): Search = coroutineScope {
        val clean = query.trim()
        if (clean.length < MIN_QUERY_CHARS) return@coroutineScope Search(emptyList(), emptyList())
        val cacheKey = clean.lowercase()
        cachedSearch(cacheKey)?.let {
            onUpdate(it.items, it.errors)
            return@coroutineScope it
        }
        val tokens = adultQueryTokens(clean)

        val jobs = AdultSources.all.map { source -> source to async { searchOne(source, clean) } }
        val videos = ArrayList<MediaItem>()
        val errors = ArrayList<String>()
        for ((source, job) in jobs) {
            job.await()
                .onSuccess { found -> videos += found.map { it.toMediaItem(source.name) } }
                .onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
            // Progressive paint: publish after *each* source, not only once all are done.
            val partial = rankSearch(videos, errors, tokens)
            onUpdate(partial.items, partial.errors)
        }

        val out = rankSearch(videos, errors, tokens)
        if (out.items.isNotEmpty()) storeSearch(cacheKey, out)
        out
    }

    /** De-dupes, relevance-filters (with unfiltered fallback) and ranks by keyword score. */
    private fun rankSearch(
        videos: List<MediaItem>,
        errors: List<String>,
        tokens: List<String>,
    ): Search {
        val deduped = videos.distinctBy { it.key }
        val relevant = deduped.filter { adultTitleMatches(it.title, tokens) }
        val ranked = (relevant.ifEmpty { deduped })
            .sortedByDescending { adultRelevanceScore(it.title, tokens) }
        return Search(ranked, errors.distinct())
    }

    /**
     * One source's search, bounded by a timeout and retried once on a *transient, non-timeout*
     * failure. A timeout is returned as-is: retrying a hung host just doubles the wait, and the
     * progressive UI has already surfaced the other sources' results.
     */
    private suspend fun searchOne(source: AdultSource, query: String): Result<List<AdultVideo>> {
        val first = runCatching {
            searchSemaphore.withPermit { withTimeout(SEARCH_TIMEOUT_MS) { source.search(query) } }
        }
        if (first.isSuccess || first.exceptionOrNull() is TimeoutCancellationException) return first
        delay(RETRY_DELAY_MS)
        return runCatching {
            searchSemaphore.withPermit { withTimeout(SEARCH_TIMEOUT_MS) { source.search(query) } }
        }
    }

    private fun cachedSearch(key: String): Search? = synchronized(searchLock) {
        searchCache[key]?.takeIf { now() - it.first < SEARCH_TTL_MS }?.second
    }

    private fun storeSearch(key: String, value: Search) = synchronized(searchLock) {
        searchCache[key] = now() to value
    }

    /** Queries shorter than this are never sent to the sites. */
    private const val MIN_QUERY_CHARS = 2

    /** Pause before the single retry, giving a rate-limited site time to recover. */
    private const val RETRY_DELAY_MS = 300L

    suspend fun categories(): List<Pair<AdultSource, AdultCategory>> = coroutineScope {
        AdultSources.all
            .map { source -> source to async { runCatching { source.categories() }.getOrDefault(emptyList()) } }
            .flatMap { (source, job) -> job.await().map { source to it } }
    }

    suspend fun detail(url: String, source: AdultSource): AdultDetail {
        cachedDetail(url)?.let { return it }
        val fresh = source.detail(url)
        synchronized(detailLock) {
            detailCache[url] = now() to fresh
        }
        return fresh
    }

    /**
     * Streams for a detail page, keeping only links that actually respond.
     *
     * Reuses the cached detail (no second HTML download) and drops every stream the source site
     * rejects outright, so the UI can light up Play only when something real will play.
     */
    suspend fun streams(url: String, source: AdultSource): List<StreamSource> {
        val detail = runCatching { detail(url, source) }.getOrNull() ?: return emptyList()
        val streams = detail.streams
        if (streams.isEmpty()) return streams
        return runCatching { AdultHttp.verifyPlayable(streams) }.getOrDefault(streams)
    }

    private fun cachedDetail(url: String): AdultDetail? = synchronized(detailLock) {
        detailCache[url]?.takeIf { now() - it.first < DETAIL_TTL_MS }?.second
    }

    private fun now() = System.currentTimeMillis()

    private fun AdultRow.toCatalogRow(source: AdultSource): CatalogRow = CatalogRow(
        title = title,
        items = videos.map { it.toMediaItem(source.name) },
        addonId = source.id,
        catalogId = null,
        type = MediaType.REAL,
    )
}
