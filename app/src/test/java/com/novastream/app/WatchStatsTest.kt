package com.novastream.app

import com.novastream.app.data.integrations.WatchStats
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.WatchEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchStatsTest {

    private fun movie(id: String, genres: List<String> = emptyList()) =
        MediaItem(id = id, type = MediaType.MOVIE, title = "Title $id", genres = genres)

    @Test
    fun `watched hours sum the recorded positions`() {
        val history = listOf(
            WatchEntry(movie("a"), null, null, 3_600_000, 7_200_000),
            WatchEntry(movie("b"), null, null, 1_800_000, 7_200_000),
        )
        val stats = WatchStats.compute(history, emptyList())
        assertEquals(1.5, stats.watchedHours, 0.001)
        assertEquals(2, stats.watchedItems)
    }

    @Test
    fun `completed uses the 95 percent threshold`() {
        val history = listOf(
            // 96% watched -> completed
            WatchEntry(movie("a"), null, null, 9_600, 10_000),
            // 90% watched -> not completed
            WatchEntry(movie("b"), null, null, 9_000, 10_000),
            // unknown duration -> not completed
            WatchEntry(movie("c"), null, null, 5_000, 0),
        )
        val stats = WatchStats.compute(history, emptyList())
        assertEquals(1, stats.completed)
    }

    @Test
    fun `top genres favour the most frequent and include favourites`() {
        val history = listOf(
            WatchEntry(movie("a", listOf("Action", "Sci-Fi")), null, null, 1_000, 2_000),
            WatchEntry(movie("b", listOf("Action")), null, null, 1_000, 2_000),
        )
        val favorites = listOf(movie("c", listOf("Action", "Comedy")))
        val stats = WatchStats.compute(history, favorites)
        // Action appears 3x, Sci-Fi 1x, Comedy 1x.
        assertEquals("Action" to 3, stats.topGenres.first())
        assertEquals(1, stats.favorites)
        assertTrue(stats.topGenres.any { it.first == "Comedy" })
    }
}
