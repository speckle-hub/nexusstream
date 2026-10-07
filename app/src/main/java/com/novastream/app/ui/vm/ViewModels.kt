package com.novastream.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novastream.app.data.adult.AdultRepository
import com.novastream.app.data.download.DownloadStorageStats
import com.novastream.app.data.hentai.HentaiRepository
import com.novastream.app.data.model.Addon
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.CloudStreamExt
import com.novastream.app.data.model.MangaExt
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import com.novastream.app.data.model.RemoteRepo
import com.novastream.app.data.model.SeasonInfo
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.data.remote.TmdbClient
import com.novastream.app.data.repo.StreamsResult
import com.novastream.app.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Like [runCatching], but **never swallows coroutine cancellation**.
 *
 * The plain [runCatching] catches [CancellationException] too, so a cancelled search keeps running
 * to completion and can publish its now-stale results over a newer query — the bug behind search
 * "glitching" and showing a completely different result set. Here cancellation is rethrown so the
 * coroutine actually stops.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/** Watched beyond this fraction of its duration, an entry is treated as finished. */
private const val COMPLETED_THRESHOLD = 0.95f

/**
 * Total wall-clock budget for filling an NSFW-anime detail with built-in episodes. The lookup
 * itself is bounded per call (see `HentaiRepository`), but up to three title variants are tried,
 * so the page needs an overall stop — detail loading must never stack into a minute-long wait.
 */
private const val EPISODE_FILL_BUDGET_MS = 10_000L

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val rows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)

    /**
     * In-progress titles, newest first, with anything essentially finished filtered out.
     *
     * Reading entries are excluded here — they are surfaced separately through [continueReading]
     * so a manga title never lands under a Play CTA.
     */
    val continueWatching: StateFlow<List<WatchEntry>> =
        container.libraryStore.history
            .map { list ->
                list.filter { !it.item.isReadable && it.progress < COMPLETED_THRESHOLD }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * In-progress manga (normal + NSFW), newest first — powers the "Continue Reading" row.
     *
     * Both kinds of progress live in the same history list (the reader records page/total as the
     * entry's position/duration, so `progress` is the reading percentage); only the split is new.
     */
    val continueReading: StateFlow<List<WatchEntry>> =
        container.libraryStore.history
            .map { list ->
                list.filter { it.item.isReadable && it.progress < COMPLETED_THRESHOLD }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            runCatching { container.catalogRepository.homeRows() }
                .onSuccess { rows.value = it }
                .onFailure { error.value = it.message ?: "Failed to load" }
            loading.value = false
        }
    }

    /** Remove one entry from Continue Watching, clearing its persisted playback progress. */
    fun removeContinueWatching(entry: WatchEntry) {
        viewModelScope.launch { container.libraryStore.removeWatch(entry.item.key) }
    }
}

class SectionViewModel(private val container: AppContainer, private val section: MediaType) : ViewModel() {
    val rows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            runCatching { container.catalogRepository.rowsForSection(section) }
                .onSuccess { rows.value = it }
                .onFailure { error.value = it.message ?: "Failed to load" }
            loading.value = false
        }
    }
}

class MangaViewModel(private val container: AppContainer) : ViewModel() {
    val rows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val installedExt = container.addonRepository.mangaInstalled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            runCatching { container.catalogRepository.rowsForSection(MediaType.MANGA) }
                .onSuccess { rows.value = it }
                .onFailure { error.value = it.message ?: "Failed to load" }
            loading.value = false
        }
    }
}

