package com.moovio.desktop

import com.moovio.desktop.domain.StreamRanking
import com.moovio.desktop.domain.model.StreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreamRankingTest {

    @Test
    fun `detects quality from add-on titles`() {
        assertEquals("2160p", StreamRanking.qualityOf("Movie.2024.2160p.WEB-DL"))
        assertEquals("1080p", StreamRanking.qualityOf("1080p", "BluRay"))
        assertEquals("720p", StreamRanking.qualityOf(null, "HDR 720p x264"))
        assertNull(StreamRanking.qualityOf("CAM rip"))
        assertEquals("2160p", StreamRanking.qualityOf("4K HDR"))
        assertEquals("1080p", StreamRanking.qualityOf("BluRay REMUX"))
    }

    @Test
    fun `parses seeders from common labels`() {
        assertEquals(1234, StreamRanking.seedersOf("👤 1234"))
        assertEquals(98, StreamRanking.seedersOf("Seeders: 98"))
        assertEquals(7, StreamRanking.seedersOf("7 seeders"))
        assertNull(StreamRanking.seedersOf("no swarm info"))
    }

    @Test
    fun `direct streams rank above torrents and higher quality wins`() {
        val torrent4k = StreamSource(
            infoHash = "abc",
            fileIdx = 0,
            title = "1080p torrent",
            seeders = 500,
            addonId = "a",
            addonName = "A",
        )
        val direct720 = StreamSource(
            url = "https://example.com/720.mp4",
            title = "720p direct",
            addonId = "b",
            addonName = "B",
        )
        val direct1080 = StreamSource(
            url = "https://example.com/1080.mp4",
            title = "1080p direct",
            addonId = "c",
            addonName = "C",
        )
        val ranked = StreamRanking.rank(listOf(torrent4k, direct720, direct1080))
        assertEquals(direct1080.fingerprint, ranked[0].fingerprint)
        assertEquals(direct720.fingerprint, ranked[1].fingerprint)
        assertEquals(torrent4k.fingerprint, ranked[2].fingerprint)
    }

    @Test
    fun `best playable ignores torrent-only results`() {
        val torrent = StreamSource(infoHash = "abc", fileIdx = 0, addonId = "a", addonName = "A")
        assertNull(StreamRanking.bestPlayable(listOf(torrent)))
    }

    @Test
    fun `deduplicates identical urls`() {
        val first = StreamSource(url = "https://x/1.m3u8", addonId = "a", addonName = "A")
        val second = first.copy(addonId = "b", addonName = "B")
        val ranked = StreamRanking.rank(listOf(first, second))
        assertEquals(1, ranked.size)
        assertEquals("a", ranked.first().addonId)
    }

    @Test
    fun `enrich keeps explicit quality`() {
        val source = StreamSource(
            url = "https://example.com/a.mp4",
            quality = "480p",
            title = "Some 1080p title",
            addonId = "a",
            addonName = "A",
        )
        assertEquals("480p", StreamRanking.enrich(source).quality)
        assertTrue(StreamRanking.rank(listOf(source)).isNotEmpty())
    }
}
