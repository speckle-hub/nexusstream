package com.moovio.desktop.data.repo

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.remote.AniListClient
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class CatalogRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)

data class AddonFailure(val addon: String, val message: String)

/**
 * Aggregates metadata for the home feed, catalog grids and search.
 *
 * Every add-on call is wrapped in its own timeout + try/catch, so a broken add-on only removes
 * its own row instead of failing the page.
 */
class CatalogRepository(
    private val cinemeta: CinemetaClient,
    private val aniList: AniListClient,
    private val stremio: StremioClient,
    private val addonStore: AddonStore,
) {
    private val enabledAddons: List<Addon> get() = addonStore.current.filter { it.enabled }

    suspend fun hero(): List<MediaItem> = withContext(Dispatchers.IO) {
        runCatching {
            val movies = cinemeta.catalog(MediaType.MOVIE, "top")
                .filter { !it.background.isNullOrBlank() }
            val series = cinemeta.catalog(MediaType.SERIES, "top")
                .filter { !it.background.isNullOrBlank() }
            interleave(movies, series).take(8)
        }.getOrDefault(emptyList())
    }

    /**
     * Explicitly requested home rows. Each is a separate unit of work so the UI can paint rows
     * progressively as they arrive.
     */
    fun curatedRows(): List<HomeRowSpec> = listOf(
        HomeRowSpec("trending-movies", "Trending Movies") {
            cinemeta.catalog(MediaType.MOVIE, "top").take(20)
        },
        HomeRowSpec("top-series", "Top Series") {
            cinemeta.catalog(MediaType.SERIES, "top").take(20)
        },
        HomeRowSpec("anime-trending", "Anime · Trending") {
            aniList.trending(24)
        },
        HomeRowSpec("anime-popular", "Anime · Popular") {
            aniList.popular(24)
        },
        HomeRowSpec("anime-top", "Anime · Top Rated") {
            aniList.topRated(24)
        },
    )

    /** Rows supplied by installed add-ons (non-adult ones only). */
    suspend fun addonRows(includeAdult: Boolean = false): List<CatalogRow> = withContext(Dispatchers.IO) {
        val addons = enabledAddons.filter { addon ->
            addon.supportsCatalog &&
                !addon.builtIn &&
                (includeAdult || !addon.adult) &&
                addon.catalogs.isNotEmpty()
        }
        coroutineScope {
            addons.map { addon ->
                async {
                    addon.catalogs
                        .filter { it.id != "top" || addon.id != CinemetaClient.CINEMETA_ID }
                        .mapNotNull { catalog ->
                            val items = withTimeoutOrNull(12_000) {
                                runCatching {
                                    stremio.catalog(addon, catalog.type, catalog.id, search = null)
                                }.getOrNull()
                            }
                            if (items.isNullOrEmpty()) null
                            else CatalogRow(
                                id = "addon:${addon.id}:${catalog.type}:${catalog.id}",
                                title = catalog.name,
                                items = items.take(20).map {
                                    it.copy(adult = addon.adult, addonId = addon.id)
                                },
                            )
                        }
                }
            }.awaitAll().flatten()
        }
    }

    suspend fun animeRows(): List<CatalogRow> = withContext(Dispatchers.IO) {
        listOf(
            async { CatalogRow("anime-trending", "Trending Now", aniList.trending(30)) },
            async { CatalogRow("anime-popular", "All-Time Popular", aniList.popular(30)) },
            async { CatalogRow("anime-top", "Highest Rated", aniList.topRated(30)) },
        ).awaitAll().filter { it.items.isNotEmpty() }
    }

    suspend fun browse(type: MediaType, genre: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        when (type) {
            MediaType.MOVIE -> cinemeta.catalog(MediaType.MOVIE, "top", genre = genre)
            MediaType.SERIES -> cinemeta.catalog(MediaType.SERIES, "top", genre = genre)
            MediaType.ANIME -> aniList.topRated(40)
            MediaType.MANGA -> emptyList()
        }
    }

    /**
     * Global search. Metadata providers and add-on catalogs are queried in parallel; NSFW
     * results are stripped unless the caller is the PIN-gated screen.
     */
    suspend fun search(
        query: String,
        includeAdult: Boolean = false,
        adultOnly: Boolean = false,
    ): Pair<List<MediaItem>, List<AddonFailure>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<MediaItem>() to emptyList()
        val failures = java.util.Collections.synchronizedList(mutableListOf<AddonFailure>())
        val addonSearchable = enabledAddons.filter { addon ->
            addon.supportsCatalog && addon.catalogs.any { it.searchable } &&
                (includeAdult || !addon.adult) && (!adultOnly || addon.adult)
        }

        val results = coroutineScope {
            val meta = listOf(
                async {
                    runCatching {
                        cinemeta.search(MediaType.MOVIE, query) + cinemeta.search(MediaType.SERIES, query)
                    }.getOrElse {
                        failures.add(AddonFailure("Cinemeta", it.message ?: "request failed"))
                        emptyList()
                    }
                },
                async {
                    runCatching { aniList.search(query, 30) }.getOrElse {
                        failures.add(AddonFailure("AniList", it.message ?: "request failed"))
                        emptyList()
                    }
                },
                async {
                    addonSearchable.flatMap { addon ->
                        addon.catalogs.filter { it.searchable }.mapNotNull { catalog ->
                            withTimeoutOrNull(8_000) {
                                runCatching {
                                    stremio.catalog(addon, catalog.type, catalog.id, search = query)
                                }.onFailure {
                                    failures.add(AddonFailure(addon.name, it.message ?: "request failed"))
                                }.getOrNull()
                            }
                        }.flatten().map { it.copy(adult = addon.adult, addonId = addon.id) }
                    }
                },
            ).awaitAll().flatten()
            meta
        }

        val filtered = results.filter { includeAdult || !it.adult }
        val seen = HashSet<String>()
        val deduped = filtered
            .filter { seen.add("${it.name.lowercase()}|${it.year ?: 0}") }
            .sortedByDescending { relevance(it, query) }
        deduped to failures.toList()
    }

    private fun relevance(item: MediaItem, query: String): Double {
        val name = item.name.lowercase()
        val q = query.lowercase().trim()
        val titleScore = when {
            name == q -> 100.0
            name.startsWith(q) -> 80.0
            name.contains(q) -> 55.0
            else -> tokenOverlap(name, q) * 30.0
        }
        return titleScore + (item.rating ?: 0.0) + if (item.poster != null) 1.0 else 0.0
    }

    private fun tokenOverlap(name: String, query: String): Double {
        val tokens = query.split(' ').filter { it.length > 2 }
        if (tokens.isEmpty()) return 0.0
        return tokens.count { name.contains(it) }.toDouble() / tokens.size
    }

    private fun interleave(a: List<MediaItem>, b: List<MediaItem>): List<MediaItem> {
        val output = mutableListOf<MediaItem>()
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            a.getOrNull(i)?.let(output::add)
            b.getOrNull(i)?.let(output::add)
        }
        return output
    }
}

class HomeRowSpec(
    val id: String,
    val title: String,
    val loader: suspend () -> List<MediaItem>,
)
