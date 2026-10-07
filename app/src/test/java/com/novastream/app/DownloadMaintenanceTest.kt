package com.novastream.app

import com.novastream.app.data.download.DownloadCacheMaintenance
import com.novastream.app.data.download.downloadRequestHeaders
import com.novastream.app.data.remote.UA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DownloadMaintenanceTest {

    // ---- downloadRequestHeaders -------------------------------------------------

    @Test
    fun `stream headers win and are all preserved`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf(
                "User-Agent" to "StreamUA/1.0",
                "Referer" to "https://page.example/watch/1",
                "Cookie" to "age_verified=1",
            ),
            customUserAgent = "CustomUA/9",
            customReferer = "https://global.example",
            customCookie = "global=1",
        )
        assertEquals("StreamUA/1.0", headers["User-Agent"])
        assertEquals("https://page.example/watch/1", headers["Referer"])
        assertEquals("age_verified=1", headers["Cookie"])
    }

    @Test
    fun `custom identity fills only the headers the stream omitted`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf("Referer" to "https://page.example/watch/1"),
            customUserAgent = "CustomUA/9",
            customReferer = "https://global.example",
            customCookie = "global=1",
        )
        assertEquals("CustomUA/9", headers["User-Agent"])
        assertEquals("https://page.example/watch/1", headers["Referer"])
        assertEquals("global=1", headers["Cookie"])
    }

    @Test
    fun `empty stream headers fall back to the app browser UA`() {
        val headers = downloadRequestHeaders(streamHeaders = emptyMap())
        assertEquals(UA, headers["User-Agent"])
        assertFalse(headers.containsKey("Referer"))
        assertFalse(headers.containsKey("Cookie"))
    }

    @Test
    fun `blank stream header values are ignored so custom can fill them`() {
        val headers = downloadRequestHeaders(
            streamHeaders = mapOf("User-Agent" to "  ", "Referer" to ""),
            customUserAgent = "CustomUA/9",
            customReferer = "https://global.example",
        )
        assertEquals("CustomUA/9", headers["User-Agent"])
        assertEquals("https://global.example", headers["Referer"])
    }

    // ---- DownloadCacheMaintenance ----------------------------------------------

    @Test
    fun `referenced chunk is not an orphan but unreferenced one is`() {
        val dir = Files.createTempDirectory("m3cache").toFile()
        val referenced = File(dir, "123.deadbeef.v3.exo").apply { writeText("kept") }
        val orphan = File(dir, "456.cafebabe.v3.exo").apply { writeText("orphan") }
        val names = DownloadCacheMaintenance.referencedNames(listOf(referenced))

        assertFalse(DownloadCacheMaintenance.isOrphan(referenced, names))
        assertTrue(DownloadCacheMaintenance.isOrphan(orphan, names))
        assertEquals(listOf(orphan), DownloadCacheMaintenance.orphans(dir, names))
    }

    @Test
    fun `index files and unrelated files are never orphans`() {
        val dir = Files.createTempDirectory("m3cache").toFile()
        val index = File(dir, "cached_content_index.exi").apply { writeText("{}") }
        val notes = File(dir, "readme.txt").apply { writeText("hi") }
        val names = DownloadCacheMaintenance.referencedNames(listOf(index))

        assertFalse(DownloadCacheMaintenance.isOrphan(index, names))
        assertFalse(DownloadCacheMaintenance.isOrphan(notes, names))
        assertTrue(DownloadCacheMaintenance.orphans(dir, names).isEmpty())
    }

    @Test
    fun `partial tmp and part files are orphans when unreferenced`() {
        val dir = Files.createTempDirectory("m3cache").toFile()
        val tmp = File(dir, "999.001122.v3.tmp").apply { writeText("partial") }
        val part = File(dir, "movie.mp4.part").apply { writeText("partial") }
        val names = emptySet<String>()

        assertTrue(DownloadCacheMaintenance.isOrphan(tmp, names))
        assertTrue(DownloadCacheMaintenance.isOrphan(part, names))
        assertEquals(2, DownloadCacheMaintenance.orphans(dir, names).size)
    }

    @Test
    fun `orphans on a missing directory returns empty`() {
        val missing = File(Files.createTempDirectory("m3cache").toFile(), "does-not-exist")
        assertTrue(DownloadCacheMaintenance.orphans(missing, emptySet()).isEmpty())
        assertTrue(DownloadCacheMaintenance.orphans(null, emptySet()).isEmpty())
    }
}
