package com.novastream.app

import com.novastream.app.data.repo.MirrorHealth
import com.novastream.app.data.repo.PageCache
import com.novastream.app.data.repo.ScraperRace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScraperRaceTest {

    // ---- isPlayableCandidate -------------------------------------------------

    @Test
    fun `accepts http media urls`() {
        assertTrue(ScraperRace.isPlayableCandidate("https://cdn.example/v/1.mp4"))
        assertTrue(ScraperRace.isPlayableCandidate("http://cdn.example/list.m3u8"))
    }

    @Test
    fun `rejects blank non-http and non-media urls`() {
        assertFalse(ScraperRace.isPlayableCandidate(null))
        assertFalse(ScraperRace.isPlayableCandidate(""))
        assertFalse(ScraperRace.isPlayableCandidate("   "))
        assertFalse(ScraperRace.isPlayableCandidate("magnet:?xt=urn:btih:abc"))
        assertFalse(ScraperRace.isPlayableCandidate("/watch/123"))
    }

    @Test
    fun `rejects thumbnails and subtitle tracks`() {
        assertFalse(ScraperRace.isPlayableCandidate("https://cdn.example/a.jpg"))
        assertFalse(ScraperRace.isPlayableCandidate("https://cdn.example/a.png"))
        assertFalse(ScraperRace.isPlayableCandidate("https://cdn.example/a.webp"))
        assertFalse(ScraperRace.isPlayableCandidate("https://cdn.example/subs.vtt"))
        assertFalse(ScraperRace.isPlayableCandidate("https://cdn.example/subs.srt"))
    }

    // ---- timeout policy ------------------------------------------------------

    @Test
    fun `provider ceiling meets the 3 to 5 second fail-fast budget`() {
        assertTrue(ScraperRace.PROVIDER_TIMEOUT_MS in 3_000L..5_000L)
        assertTrue(ScraperRace.SUBTITLE_TIMEOUT_MS in 3_000L..5_000L)
        assertTrue(ScraperRace.DETAIL_TIMEOUT_MS in 3_000L..5_000L)
    }

    // ---- MirrorHealth --------------------------------------------------------

    @Test
    fun `quarantines after consecutive failures and recovers after ttl`() {
        var now = 0L
        val health = MirrorHealth(quarantineMs = 1_000L, maxFailures = 2, clockMs = { now })
        assertFalse(health.isQuarantined("xh"))
        health.recordFailure("xh")
        assertFalse(health.isQuarantined("xh"))
        health.recordFailure("xh")
        assertTrue(health.isQuarantined("xh"))
        now += 999
        assertTrue(health.isQuarantined("xh"))
        now += 1
        assertFalse(health.isQuarantined("xh"))
    }

    @Test
    fun `success resets the failure count`() {
        var now = 0L
        val health = MirrorHealth(quarantineMs = 1_000L, maxFailures = 2, clockMs = { now })
        health.recordFailure("rd")
        health.recordSuccess("rd")
        health.recordFailure("rd")
        assertFalse(health.isQuarantined("rd"))
    }

    @Test
    fun `tracks ids independently`() {
        val health = MirrorHealth(maxFailures = 1)
        health.recordFailure("a")
        assertTrue(health.isQuarantined("a"))
        assertFalse(health.isQuarantined("b"))
    }

    // ---- PageCache -----------------------------------------------------------

    @Test
    fun `serves fresh entries and expires stale ones`() {
        var now = 0L
        val cache = PageCache(maxSize = 4, ttlMs = 100L, clockMs = { now })
        cache.put("u1", "<html>one</html>")
        assertEquals("<html>one</html>", cache.get("u1"))
        now += 99
        assertEquals("<html>one</html>", cache.get("u1"))
        now += 1
        assertNull(cache.get("u1"))
    }

    @Test
    fun `evicts eldest beyond max size`() {
        val cache = PageCache(maxSize = 2, ttlMs = 60_000L, clockMs = { 0L })
        cache.put("a", "1")
        cache.put("b", "2")
        cache.put("c", "3")
        assertEquals(2, cache.size())
        assertNull(cache.get("a"))
        assertEquals("3", cache.get("c"))
    }

    // ---- raceResult ----------------------------------------------------------

    @Test
    fun `raceResult returns value inside the timeout`() = runBlocking {
        val out = ScraperRace.raceResult(5_000L) { "ok" }
        assertEquals("ok", out.getOrNull())
    }

    @Test
    fun `raceResult degrades a timeout to failure`() = runBlocking {
        val out = ScraperRace.raceResult(20L) { delay(5_000L); "late" }
        assertTrue(out.isFailure)
        assertTrue(out.exceptionOrNull() is TimeoutCancellationException)
    }

    @Test
    fun `raceResult rethrows genuine caller cancellation`() = runBlocking {
        val job = launch {
            try {
                ScraperRace.raceResult(5_000L) { delay(5_000L); "late" }
                fail("expected CancellationException")
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) fail("must not be a timeout")
                throw e
            }
        }
        delay(50L)
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `raceResult surfaces provider errors as failure`() = runBlocking {
        val out = ScraperRace.raceResult(5_000L) { throw IllegalStateException("boom") }
        assertTrue(out.isFailure)
        assertEquals("boom", out.exceptionOrNull()?.message)
    }
}
