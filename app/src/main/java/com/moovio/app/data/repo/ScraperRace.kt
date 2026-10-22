package com.moovio.app.data.repo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Phase 32 — stream-scraper racing helpers (pure JVM, unit-tested).
 *
 * The network fan-outs ([StreamRepository], `AdultRepository`, `CatalogRepository`) used to
 * await providers in registry order with generous (or no) timeouts, so one hung hoster gated
 * the whole stream list / tab. The helpers here are the shared policy behind the fix:
 *
 * - [PROVIDER_TIMEOUT_MS]: hard per-hoster ceiling (fail-fast, never stall the UI).
 * - [isPlayableCandidate]: cheap pre-filter so obviously-dead URLs never win a race.
 * - [MirrorHealth]: quarantines a host/source id after consecutive failures so the next
 *   load skips it instead of paying another timeout (slow-mirror isolation).
 * - [PageCache]: tiny TTL LRU for raw list HTML / catalog JSON so back-navigation is instant.
 */
object ScraperRace {
    /** Hard ceiling per provider/hoster — a hung server degrades to "no result", never a stall. */
    const val PROVIDER_TIMEOUT_MS = 5_000L

    /** Subtitle fan-out gets the same ceiling as streams. */
    const val SUBTITLE_TIMEOUT_MS = 5_000L

    /** Detail/category fetches for built-in scrapers. */
    const val DETAIL_TIMEOUT_MS = 5_000L

    /** Raw list/catalog HTML stays good this long for back-navigation. */
    const val PAGE_CACHE_TTL_MS = 5 * 60_000L
    const val PAGE_CACHE_MAX = 64

    /** A mirror is skipped for this long after it trips the failure threshold. */
    const val MIRROR_QUARANTINE_MS = 5 * 60_000L

    /** Consecutive failures before a mirror is quarantined. */
    const val MIRROR_MAX_FAILURES = 2

    /**
     * Cheap pre-filter for a candidate stream URL: non-blank http(s) and not an obvious
     * non-media page (playlist pages, thumbnails, subtitle tracks).
     */
    fun isPlayableCandidate(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) return false
        val lower = u.lowercase()
        if (lower.endsWith(".vtt") || lower.endsWith(".srt")) return false
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")
        ) return false
        return true
    }

    /**
     * Runs [block] under [timeoutMs], degrading a timeout to an ordinary failure while
     * rethrowing genuine caller cancellation (navigating away, a superseding search).
     * Plain `runCatching { withTimeout { } }` swallows that cancellation, letting a dead
     * load masquerade as a mirror failure (and trip [MirrorHealth]) — the ISSUE-10 lesson.
     */
    suspend fun <T> raceResult(timeoutMs: Long, block: suspend () -> T): Result<T> = try {
        Result.success(withTimeout(timeoutMs.coerceAtLeast(1)) { block() })
    } catch (e: TimeoutCancellationException) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Consecutive-failure tracker that quarantines a flaky host/source id for [quarantineMs].
 * A success resets the count; a quarantined id is skipped until its quarantine expires.
 * [clockMs] is injectable so unit tests can advance time deterministically.
 */
class MirrorHealth(
    private val quarantineMs: Long = ScraperRace.MIRROR_QUARANTINE_MS,
    private val maxFailures: Int = ScraperRace.MIRROR_MAX_FAILURES,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val failures = HashMap<String, Int>()
    private val quarantinedUntil = HashMap<String, Long>()

    fun recordSuccess(id: String) = synchronized(lock) {
        failures.remove(id)
        quarantinedUntil.remove(id)
    }

    fun recordFailure(id: String) = synchronized(lock) {
        val count = (failures[id] ?: 0) + 1
        failures[id] = count
        if (count >= maxFailures) {
            quarantinedUntil[id] = clockMs() + quarantineMs
        }
    }

    fun isQuarantined(id: String): Boolean = synchronized(lock) {
        val until = quarantinedUntil[id] ?: return false
        if (clockMs() >= until) {
            quarantinedUntil.remove(id)
            failures.remove(id)
            return false
        }
        true
    }
}

/**
 * Tiny TTL LRU for raw list HTML / catalog JSON bodies. Pure JVM so the eviction/TTL policy
 * is unit-testable. Internally synchronized, so concurrent coroutines may share one safely.
 */
class PageCache(
    private val maxSize: Int = ScraperRace.PAGE_CACHE_MAX,
    private val ttlMs: Long = ScraperRace.PAGE_CACHE_TTL_MS,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val map = object : LinkedHashMap<String, Pair<Long, String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, String>>) =
            size > maxSize
    }

    fun get(key: String): String? = synchronized(lock) {
        val entry = map[key] ?: return null
        if (clockMs() - entry.first >= ttlMs) {
            map.remove(key)
            return null
        }
        entry.second
    }

    fun put(key: String, body: String) = synchronized(lock) {
        map[key] = clockMs() to body
    }

    fun clear() = synchronized(lock) { map.clear() }

    fun size(): Int = synchronized(lock) { map.size }
}
