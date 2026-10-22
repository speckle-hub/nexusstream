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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Aggregates content rows from TMDB, AniList, MangaDex and Stremio addon catalogs. */
class CatalogRepository(
    private val addonRepo: AddonRepository,
    private val settings: SettingsStore,
    private val cloudStreamRepo: CloudStreamRepository,
) {

    /** Search every loaded CloudStream provider, bounded and failure-isolated per provider. */
    private suspend fun cloudStreamSearch(query: String): List<MediaItem> = coroutineScope {
        val providers = runCatching { cloudStreamRepo.providers() }.getOrDefault(emptyList()).take(15)
        providers.map { provider ->
            async {
                ScraperRace.raceResult(ADDON_TIMEOUT_MS) { CloudStreamSources.search(provider, query) }
                    .getOrDefault(emptyList())
            }
        }.flatMap { it.await() }
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

    suspend fun rowsForSection(section: MediaType): List<CatalogRow> = coroutineScope {
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
        val stremio = if (!autoNsfw) emptyList() else runCatching {
            addonSearch(query, MediaType.REAL, nsfwOnly = true)
        }.getOrDefault(emptyList())

        val items = stremio.filter { it.type == MediaType.REAL }.distinctBy { it.key }
        onUpdate(items, builtIn.errors)
        AdultRepository.Search(items = items, errors = builtIn.errors)
    }

    suspend fun search(query: String, section: MediaType): List<MediaItem> {
        // Real 18+ has its own path so built-in sources are authoritative.
        if (section == MediaType.REAL) return searchReal(query).items
        // NSFW anime prefers AniList (fast) and only reaches for Jikan/scrapers when needed.
        if (section == MediaType.NSFW_ANIME) return searchNsfwAnime(query)
        return searchInternal(query, section)
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
    private suspend fun searchNsfwAnime(query: String): List<MediaItem> = coroutineScope {
        val aniList = async {
            ScraperRace.raceResult(ANILIST_TIMEOUT_MS) { AniListClient.nsfwSearch(query) }
                .getOrDefault(emptyList())
        }
        val jikan = async {
            ScraperRace.raceResult(JIKAN_TIMEOUT_MS) { JikanClient.nsfwSearch(query) }
                .getOrDefault(emptyList())
        }
        val addons = async {
            ScraperRace.raceResult(ADDON_TIMEOUT_MS) { addonSearch(query, MediaType.NSFW_ANIME, nsfwOnly = true) }
                .getOrDefault(emptyList())
        }
        (aniList.await() + jikan.await() + addons.await())
            .filter { it.type == MediaType.NSFW_ANIME }
            .distinctBy { it.key }
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

    private suspend fun searchInternal(query: String, section: MediaType): List<MediaItem> = coroutineScope {
        val isNsfwSection = section == MediaType.NSFW_ANIME ||
            section == MediaType.NSFW_MANGA ||
            section == MediaType.REAL

        // Phase 32: race the built-in search against the add-on fan-out concurrently instead of
        // running one after the other — the total costs the slowest, not the sum.
        // Built-in SFW sources join the metadata search so Movies/TV/Anime surface
        // playable titles with zero add-ons installed (TMDB/AniList stay authoritative
        // for metadata; built-ins only add their own title rows).
        val builtInDeferred = async {
            when (section) {
                MediaType.MOVIE -> ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.searchMovies(query) }
                    .getOrDefault(emptyList()) +
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query, MediaType.MOVIE).items }
                        .getOrDefault(emptyList())
                MediaType.SERIES -> ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.searchTv(query) }
                    .getOrDefault(emptyList()) +
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query, MediaType.SERIES).items }
                        .getOrDefault(emptyList())
                MediaType.ANIME -> ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.searchAnime(query) }
                    .getOrDefault(emptyList()) +
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwAnimeRepository.searchTitles(query).first }
                        .getOrDefault(emptyList())
                MediaType.MANGA -> ScraperRace.raceResult(ADDON_TIMEOUT_MS) { MangaDexClient.search(query) }
                    .getOrDefault(emptyList())
                MediaType.NSFW_ANIME -> {
                    // Routed through searchNsfwAnime() before this method is reached; kept as a strict
                    // adult-only pair here for any future caller.
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.nsfwSearch(query) }
                        .getOrDefault(emptyList()) + ScraperRace.raceResult(JIKAN_TIMEOUT_MS) {
                        JikanClient.nsfwSearch(query)
                    }.getOrDefault(emptyList())
                }
                // limit = 100 (MangaDex's maximum) so an adult search isn't cut short at the old 30.
                MediaType.NSFW_MANGA -> ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                    MangaDexClient.search(query, nsfw = true, limit = 100)
                }.getOrDefault(emptyList())
                MediaType.REAL -> emptyList() // Never reached: search() routes REAL through searchReal().
            }
        }

        // Stremio addon search. For NSFW sections ONLY NSFW-flagged add-ons are queried; for SFW
        // sections NSFW-flagged add-ons are excluded unless the user opted in from the NSFW Hub.
        val allowNsfwAddons = settings.nsfwGlobalSearch.first()
        val addonsDeferred = async {
            addonSearch(query, section, nsfwOnly = isNsfwSection, includeNsfw = allowNsfwAddons)
        }

        val results = builtInDeferred.await() + addonsDeferred.await()

        // Final safety net: NSFW sections must never surface non-adult built-in items.
        val filtered = if (isNsfwSection) {
            results.filter { it.addonId != "anilist" && it.addonId != "jikan" && it.addonId != "mangadex" || it.type == section }
        } else {
            // Phase 28 isolation: a public (SFW) section must never surface an adult item, even
            // if a mis-tagged add-on catalog or a mis-labelled built-in result slips one in.
            results.filterNot { it.isAdult }
        }
        filtered.distinctBy { it.key }
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
    }

    suspend fun globalSearch(query: String): List<MediaItem> = coroutineScope {
        // Phase 32: race all three phases concurrently — built-ins, CloudStream providers and
        // add-ons start together instead of summing three sequential waits.
        val builtIns = async {
            listOf(
                async {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { TmdbClient.search(query) }
                        .getOrDefault(emptyList())
                },
                async {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { AniListClient.searchAnime(query) }
                        .getOrDefault(emptyList())
                },
                async {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { MangaDexClient.search(query) }
                        .getOrDefault(emptyList())
                },
                async {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwRepository.search(query).items }
                        .getOrDefault(emptyList())
                },
                async {
                    ScraperRace.raceResult(ADDON_TIMEOUT_MS) { SfwAnimeRepository.searchTitles(query).first }
                        .getOrDefault(emptyList())
                },
            ).flatMap { it.await() }
        }
        // CloudStream provider plugins (installed .cs3) also serve the search screen.
        val cloud = async { cloudStreamSearch(query) }
        // NSFW-flagged add-ons are only consulted when the user explicitly opted in from the
        // isolated NSFW Hub; otherwise the general Search screen stays adult-free.
        val includeNsfw = settings.nsfwGlobalSearch.first()
        val addonJobs = addonRepo.enabledAddons()
            .filter { includeNsfw || !it.nsfw }
            .flatMap { addon ->
                addon.catalogs.filter { c -> c.extra.any { it.name == "search" } }.map { cat ->
                    async {
                        ScraperRace.raceResult(ADDON_TIMEOUT_MS) {
                            StremioClient.search(addon, cat.type, cat.id, query)
                        }.getOrDefault(emptyList())
                    }
                }
            }
        val results = builtIns.await() + cloud.await() + addonJobs.flatMap { it.await() }
        // Phase 28 isolation: unless the user explicitly opted in from inside the NSFW Hub,
        // the general Search screen stays adult-free at the *item* level too (the add-on filter
        // above is not enough — a mis-tagged catalog on an SFW add-on could still leak one in).
        val scrubbed = if (includeNsfw) results else results.filterNot { it.isAdult }
        scrubbed.distinctBy { it.key }
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
