package com.moovio.app

import com.moovio.app.data.download.downloadRequestHeaders
import com.moovio.app.data.download.videoDownloadId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VideoDownloadManagerTest {

    // ---- videoDownloadId -------------------------------------------------------

    @Test
    fun `video download id is deterministic for the same item and url`() {
        val itemKey = "tmdb::movie::550"
        val url = "https://example.com/video.mp4"
        assertEquals(videoDownloadId(itemKey, url), videoDownloadId(itemKey, url))
    }

    @Test
    fun `video download id differs for different items or urls`() {
        val idSameItemDiffUrl = videoDownloadId("item-1", "https://example.com/a.mp4")
        val idDiffItemSameUrl = videoDownloadId("item-2", "https://example.com/a.mp4")
        val idDiffItemDiffUrl = videoDownloadId("item-2", "https://example.com/b.mp4")

        assertNotEquals(idSameItemDiffUrl, idDiffItemSameUrl)
        assertNotEquals(idSameItemDiffUrl, idDiffItemDiffUrl)
        assertNotEquals(idDiffItemSameUrl, idDiffItemDiffUrl)
    }

    @Test
    fun `video download id is a valid UUID`() {
        val id = videoDownloadId("tmdb::movie::550", "https://example.com/video.mp4")
        assertNotNull(UUID.fromString(id))
    }

    @Test
    fun `video download id is not blank`() {
        assertTrue(videoDownloadId("item-1", "https://example.com/a.mp4").isNotBlank())
    }

    // ---- downloadRequestHeaders ------------------------------------------------

    @Test
    fun `download request headers preserves all stream headers when present`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf(
                "User-Agent" to "StreamUA/1.0",
                "Referer" to "https://page.example/watch/1",
                "Cookie" to "age_verified=1",
            ),
        )
        assertEquals("StreamUA/1.0", headers["User-Agent"])
        assertEquals("https://page.example/watch/1", headers["Referer"])
        assertEquals("age_verified=1", headers["Cookie"])
    }

    @Test
    fun `download request headers falls back to app UA when stream omits it`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf("Referer" to "https://page.example/watch/1"),
            customUserAgent = "CustomUA/9",
        )
        assertEquals("CustomUA/9", headers["User-Agent"])
    }

    @Test
    fun `download request headers drops blank stream header values`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf("User-Agent" to "  ", "Referer" to ""),
            customUserAgent = "CustomUA/9",
            customReferer = "https://global.example",
            customCookie = "global=1",
        )
        assertEquals("CustomUA/9", headers["User-Agent"])
        assertEquals("https://global.example", headers["Referer"])
        assertEquals("global=1", headers["Cookie"])
    }

    @Test
    fun `download request headers with empty stream headers uses app defaults`() {
        val headers = downloadRequestHeaders(streamHeaders = emptyMap<String, String>())
        assertFalse(headers.isEmpty())
        assertEquals("Mozilla/5.0", headers["User-Agent"]!!.substring(0, 11))
    }
}
