package com.moovio.desktop

import com.moovio.desktop.data.local.LibraryStore
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.WatchProgress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class LibraryStoreTest {

    private val dune = MediaItem(id = "tt1160419", type = MediaType.MOVIE, name = "Dune")

    private fun progress(
        key: String = dune.key,
        position: Long = 600_000,
        duration: Long = 9_000_000,
        updatedAt: Long = 5,
        adult: Boolean = false,
    ) = WatchProgress(
        mediaKey = key,
        mediaId = dune.id,
        type = MediaType.MOVIE,
        name = dune.name,
        positionMs = position,
        durationMs = duration,
        updatedAt = updatedAt,
        adult = adult,
    )

    @Test
    fun `favorites toggle and remove`() {
        val dir = Files.createTempDirectory("moovio-library").toFile()
        val store = LibraryStore(libraryFile = java.io.File(dir, "library.json"))
        assertTrue(store.toggleFavorite(dune))
        assertTrue(store.isFavorite(dune))
        assertEquals(1, store.state.value.favorites.size)

        assertFalse(store.toggleFavorite(dune))
        assertFalse(store.isFavorite(dune))
        dir.deleteRecursively()
    }

    @Test
    fun `continue watching ignores finished and adult entries by default`() {
        val dir = Files.createTempDirectory("moovio-library").toFile()
        val store = LibraryStore(libraryFile = java.io.File(dir, "library.json"))
        store.recordProgress(progress())
        store.recordProgress(progress(key = "finished", position = 8_900_000))
        store.recordProgress(progress(key = "adult", position = 500_000, adult = true))

        val rows = store.continueWatching()
        assertEquals(1, rows.size)
        assertEquals(dune.key, rows.single().mediaKey)
        assertEquals(1, store.continueWatching(adult = true).size)
        dir.deleteRecursively()
    }

    @Test
    fun `progress persists across a new store instance`() = runBlocking {
        val dir = Files.createTempDirectory("moovio-library").toFile()
        val file = java.io.File(dir, "library.json")
        val first = LibraryStore(libraryFile = file)
        first.recordProgress(progress(position = 123_000))
        delay(600)

        val second = LibraryStore(libraryFile = file)
        val loaded = second.progressFor(dune.key)
        assertEquals(123_000, loaded?.positionMs)
        assertEquals(1, second.continueWatching().size)
        dir.deleteRecursively()
    }

    @Test
    fun `removing a favorite also clears its progress`() {
        val dir = Files.createTempDirectory("moovio-library").toFile()
        val store = LibraryStore(libraryFile = java.io.File(dir, "library.json"))
        store.toggleFavorite(dune)
        store.recordProgress(progress())
        store.remove(dune)
        assertTrue(store.state.value.favorites.isEmpty())
        assertTrue(store.state.value.progress.isEmpty())
        dir.deleteRecursively()
    }

    @Test
    fun `progress for an episode records season and episode`() {
        val dir = Files.createTempDirectory("moovio-library").toFile()
        val store = LibraryStore(libraryFile = java.io.File(dir, "library.json"))
        store.recordProgress(
            progress().copy(season = 2, episode = 4, episodeTitle = "The Siege"),
        )
        val row = store.continueWatching().single()
        assertEquals(2, row.season)
        assertEquals(4, row.episode)
        assertEquals("The Siege", row.episodeTitle)
        dir.deleteRecursively()
    }
}
