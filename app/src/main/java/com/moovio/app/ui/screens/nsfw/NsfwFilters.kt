package com.moovio.app.ui.screens.nsfw

import com.moovio.app.data.model.CatalogRow
import com.moovio.app.data.model.MediaItem

/**
 * Phase 36 source-filter chips for the Real 18+ hub.
 *
 * Pure-JVM filtering so the chip behaviour is unit-tested: a selected source id
 * keeps only that source's rows and results (`null`/blank = unfiltered "All").
 * Built-in rows/results carry the source id as `addonId` (see
 * `AdultRow.toCatalogRow` and `AdultVideo.toMediaItem`); Stremio add-on rows
 * carry their add-on id instead, so they are hidden while a built-in filter is
 * active rather than leaking through it.
 */
fun applySourceFilter(
    rows: List<CatalogRow>,
    results: List<MediaItem>,
    sourceId: String?,
): Pair<List<CatalogRow>, List<MediaItem>> {
    if (sourceId.isNullOrBlank()) return rows to results
    val keptRows = rows.filter { it.addonId == sourceId }
    val keptResults = results.filter { it.addonId == sourceId }
    return keptRows to keptResults
}
