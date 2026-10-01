package com.novastream.app.data.adult

import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    /** Fresh enough to survive a quick tab switch without another network round-trip. */
    private const val HOME_TTL_MS = 60_000L

    /** Detail pages are heavy (~1 MB each) and users back-navigate constantly. */
    private const val DETAIL_TTL_MS = 10 * 60_000L
    private const val DETAIL_CACHE_MAX = 32

    private val homeMutex = Mutex()
    private var homeCache: Pair<Long, Home>? = null
    private val detailLock = Any()
    private val detailCache =
        object : LinkedHashMap<String, Pair<Long, AdultDetail>>(8, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, AdultDetail>>) =
                size > DETAIL_CACHE_MAX
        }

    suspend fun home(): Home = homeMutex.withLock {
        homeCache?.takeIf { now() - it.first < HOME_TTL_MS }?.second
            ?: buildHome().also { homeCache = now() to it }
    }

    private suspend fun buildHome(): Home = coroutineScope {
        val jobs = AdultSources.all.map { source -> source to async { runCatching { source.home() } } }
        val rows = ArrayList<CatalogRow>()
        val errors = ArrayList<String>()
        for ((source, job) in jobs) {
            job.await()
                .onSuccess { adultRows -> rows += adultRows.map { it.toCatalogRow(source) } }
                .onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
        }
        Home(rows, errors)
    }

    /** Convenience wrapper for callers that don't need the per-source failures. */
    suspend fun search(query: String): List<MediaItem> = searchDetailed(query).items

    /**
     * Query every built-in source concurrently and merge the results.
     *
     * Each source is **retried once** (timeouts, Cloudflare challenges and 5xx responses from
     * these sites are transient) and its failure is recorded rather than swallowed, so a single
     * dead site degrades to a notice while the others still return results. Results are then
     * filtered and ranked against the query keywords: only titles matching at least one keyword
     * are shown, most-relevant first. If that filter would empty an otherwise non-empty list (a
     * site can legitimately return synonym-only matches) the unfiltered set is used instead, so
     * search never returns nothing while real results exist.
     */
    suspend fun searchDetailed(query: String): Search = coroutineScope {
        val clean = query.trim()
        if (clean.length < MIN_QUERY_CHARS) return@coroutineScope Search(emptyList(), emptyList())
        val tokens = adultQueryTokens(clean)

        val jobs = AdultSources.all.map { source -> source to async { searchOne(source, clean) } }
        val videos = ArrayList<MediaItem>()
        val errors = ArrayList<String>()
        for ((source, job) in jobs) {
            job.await()
                .onSuccess { found -> videos += found.map { it.toMediaItem(source.name) } }
                .onFailure { errors += "${source.name}: ${it.message ?: "unavailable"}" }
        }

        val deduped = videos.distinctBy { it.key }
        val relevant = deduped.filter { adultTitleMatches(it.title, tokens) }
        val ranked = (relevant.ifEmpty { deduped })
            .sortedByDescending { adultRelevanceScore(it.title, tokens) }
        Search(ranked, errors)
    }

    /** One source's search, retried once on a transient failure (timeout / block / 5xx). */
    private suspend fun searchOne(source: AdultSource, query: String): Result<List<AdultVideo>> {
        val first = runCatching { source.search(query) }
        if (first.isSuccess) return first
        delay(RETRY_DELAY_MS)
        return runCatching { source.search(query) }
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
