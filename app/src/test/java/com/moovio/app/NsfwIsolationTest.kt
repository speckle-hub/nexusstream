package com.moovio.app

import com.moovio.app.data.model.CatalogRow
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.WatchEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 28 isolation: adult titles must be scrubbable from every public surface at the model
 * level, so Home / global search / public Library rails stay adult-free while the gated NSFW
 * Hub keeps its own isolated flows.
 */
class NsfwIsolationTest {

    private fun item(id: String, type: MediaType) =
        MediaItem(id = id, type = type, title = "Title $id")

    @Test
    fun `adult media types are exactly the three NSFW kinds`() {
        assertTrue(MediaType.NSFW_ANIME.isAdult)
        assertTrue(MediaType.NSFW_MANGA.isAdult)
        assertTrue(MediaType.REAL.isAdult)
        assertFalse(MediaType.MOVIE.isAdult)
        assertFalse(MediaType.SERIES.isAdult)
        assertFalse(MediaType.ANIME.isAdult)
        assertFalse(MediaType.MANGA.isAdult)
    }

    @Test
    fun `media item mirrors its type adult flag`() {
        assertTrue(item("a", MediaType.REAL).isAdult)
        assertTrue(item("b", MediaType.NSFW_ANIME).isAdult)
        assertTrue(item("c", MediaType.NSFW_MANGA).isAdult)
        assertFalse(item("d", MediaType.MOVIE).isAdult)
        assertFalse(item("e", MediaType.ANIME).isAdult)
        assertFalse(item("f", MediaType.MANGA).isAdult)
    }

    @Test
    fun `public continue watching keeps SFW and drops adult`() {
        val history = listOf(
            WatchEntry(item("m1", MediaType.MOVIE), null, null, 1_000, 10_000),
            WatchEntry(item("h1", MediaType.NSFW_ANIME), null, null, 1_000, 10_000),
            WatchEntry(item("r1", MediaType.REAL), null, null, 1_000, 10_000),
            WatchEntry(item("s1", MediaType.SERIES), null, null, 1_000, 10_000),
        )
        // Mirrors HomeViewModel/LibraryViewModel public filtering.
        val public = history.filter { !it.item.isReadable && !it.item.isAdult }
        assertEquals(listOf("m1", "s1"), public.map { it.item.id })
    }

    @Test
    fun `public continue reading keeps SFW manga and drops NSFW manga`() {
        val history = listOf(
            WatchEntry(item("mg1", MediaType.MANGA), null, null, 50, 100),
            WatchEntry(item("nm1", MediaType.NSFW_MANGA), null, null, 50, 100),
        )
        val public = history.filter { it.item.isReadable && !it.item.isAdult }
        assertEquals(listOf("mg1"), public.map { it.item.id })
    }

    @Test
    fun `hub flows keep only adult entries`() {
        val history = listOf(
            WatchEntry(item("m1", MediaType.MOVIE), null, null, 1_000, 10_000),
            WatchEntry(item("h1", MediaType.NSFW_ANIME), null, null, 1_000, 10_000),
            WatchEntry(item("r1", MediaType.REAL), null, null, 1_000, 10_000),
            WatchEntry(item("nm1", MediaType.NSFW_MANGA), null, null, 50, 100),
            WatchEntry(item("mg1", MediaType.MANGA), null, null, 50, 100),
        )
        // Mirrors NsfwViewModel hub filtering.
        val hubWatching = history.filter { it.item.isAdult && !it.item.isReadable }
        val hubReading = history.filter { it.item.isAdult && it.item.isReadable }
        assertEquals(listOf("h1", "r1"), hubWatching.map { it.item.id })
        assertEquals(listOf("nm1"), hubReading.map { it.item.id })
    }

    @Test
    fun `public row scrub drops adult posters and empty rows`() {
        val rows = listOf(
            CatalogRow("Mixed", listOf(item("m1", MediaType.MOVIE), item("h1", MediaType.NSFW_ANIME))),
            CatalogRow("Adult only", listOf(item("r1", MediaType.REAL))),
            CatalogRow("Clean", listOf(item("s1", MediaType.SERIES))),
        )
        // Mirrors CatalogRepository scrubbed(): drop adult items, drop emptied rows.
        val scrubbed = rows.mapNotNull { row ->
            val kept = row.items.filterNot { it.isAdult }
            if (kept.isEmpty()) null else row.copy(items = kept)
        }
        assertEquals(2, scrubbed.size)
        assertEquals(listOf("m1"), scrubbed[0].items.map { it.id })
        assertEquals(listOf("s1"), scrubbed[1].items.map { it.id })
    }

    @Test
    fun `public favourites keep SFW and drop adult`() {
        val favs = listOf(
            item("m1", MediaType.MOVIE),
            item("h1", MediaType.NSFW_ANIME),
            item("a1", MediaType.ANIME),
            item("r1", MediaType.REAL),
        )
        val public = favs.filterNot { it.isAdult }
        val hub = favs.filter { it.isAdult }
        assertEquals(listOf("m1", "a1"), public.map { it.id })
        assertEquals(listOf("h1", "r1"), hub.map { it.id })
    }
}
