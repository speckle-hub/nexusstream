package com.moovio.desktop

import androidx.compose.runtime.snapshots.Snapshot
import com.moovio.desktop.di.AppContainer
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The NSFW gate is driven by `container.isNsfwUnlocked` read during composition. If that flag is a
 * plain field the lock screen never recomposes after a correct PIN, so it must be Compose state.
 */
class NsfwSessionTest {

    private val previousDataDir = System.getProperty("moovio.data.dir")

    @BeforeTest
    fun useTemporaryDataDir() {
        System.setProperty("moovio.data.dir", Files.createTempDirectory("moovio-nsfw").toString())
    }

    @AfterTest
    fun restoreDataDir() {
        if (previousDataDir == null) System.clearProperty("moovio.data.dir")
        else System.setProperty("moovio.data.dir", previousDataDir)
    }

    @Test
    fun `nsfw unlock is snapshot state that composition can observe`() {
        val container = AppContainer()
        try {
            assertFalse(container.isNsfwUnlocked)

            val reads = mutableListOf<Any>()
            fun readLikeComposition(): Boolean =
                Snapshot.observe(readObserver = { reads.add(it) }) { container.isNsfwUnlocked }

            assertFalse(readLikeComposition())
            assertTrue(
                reads.isNotEmpty(),
                "isNsfwUnlocked must be Compose state or the lock screen cannot recompose after unlock",
            )

            container.unlockNsfw()
            assertTrue(readLikeComposition())

            container.lockNsfw()
            assertFalse(readLikeComposition())
        } finally {
            container.dispose()
        }
    }
}
