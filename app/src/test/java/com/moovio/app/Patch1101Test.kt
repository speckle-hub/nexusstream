package com.moovio.app

import com.moovio.app.data.remote.MangaDexQuery
import com.moovio.app.data.remote.TorrentWatchdog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.10.1 patch release: the two pure policies behind the ADB test-report fixes.
 *
 * NSFW Manga chapters were permanently empty because the feed asked for `contentRating[]=safe`
 * (or a rating set the series didn't use), which MangaDex answers with HTTP 200 and no data;
 * torrent preparation hung forever when no peer answered. Both rules live in plain objects so
 * they are testable without a device or network.
 */
class Patch1101Test {

    // ---- MangaDex rating rules --------------------------------------------------

    @Test
    fun `nsfw feed never asks for safe content`() {
        val ratings = MangaDexQuery.feedRatings(nsfw = true)
        assertFalse("safe must never be part of an NSFW query", "safe" in ratings)
        assertEquals(listOf("suggestive", "erotica", "pornographic"), ratings)
    }

    @Test
    fun `nsfw catalogue listings stay strictly erotica and pornographic`() {
        assertEquals(listOf("erotica", "pornographic"), MangaDexQuery.ratings(nsfw = true))
        assertEquals(listOf("erotica", "pornographic"), MangaDexQuery.NSFW)
    }

    @Test
    fun `sfw queries keep the safe and suggestive filter`() {
        assertEquals(listOf("safe", "suggestive"), MangaDexQuery.ratings(nsfw = false))
        assertEquals(listOf("safe", "suggestive"), MangaDexQuery.feedRatings(nsfw = false))
    }

    @Test
    fun `rating params keep one entry per rating`() {
        assertEquals(
            listOf(
                "contentRating[]" to "suggestive",
                "contentRating[]" to "erotica",
                "contentRating[]" to "pornographic",
            ),
            MangaDexQuery.ratingParams(MangaDexQuery.NSFW_FEED),
        )
    }

    @Test
    fun `nsfw feeds are attempted once and never fall back to a safe query`() {
        val attempts = MangaDexQuery.feedAttempts(nsfw = true)
        assertEquals(listOf(MangaDexQuery.NSFW_FEED), attempts)
        assertTrue(attempts.flatten().none { it == "safe" })
    }

    @Test
    fun `sfw feeds widen to the nsfw set exactly once when the series has no sfw chapters`() {
        // MangaDex treats an unfiltered feed like the safe ratings, so widening is the only
        // fallback that recovers an adult title whose MediaItem lost its NSFW type.
        assertEquals(listOf(MangaDexQuery.SAFE, MangaDexQuery.NSFW_FEED), MangaDexQuery.feedAttempts(nsfw = false))
    }

    // ---- Torrent watchdog -------------------------------------------------------

    @Test
    fun `preparation is not expired one tick before the deadline`() {
        val started = 1_000L
        assertFalse(TorrentWatchdog.expired(started, started))
        assertFalse(TorrentWatchdog.expired(started, started + TorrentWatchdog.TIMEOUT_MS - 1))
    }

    @Test
    fun `preparation expires exactly at and after the deadline`() {
        val started = 5_000L
        assertEquals(20_000L, TorrentWatchdog.TIMEOUT_MS)
        assertTrue(TorrentWatchdog.expired(started, started + TorrentWatchdog.TIMEOUT_MS))
        assertTrue(TorrentWatchdog.expired(started, started + 60_000L))
    }

    @Test
    fun `timeout message states the threshold and offers a retry`() {
        val message = TorrentWatchdog.message()
        assertTrue(message.contains("20 seconds"))
        assertTrue(message.contains("retry"))
    }
}