class NsfwViewModel(private val container: AppContainer) : ViewModel() {
    val animeRows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val mangaRows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val realRows = MutableStateFlow<List<CatalogRow>>(emptyList())
    /** Built-in adult sources that are currently down/blocked (shown as a notice on Real 18+). */
    val realErrors = MutableStateFlow<List<String>>(emptyList())
    val loading = MutableStateFlow(true)
    /** Per-section flags so each tab fills in as soon as *its* data lands, not when all three do. */
    val animeLoading = MutableStateFlow(true)
    val mangaLoading = MutableStateFlow(true)
    val realLoading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)

    val animeResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val mangaResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val realResults = MutableStateFlow<List<MediaItem>>(emptyList())

    /** Per-source failures from the last Real 18+ search, so an empty result can explain itself. */
    val realSearchErrors = MutableStateFlow<List<String>>(emptyList())

    /** Per-section search-in-progress flags so one section's search never affects another's UI. */
    val animeSearching = MutableStateFlow(false)
    val mangaSearching = MutableStateFlow(false)

    /** Search-in-progress flag dedicated to Real 18+ (independent of the anime/manga flag). */
    val realSearching = MutableStateFlow(false)

    /**
     * The last query each section was handed. The UI reads this to tell a *restored* query (after
     * process death, when this ViewModel was recreated empty) apart from one whose results this
     * instance still holds — the former gets re-searched once, the latter never does. Tab switches
     * keep the ViewModel alive but dispose the UI, so this is exactly the discriminator needed.
     */
    var animeSearchedFor: String? = null
        private set
    var mangaSearchedFor: String? = null
        private set
    var realSearchedFor: String? = null
        private set

    /**
     * At most one in-flight search per section. Each new keystroke cancels the previous job, so
     * a slow response from an earlier query can never overwrite a newer query's results — which
     * is what made search look "confused" and return stale/irrelevant items.
     */
    private var animeSearchJob: Job? = null
    private var mangaSearchJob: Job? = null
    private var realSearchJob: Job? = null

    /** Monotonic request ids — only the newest search per section may publish its results. */
    private var animeGeneration = 0
    private var mangaGeneration = 0
    private var realGeneration = 0

    /** Guards against overlapping NSFW-anime browse-row loads (initial refresh vs. a reload). */
    private var animeRowsLoading = false

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            animeLoading.value = true
            mangaLoading.value = true
            realLoading.value = true
            coroutineScope {
                launch { loadAnimeRows() }
                launch {
                    mangaRows.value = runCatching { container.catalogRepository.nsfwMangaRows() }
                        .getOrDefault(emptyList())
                    mangaLoading.value = false
                }
                launch {
                    // Progressive: the tab paints each source as it lands and drops its skeleton on
                    // the first rows, so one slow site no longer stalls the whole section.
                    val (real, realErrs) = runCatching {
                        container.catalogRepository.realRowsProgressive { rows, errs ->
                            realRows.value = rows
                            realErrors.value = errs
                            if (rows.isNotEmpty()) realLoading.value = false
                        }
                    }.getOrDefault(emptyList<CatalogRow>() to emptyList<String>())
                    realRows.value = real
                    realErrors.value = realErrs
                    realLoading.value = false
                }
            }
            loading.value = false
        }
    }

    /**
     * Fetches the NSFW-anime browse rows, painting AniList rows progressively as each lands.
     *
     * The current rows are only replaced when the fetch actually returned — a failed or cancelled
     * load must never wipe rows the user can already see. It previously assigned
     * `getOrDefault(emptyList())`, so a flaky/timed-out load overwrote the progressive paint and
     * left the section stuck on its "Nothing here yet" state.
     */
    private suspend fun loadAnimeRows() {
        if (animeRowsLoading) return
        animeRowsLoading = true
        try {
            val rows = runCatching {
                container.catalogRepository.nsfwAnimeRows { partial ->
                    animeRows.value = partial
                }
            }.getOrNull()
            if (rows != null) animeRows.value = rows
        } finally {
            animeRowsLoading = false
            animeLoading.value = false
        }
    }

    /**
     * Reloads the NSFW-anime browse rows if they're still empty. Clearing the search drops the
     * section back to browse mode; because those rows are fetched only once (at init), a load that
     * failed back then would otherwise leave the tab on its empty state with no way back to the
     * catalogue. Triggered on clear so exiting a search always restores the browse rows.
     */
    fun reloadAnimeRowsIfEmpty() {
        if (animeRowsLoading || animeRows.value.isNotEmpty()) return
        viewModelScope.launch {
            animeLoading.value = true
            loadAnimeRows()
        }
    }

    fun searchAnime(q: String) {
        animeSearchedFor = q
        animeSearchJob?.cancel()
        val query = q.trim()
        if (query.length < MIN_SEARCH_CHARS) {
            animeGeneration++
            animeResults.value = emptyList()
            // Clearing the field cancels the in-flight job; without this the spinner would stay
            // stuck "searching" forever (the cancelled job never reaches its own reset).
            animeSearching.value = false
            // Clearing (or a too-short query) returns the section to its browse rows. If they
            // never landed, refetch now rather than leaving the tab on "Nothing here yet".
            if (query.isEmpty()) reloadAnimeRowsIfEmpty()
            return
        }
        val id = ++animeGeneration
        animeSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            animeSearching.value = true
            try {
                val found = runCatchingCancellable {
                    container.catalogRepository.search(query, MediaType.NSFW_ANIME)
                }.getOrDefault(emptyList())
                if (id != animeGeneration) return@launch
                animeResults.value = found
            } finally {
                // `finally` also runs on cancellation, so the flag is always cleared — but only
                // when this is still the newest request (a superseding job owns the flag otherwise).
                if (id == animeGeneration) animeSearching.value = false
            }
        }
    }

    fun searchManga(q: String) {
        mangaSearchedFor = q
        mangaSearchJob?.cancel()
        val query = q.trim()
        if (query.length < MIN_SEARCH_CHARS) {
            mangaGeneration++
            mangaResults.value = emptyList()
            mangaSearching.value = false
            return
        }
        val id = ++mangaGeneration
        mangaSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            mangaSearching.value = true
            try {
                val found = runCatchingCancellable {
                    container.catalogRepository.search(query, MediaType.NSFW_MANGA)
                }.getOrDefault(emptyList())
                if (id != mangaGeneration) return@launch
                mangaResults.value = found
            } finally {
                if (id == mangaGeneration) mangaSearching.value = false
            }
        }
    }

    /**
     * Real 18+ search. Debounced and cancellable: typing a word issues a single request instead
     * of one per keystroke, and only the newest query is allowed to publish results.
     */
    fun searchReal(q: String) {
        realSearchedFor = q
        realSearchJob?.cancel()
        val query = q.trim()
        if (query.length < MIN_SEARCH_CHARS) {
            realGeneration++
            realResults.value = emptyList()
            realSearchErrors.value = emptyList()
            realSearching.value = false
            return
        }
        val id = ++realGeneration
        realSearching.value = true
        realSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            // Progressive: paint each source's results as it lands so the slowest (or a hung) site
            // never holds the whole search back. Only this request's generation may publish.
            val result = runCatchingCancellable {
                container.catalogRepository.searchRealProgressive(query) { items, errors ->
                    if (id == realGeneration) {
                        realResults.value = items
                        realSearchErrors.value = errors
                    }
                }
            }.getOrElse { AdultRepository.Search(emptyList(), listOf(it.message ?: "Search failed")) }
            if (id != realGeneration) return@launch
            realResults.value = result.items
            realSearchErrors.value = result.errors
            realSearching.value = false
        }
    }

    private companion object {
        /** Keystroke debounce so typing a word issues one request, not one per letter. */
        const val SEARCH_DEBOUNCE_MS = 350L

        /** Queries shorter than this are never sent. */
        const val MIN_SEARCH_CHARS = 2
    }
}

