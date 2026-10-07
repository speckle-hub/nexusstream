package com.novastream.app.data.integrations

import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry

/**
 * Aggregate watch/read metrics shown in the Library's Stats tab.
 *
 * Pure and side-effect free so it can be unit-tested without Android.
 */
data class WatchStats(
    val watchedItems: Int,
    val watchedHours: Double,
    val completed: Int,
    val favorites: Int,
    val topGenres: List<Pair<String, Int>>,
    val byType: List<Pair<String, Int>>,
) {
    companion object {
        /** A title is "completed" once it is watched past this fraction of its duration. */
        const val COMPLETED_THRESHOLD = 0.95f

        fun compute(history: List<WatchEntry>, favorites: List<MediaItem>): WatchStats {
            val watchedMs = history.sumOf { it.positionMs.coerceAtLeast(0) }
            val completed = history.count {
                it.durationMs > 0 && it.positionMs >= (it.durationMs * COMPLETED_THRESHOLD).toLong()
            }
            val genreCounts = HashMap<String, Int>()
            history.forEach { e -> e.item.genres.forEach { g -> genreCounts[g] = (genreCounts[g] ?: 0) + 1 } }
            favorites.forEach { f -> f.genres.forEach { g -> genreCounts[g] = (genreCounts[g] ?: 0) + 1 } }
            val byType = history.groupingBy { it.item.type.label }.eachCount()
            return WatchStats(
                watchedItems = history.size,
                watchedHours = watchedMs / 3_600_000.0,
                completed = completed,
                favorites = favorites.size,
                topGenres = genreCounts.entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
                byType = byType.entries.sortedByDescending { it.value }.map { it.key to it.value },
            )
        }
    }
}
