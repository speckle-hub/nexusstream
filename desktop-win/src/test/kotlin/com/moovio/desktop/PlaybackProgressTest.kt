package com.moovio.desktop

import com.moovio.desktop.data.local.LibraryStore
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.PlaybackRequest
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.player.MpvSnapshot
import com.moovio.desktop.player.recordPlaybackProgress
import com.moovio.desktop.player.resumePosition
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The player → library → Continue Watching pipeline, independent of the player window.
 */
class PlaybackProgressTest {

    private val episode = MediaItem(id = "tt1196946", type = MediaType.SERIES, name = "The Vampire Diaries")

    private val request = PlaybackRequest(
        media = episode,
        source = StreamSource(
            url = "https://example.com/episode.mp4",
            addonId = "test.streams",
            addonName = "Test streams",
        ),
        season = 1,
        episode = 1,
        episodeTitle = "Pilot",
    )

    private fun newStore(dir: File) = LibraryStore(libraryFile = File(dir, "library.json"))

    @Test
    fun `records title, position, duration and timestamp`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            val before = System.currentTimeMillis()

            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 34_000, durationMs = 2_698_133))

            val saved = library.progressFor(episode.key)
            assertNotNull(saved)
            assertEquals(34_000, saved.positionMs)
            assertEquals(2_698_133, saved.durationMs)
            assertEquals(1, saved.season)
            assertEquals(1, saved.episode)
            assertEquals("Pilot", saved.episodeTitle)
            assertTrue(saved.updatedAt >= before)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `short sessions still appear under continue watching`() {
        // Regression: the library used to require >15 s, so a real short session (or a stream that
        // advanced slowly while buffering) never showed up at all.
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 3_461, durationMs = 2_698_133))

            val row = library.continueWatching().single()
            assertEquals(episode.key, row.mediaKey)
            assertTrue(row.inProgress)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `zero progress and finished titles stay out of continue watching`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 0, durationMs = 100_000))
            assertNull(library.progressFor(episode.key))
            assertTrue(library.continueWatching().isEmpty())

            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 20_000, durationMs = 100_000))
            assertEquals(1, library.continueWatching().size)

            recordPlaybackProgress(
                library,
                request,
                MpvSnapshot(positionMs = 99_000, durationMs = 100_000, ended = true),
            )
            assertTrue(library.continueWatching().isEmpty())
            assertFalse(library.progressFor(episode.key)!!.inProgress)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `saved progress survives a restart`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val first = newStore(dir)
            recordPlaybackProgress(first, request, MpvSnapshot(positionMs = 34_000, durationMs = 2_698_133))
            // What Main.kt does when the window closes while the async writer may still be queued.
            first.flush()

            val second = newStore(dir)
            assertEquals(34_000, second.progressFor(episode.key)?.positionMs)
            assertEquals(1, second.continueWatching().size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `unchanged paused ticks do not rewrite progress`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 34_000, durationMs = 2_698_133))
            val first = library.progressFor(episode.key)!!.updatedAt

            Thread.sleep(5)
            recordPlaybackProgress(
                library,
                request,
                MpvSnapshot(positionMs = 34_000, durationMs = 2_698_133, paused = true),
            )
            assertEquals(first, library.progressFor(episode.key)!!.updatedAt)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `resume starts from the saved position for the same episode`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 34_000, durationMs = 2_698_133))
            val target = PlayTarget(media = episode, season = 1, episode = 1)

            assertEquals(34_000, resumePosition(target, library, autoResume = true))
            assertEquals(0, resumePosition(target, library, autoResume = false))
            assertEquals(0, resumePosition(target.copy(episode = 2), library, autoResume = true))
            assertEquals(9_000, resumePosition(target.copy(startPositionMs = 9_000), library, autoResume = true))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `finished titles restart from the beginning`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(
                library,
                request,
                MpvSnapshot(positionMs = 99_000, durationMs = 100_000, ended = true),
            )
            assertEquals(0, resumePosition(PlayTarget(media = episode), library, autoResume = true))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `streams without a duration are not recorded`() {
        val dir = Files.createTempDirectory("moovio-progress").toFile()
        try {
            val library = newStore(dir)
            recordPlaybackProgress(library, request, MpvSnapshot(positionMs = 40_000, durationMs = 0))
            assertNull(library.progressFor(episode.key))
        } finally {
            dir.deleteRecursively()
        }
    }
}
