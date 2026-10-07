package com.novastream.app

import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.repo.StreamRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamRankingTest {

    // ---- seedersOf -------------------------------------------------------------

    @Test
    fun `parses the Torrentio person glyph seeder count`() {
        assertEquals(1234, StreamRanking.seedersOf("1080p \uD83D\uDC64 1,234"))
    }

    @Test
    fun `parses seeders and seeds spellings`() {
        assertEquals(42, StreamRanking.seedersOf("42 seeders"))
        assertEquals(7, StreamRanking.seedersOf("Seeds: 7"))
        assertEquals(9, StreamRanking.seedersOf("seed 9"))
    }

    @Test
    fun `returns null when no seeder count is present`() {
        assertNull(StreamRanking.seedersOf("1080p BluRay"))
        assertNull(StreamRanking.seedersOf(null, ""))
    }

    // ---- preferredIndex --------------------------------------------------------

    @Test
    fun `known add-ons rank ahead of unknown ones`() {
        val preferred = listOf("torrentio", "thepiratebay-plus")
        val torrentio = StreamSource(addonName = "Torrentio")
        val unknown = StreamSource(addonName = "Some Random Addon")
        assertEquals(true, StreamRanking.preferredIndex(torrentio, preferred) <
            StreamRanking.preferredIndex(unknown, preferred))
    }

    @Test
    fun `matching is case-insensitive against name or id`() {
        val preferred = listOf("torrentio")
        assertEquals(0, StreamRanking.preferredIndex(StreamSource(addonId = "TORRENTIO"), preferred))
    }

    // ---- rank ------------------------------------------------------------------

    @Test
    fun `reliable add-ons lead even when an unknown one has higher quality`() {
        val ranked = StreamRanking.rank(
            listOf(
                StreamSource(url = "http://a", quality = "4K", addonName = "Unknown"),
                StreamSource(url = "http://b", quality = "720p", addonName = "Torrentio"),
            ),
            preferredHosts = listOf("torrentio"),
        )
        assertEquals("http://b", ranked.first().url)
    }

    @Test
    fun `within one add-on higher quality then more seeders win`() {
        val ranked = StreamRanking.rank(
            listOf(
                StreamSource(url = "http://a", quality = "1080p", seeders = 5, addonName = "Torrentio"),
                StreamSource(url = "http://b", quality = "4K", seeders = 1, addonName = "Torrentio"),
                StreamSource(url = "http://c", quality = "1080p", seeders = 500, addonName = "Torrentio"),
            ),
            preferredHosts = listOf("torrentio"),
        )
        assertEquals(listOf("http://b", "http://c", "http://a"), ranked.map { it.url })
    }

    @Test
    fun `duplicate streams are collapsed`() {
        val ranked = StreamRanking.rank(
            listOf(
                StreamSource(infoHash = "ABC", quality = "1080p"),
                StreamSource(infoHash = "ABC", quality = "1080p", addonName = "dupe"),
            ),
        )
        assertEquals(1, ranked.size)
    }

    @Test
    fun `qualityRank orders recognised tiers above unknown`() {
        assertEquals(true, StreamRanking.qualityRank("4K") > StreamRanking.qualityRank("1080p"))
        assertEquals(true, StreamRanking.qualityRank("360p") > StreamRanking.qualityRank(null))
    }
}