class SearchViewModel(private val container: AppContainer) : ViewModel() {
    val results = MutableStateFlow<List<MediaItem>>(emptyList())
    val loading = MutableStateFlow(false)
    val section = MutableStateFlow<MediaType?>(null)

    /** Past searches, newest first — powers the suggestion chips on the empty state. */
    val recentSearches: StateFlow<List<String>> = container.settings.recentSearches

    private var searchJob: Job? = null

    /**
     * Monotonic id for the latest request. Only the newest one may publish, which makes results
     * correct even if a cancelled (non-cooperative) network call still finishes in the background.
     */
    private var generation = 0

    fun setSection(s: MediaType?) { section.value = s }

    fun clearRecentSearches() { viewModelScope.launch { container.settings.clearRecentSearches() } }

    /**
     * Debounced, cancellable search.
     *
     * Typing fires [search] on every keystroke; without this each letter launched its own request
     * and the responses could land out of order, so an earlier prefix overwrote the final query
     * (results for "bat" showing up under "batman"). Now every keystroke cancels the previous job,
     * waits for a short pause, and only the newest generation is allowed to publish.
     *
     * [remember] marks a deliberate search (chip tap / pre-filled query): it is recorded even when
     * it finds nothing. Plain typing records any query the user actually let run — the 350 ms
     * debounce above already collapses a typed word into a single request, so this fills the
     * recent-search list from normal typing instead of only from chip taps (which is why the
     * "Recent" section on the Search screen looked like it had disappeared).
     */
    fun search(q: String, remember: Boolean = false) {
        searchJob?.cancel()
        val query = q.trim()
        if (query.isEmpty()) {
            generation++
            results.value = emptyList()
            loading.value = false
            return
        }
        val id = ++generation
        val requestedSection = section.value
        loading.value = true
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val found = runCatchingCancellable {
                if (requestedSection == null) container.catalogRepository.globalSearch(query)
                else container.catalogRepository.search(query, requestedSection)
            }.getOrDefault(emptyList())
            // A newer keystroke (or a filter change) superseded this request; drop the result.
            if (id != generation) return@launch
            results.value = found
            loading.value = false
            if (found.isNotEmpty() || remember) container.settings.recordSearch(query)
        }
    }

    private companion object {
        /** Keystroke debounce so typing a word issues one request, not one per letter. */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    /**
     * Favourites, defensively re-validated on the way to the UI.
     *
     * [LibraryCodec][com.novastream.app.data.local.LibraryCodec] already sanitizes on read, but
     * this screen is where a malformed entry would turn into a crash (a null `type` reaching
     * `Routes.detail`), so each entry is checked individually: one bad title is dropped, the rest
     * of the list still renders.
     */
    val favorites: StateFlow<List<MediaItem>> =
        container.libraryStore.favorites
            .map { list -> list.filterSafe { it.id.isNotBlank() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Continue-watching entries whose title is still usable, de-duplicated by title key. */
    val history: StateFlow<List<WatchEntry>> =
        container.libraryStore.history
            .map { list ->
                list.filterSafe { it.item.id.isNotBlank() }
                    .distinctBy { it.item.key }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(item: MediaItem) { viewModelScope.launch { container.libraryStore.removeFavorite(item.key) } }
    fun removeWatch(key: String) { viewModelScope.launch { container.libraryStore.removeWatch(key) } }
    fun clearHistory() { viewModelScope.launch { container.libraryStore.clearHistory() } }
}

/**
 * Keeps every element for which [keep] succeeds and drops the rest — unlike a whole-list
 * `runCatching`, a single hostile entry can no longer blank an entire section of the Library.
 */
private fun <T> List<T>.filterSafe(keep: (T) -> Boolean): List<T> = mapNotNull { entry ->
    runCatching { entry.takeIf { keep(it) } }.getOrNull()
}

class DetailViewModel(private val container: AppContainer, private val item: MediaItem) : ViewModel() {
    val detail = MutableStateFlow<MetaDetail?>(null)
    val streams = MutableStateFlow<List<StreamSource>>(emptyList())
    /** Per-add-on failure reasons when a stream lookup came back empty or partial. */
    val streamErrors = MutableStateFlow<List<String>>(emptyList())
    val subtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val related = MutableStateFlow<List<MediaItem>>(emptyList())
    val chapters = MutableStateFlow<List<Video>>(emptyList())
    val loading = MutableStateFlow(true)
    val streamsLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val isFavorite = MutableStateFlow(false)
    val selectedVideo = MutableStateFlow<Video?>(null)

    // ---- Seasons & episodes (unified, expandable) ------------------------
    /** Season headers shown on the detail page. */
    val seasons = MutableStateFlow<List<SeasonInfo>>(emptyList())
    /** Currently expanded season number, or null when all are collapsed. */
    val expandedSeason = MutableStateFlow<Int?>(null)
    /** Episodes keyed by season number (pre-grouped for Stremio, lazy for TMDB). */
    val seasonEpisodes = MutableStateFlow<Map<Int, List<Video>>>(emptyMap())
    /** Season currently being fetched, or null. */
    val seasonLoading = MutableStateFlow<Int?>(null)
    /** True when episodes must be fetched lazily from TMDB per season. */
    val tmdbDrivenSeasons = MutableStateFlow(false)
    private var tmdbSeriesId: String? = null

    init {
        load()
        // Keep the heart button in sync with persisted favourites (survives restarts).
        viewModelScope.launch {
            container.libraryStore.favorites.collect { favs ->
                isFavorite.value = favs.any { it.key == item.key }
            }
        }
    }

    /**
     * Select a season. Seasons are a picker, not an accordion: the chosen one stays selected and
     * only its episodes are shown, so the page never stacks every season's episode list on top of
     * each other. For TMDB-backed series the episodes are fetched here on first selection.
     */
    fun selectSeason(season: Int) {
        expandedSeason.value = season
        if (!tmdbDrivenSeasons.value) return
        if (seasonEpisodes.value.containsKey(season)) return
        if (tmdbSeriesId == null) return
        viewModelScope.launch {
            seasonLoading.value = season
            loadSeasonEpisodes(season)
            if (seasonLoading.value == season) seasonLoading.value = null
        }
    }

    /**
     * Episodes for a season, lazily fetched from TMDB when the season hasn't been expanded yet.
     * Also used by the bulk "download season" action, which must not depend on UI state.
     */
    suspend fun loadSeasonEpisodes(season: Int): List<Video> {
        seasonEpisodes.value[season]?.takeIf { it.isNotEmpty() }?.let { return it }
        if (!tmdbDrivenSeasons.value) return seasonEpisodes.value[season].orEmpty()
        val tmdbId = tmdbSeriesId ?: return emptyList()
        val imdb = detail.value?.id?.takeIf { it.startsWith("tt") }
        val eps = runCatching { TmdbClient.seasonEpisodes(tmdbId, season, imdb) }
            .getOrDefault(emptyList())
        if (eps.isNotEmpty()) seasonEpisodes.value = seasonEpisodes.value + (season to eps)
        return eps
    }

    fun retry() { load() }

    fun load() {
        viewModelScope.launch {
            loading.value = true; error.value = null
            // The NSFW-anime episode list and its streams both take a couple of network hops, so
            // the panel must say "Finding streams…" from the very first frame instead of flashing
            // "No streams found" while they resolve.
            streamsLoading.value = true
            seasons.value = emptyList(); seasonEpisodes.value = emptyMap(); expandedSeason.value = null
            val d = runCatching { container.metadataRepository.detail(item) }.getOrNull()
            detail.value = d
            if (d != null) {
                related.value = runCatching { container.metadataRepository.related(item, d) }.getOrDefault(emptyList())
                if (item.type == MediaType.MANGA || item.type == MediaType.NSFW_MANGA) {
                    chapters.value = runCatching {
                        MangaDexClient.chapters(item.id, nsfw = item.type == MediaType.NSFW_MANGA)
                    }.getOrDefault(emptyList())
                }
                buildSeasons(d)
                // AniList ships no episode list for adult titles. The built-in hentai sources
                // know the real episodes, so they fill it in — everything else on this page
                // (poster, synopsis, cast, related) still comes from AniList.
                if (item.type == MediaType.NSFW_ANIME && d.videos.isEmpty()) fillBuiltInEpisodes()
            } else {
                error.value = "Couldn't load metadata for this title. Check your connection and try again."
            }
            loading.value = false
            // A movie resolves streams for the title itself. A series resolves them for whichever
            // episode the detail page picks (see its auto-select effect), so issuing a title-level
            // lookup here would race that coroutine and could leave Play pointing at an empty
            // "no video id" result — which is exactly how a series ended up unplayable.
            if (seasons.value.isEmpty()) {
                loadStreams(null)
            } else {
                // Season headers exist but no episodes landed yet: clear the spinner so the page
                // never sits on a permanent "Finding streams…" if the episode fetch fails.
                streamsLoading.value = false
            }
        }
    }

    /**
     * Ask the built-in hentai sources which episodes exist for this title and splice them into
     * the detail. AniList's English title is tried first, then its romaji / native / synonym
     * titles, because the source sites index the Japanese romanisation far more often than the
     * localized one.
     */
    private suspend fun fillBuiltInEpisodes() {
        val d = detail.value ?: return
        val titles = (listOf(d.name) + d.altTitles)
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        val started = System.currentTimeMillis()
        for (title in titles) {
            // Overall budget across all title variants: at some point the page must stop waiting
            // for an episode list that isn't coming.
            if (System.currentTimeMillis() - started > EPISODE_FILL_BUDGET_MS) return
            val found = runCatching { HentaiRepository.episodes(title) }.getOrNull() ?: continue
            if (found.episodes.isEmpty()) continue
            val updated = d.copy(
                videos = found.episodes.map { it.toVideo() },
                totalEpisodes = d.totalEpisodes ?: found.episodes.size,
            )
            detail.value = updated
            buildSeasons(updated)
            return
        }
    }

    /** Build season headers + episodes, preferring rich TMDB seasons when a TMDB id is resolvable. */
    private suspend fun buildSeasons(d: MetaDetail) {
        val isSeries = item.type == MediaType.SERIES || item.type == MediaType.ANIME ||
            item.type == MediaType.NSFW_ANIME || d.type == "series"
        if (!isSeries) return

        // Real 18+ must never resolve seasons via TMDB; use only the add-on's own episode list.
        val tmdbId = if (item.type == MediaType.REAL) null else when {
            d.addonId == "tmdb" && item.addonId == "tmdb" -> item.id
            d.addonId == "tmdb" && d.id.isNotEmpty() && d.id.all { it.isDigit() } -> d.id
            d.id.startsWith("tt") -> TmdbClient.findByImdb(d.id)?.takeIf { it.first == MediaType.SERIES }?.second
            item.id.startsWith("tt") -> TmdbClient.findByImdb(item.id)?.takeIf { it.first == MediaType.SERIES }?.second
            else -> null
        }

        if (tmdbId != null) {
            tmdbSeriesId = tmdbId
            tmdbDrivenSeasons.value = true
            // Episode counts from the raw TMDB seasons array (when available).
            val counts = HashMap<Int, Int>()
            d.raw?.getAsJsonArray("seasons")?.forEach { el ->
                if (el.isJsonObject) {
                    val so = el.asJsonObject
                    val sn = so.get("season_number")?.takeIf { it.isJsonPrimitive }?.asInt
                    val ec = so.get("episode_count")?.takeIf { it.isJsonPrimitive }?.asInt
                    if (sn != null && sn > 0 && ec != null) counts[sn] = ec
                }
            }
            val list = d.videos.filter { (it.season ?: 0) > 0 }
                .distinctBy { it.season }
                .map { v ->
                    val sn = v.season!!
                    SeasonInfo(sn, v.title ?: "Season $sn", v.thumbnail, counts[sn])
                }
                .ifEmpty {
                    // Fall back to the raw seasons array when the client emitted no placeholders.
                    counts.keys.sorted().map { sn -> SeasonInfo(sn, "Season $sn", null, counts[sn]) }
                }
            seasons.value = list
            list.firstOrNull()?.season?.let { selectSeason(it) }
            return
        }

        // Stremio-style: real episodes, grouped by season.
        tmdbDrivenSeasons.value = false
        val eps = d.videos
        val grouped = eps.filter { (it.season ?: 0) > 0 }.groupBy { it.season!! }
        if (grouped.isEmpty()) {
            if (eps.isNotEmpty()) {
                seasons.value = listOf(SeasonInfo(1, "Episodes", null, eps.size))
                seasonEpisodes.value = mapOf(1 to eps)
                expandedSeason.value = 1
            }
        } else {
            seasons.value = grouped.keys.sorted().map { sn -> SeasonInfo(sn, "Season $sn", null, grouped[sn]?.size) }
            seasonEpisodes.value = grouped
            expandedSeason.value = grouped.keys.minOrNull()
        }
    }

    /**
     * Resolve the stream list for [video] (null = the title itself, i.e. a movie).
     *
     * A monotonically increasing request id guards the two StateFlows: tapping episode 3 and then
     * episode 7 issues two overlapping add-on lookups, and without the guard the slower one could
     * land last and publish the wrong episode's mirrors under episode 7's name.
     */
    fun loadStreams(video: Video?) {
        selectedVideo.value = video
        val request = ++streamsRequest
        viewModelScope.launch {
            streamsLoading.value = true
            val result = runCatching { container.streamRepository.streamsForDetailed(item, video?.id) }
                .getOrDefault(StreamsResult())
            val tracks = runCatching { container.streamRepository.subtitlesFor(item, video?.id) }
                .getOrDefault(emptyList())
            if (request != streamsRequest) return@launch
            streams.value = result.streams
            streamErrors.value = result.errors
            subtitles.value = tracks
            streamsLoading.value = false
        }
    }

    /** Increments once per [loadStreams] call; only the newest request is allowed to publish. */
    private var streamsRequest = 0

    fun toggleFavorite() {
        viewModelScope.launch { container.libraryStore.toggleFavorite(item) }
    }

    fun recordWatch(video: Video?, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            container.libraryStore.recordWatch(
                WatchEntry(item, video?.id, video?.title, positionMs, durationMs)
            )
        }
    }
}

class AddonViewModel(private val container: AppContainer) : ViewModel() {
    val addons: StateFlow<List<Addon>> =
        container.addonRepository.addons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val repos: StateFlow<List<RemoteRepo>> =
        container.addonRepository.repos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cloudStreamInstalled: StateFlow<List<CloudStreamExt>> =
        container.addonRepository.cloudStreamInstalled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mangaInstalled: StateFlow<List<MangaExt>> =
        container.addonRepository.mangaInstalled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val catalog = MutableStateFlow<List<Any>>(emptyList())

    fun install(url: String) {
        viewModelScope.launch {
            busy.value = true
            container.addonRepository.install(url)
                .onSuccess { message.value = "Installed ${it.name}" }
                .onFailure { message.value = "Failed: ${it.message}" }
            busy.value = false
        }
    }

    fun setEnabled(id: String, enabled: Boolean) { viewModelScope.launch { container.addonRepository.setEnabled(id, enabled) } }
    fun remove(id: String) { viewModelScope.launch { container.addonRepository.remove(id) } }
    fun update(id: String) {
        viewModelScope.launch {
            container.addonRepository.update(id)
                .onSuccess { message.value = "Updated ${it.name}" }
                .onFailure { message.value = "Update failed" }
        }
    }
    fun updateAll() { viewModelScope.launch { container.addonRepository.updateAll(); message.value = "All addons updated" } }

    /** Refresh every extension repository index into the cache (background worker + manual sync). */
    fun syncRepos() {
        viewModelScope.launch {
            busy.value = true
            val synced = runCatching { container.addonRepository.syncAllRepos() }.getOrDefault(0)
            message.value = "Synced $synced repositories"
            busy.value = false
        }
    }

    fun addRepo(repo: RemoteRepo) { viewModelScope.launch { container.addonRepository.addRepo(repo) } }
    fun removeRepo(url: String) { viewModelScope.launch { container.addonRepository.removeRepo(url) } }

    fun browseCloudStream(url: String) {
        viewModelScope.launch {
            busy.value = true
            catalog.value = container.addonRepository.fetchCloudStream(url)
            if (catalog.value.isEmpty()) message.value = "Repository unavailable or empty"
            busy.value = false
        }
    }
    fun browseAniyomi(url: String) {
        viewModelScope.launch {
            busy.value = true
            catalog.value = container.addonRepository.fetchAniyomi(url)
            if (catalog.value.isEmpty()) message.value = "Repository unavailable or empty"
            busy.value = false
        }
    }
    fun browseKeiyoushi(url: String) {
        viewModelScope.launch {
            busy.value = true
            catalog.value = container.addonRepository.fetchKeiyoushi(url)
            if (catalog.value.isEmpty()) message.value = "Repository unavailable or empty"
            busy.value = false
        }
    }
    fun installCloudStream(ext: CloudStreamExt) { viewModelScope.launch { container.addonRepository.installCloudStream(ext); message.value = "Added ${ext.name}" } }
    fun installMangaExt(ext: MangaExt) { viewModelScope.launch { container.addonRepository.installMangaExt(ext); message.value = "Added ${ext.name}" } }
    fun uninstallCloudStream(url: String) { viewModelScope.launch { container.addonRepository.uninstallCloudStream(url) } }
    fun uninstallMangaExt(pkg: String) { viewModelScope.launch { container.addonRepository.uninstallMangaExt(pkg) } }
    fun clearMessage() { message.value = null }
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val theme = container.settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")
    val accent = container.settings.accent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "violet")
    val playerExternal = container.settings.playerExternal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val preferredQuality = container.settings.preferredQuality.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")
    val autoplay = container.settings.autoplay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val subsEnabled = container.settings.subsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val nsfwLock = container.settings.nsfwLock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val nsfwBiometric = container.settings.nsfwBiometric.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoNsfw = container.settings.autoNsfwFromAddons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val cacheMeta = container.settings.cacheMeta.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // ---- Phase 11 ----
    val dynamicColor = container.settings.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val amoledBlack = container.settings.amoledBlack.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val subtitleScale = container.settings.subtitleScale.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)
    val subtitleColor = container.settings.subtitleColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "white")
    val subtitleBg = container.settings.subtitleBg.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.4f)
    val subtitleOffset = container.settings.subtitleOffset.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.05f)
    val playerSpeed = container.settings.playerSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)
    val audioBoost = container.settings.audioBoost.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val audioNormalize = container.settings.audioNormalize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaInvert = container.settings.mangaInvert.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaGrayscale = container.settings.mangaGrayscale.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaCrop = container.settings.mangaCrop.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaDoublePage = container.settings.mangaDoublePage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaZoomLock = container.settings.mangaZoomLock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val mangaVolumeKeys = container.settings.mangaVolumeKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val mangaWarm = container.settings.mangaWarm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val subtitleSyncMs = container.settings.subtitleSyncMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- Phase 12 ----
    val incognito = container.settings.incognito.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val flagSecure = container.settings.flagSecure.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val appLock = container.settings.appLock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customUserAgent = container.settings.customUserAgent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val customReferer = container.settings.customReferer.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val customCookie = container.settings.customCookie.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val maxParallel = container.settings.maxParallel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val autoDownloadNext = container.settings.autoDownloadNext.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoDownloadWifiOnly = container.settings.autoDownloadWifiOnly.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoDeleteWatched = container.settings.autoDeleteWatched.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val storageUri = container.settings.storageUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val updateRepo = container.settings.updateRepo.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val notifyNewContent = container.settings.notifyNewContent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleTrakt = container.settings.scrobbleTrakt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleAniList = container.settings.scrobbleAniList.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleMal = container.settings.scrobbleMal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleKitsu = container.settings.scrobbleKitsu.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleSimkl = container.settings.scrobbleSimkl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val tokenTrakt = container.settings.tokenTrakt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val tokenAniList = container.settings.tokenAniList.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val tokenMal = container.settings.tokenMal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val tokenKitsu = container.settings.tokenKitsu.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val tokenSimkl = container.settings.tokenSimkl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val cacheSize = MutableStateFlow(0L)

    /** Completed vs temporary vs orphaned download bytes (Phase 14 storage meter). */
    val downloadStats = MutableStateFlow(DownloadStorageStats())

    init {
        refreshCacheSize()
        refreshDownloadStats()
    }

    fun setTheme(v: String) = viewModelScope.launch { container.settings.setTheme(v) }
    fun setAccent(v: String) = viewModelScope.launch { container.settings.setAccent(v) }
    fun setPlayerExternal(v: Boolean) = viewModelScope.launch { container.settings.setPlayerExternal(v) }
    fun setPreferredQuality(v: String) = viewModelScope.launch { container.settings.setPreferredQuality(v) }
    fun setAutoplay(v: Boolean) = viewModelScope.launch { container.settings.setAutoplay(v) }
    fun setSubs(v: Boolean) = viewModelScope.launch { container.settings.setSubsEnabled(v) }
    fun setNsfwLock(v: Boolean) = viewModelScope.launch { container.settings.setNsfwLock(v) }
    fun setNsfwBiometric(v: Boolean) = viewModelScope.launch { container.settings.setNsfwBiometric(v) }
    fun setAutoNsfw(v: Boolean) = viewModelScope.launch { container.settings.setAutoNsfwFromAddons(v) }
    fun setCacheMeta(v: Boolean) = viewModelScope.launch { container.settings.setCacheMeta(v) }

    fun setDynamicColor(v: Boolean) = viewModelScope.launch { container.settings.setDynamicColor(v) }
    fun setAmoledBlack(v: Boolean) = viewModelScope.launch { container.settings.setAmoledBlack(v) }
    fun setSubtitleScale(v: Float) = viewModelScope.launch { container.settings.setSubtitleScale(v) }
    fun setSubtitleColor(v: String) = viewModelScope.launch { container.settings.setSubtitleColor(v) }
    fun setSubtitleBg(v: Float) = viewModelScope.launch { container.settings.setSubtitleBg(v) }
    fun setSubtitleOffset(v: Float) = viewModelScope.launch { container.settings.setSubtitleOffset(v) }
    fun setPlayerSpeed(v: Float) = viewModelScope.launch { container.settings.setPlayerSpeed(v) }
    fun setAudioBoost(v: Boolean) = viewModelScope.launch { container.settings.setAudioBoost(v) }
    fun setAudioNormalize(v: Boolean) = viewModelScope.launch { container.settings.setAudioNormalize(v) }
    fun setMangaInvert(v: Boolean) = viewModelScope.launch { container.settings.setMangaInvert(v) }
    fun setMangaGrayscale(v: Boolean) = viewModelScope.launch { container.settings.setMangaGrayscale(v) }
    fun setMangaCrop(v: Boolean) = viewModelScope.launch { container.settings.setMangaCrop(v) }
    fun setMangaDoublePage(v: Boolean) = viewModelScope.launch { container.settings.setMangaDoublePage(v) }
    fun setMangaZoomLock(v: Boolean) = viewModelScope.launch { container.settings.setMangaZoomLock(v) }
    fun setMangaVolumeKeys(v: Boolean) = viewModelScope.launch { container.settings.setMangaVolumeKeys(v) }
    fun setMangaWarm(v: Boolean) = viewModelScope.launch { container.settings.setMangaWarm(v) }
    fun setSubtitleSyncMs(v: Int) = viewModelScope.launch { container.settings.setSubtitleSyncMs(v) }

    fun setIncognito(v: Boolean) = viewModelScope.launch { container.settings.setIncognito(v) }
    fun setFlagSecure(v: Boolean) = viewModelScope.launch { container.settings.setFlagSecure(v) }
    fun setAppLock(v: Boolean) = viewModelScope.launch { container.settings.setAppLock(v) }
    fun setCustomHeaders(ua: String, referer: String, cookie: String) = viewModelScope.launch {
        container.settings.setCustomUserAgent(ua)
        container.settings.setCustomReferer(referer)
        container.settings.setCustomCookie(cookie)
    }
    fun setMaxParallel(v: Int) = viewModelScope.launch { container.settings.setMaxParallel(v) }
    fun setAutoDownloadNext(v: Boolean) = viewModelScope.launch { container.settings.setAutoDownloadNext(v) }
    fun setAutoDownloadWifiOnly(v: Boolean) = viewModelScope.launch { container.settings.setAutoDownloadWifiOnly(v) }
    fun setAutoDeleteWatched(v: Boolean) = viewModelScope.launch { container.settings.setAutoDeleteWatched(v) }
    fun setStorageUri(v: String) = viewModelScope.launch { container.settings.setStorageUri(v) }
    fun setUpdateRepo(v: String) = viewModelScope.launch { container.settings.setUpdateRepo(v) }
    fun setNotifyNewContent(v: Boolean) = viewModelScope.launch { container.settings.setNotifyNewContent(v) }
    fun setScrobbleTrakt(v: Boolean) = viewModelScope.launch { container.settings.setScrobbleTrakt(v) }
    fun setScrobbleAniList(v: Boolean) = viewModelScope.launch { container.settings.setScrobbleAniList(v) }
    fun setScrobbleMal(v: Boolean) = viewModelScope.launch { container.settings.setScrobbleMal(v) }
    fun setScrobbleKitsu(v: Boolean) = viewModelScope.launch { container.settings.setScrobbleKitsu(v) }
    fun setScrobbleSimkl(v: Boolean) = viewModelScope.launch { container.settings.setScrobbleSimkl(v) }
    fun setToken(provider: String, v: String) = viewModelScope.launch { container.settings.setToken(provider, v) }
    fun setNsfwPin(pin: String?) = viewModelScope.launch { container.settings.setNsfwPin(pin) }
    fun unlockNsfw() = viewModelScope.launch { container.settings.setNsfwUnlocked(true) }

    fun clearCache() {
        viewModelScope.launch {
            container.cache.clear()
            refreshCacheSize()
        }
    }

    /** Remove partial/failed downloads and orphaned cache chunks, then re-measure. */
    fun clearDownloadCache() {
        viewModelScope.launch {
            downloadStats.value = container.videoDownloadManager.clearTemporaryFiles()
            refreshCacheSize()
        }
    }

    fun refreshDownloadStats() {
        viewModelScope.launch {
            downloadStats.value = runCatching { container.videoDownloadManager.storageStats() }
                .getOrDefault(DownloadStorageStats())
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch { cacheSize.value = container.cache.sizeBytes() }
    }
}
