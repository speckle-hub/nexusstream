package com.novastream.app

import com.google.gson.Gson
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import com.novastream.app.ui.player.isPlayableStreamSource
import com.novastream.app.ui.player.isValidPlayerItem
import com.novastream.app.ui.player.safeStreamAlternates
import com.novastream.app.ui.player.safeStreamHeaders
import com.novastream.app.ui.player.sanitizeEpisodes
import com.novastream.app.ui.player.sanitizeSubtitleTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 Play-crash guardrails: player launch validation (pure JVM, no player needed).
 *
 * Reproduces the Gson null-field hole on the exact intent payload shapes
 * `PlayerActivity` decodes (`MediaItem` / `StreamSource` / `SubtitleTrack` / `Video`):
 * a missing JSON field lands as a runtime null in a non-null slot, and the guards
 * must reject/sanitize it instead of letting it NPE the launch path.
 */
class PlayerStartupTest {

    private val gson = Gson()

    @Test
    fun `well-formed item passes`() {
        val item = MediaItem(id = "tt123", type = MediaType.MOVIE, title = "Dune")
        assertTrue(isValidPlayerItem(item))
    }

    @Test
    fun `null item fails`() {
        assertFalse(isValidPlayerItem(null))
    }

    @Test
    fun `gson-hole item fails without throwing`() {
        // "{}" decodes every non-null slot to a runtime null (Unsafe, no defaults applied).
        val holey = gson.fromJson("{}", MediaItem::class.java)
        // The hole is real: touching the poisoned slot throws.
        var threw = false
        try {
            holey.id.isNotBlank()
        } catch (t: Throwable) {
            threw = true
        }
        assertTrue(threw)
        // ...but the guard absorbs it.
        assertFalse(isValidPlayerItem(holey))
    }

    @Test
    fun `blank id or title fails`() {
        assertFalse(isValidPlayerItem(MediaItem(id = " ", type = MediaType.MOVIE, title = "X")))
        assertFalse(isValidPlayerItem(MediaItem(id = "a", type = MediaType.MOVIE, title = "")))
    }

    @Test
    fun `direct and external urls are playable`() {
        assertTrue(isPlayableStreamSource(StreamSource(url = "https://cdn/x.mp4")))
        assertTrue(isPlayableStreamSource(StreamSource(externalUrl = "https://cdn/y.mp4")))
        assertFalse(isPlayableStreamSource(null))
        assertFalse(isPlayableStreamSource(StreamSource()))
        assertFalse(isPlayableStreamSource(StreamSource(url = "  ")))
    }

    @Test
    fun `missing urls are not playable`() {
        val holey = gson.fromJson("{\"headers\":{\"X\":null}}", StreamSource::class.java)
        assertFalse(isPlayableStreamSource(holey))
    }

    @Test
    fun `subtitle sanitizer keeps good and drops poisoned entries`() {
        val good = SubtitleTrack(id = "s1", url = "https://cdn/a.srt", lang = "en")
        val blank = SubtitleTrack(id = "s2", url = " ", lang = "en")
        val holey = gson.fromJson("{\"id\":\"s3\",\"lang\":\"en\"}", SubtitleTrack::class.java)
        assertEquals(listOf(good), sanitizeSubtitleTracks(listOf(good, blank, holey)))
    }

    @Test
    fun `header sanitizer drops null map and null or blank entries`() {
        val clean = StreamSource(url = "https://cdn/x.mp4", headers = mapOf("Referer" to "https://site/"))
        assertEquals(mapOf("Referer" to "https://site/"), safeStreamHeaders(clean))
        val holey = gson.fromJson(
            "{\"url\":\"https://cdn/x.mp4\",\"headers\":{\"Referer\":null,\"\":\"x\",\"UA\":\" \"}}",
            StreamSource::class.java,
        )
        assertTrue(safeStreamHeaders(holey).isEmpty())
        val nullMap = gson.fromJson("{\"url\":\"https://cdn/x.mp4\"}", StreamSource::class.java)
        assertTrue(safeStreamHeaders(nullMap).isEmpty())
    }

    @Test
    fun `alternates sanitizer drops null list and blanks`() {
        val withAlts = StreamSource(url = "https://a/x.mp4", alternates = listOf("https://b/x.mp4", " "))
        assertEquals(listOf("https://b/x.mp4"), safeStreamAlternates(withAlts))
        val nullList = gson.fromJson("{\"url\":\"https://a/x.mp4\"}", StreamSource::class.java)
        assertTrue(safeStreamAlternates(nullList).isEmpty())
    }

    @Test
    fun `episode sanitizer drops poisoned ids`() {
        val good = Video(id = "ep1", title = "Pilot")
        val holey = gson.fromJson("{\"title\":\"Pilot\"}", Video::class.java)
        assertEquals(listOf(good), sanitizeEpisodes(listOf(good, holey)))
    }
}
