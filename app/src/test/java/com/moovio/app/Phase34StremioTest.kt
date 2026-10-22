package com.moovio.app

import com.google.gson.JsonParser
import com.moovio.app.data.remote.StremioClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 34 — Stremio add-on fixes: V3 stream parsing must be total (one poisoned entry can no
 * longer void the list) and id routing must preserve IMDb / season:episode forms.
 */
class Phase34StremioTest {

    @Test
    fun `parseStream accepts a plain url stream`() {
        val el = JsonParser.parseString(
            """{"url":"https://cdn.example.com/v.mp4","name":"CyberFlix","title":"1080p"}"""
        )
        val s = StremioClient.parseStream(el, null)
        assertNotNull(s)
        assertEquals("https://cdn.example.com/v.mp4", s!!.url)
        assertEquals("1080p", s.quality)
    }

    @Test
    fun `parseStream accepts an infoHash torrent stream`() {
        val el = JsonParser.parseString(
            """{"infoHash":"abcdef1234567890","name":"Torrentio","title":"4K Dolby"}"""
        )
        val s = StremioClient.parseStream(el, null)
        assertNotNull(s)
        assertEquals("abcdef1234567890", s!!.infoHash)
        assertTrue(s.isTorrent)
        assertEquals("4K", s.quality)
    }

    @Test
    fun `parseStream rejects an entry with no playable target`() {
        val el = JsonParser.parseString("""{"name":"Empty","title":"nothing here"}""")
        assertNull(StremioClient.parseStream(el, null))
    }

    @Test
    fun `parseStream survives JsonNull fields instead of throwing`() {
        val el = JsonParser.parseString(
            """{"url":null,"infoHash":null,"name":null,"title":null,"description":null,"sources":null,"behaviorHints":null}"""
        )
        assertNull(StremioClient.parseStream(el, null))
    }

    @Test
    fun `parseStream treats blank url as absent but keeps infoHash`() {
        val el = JsonParser.parseString(
            """{"url":"   ","infoHash":"abc123","name":"Torrentio"}"""
        )
        val s = StremioClient.parseStream(el, null)
        assertNotNull(s)
        assertNull(s!!.url)
        assertEquals("abc123", s.infoHash)
    }

    @Test
    fun `parseStream extracts proxyHeaders for hotlink-protected CDNs`() {
        val el = JsonParser.parseString(
            """{"url":"https://cdn.example.com/v.mp4",
               "behaviorHints":{"filename":"Episode 1.mp4",
                 "proxyHeaders":{"request":{"Referer":"https://site.example/","User-Agent":"Custom"}}}}"""
        )
        val s = StremioClient.parseStream(el, null)
        assertNotNull(s)
        assertEquals("Episode 1.mp4", s!!.title)
        assertEquals("https://site.example/", s.headers["Referer"])
        assertEquals("Custom", s.headers["User-Agent"])
    }

    @Test
    fun `parseStream never throws on a non-object entry`() {
        val el = JsonParser.parseString("""["not","an","object"]""").asJsonArray[0]
        assertNull(StremioClient.parseStream(el, null))
    }

    @Test
    fun `candidateRequests offers series form for tt episode ids`() {
        val out = StremioClient.candidateRequests("movie", "tt1234567", false, "tt1234567:1:2")
        assertTrue(out.contains("series" to "tt1234567:1:2"))
        assertTrue(out.last() == "movie" to "tt1234567:1:2")
    }

    @Test
    fun `candidateRequests offers anilist namespace for numeric anime ids`() {
        val out = StremioClient.candidateRequests("anime", "113804", true, null)
        assertTrue(out.contains("anime" to "anilist:113804"))
    }

    @Test
    fun `chooseRequest honours idPrefixes without dropping the addon`() {
        val default = "movie" to "tt1234567:1:2"
        val candidates = listOf("series" to "tt1234567:1:2", "movie" to "tt1234567:1:2")
        // Add-on only serves tt ids — first candidate wins.
        assertEquals(
            "series" to "tt1234567:1:2",
            StremioClient.chooseRequest(default, candidates, listOf("tt")),
        )
        // Add-on serves nothing matching — falls back to default instead of dropping.
        assertEquals(
            default,
            StremioClient.chooseRequest(default, candidates, listOf("kitsu:")),
        )
        // No prefixes — default verbatim.
        assertEquals(default, StremioClient.chooseRequest(default, candidates, emptyList()))
    }
}
