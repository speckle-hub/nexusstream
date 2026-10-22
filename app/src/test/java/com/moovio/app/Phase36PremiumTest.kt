package com.moovio.app

import com.moovio.app.data.model.CatalogRow
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.ui.player.NEXT_EPISODE_PREFETCH_MS
import com.moovio.app.ui.player.clampSubtitleSyncMs
import com.moovio.app.ui.player.shouldPrefetchNextEpisode
import com.moovio.app.ui.screens.nsfw.applySourceFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 36 premium pass (pure JVM): Real 18+ source-filter chips, next-episode
 * pre-fetch policy and the 100 ms subtitle-sync granularity.
 */
class Phase36PremiumTest {

    private fun row(sourceId: String?) = CatalogRow(
        title = "Row $sourceId",
        items = listOf(
            MediaItem(id = "a", type = MediaType.REAL, title = "A", addonId = sourceId),
        ),
        addonId = sourceId,
    )

    private fun video(sourceId: String?) = MediaItem(
        id = "v-$sourceId",
        type = MediaType.REAL,
        title = "V",
        addonId = sourceId,
    )

    @Test
    fun `blank filter keeps every row and result`() {
        val rows = listOf(row("pornhub"), row("txxx"))
        val results = listOf(video("pornhub"), video("txxx"))
        val (keptRows, keptResults) = applySourceFilter(rows, results, null)
        assertEquals(2, keptRows.size)
        assertEquals(2, keptResults.size)
        val (keptRows2, _) = applySourceFilter(rows, results, "")
        assertEquals(2, keptRows2.size)
    }

    @Test
    fun `selected source keeps only its rows and results`() {
        val rows = listOf(row("pornhub"), row("txxx"))
        val results = listOf(video("pornhub"), video("txxx"), video("stremio-addon"))
        val (keptRows, keptResults) = applySourceFilter(rows, results, "txxx")
        assertEquals(listOf("txxx"), keptRows.map { it.addonId })
        assertEquals(listOf("txxx"), keptResults.map { it.addonId })
    }

    @Test
    fun `unknown source id filters everything out`() {
        val rows = listOf(row("pornhub"))
        val (keptRows, keptResults) = applySourceFilter(rows, listOf(video("pornhub")), "nope")
        assertTrue(keptRows.isEmpty())
        assertTrue(keptResults.isEmpty())
    }

    @Test
    fun `prefetch horizon is two minutes`() {
        assertEquals(120_000L, NEXT_EPISODE_PREFETCH_MS)
    }

    @Test
    fun `prefetch fires inside the final two minutes with a next episode`() {
        assertTrue(shouldPrefetchNextEpisode(119_000L, 3_600_000L, hasNext = true, alreadyPrefetched = false))
        assertTrue(shouldPrefetchNextEpisode(1L, 3_600_000L, hasNext = true, alreadyPrefetched = false))
    }

    @Test
    fun `prefetch stays off outside the window or without a next episode`() {
        assertFalse(shouldPrefetchNextEpisode(121_000L, 3_600_000L, hasNext = true, alreadyPrefetched = false))
        assertFalse(shouldPrefetchNextEpisode(60_000L, 3_600_000L, hasNext = false, alreadyPrefetched = false))
        assertFalse(shouldPrefetchNextEpisode(60_000L, 3_600_000L, hasNext = true, alreadyPrefetched = true))
        assertFalse(shouldPrefetchNextEpisode(0L, 3_600_000L, hasNext = true, alreadyPrefetched = false))
        assertFalse(shouldPrefetchNextEpisode(60_000L, 0L, hasNext = true, alreadyPrefetched = false))
    }

    @Test
    fun `sync clamp keeps 100 ms granularity values inside range`() {
        assertEquals(150, clampSubtitleSyncMs(150))
        assertEquals(-250, clampSubtitleSyncMs(-250))
        assertEquals(10_000, clampSubtitleSyncMs(10_050))
        assertEquals(-10_000, clampSubtitleSyncMs(-10_050))
    }
}
