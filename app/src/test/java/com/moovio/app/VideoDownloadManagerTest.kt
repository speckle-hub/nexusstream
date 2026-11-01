package com.moovio.app

import com.moovio.app.data.download.downloadRequestHeaders
import com.moovio.app.data.download.supportsCustomCacheKey
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

    // ---- supportsCustomCacheKey ------------------------------------------------
    //
    // Media3's DownloadRequest asserts `customCacheKey must be null for type: <contentType>` for
    // every adaptive content type. Attaching one unconditionally made every HLS download throw
    // while the request was being built, so no adaptive download could ever be queued.

    @Test
    fun `progressive mp4 accepts a custom cache key`() {
        assertTrue(supportsCustomCacheKey("https://cdn.example/video.mp4"))
        assertTrue(supportsCustomCacheKey("https://cdn.example/v.mp4?sig=abc&exp=1"))
    }

    @Test
    fun `hls by extension refuses a custom cache key`() {
        assertFalse(supportsCustomCacheKey("https://cdn.example/master.m3u8"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/master.m3u8?token=abc"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/MASTER.M3U8"))
    }

    @Test
    fun `hls by mime type refuses a custom cache key without an m3u8 path`() {
        assertFalse(supportsCustomCacheKey("https://cdn.example/play/12345", "application/x-mpegURL"))
    }

    @Test
    fun `dash and smooth streaming refuse a custom cache key`() {
        assertFalse(supportsCustomCacheKey("https://cdn.example/manifest.mpd"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/manifest.ism"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/manifest.isml"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/stream", "application/dash+xml"))
        assertFalse(supportsCustomCacheKey("https://cdn.example/stream", "application/vnd.ms-sstr+xml"))
    }

    @Test
    fun `a non adaptive mime type still accepts a custom cache key`() {
        assertTrue(supportsCustomCacheKey("https://cdn.example/stream", "video/mp4"))
        assertTrue(supportsCustomCacheKey("https://cdn.example/stream", "video/webm"))
    }
}
