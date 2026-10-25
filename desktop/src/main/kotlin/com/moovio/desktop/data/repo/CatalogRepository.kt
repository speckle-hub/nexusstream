package com.moovio.desktop.data.repo

import com.moovio.desktop.data.remote.StremioRepository
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Catalog reads: home rows, search, detail metadata.
 *
 * Falls back to an empty (placeholder) state when no add-on is reachable — screens render
 * empty/placeholder UI instead of crashing.
 */
class CatalogRepository(
    private val stremio: StremioRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun homeRow(type: MediaType, catalogId: String = "top"): List<MediaItem> = withContext(dispatcher) {
        stremio.catalogItems(type, catalogId)
    }

    suspend fun search(query: String): List<MediaItem> = withContext(dispatcher) {
        // Stremio catalog search isn't part of the core protocol; search per add-on catalog id
        // matching the query, then merge. Keep it simple: scan the "top" catalog of each add-on.
        val all = enabledCatalogs().flatMap { it }
        query.lowercase().let { term ->
            all.filter { it.title.lowercase().contains(term) }
        }
    }

    private suspend fun enabledCatalogs(): List<List<MediaItem>> = withContext(dispatcher) {
        // Best-effort: pull a few common catalogs from each enabled add-on.
        val catalogIds = listOf("top", "trending", "popular", "new")
        catalogIds.flatMap { id ->
            stremio.catalogItems(MediaType.MOVIE, id) + stremio.catalogItems(MediaType.TV, id)
        }
    }
}
