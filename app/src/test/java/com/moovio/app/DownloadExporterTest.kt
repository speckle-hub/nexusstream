package com.moovio.app

import com.moovio.app.data.download.DownloadExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM coverage for the HLS export rules (roadmap §2bs): which playlist lines become segments, how a
 * relative segment resolves, and how the SAF directory name is sanitized.
 *
 * [DownloadExporter.export] itself stays device-only — it needs a `ContentResolver`, a SAF tree and
 * a Media3 `SimpleCache` — but the parsing rules that decide whether an exported HLS download is
 * playable are plain string work.
 */
class DownloadExporterTest {

    private val manifestUrl = "https://cdn.example/hls/master.m3u8"

    @Test
    fun `urls after extinf become segments`() {
        val playlist = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXTINF:9.009,
            seg-1.ts
            #EXTINF:9.009,
            seg-2.ts
        """.trimIndent()

        assertEquals(
            listOf("https://cdn.example/hls/seg-1.ts", "https://cdn.example/hls/seg-2.ts"),
            DownloadExporter.parseHlsSegments(playlist, manifestUrl),
        )
    }

    @Test
    fun `relative segments resolve against the manifest directory`() {
        assertEquals(
            "https://cdn.example/hls/a/seg.ts",
            DownloadExporter.resolveSegmentUrl(manifestUrl, "a/seg.ts"),
        )
        assertEquals(
            "https://cdn.example/hls/seg.ts",
            DownloadExporter.resolveSegmentUrl(manifestUrl, "seg.ts"),
        )
    }

    @Test
    fun `absolute segment urls are left untouched`() {
        assertEquals(
            "https://other.cdn/x.ts",
            DownloadExporter.resolveSegmentUrl(manifestUrl, "https://other.cdn/x.ts"),
        )
        assertEquals(
            "HTTP://other.cdn/x.ts",
            DownloadExporter.resolveSegmentUrl(manifestUrl, "HTTP://other.cdn/x.ts"),
        )
    }

    @Test
    fun `a tag between extinf and its url does not lose the segment`() {
        val playlist = """
            #EXTM3U
            #EXTINF:4,
            #EXT-X-BYTERANGE:1000@0
            seg.ts
        """.trimIndent()

        assertEquals(
            listOf("https://cdn.example/hls/seg.ts"),
            DownloadExporter.parseHlsSegments(playlist, manifestUrl),
        )
    }

    @Test
    fun `a master playlist without extinf yields no segments`() {
        val playlist = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000
            low.m3u8
        """.trimIndent()

        assertTrue(DownloadExporter.parseHlsSegments(playlist, manifestUrl).isEmpty())
        assertTrue(DownloadExporter.parseHlsSegments("", manifestUrl).isEmpty())
    }

    @Test
    fun `directory names are sanitized for saf`() {
        assertEquals("A_B_C", DownloadExporter.sanitizeDirName("A/B\\C"))
        assertEquals("Title_ S1_E1", DownloadExporter.sanitizeDirName("Title: S1/E1"))
        assertEquals("Moovio download", DownloadExporter.sanitizeDirName("   "))
    }
}
