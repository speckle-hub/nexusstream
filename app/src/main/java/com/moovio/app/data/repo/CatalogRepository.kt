package com.moovio.app.data.repo

import com.moovio.app.data.adult.AdultRepository
import com.moovio.app.data.cloudstream.CloudStreamSources
import com.moovio.app.data.local.SettingsStore
import com.moovio.app.data.model.Addon
import com.moovio.app.data.model.CatalogDef
import com.moovio.app.data.model.CatalogRow
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.remote.AniListClient
import com.moovio.app.data.remote.JikanClient
import com.moovio.app.data.remote.MangaDexClient
import com.moovio.app.data.remote.StremioClient
import com.moovio.app.data.remote.TmdbClient
import com.moovio.app.data.sfw.SfwAnimeRepository
import com.moovio.app.data.sfw.SfwRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Aggregates content rows from TMDB, AniList, MangaDex and Stremio addon catalogs. */
class CatalogRepository(
    private val addonRepo: AddonRepository,
    private val settings: SettingsStore,
    private val cloudStreamRepo: CloudStreamRepository,
) {

    /**
     * Section rows from the last successful fetch, keyed by section.
     *
     * Browse reads this before it touches the network so switching to a segment paints the rails
     * it showed last time immediately; [rowsForSection] then refreshes them in the background and
     * replaces the entry.
     */
    private val rowsLock = Any()
    private val sectionRows = HashMap<MediaType, Pair<Long, List<CatalogRow>>>()

    /**
     * The cached rows for [section], or null when they are missing, empty or older than
     * [SECTION_ROWS_TTL_MS]. Never suspends and never hits the network.
     */
    fun cachedSectionRows(section: MediaType): List<CatalogRow>? = synchronized(rowsLock) {
        sectionRows[section]
            ?.takeIf { System.currentTimeMillis() - it.first < SECTION_ROWS_TTL_MS }
            ?.second
            ?.takeIf { it.isNotEmpty() }
    }

    /** Stores a successful fetch; an empty result is a failed load, not an empty section. */
    private fun storeSectionRows(section: MediaType, rows: List<CatalogRow>) = synchronized(rowsLock) {
        if (rows.isNotEmpty()) sectionRows[section] = System.currentTimeMillis() to rows
    }

    /** Search every loaded CloudStream provider, bounded and failure-isolated per provider. */
    /**
     * Search the installed CloudStream providers.
     *
     * [includeNsfw] is the content gate: providers installed from an adult repository (CXXX) are
     * skipped unless the caller is the NSFW/Real 18+ path, so adult results never reach general
     * search. Their results are retyped as [MediaType.REAL] when surfaced through the NSFW path so
     * the rest of the app treats them as adult content.
     */
    private suspend fun cloudStreamSearch(query: String, includeNsfw: Boolean = false): List<MediaItem> = coroutineScope {
        val providers = runCatching { cloudStreamRepo.providers(includeNsfw) }
            .getOrDefault(emptyList())
            .take(MAX_CLOUDSTREAM_PROVIDERS)
        providers.map { provider ->
            async {
                ScraperRace.raceResult(ADDON_TIMEOUT_MS) { CloudStreamSources.search(provider, query) }
                    .getOrDefault(emptyList())
            }
        }.flatMap { it.await() }.let { items ->
            if (includeNsfw) items.map { if (it.type == MediaType.MOVIE || it.type == MediaType.SERIES) it.copy(type = MediaType.REAL) else it }
            else items
        }
    }

    private suspend fun safeRow(title: String, type: MediaType, block: suspend () -> List<MediaItem>): CatalogRow? =
        ScraperRace.raceResult(SAFE_ROW_TIMEOUT_MS) { block() }.getOrNull()?.distinctBy { it.key }?.takeIf { it.isNotEmpty() }?.let { CatalogRow(title, it, type = type) }

    // ---- Home ---------------------------------------------------------------

    suspend fun homeRows(): List<CatalogRow> = coroutineScope {
        val jobs = listOf(
            async { safeRow("Trending Now", MediaType.MOVIE) { TmdbClient.trending("all", "week") } },
            async { safeRow("Popular Movies", MediaType.MOVIE) { TmdbClient.popularMovies() } },
            async { safeRow("Popular TV Shows", MediaType.SERIES) { TmdbClient.popularTv() } },
            async { safeRow("Top Rated Movies", MediaType.MOVIE) { TmdbClient.topRatedMovies() } },
            async { safeRow("Trending Anime", MediaType.ANIME) { AniListClient.trendingAnime() } },
            async { safeRow("Now Playing", MediaType.MOVIE) { TmdbClient.nowPlaying() } },
        )
        val base = jobs.mapNotNull { it.await() }
        (base + stremioRowsFor(setOf(MediaType.MOVIE, MediaType.SERIES, MediaType.ANIME)))
            .mapNotNull { it.scrubbed() }
    }

    /**
     * Phase 28 isolation safety net: the public Home feed must never carry an adult poster,
     * even if a mis-tagged add-on catalog or a mis-labelled built-in item slips one in.
     * Rows left empty by the scrub are dropped so the feed never renders an empty rail.
     */
    private fun CatalogRow.scrubbed(): CatalogRow? {
        val kept = items.filterNot { it.isAdult }
        if (kept.isEmpty()) return null
        if (kept.size == items.size) return this
        return copy(items = kept)
    }

    // ---- Generic section ----------------------------------------------------

    /**
     * Rows for one section.
     *
     * Runs on [Dispatchers.IO] so a cache-miss network fetch (and the JSON mapping that follows
     * it) never occupies the main thread, and caches the result so the next visit can paint it
     * before the network answers.
     */
    suspend fun rowsForSection(section: MediaType): List<CatalogRow> {
        val rows = withContext(Dispatchers.IO) { fetchSectionRows(section) }
        storeSectionRows(section, rows)
        return rows
    }

    /**
     * [supervisorScope], not `coroutineScope`: a row fetcher that throws can no longer cancel its
     * siblings, so one broken source costs its own row instead of the whole section.
     */
    private suspend fun fetchSectionRows(section: MediaType): List<CatalogRow> = supervisorScope {
        when (section) {
            MediaType.MOVIE -> {
                // Phase 32: race the five TMDB rows concurrently (each already fail-fast via
                // safeRow) instead of awaiting them one after another.
                val rows = listOf(
                    async { safeRow("Popular Movies", MediaType.MOVIE) { TmdbClient.popularMovies() } },
                    async { safeRow("Top Rated", MediaType.MOVIE) { TmdbClient.topRatedMovies() } },
                    async { safeRow("Upcoming", MediaType.MOVIE) { TmdbClient.upcoming() } },
                    async { safeRow("Action", MediaType.MOVIE) { TmdbClient.discoverMovies("28") } },
                    async { safeRow("Comedy", MediaType.MOVIE) { TmdbClient.discoverMovies("35") } },
                ).mapNotNull { it.await() }
                rows + sfwSectionRows(section) + stremioRowsFor(setOf(MediaType.MOVIE))
            }

            MediaType.SERIES -> {
                val rows = listOf(
                    async { safeRow("Popular TV", MediaType.SERIES) { TmdbClient.popularTv() } },
                    async { safeRow("Top Rated", MediaType.SERIES) { TmdbClient.topRatedTv() } },
                    async { safeRow("Airing Today", MediaType.SERIES) { TmdbClient.airingToday() } },
                    async { safeRow("Drama", MediaType.SERIES) { TmdbClient.discoverTv("18") } },
                ).mapNotNull { it.await() }
                rows + sfwSectionRows(section) + stremioRowsFor(setOf(MediaType.SERIES))
            }

            MediaType.ANIME -> {
                val rows = listOf(
                    async { safeRow("Trending Anime", MediaType.ANIME) { AniListClient.trendingAnime() } },
                    async { safeRow("Popular Anime", MediaType.ANIME) { AniListClient.popularAnime() } },
                    async { safeRow("Action", MediaType.ANIME) { AniListClient.byGenre("Action") } },
                    async { safeRow("Romance", MediaType.ANIME) { AniListClient.byGenre("Romance") } },
                    async { safeRow("Fantasy", MediaType.ANIME) { AniListClient.byGenre("Fantasy") } },
                ).mapNotNull { it.await() }
                rows + sfwSectionRows(section) + stremioRowsFor(setOf(MediaType.ANIME))
            }

            MediaType.MANGA -> listOf(
                async { safeRow("Popular Manga", MediaType.MANGA) { MangaDexClient.popular() } },
                async { safeRow("Latest Updates", MediaType.MANGA) { MangaDexClient.latest() } },
                async { safeRow("Action", MediaType.MANGA) { MangaDexClient.byGenre("391b0423-d847-456f-aff0-8b0cfc03066b") } },
                async { safeRow("Romance", MediaType.MANGA) { MangaDexClient.byGenre("423e2eae-a7a2-4a8b-ac03-a8351462d71d") } },
            ).mapNotNull { it.await() }

            else -> emptyList()
        }
    }

    /**
     * Built-in SFW catalog rows for a section (Popular / Latest / Trending where the
     * sites offer them). Bounded like every other row fetch: a hung scraper degrades
     * to "no rows" instead of gating the section.
     */
    private suspend fun sfwSectionRows(section: MediaType): List<CatalogRow> =
        ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.homeRows(section).rows }
            .getOrDefault(emptyList())

    // ---- NSFW ---------------------------------------------------------------

    /**
     * NSFW-anime home rows, painted progressively. Every row fetch runs concurrently under
     * [ROW_TIMEOUT_MS] and [onUpdate] fires as each row lands (always in registry order), so the
     * section drops its skeleton on the first AniList row (about a second) instead of waiting for
     * the slowest source — Jikan's flaky endpoints used to gate the tab for as long as they liked.
     */
    suspend fun nsfwAnimeRows(onUpdate: (List<CatalogRow>) -> Unit = {}): List<CatalogRow> = coroutineScope {
        val fetchers: List<suspend () -> CatalogRow?> = listOf(
            { nsfwRow("Popular") { AniListClient.nsfwPopular() } },
            { nsfwRow("Trending") { AniListClient.nsfwTrending() } },
            { nsfwRow("Top Rated") { AniListClient.nsfwTopRated() } },
            { nsfwRow("Recently Released") { AniListClient.nsfwRecent() } },
            { nsfwRow("Ecchi") { AniListClient.nsfwByGenre("Ecchi") } },
            { nsfwRow("Hentai") { AniListClient.nsfwByGenre("Hentai") } },
            { nsfwRow("Romance") { AniListClient.nsfwByGenre("Romance") } },
            { nsfwRow("Jikan Top") { JikanClient.nsfwTop() } },
        )
        val slots = arrayOfNulls<CatalogRow>(fetchers.size)
        fetchers.forEachIndexed { index, fetch ->
            launch {
                slots[index] = runCatching { fetch() }.getOrNull()
                onUpdate(slots.filterNotNull())
            }
        }
        // coroutineScope joins the row jobs above, so every slot write is published by here.
        slots.filterNotNull() + stremioNsfwRows(MediaType.NSFW_ANIME)
    }

    /** One built-in NSFW-anime row under [ROW_TIMEOUT_MS]; a slow or failing source just drops its row. */
    private suspend fun nsfwRow(title: String, block: suspend () -> List<MediaItem>): CatalogRow? =
        ScraperRace.raceResult(ROW_TIMEOUT_MS) { block() }.getOrNull()
            ?.distinctBy { it.key }
            ?.takeIf { it.isNotEmpty() }
            ?.let { CatalogRow(title, it, type = MediaType.NSFW_ANIME) }

    suspend fun nsfwMangaRows(): List<CatalogRow> = coroutineScope {
        val jobs = listOf(
            async { safeRow("Popular", MediaType.NSFW_MANGA) { MangaDexClient.nsfwPopular() } },
            async { safeRow("Latest Updates", MediaType.NSFW_MANGA) { MangaDexClient.nsfwLatest() } },
            async { safeRow("Top Rated", MediaType.NSFW_MANGA) { MangaDexClient.nsfwTopRated() } },
            async { safeRow("Romance", MediaType.NSFW_MANGA) { MangaDexClient.nsfwByTag("423e2eae-a7a2-4a8b-ac03-a8351462d71d") } },
            async { safeRow("Harem", MediaType.NSFW_MANGA) { MangaDexClient.nsfwByTag("aafb99c1-7f60-43fa-b75f-fc9502ce29c7") } },
            async { safeRow("Drama", MediaType.NSFW_MANGA) { MangaDexClient.nsfwByTag("b9af3a63-f058-46de-a9a0-e0c13906197a") } },
        )
        jobs.mapNotNull { it.await() }
    }

    /**
     * Real 18+ comes from the built-in adult sources first, and additionally from any installed
     * NSFW Stremio add-ons. Per-source errors are returned so the UI can report a site that's down.
     */
    suspend fun realRowsWithErrors(): Pair<List<CatalogRow>, List<String>> =
        realRowsProgressive { _, _ -> }

    /**
     * Real 18+ home with progressive updates. [onUpdate] fires as each built-in source finishes, so
     * the UI can drop its skeleton and paint rows immediately instead of waiting for every site
     * (the slowest of which used to gate the whole tab).
     */
    suspend fun realRowsProgressive(
        onUpdate: suspend (rows: List<CatalogRow>, errors: List<String>) -> Unit,
    ): Pair<List<CatalogRow>, List<String>> = coroutineScope {
        // "Auto-pull NSFW from add-ons" gates whether installed NSFW Stremio add-ons feed Real 18+.
        val autoNsfw = settings.autoNsfwFromAddons.first()
        val stremio = async {
            if (!autoNsfw) emptyList()
            else runCatching { stremioNsfwRows(MediaType.REAL) }.getOrDefault(emptyList())
        }
        val home = AdultRepository.home { rows, errors -> onUpdate(rows, errors) }
        val rows = home.rows + stremio.await()
        // Final snapshot includes any Stremio NSFW add-on rows appended after the built-ins.
        onUpdate(rows, home.errors)
        rows to home.errors
    }

    suspend fun realRows(): List<CatalogRow> = realRowsWithErrors().first

    /**
     * Rows sourced exclusively from installed NSFW Stremio add-ons for a given NSFW section.
     * Catalog types are resolved through [StremioClient.catalogsFor], so add-ons advertising
     * non-standard types ("porn", "Porn", "hentai", ...) are handled correctly.
     */
    private suspend fun stremioNsfwRows(section: MediaType): List<CatalogRow> = coroutineScope {
        val nsfwAddons = addonRepo.enabledAddons().filter { it.nsfw }
        val jobs = nsfwAddons.flatMap { addon ->
            StremioClient.catalogsFor(addon, section).map { cat ->
                async {
                    ScraperRace.raceResult(CATALOG_TIMEOUT_MS) {
                        StremioClient.fetchCatalog(addon, cat.type, cat.id)
                    }.getOrNull()?.distinctBy { it.key }?.takeIf { it.isNotEmpty() }?.let {
                        CatalogRow("${addon.name} \u00b7 ${cat.name}", it, addon.id, cat.id, section)
                    }
                }
            }
        }
        jobs.mapNotNull { it.await() }
    }

    // ---- Search -------------------------------------------------------------

    /**
     * Real 18+ search. Built-in adult sources are authoritative; installed NSFW Stremio add-ons
     * are queried **only** as a fallback when the built-ins return nothing, so add-on results can
     * never pollute or outrank the built-in ones (and SFW add-ons are never consulted).
     */
    suspend fun searchReal(query: String): AdultRepository.Search =
        searchRealProgressive(query) { _, _ -> }

    /**
     * Progressive Real 18+ search: built-in sources publish [onUpdate] as each one finishes, so
     * the first results appear immediately instead of waiting on the slowest (or a hung) site.
     * NSFW Stremio add-ons are queried only as a fallback when the built-ins return nothing.
     */
    suspend fun searchRealProgressive(
        query: String,
        onUpdate: suspend (items: List<MediaItem>, errors: List<String>) -> Unit,
    ): AdultRepository.Search = coroutineScope {
        val builtIn = AdultRepository.searchDetailedProgressive(query) { items, errors ->
            onUpdate(items, errors)
        }
        if (builtIn.items.isNotEmpty()) return@coroutineScope builtIn

        val autoNsfw = settings.autoNsfwFromAddons.first()
        // Adult CloudStream providers (CXXX) are a first-class Real 18+ source; they are only
        // consulted here, never from general search.
        val plugins = runCatching { cloudStreamSearch(query, includeNsfw = true) }.getOrDefault(emptyList())
        val stremio = if (!autoNsfw) emptyList() else runCatching {
            addonSearch(query, MediaType.REAL, nsfwOnly = true)
        }.getOrDefault(emptyList())

        val items = (plugins + stremio.filter { it.type == MediaType.REAL }).distinctBy { it.key }
        onUpdate(items, builtIn.errors)
        AdultRepository.Search(items = items, errors = builtIn.errors)
    }

    /**
     * Search one section.
     *
     * [onUpdate] receives partial, already-filtered lists as individual sources land; the return
     * value is always the complete merged list. Callers that don't need progressive paint simply
     * omit it.
     */
    suspend fun search(
        query: String,
        section: MediaType,
        onUpdate: (suspend (List<MediaItem>) -> Unit)? = null,
    ): List<MediaItem> {
        // Real 18+ has its own path so built-in sources are authoritative.
        if (section == MediaType.REAL) return searchReal(query).items
        // NSFW anime merges AniList + Jikan + add-ons, each bounded on its own.
        if (section == MediaType.NSFW_ANIME) return searchNsfwAnime(query, onUpdate)
        return searchInternal(query, section, onUpdate)
    }

    /**
     * NSFW-anime search. **Every source is queried together and merged** so a search returns
     * everything available — AniList, Jikan and the installed NSFW Stremio add-ons — instead of
     * stopping at the first source that answers (which used to cap a search at AniList's page).
     *
     * The three hops run concurrently and each keeps its own hard timeout ([ANILIST_TIMEOUT_MS],
     * [JIKAN_TIMEOUT_MS], [ADDON_TIMEOUT_MS]), so the whole search costs the slowest hop, not their
     * sum. Results are merged in built-in-first order and de-duplicated by key.
     */
    private suspend fun searchNsfwAnime(
        query: String,
        onUpdate: (suspend (List<MediaItem>) -> Unit)? = null,
    ): List<MediaItem> = supervisorScope {
        val collected = ArrayList<MediaItem>()
        val mutex = Mutex()

        fun merged(items: List<MediaItem>): List<MediaItem> =
            items.filter { it.type == MediaType.NSFW_ANIME }.distinctBy { it.key }

        suspend fun publish() {
            val snapshot = merged(mutex.withLock { collected.toList() })
            if (snapshot.isNotEmpty()) runCatching { onUpdate?.invoke(snapshot) }
        }

        val sources: List<suspend () -> List<MediaItem>> = listOf(
            {
                ScraperRace.raceResult(ANILIST_TIMEOUT_MS) { AniListClient.nsfwSearch(query) }
                    .getOrDefault(emptyList())
            },
            {
                ScraperRace.raceResult(JIKAN_TIMEOUT_MS) { JikanClient.nsfwSearch(query) }
                    .getOrDefault(emptyList())
            },
            {
                ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                    addonSearch(query, MediaType.NSFW_ANIME, nsfwOnly = true)
                }.getOrDefault(emptyList())
            },
        )
        sources.map { fetch ->
            launch {
                val found = try {
                    fetch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                if (found.isEmpty()) return@launch
                mutex.withLock { collected += found }
                publish()
            }
        }.joinAll()

        merged(mutex.withLock { collected.toList() })
    }

    /**
     * Search every eligible add-on's searchable catalogs for [section], in parallel.
     *
     * [includeNsfw] is the NSFW-Hub opt-in: for a regular (SFW) section, NSFW-flagged add-ons are
     * excluded unless the user explicitly enabled `nsfwGlobalSearch` from inside the hub, so adult
     * add-ons can never leak into a general search. NSFW sections always stay NSFW-only.
     */
    private suspend fun addonSearch(
        query: String,
        section: MediaType,
        nsfwOnly: Boolean,
        includeNsfw: Boolean = false,
    ): List<MediaItem> = coroutineScope {
        val candidateAddons = addonRepo.enabledAddons().let { addons ->
            when {
                nsfwOnly -> addons.filter { it.nsfw }
                includeNsfw -> addons
                else -> addons.filter { !it.nsfw }
            }
        }
        candidateAddons.flatMap { addon ->
            StremioClient.catalogsFor(addon, section)
                .filter { cat -> cat.extra.any { it.name == "search" } }
                .map { cat ->
                    async {
                        ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                            StremioClient.search(addon, cat.type, cat.id, query)
                        }.getOrDefault(emptyList())
                    }
                }
        }.flatMap { it.await() }
    }

    /**
     * Search one section across its built-in source(s) and the Stremio add-ons.
     *
     * Every source is its own child of a [supervisorScope] and collects under a mutex, so one
     * source that throws neither cancels its siblings nor withholds the answers that already
     * landed: [onUpdate] fires on each arrival, and built-in results always stay ahead of add-on
     * results in the merged list.
     */
    private suspend fun searchInternal(
        query: String,
        section: MediaType,
        onUpdate: (suspend (List<MediaItem>) -> Unit)? = null,
    ): List<MediaItem> = supervisorScope {
        val isNsfwSection = section == MediaType.NSFW_ANIME ||
            section == MediaType.NSFW_MANGA ||
            section == MediaType.REAL

        // Read once, before the fan-out, so a stalled preference read can't look like a failure.
        val allowNsfwAddons = runCatching { settings.nsfwGlobalSearch.first() }.getOrDefault(false)

        val builtIns = ArrayList<MediaItem>()
        val addons = ArrayList<MediaItem>()
        val mutex = Mutex()

        fun merged(items: List<MediaItem>): List<MediaItem> {
            val filtered = if (isNsfwSection) {
                // NSFW sections must never surface non-adult built-in items.
                items.filter { it.addonId != "anilist" && it.addonId != "jikan" && it.addonId != "mangadex" || it.type == section }
            } else {
                // Phase 28 isolation: a public (SFW) section must never surface an adult item, even
                // if a mis-tagged add-on catalog or a mis-labelled built-in result slips one in.
                items.filterNot { it.isAdult }
            }
            return filtered.distinctBy { it.key }
        }

        suspend fun publish() {
            val snapshot = merged(mutex.withLock { builtIns.toList() + addons.toList() })
            if (snapshot.isNotEmpty()) runCatching { onUpdate?.invoke(snapshot) }
        }

        suspend fun collect(target: MutableList<MediaItem>, fetch: suspend () -> List<MediaItem>) {
            val found = try {
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            if (found.isEmpty()) return
            mutex.withLock { target += found }
            publish()
        }

        // Built-in SFW sources join the metadata search so Movies/TV/Anime surface playable titles
        // with zero add-ons installed (TMDB/AniList stay authoritative for metadata; the built-ins
        // only add their own title rows).
        val builtInFetchers: List<suspend () -> List<MediaItem>> = when (section) {
            MediaType.MOVIE -> listOf(
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.searchMovies(query) }.getOrDefault(emptyList()) },
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query, MediaType.MOVIE).items }.getOrDefault(emptyList()) },
            )
            MediaType.SERIES -> listOf(
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.searchTv(query) }.getOrDefault(emptyList()) },
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query, MediaType.SERIES).items }.getOrDefault(emptyList()) },
            )
            MediaType.ANIME -> listOf(
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.searchAnime(query) }.getOrDefault(emptyList()) },
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwAnimeRepository.searchTitles(query).first }.getOrDefault(emptyList()) },
            )
            MediaType.MANGA -> listOf(
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { MangaDexClient.search(query) }.getOrDefault(emptyList()) },
            )
            MediaType.NSFW_ANIME -> listOf(
                // Routed through searchNsfwAnime() before this method is reached; kept as a strict
                // adult-only pair here for any future caller.
                { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.nsfwSearch(query) }.getOrDefault(emptyList()) },
                { ScraperRace.raceResult(JIKAN_TIMEOUT_MS) { JikanClient.nsfwSearch(query) }.getOrDefault(emptyList()) },
            )
            // limit = 100 (MangaDex's maximum) so an adult search isn't cut short at the old 30.
            MediaType.NSFW_MANGA -> listOf(
                {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                        MangaDexClient.search(query, nsfw = true, limit = 100)
                    }.getOrDefault(emptyList())
                },
            )
            MediaType.REAL -> emptyList() // Never reached: search() routes REAL through searchReal().
        }

        // For NSFW sections ONLY NSFW-flagged add-ons are queried; for SFW sections NSFW-flagged
        // add-ons are excluded unless the user opted in from inside the NSFW Hub.
        val jobs = builtInFetchers.map { fetch -> launch { collect(builtIns, fetch) } } +
            launch {
                collect(addons) {
                    addonSearch(query, section, nsfwOnly = isNsfwSection, includeNsfw = allowNsfwAddons)
                }
            }
        jobs.joinAll()

        merged(mutex.withLock { builtIns.toList() + addons.toList() })
    }

    private companion object {
        /**
         * Caps the AniList primary for NSFW-anime search. AniList is normally sub-second; the cap
         * only exists so a hung GraphQL call degrades to Jikan/add-ons instead of holding the
         * search open for a full OkHttp timeout (20 s connect / 30 s read).
         */
        const val ANILIST_TIMEOUT_MS = 8_000L

        /** Caps the Jikan fallback so an upstream 504 can't stall an AniList-primary search. */
        const val JIKAN_TIMEOUT_MS = 6_000L

/** Caps every Stremio add-on search so one slow host can't stall the whole list. */
        const val ADDON_TIMEOUT_MS = 5_000L

        /** Caps a Stremio catalog-row fetch; a hung add-on is dropped rather than blocking Home. */
        const val CATALOG_TIMEOUT_MS = 5_000L

        /** CloudStream providers queried per search (each is a full scrape of its own site). */
        const val MAX_CLOUDSTREAM_PROVIDERS = 15

        /**
         * Caps one built-in home row (NSFW anime). Every row fetch used to be unbounded, so the
         * flakiest source (Jikan's documented 504s, 429 backoff) gated the whole tab's spinner.
         */
        const val ROW_TIMEOUT_MS = 5_000L

        /**
         * Phase 32 fail-fast for generic home/section rows: previously unbounded, so one hung
         * metadata call gated the whole feed. Now every safeRow degrades to "no row".
         */
        const val SAFE_ROW_TIMEOUT_MS = 5_000L

        /** Section rows stay good this long, so tab switches paint without touching the network. */
        const val SECTION_ROWS_TTL_MS = 10 * 60_000L
    }

    /**
     * Whole-app search: metadata providers, built-in scrapers, CloudStream plugins and every
     * searchable Stremio catalog.
     *
     * Each source is a separate child of a [supervisorScope], so a failing source costs only its
     * own results, and [onUpdate] publishes the merged list as each source lands — a fast TMDB
     * answer no longer waits behind a hung add-on.
     */
    suspend fun globalSearch(
        query: String,
        onUpdate: (suspend (List<MediaItem>) -> Unit)? = null,
    ): List<MediaItem> = supervisorScope {
        // NSFW-flagged add-ons are only consulted when the user explicitly opted in from the
        // isolated NSFW Hub; otherwise the general Search screen stays adult-free.
        val includeNsfw = runCatching { settings.nsfwGlobalSearch.first() }.getOrDefault(false)
        val collected = ArrayList<MediaItem>()
        val mutex = Mutex()

        // Phase 28 isolation: unless the user opted in from inside the NSFW Hub, the general
        // search stays adult-free at the *item* level (filtering the add-ons is not enough — a
        // mis-tagged catalog on an SFW add-on could still leak one in).
        fun merged(items: List<MediaItem>): List<MediaItem> =
            (if (includeNsfw) items else items.filterNot { it.isAdult }).distinctBy { it.key }

        suspend fun publish() {
            val snapshot = merged(mutex.withLock { collected.toList() })
            if (snapshot.isNotEmpty()) runCatching { onUpdate?.invoke(snapshot) }
        }

        suspend fun collect(fetch: suspend () -> List<MediaItem>) {
            val found = try {
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            if (found.isEmpty()) return
            mutex.withLock { collected += found }
            publish()
        }

        val builtInFetchers: List<suspend () -> List<MediaItem>> = listOf(
            { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.search(query) }.getOrDefault(emptyList()) },
            { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.searchAnime(query) }.getOrDefault(emptyList()) },
            { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { MangaDexClient.search(query) }.getOrDefault(emptyList()) },
            { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query).items }.getOrDefault(emptyList()) },
            { ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwAnimeRepository.searchTitles(query).first }.getOrDefault(emptyList()) },
        )

        val jobs = builtInFetchers.map { fetch -> launch { collect(fetch) } } +
            // CloudStream provider plugins (installed .cs3) also serve the search screen.
            launch { collect { cloudStreamSearch(query) } } +
            // Every searchable add-on catalog is its own job: one dead add-on no longer cancels
            // the batch, and a fast one paints before a slow one answers.
            addonRepo.enabledAddons()
                .filter { includeNsfw || !it.nsfw }
                .flatMap { addon ->
                    addon.catalogs.filter { c -> c.extra.any { it.name == "search" } }.map { cat ->
                        launch {
                            collect {
                                ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                                    StremioClient.search(addon, cat.type, cat.id, query)
                                }.getOrDefault(emptyList())
                            }
                        }
                    }
                }
        jobs.joinAll()

        merged(mutex.withLock { collected.toList() })
    }

    // ---- Stremio rows -------------------------------------------------------

    private suspend fun stremioRowsFor(types: Set<MediaType>): List<CatalogRow> = coroutineScope {
        val addons = addonRepo.enabledAddons().filter { !it.nsfw }
        val jobs = addons.flatMap { addon ->
            addon.catalogs
                .filter { cat -> types.any { StremioClient.mapType(cat.type, addon) == it } }
                .map { cat ->
                    async {
                        ScraperRace.raceResult(CATALOG_TIMEOUT_MS) {
                            StremioClient.fetchCatalog(addon, cat.type, cat.id)
                        }.getOrNull()?.distinctBy { it.key }?.takeIf { it.isNotEmpty() }?.let { items ->
                            CatalogRow("${addon.name} · ${cat.name}", items, addon.id, cat.id, StremioClient.mapType(cat.type, addon))
                        }
                    }
                }
        }
        jobs.mapNotNull { it.await() }
    }

    suspend fun loadCatalog(addon: Addon, catalog: CatalogDef, extra: Map<String, String> = emptyMap()): List<MediaItem> =
        ScraperRace.raceResult(CATALOG_TIMEOUT_MS) {
            StremioClient.fetchCatalog(addon, catalog.type, catalog.id, extra)
        }.getOrDefault(emptyList()).distinctBy { it.key }
}
