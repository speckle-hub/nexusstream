package com.moovio.app

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.moovio.app.data.download.DownloadStatus
import com.moovio.app.data.download.MangaDownload
import com.moovio.app.data.download.videoDownloadId
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.remote.StremioClient
import com.moovio.app.data.remote.optJsonArray
import com.moovio.app.data.remote.optJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase26P0FixesTest {

    // ---- 1. Manga chapter downloads --------------------------------------------

    @Test
    fun `manga download key and initial state are deterministic`() {
        val download = MangaDownload(
            mangaId = "manga-123",
            mangaTitle = "One Piece",
            chapterId = "ch-1000",
            chapterTitle = "Chapter 1000",
            pages = listOf("https://cdn.example/p1.jpg", "https://cdn.example/p2.jpg"),
        )
        assertEquals("manga-123::ch-1000", download.key)
        assertEquals(DownloadStatus.QUEUED, download.status)
        assertEquals(2, download.pages.size)
    }

    // ---- 2. TMDB client safe JSON parsing --------------------------------------

    @Test
    fun `optJsonArray and optJsonObject safely handle missing or malformed keys`() {
        val json = JsonParser.parseString(
            """
            {
                "validArray": [1, 2, 3],
                "validObject": {"key": "value"},
                "primitiveString": "not an array",
                "primitiveNumber": 42,
                "nullProperty": null
            }
            """.trimIndent()
        ).asJsonObject

        // Valid cases
        assertNotNull(json.optJsonArray("validArray"))
        assertEquals(3, json.optJsonArray("validArray")?.size())
        assertNotNull(json.optJsonObject("validObject"))
        assertEquals("value", json.optJsonObject("validObject")?.get("key")?.asString)

        // Malformed / mismatched types that previously threw ClassCastException
        assertNull(json.optJsonArray("primitiveString"))
        assertNull(json.optJsonArray("primitiveNumber"))
        assertNull(json.optJsonArray("nullProperty"))
        assertNull(json.optJsonArray("missingKey"))

        assertNull(json.optJsonObject("primitiveString"))
        assertNull(json.optJsonObject("validArray"))
        assertNull(json.optJsonObject("nullProperty"))
        assertNull(json.optJsonObject("missingKey"))
    }

    // ---- 3. Video download cache key -------------------------------------------

    @Test
    fun `video download id is deterministic for cache key matching`() {
        val itemKey = "tmdb::movie::550"
        val url = "https://example.com/video.mp4"
        val id1 = videoDownloadId(itemKey, url)
        val id2 = videoDownloadId(itemKey, url)

        assertEquals(id1, id2)
        assertTrue(id1.isNotBlank())
    }

    // ---- 4. Stremio catalog deduplication --------------------------------------

    @Test
    fun `stremio meta item parsing and deduplication by key`() {
        val item1 = JsonParser.parseString(
            """{"id":"tt1234567","name":"Test Movie","type":"movie","poster":"https://img/1.jpg"}"""
        )
        val item2 = JsonParser.parseString(
            """{"id":"tt1234567","name":"Duplicate Test Movie","type":"movie","poster":"https://img/1.jpg"}"""
        )
        val item3 = JsonParser.parseString(
            """{"id":"tt7654321","name":"Another Movie","type":"movie"}"""
        )

        val parsed1 = StremioClient.parseMetaItem(item1, null)
        val parsed2 = StremioClient.parseMetaItem(item2, null)
        val parsed3 = StremioClient.parseMetaItem(item3, null)

        assertNotNull(parsed1)
        assertNotNull(parsed2)
        assertNotNull(parsed3)

        val rawList: List<MediaItem> = listOf(parsed1!!, parsed2!!, parsed3!!)
        assertEquals(3, rawList.size)

        val deduplicated = rawList.distinctBy { it.key }
        assertEquals(2, deduplicated.size)
        assertEquals("local::tt1234567", deduplicated[0].key)
        assertEquals("local::tt7654321", deduplicated[1].key)
    }

    @Test
    fun `stremio meta item parsing ignores non-object elements safely`() {
        val primitiveElement = JsonParser.parseString("\"just a string\"")
        assertNull(StremioClient.parseMetaItem(primitiveElement, null))
    }
}
