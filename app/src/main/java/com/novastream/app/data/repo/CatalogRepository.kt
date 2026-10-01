package com.novastream.app.data.repo

import com.novastream.app.data.adult.AdultRepository
import com.novastream.app.data.local.SettingsStore
import com.novastream.app.data.model.Addon
import com.novastream.app.data.model.CatalogDef
import com.novastream.app.data.model.CatalogRow
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.remote.AniListClient
import com.novastream.app.data.remote.JikanClient
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.data.remote.StremioClient
import com.novastream.app.data.remote.TmdbClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/** Aggregates content rows from TMDB, AniList, MangaDex and Stremio addon catalogs. */
class CatalogRepository(
    private val addonRepo: AddonRepository,
    private val settings: SettingsStore,
) {

    private suspend fun safeRow(title: String, type: MediaType, block: suspend () -> List<MediaItem>): CatalogRow? =
        runCatching { block() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { CatalogRow(title, it, type = type) }

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
        base + stremioRowsFor(setOf(MediaType.MOVIE, MediaType.SERIES, MediaType.ANIME))
    }

    // ---- Generic section ----------------------------------------------------

    suspend fun rowsForSection(section: MediaType): List<CatalogRow> = when (section) {
        MediaType.MOVIE -> listOfNotNull(
            safeRow("Popular Movies", MediaType.MOVIE) { TmdbClient.popularMovies() },
            safeRow("Top Rated", MediaType.MOVIE) { TmdbClient.topRatedMovies() },
            safeRow("Upcoming", MediaType.MOVIE) { TmdbClient.upcoming() },
            safeRow("Action", MediaType.MOVIE) { TmdbClient.discoverMovies("28") },
            safeRow("Comedy", MediaType.MOVIE) { TmdbClient.discoverMovies("35") },
        ) + stremioRowsFor(setOf(MediaType.MOVIE))

        MediaType.SERIES -> listOfNotNull(
            safeRow("Popular TV", MediaType.SERIES) { TmdbClient.popularTv() },
            safeRow("Top Rated", MediaType.SERIES) { TmdbClient.topRatedTv() },
            safeRow("Airing Today", MediaType.SERIES) { TmdbClient.airingToday() },
            safeRow("Drama", MediaType.SERIES) { TmdbClient.discoverTv("18") },
        ) + stremioRowsFor(setOf(MediaType.SERIES))

        MediaType.ANIME -> listOfNotNull(
            safeRow("Trending Anime", MediaType.ANIME) { AniListClient.trendingAnime() },
            safeRow("Popular Anime", MediaType.ANIME) { AniListClient.popularAnime() },
            safeRow("Action", MediaType.ANIME) { AniListClient.byGenre("Action") },
            safeRow("Romance", MediaType.ANIME) { AniListClient.byGenre("Romance") },
            safeRow("Fantasy", MediaType.ANIME) { AniListClient.byGenre("Fantasy") },
        ) + stremioRowsFor(setOf(MediaType.ANIME))

        MediaType.MANGA -> listOfNotNull(
            safeRow("Popular Manga", MediaType.MANGA) { MangaDexClient.popular() },
            safeRow("Latest Updates", MediaType.MANGA) { MangaDexClient.latest() },
            safeRow("Action", MediaType.MANGA) { MangaDexClient.byGenre("391b0423-d847-456f-aff0-8b0cfc03066b") },
            safeRow("Romance", MediaType.MANGA) { MangaDexClient.byGenre("423e2eae-a7a2-4a8b-ac03-a8351462d71d") },
        )

        else -> emptyList()
    }

    // ---- NSFW ---------------------------------------------------------------

    suspend fun nsfwAnimeRows(): List<CatalogRow> = coroutineScope {
        val jobs = listOf(
            async { safeRow("Popular", MediaType.NSFW_ANIME) { AniListClient.nsfwPopular() } },
            async { safeRow("Trending", MediaType.NSFW_ANIME) { AniListClient.nsfwTrending() } },
            async { safeRow("Top Rated", MediaType.NSFW_ANIME) { AniListClient.nsfwTopRated() } },
            async { safeRow("Recently Released", MediaType.NSFW_ANIME) { AniListClient.nsfwRecent() } },
            async { safeRow("Ecchi", MediaType.NSFW_ANIME) { AniListClient.nsfwByGenre("Ecchi") } },
            async { safeRow("Hentai", MediaType.NSFW_ANIME) { AniListClient.nsfwByGenre("Hentai") } },
            async { safeRow("Romance", MediaType.NSFW_ANIME) { AniListClient.nsfwByGenre("Romance") } },
            async { safeRow("Jikan Top", MediaType.NSFW_ANIME) { JikanClient.nsfwTop() } },
        )
        jobs.mapNotNull { it.await() } + stremioNsfwRows(MediaType.NSFW_ANIME)
    }

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
    suspend fun realRowsWithErrors(): Pair<List<CatalogRow>, List<String>> = coroutineScope {
        val builtIn = async { AdultRepository.home() }
        // "Auto-pull NSFW from add-ons" gates whether installed NSFW Stremio add-ons feed Real 18+.
        val autoNsfw = settings.autoNsfwFromAddons.first()
        val stremio = async {
            if (!autoNsfw) emptyList()
            else runCatching { stremioNsfwRows(MediaType.REAL) }.getOrDefault(emptyList())
        }
        val home = builtIn.await()
        (home.rows + stremio.await()) to home.errors
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
                    runCatching {
                        val items = StremioClient.fetchCatalog(addon, cat.type, cat.id)
                        if (items.isEmpty()) null
                        else CatalogRow("${addon.name} \u00b7 ${cat.name}", items, addon.id, cat.id, section)
                    }.getOrNull()
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
    suspend fun searchReal(query: String): AdultRepository.Search = coroutineScope {
        val builtIn = AdultRepository.searchDetailed(query)
        if (builtIn.items.isNotEmpty()) return@coroutineScope builtIn

        val autoNsfw = settings.autoNsfwFromAddons.first()
        val stremio = if (!autoNsfw) emptyList() else runCatching {
            addonRepo.enabledAddons().filter { it.nsfw }.flatMap { addon ->
                StremioClient.catalogsFor(addon, MediaType.REAL)
                    .filter { cat -> cat.extra.any { it.name == "search" } }
                    .map { cat ->
                        async {
                            runCatching { StremioClient.search(addon, cat.type, cat.id, query) }
                                .getOrDefault(emptyList())
                        }
                    }
            }.flatMap { it.await() }
        }.getOrDefault(emptyList())

        AdultRepository.Search(
            items = stremio.filter { it.type == MediaType.REAL }.distinctBy { it.key },
            errors = builtIn.errors,
        )
    }

    suspend fun search(query: String, section: MediaType): List<MediaItem> {
        // Real 18+ has its own path so built-in sources are authoritative.
        if (section == MediaType.REAL) return searchReal(query).items
        return searchInternal(query, section)
    }

    private suspend fun searchInternal(query: String, section: MediaType): List<MediaItem> = coroutineScope {
        val isNsfwSection = section == MediaType.NSFW_ANIME ||
            section == MediaType.NSFW_MANGA ||
            section == MediaType.REAL

        val results = mutableListOf<MediaItem>()
        when (section) {
            MediaType.MOVIE -> results += runCatching { TmdbClient.searchMovies(query) }.getOrDefault(emptyList())
            MediaType.SERIES -> results += runCatching { TmdbClient.searchTv(query) }.getOrDefault(emptyList())
            MediaType.ANIME -> results += runCatching { AniListClient.searchAnime(query) }.getOrDefault(emptyList())
            MediaType.MANGA -> results += runCatching { MangaDexClient.search(query) }.getOrDefault(emptyList())
            MediaType.NSFW_ANIME -> {
                // Strictly adult: AniList isAdult:true + Jikan rating=rx only.
                results += runCatching { AniListClient.nsfwSearch(query) }.getOrDefault(emptyList())
                results += runCatching { JikanClient.nsfwSearch(query) }.getOrDefault(emptyList())
            }
            MediaType.NSFW_MANGA -> results += runCatching { MangaDexClient.search(query, nsfw = true) }.getOrDefault(emptyList())
            MediaType.REAL -> Unit // Never reached: search() routes REAL through searchReal().
        }

        // Stremio addon search. For NSFW sections ONLY NSFW-flagged add-ons are queried,
        // so regular (SFW) add-ons can never leak normal content into NSFW results.
        val candidateAddons = addonRepo.enabledAddons().let { addons ->
            if (isNsfwSection) addons.filter { it.nsfw } else addons
        }
        val jobs = candidateAddons.flatMap { addon ->
            StremioClient.catalogsFor(addon, section)
                .filter { cat -> cat.extra.any { it.name == "search" } }
                .map { cat ->
                    async {
                        runCatching { StremioClient.search(addon, cat.type, cat.id, query) }.getOrDefault(emptyList())
                    }
                }
        }
        jobs.forEach { results += it.await() }

        // Final safety net: NSFW sections must never surface non-adult built-in items.
        val filtered = if (isNsfwSection) {
            results.filter { it.addonId != "anilist" && it.addonId != "jikan" && it.addonId != "mangadex" || it.type == section }
        } else {
            results
        }
        filtered.distinctBy { it.key }
    }

    suspend fun globalSearch(query: String): List<MediaItem> = coroutineScope {
        val results = mutableListOf<MediaItem>()
        val jobs = listOf(
            async { runCatching { TmdbClient.search(query) }.getOrDefault(emptyList()) },
            async { runCatching { AniListClient.searchAnime(query) }.getOrDefault(emptyList()) },
            async { runCatching { MangaDexClient.search(query) }.getOrDefault(emptyList()) },
        )
        jobs.forEach { results += it.await() }
        val addonJobs = addonRepo.enabledAddons().flatMap { addon ->
            addon.catalogs.filter { c -> c.extra.any { it.name == "search" } }.map { cat ->
                async { runCatching { StremioClient.search(addon, cat.type, cat.id, query) }.getOrDefault(emptyList()) }
            }
        }
        addonJobs.forEach { results += it.await() }
        results.distinctBy { it.key }
    }

    // ---- Stremio rows -------------------------------------------------------

    private suspend fun stremioRowsFor(types: Set<MediaType>): List<CatalogRow> = coroutineScope {
        val addons = addonRepo.enabledAddons().filter { !it.nsfw }
        val jobs = addons.flatMap { addon ->
            addon.catalogs
                .filter { cat -> types.any { StremioClient.mapType(cat.type, addon) == it } }
                .map { cat ->
                    async {
                        runCatching {
                            val items = StremioClient.fetchCatalog(addon, cat.type, cat.id)
                            if (items.isEmpty()) null
                            else CatalogRow("${addon.name} · ${cat.name}", items, addon.id, cat.id, StremioClient.mapType(cat.type, addon))
                        }.getOrNull()
                    }
                }
        }
        jobs.mapNotNull { it.await() }
    }

    suspend fun loadCatalog(addon: Addon, catalog: CatalogDef, extra: Map<String, String> = emptyMap()): List<MediaItem> =
        runCatching { StremioClient.fetchCatalog(addon, catalog.type, catalog.id, extra) }.getOrDefault(emptyList())
}
