package com.novastream.app

import com.novastream.app.data.download.HlsPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsDownloadTest {

    @Test
    fun `detects master playlist and picks highest bandwidth variant`() {
        val master = listOf(
            "#EXTM3U",
            "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360",
            "360/index.m3u8",
            "#EXT-X-STREAM-INF:BANDWIDTH=2400000,RESOLUTION=1280x720",
            "720/index.m3u8",
        ).joinToString("\n")

        assertTrue(HlsPlaylist.isMaster(master))
        assertEquals(
            "https://cdn.example.com/hls/720/index.m3u8",
            HlsPlaylist.pickVariant(master, "https://cdn.example.com/hls/master.m3u8"),
        )
    }

    @Test
    fun `segment uris resolve relative paths and names preserve order`() {
        val media = listOf(
            "#EXTM3U",
            "#EXTINF:10,",
            "seg0.ts",
            "#EXTINF:10,",
            "sub/seg1.ts",
            "#EXT-X-ENDLIST",
        ).joinToString("\n")

        val uris = HlsPlaylist.segmentUris(media, "https://cdn.example.com/hls/index.m3u8")
        assertEquals(
            listOf("https://cdn.example.com/hls/seg0.ts", "https://cdn.example.com/hls/sub/seg1.ts"),
            uris,
        )
        assertEquals(listOf("seg_00000.ts", "seg_00001.ts"), HlsPlaylist.segmentNames(uris))
    }

    @Test
    fun `local playlist rewrites segment lines but keeps tags`() {
        val media = "#EXTM3U\n#EXTINF:10,\nseg0.ts\n#EXTINF:10,\nseg1.ts\n#EXT-X-ENDLIST\n"
        val local = HlsPlaylist.localPlaylist(
            media,
            "https://cdn.example.com/hls/index.m3u8",
            listOf("seg_00000.ts", "seg_00001.ts"),
        )
        assertTrue(local.contains("seg_00000.ts"))
        assertTrue(local.contains("seg_00001.ts"))
        assertFalse(local.contains("seg0.ts\n"))
        assertTrue(local.contains("#EXT-X-ENDLIST"))
        assertTrue(local.contains("#EXTINF:10,"))
    }

    @Test
    fun `non-http base url resolves to the raw reference`() {
        assertEquals("seg.ts", HlsPlaylist.resolve("not a url", "seg.ts"))
    }
}
