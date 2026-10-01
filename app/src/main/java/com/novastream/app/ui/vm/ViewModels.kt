package com.novastream.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novastream.app.data.adult.AdultRepository
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

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val rows = MutableStateFlow<List<CatalogRow>>(emptyList())
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)

    /** In-progress titles, newest first, with anything essentially finished filtered out. */
    val continueWatching: StateFlow<List<WatchEntry>> =
        container.libraryStore.history
            .map { list -> list.filter { it.progress < COMPLETED_THRESHOLD } }
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

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            animeLoading.value = true
            mangaLoading.value = true
            realLoading.value = true
            coroutineScope {
                launch {
                    animeRows.value = runCatching { container.catalogRepository.nsfwAnimeRows() }
                        .getOrDefault(emptyList())
                    animeLoading.value = false
                }
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

    fun searchAnime(q: String) {
        animeSearchedFor = q
        animeSearchJob?.cancel()
        val query = q.trim()
        if (query.length < MIN_SEARCH_CHARS) {
            animeGeneration++
            animeResults.value = emptyList()
            return
        }
        val id = ++animeGeneration
        animeSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            animeSearching.value = true
            val found = runCatchingCancellable {
                container.catalogRepository.search(query, MediaType.NSFW_ANIME)
            }.getOrDefault(emptyList())
            if (id != animeGeneration) return@launch
            animeResults.value = found
            animeSearching.value = false
        }
    }

    fun searchManga(q: String) {
        mangaSearchedFor = q
        mangaSearchJob?.cancel()
        val query = q.trim()
        if (query.length < MIN_SEARCH_CHARS) {
            mangaGeneration++
            mangaResults.value = emptyList()
            return
        }
        val id = ++mangaGeneration
        mangaSearchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            mangaSearching.value = true
            val found = runCatchingCancellable {
                container.catalogRepository.search(query, MediaType.NSFW_MANGA)
            }.getOrDefault(emptyList())
            if (id != mangaGeneration) return@launch
            mangaResults.value = found
            mangaSearching.value = false
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
            val result = runCatchingCancellable { container.catalogRepository.searchReal(query) }
                .getOrElse { AdultRepository.Search(emptyList(), listOf(it.message ?: "Search failed")) }
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

    private var searchJob: Job? = null

    /**
     * Monotonic id for the latest request. Only the newest one may publish, which makes results
     * correct even if a cancelled (non-cooperative) network call still finishes in the background.
     */
    private var generation = 0

    fun setSection(s: MediaType?) { section.value = s }

    /**
     * Debounced, cancellable search.
     *
     * Typing fires [search] on every keystroke; without this each letter launched its own request
     * and the responses could land out of order, so an earlier prefix overwrote the final query
     * (results for "bat" showing up under "batman"). Now every keystroke cancels the previous job,
     * waits for a short pause, and only the newest generation is allowed to publish.
     */
    fun search(q: String) {
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
        }
    }

    private companion object {
        /** Keystroke debounce so typing a word issues one request, not one per letter. */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    val favorites: StateFlow<List<MediaItem>> =
        container.libraryStore.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history: StateFlow<List<WatchEntry>> =
        container.libraryStore.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(item: MediaItem) { viewModelScope.launch { container.libraryStore.removeFavorite(item.key) } }
    fun removeWatch(key: String) { viewModelScope.launch { container.libraryStore.removeWatch(key) } }
    fun clearHistory() { viewModelScope.launch { container.libraryStore.clearHistory() } }
}

class DetailViewModel(private val container: AppContainer, private val item: MediaItem) : ViewModel() {
    val detail = MutableStateFlow<MetaDetail?>(null)
    val streams = MutableStateFlow<List<StreamSource>>(emptyList())
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

    /** Expand/collapse a season, lazily fetching its episodes for TMDB series. */
    fun toggleSeason(season: Int) {
        if (expandedSeason.value == season) {
            expandedSeason.value = null
            return
        }
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
            } else {
                error.value = "Couldn't load metadata for this title. Check your connection and try again."
            }
            loading.value = false
            loadStreams(null)
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
            list.firstOrNull()?.season?.let { toggleSeason(it) }
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

    fun loadStreams(video: Video?) {
        selectedVideo.value = video
        viewModelScope.launch {
            streamsLoading.value = true
            streams.value = runCatching { container.streamRepository.streamsFor(item, video?.id) }.getOrDefault(emptyList())
            subtitles.value = runCatching { container.streamRepository.subtitlesFor(item, video?.id) }.getOrDefault(emptyList())
            streamsLoading.value = false
        }
    }

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
            busy.value = false
        }
    }
    fun browseAniyomi(url: String) {
        viewModelScope.launch {
            busy.value = true
            catalog.value = container.addonRepository.fetchAniyomi(url)
            busy.value = false
        }
    }
    fun browseKeiyoushi(url: String) {
        viewModelScope.launch {
            busy.value = true
            catalog.value = container.addonRepository.fetchKeiyoushi(url)
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

    val cacheSize = MutableStateFlow(0L)

    init { refreshCacheSize() }

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
    fun setNsfwPin(pin: String?) = viewModelScope.launch { container.settings.setNsfwPin(pin) }
    fun unlockNsfw() = viewModelScope.launch { container.settings.setNsfwUnlocked(true) }

    fun clearCache() {
        viewModelScope.launch {
            container.cache.clear()
            refreshCacheSize()
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch { cacheSize.value = container.cache.sizeBytes() }
    }
}
