package com.moovio.app.data.remote

/**
 * Watchdog policy for torrent stream preparation.
 *
 * A magnet whose trackers are unreachable never emits `onStreamReady` and frequently emits no
 * error either, so the player sat on "Preparing stream…" forever (v1.10.0 test report, P1).
 * [TorrentStreamer] arms this watchdog when preparation starts, cancels the torrent when it
 * expires and reports [message] to the UI, which offers a retry.
 */
object TorrentWatchdog {

    /** How long "Preparing stream…" may sit before the torrent is cancelled. */
    const val TIMEOUT_MS = 20_000L

    /** True once [startedAtMs] is at least [TIMEOUT_MS] in the past. */
    fun expired(startedAtMs: Long, nowMs: Long): Boolean = nowMs - startedAtMs >= TIMEOUT_MS

    /** User-facing failure text; always shown next to a Retry action. */
    fun message(): String =
        "The torrent didn't start within ${TIMEOUT_MS / 1000} seconds — no peers or metadata " +
            "were found. Check your connection (trackers may be blocked on this network) and " +
            "retry, or pick a different stream."
}
