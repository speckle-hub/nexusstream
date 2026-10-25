package com.moovio.desktop

import com.moovio.desktop.data.local.AppSettings
import com.moovio.desktop.data.local.JsonFile
import com.moovio.desktop.data.local.LibraryState
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.WatchProgress
import com.moovio.desktop.security.PinHasher
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SecurityAndPersistenceTest {

    @Test
    fun `pin hashing verifies and rejects wrong pin`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertTrue(PinHasher.verify("4821", salt, hash))
        assertFalse(PinHasher.verify("4822", salt, hash))
        assertFalse(PinHasher.verify("4821", PinHasher.newSalt(), hash))
    }

    @Test
    fun `pin salt is random and hashes are not plaintext`() {
        val first = PinHasher.newSalt()
        val second = PinHasher.newSalt()
        assertTrue(first != second)
        val hash = PinHasher.hash("1234", first)
        assertFalse(hash.contains("1234"))
    }

    @Test
    fun `pin validation accepts 4 to 8 digits`() {
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("123456789"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }

    @Test
    fun `library file round trips through atomic json store`() = runBlocking {
        val dir = Files.createTempDirectory("moovio-test").toFile()
        val file = File(dir, "library.json")
        val store = JsonFile(file, LibraryState.serializer()) { LibraryState() }

        val item = MediaItem(id = "tt0111161", type = MediaType.MOVIE, name = "The Shawshank Redemption")
        val progress = WatchProgress(
            mediaKey = item.key,
            mediaId = item.id,
            type = MediaType.MOVIE,
            name = item.name,
            positionMs = 60_000,
            durationMs = 120_000,
            updatedAt = 1,
        )
        store.write(LibraryState(favorites = listOf(item), progress = listOf(progress)))
        val loaded = store.read()
        assertEquals("The Shawshank Redemption", loaded.favorites.single().name)
        assertEquals(60_000, loaded.progress.single().positionMs)
        dir.deleteRecursively()
    }

    @Test
    fun `corrupt settings file falls back to defaults`() {
        val dir = Files.createTempDirectory("moovio-test").toFile()
        val file = File(dir, "settings.json")
        file.writeText("{not json")
        val store = JsonFile(file, AppSettings.serializer()) { AppSettings() }
        assertEquals(AppSettings(), store.read())
        dir.deleteRecursively()
    }
}
