package com.moovio.app.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import com.moovio.app.data.model.StreamSource
import java.io.File

/**
 * In-app P2P streaming engine backed by jlibtorrent (via TorrentStream-Android).
 *
 * NSFW / Real 18+ Stremio add-ons almost always return BitTorrent streams (an info hash plus
 * trackers) instead of direct HTTP links. This wrapper turns such a stream into a local
 * `http://127.0.0.1:<port>/...` URL that ExoPlayer can play and seek through, downloading
 * only the pieces that are needed as playback progresses.
 */
object TorrentStreamer {

    private var engine: TorrentStream? = null
    private val main = Handler(Looper.getMainLooper())

    private class Pending(
        val onReady: (String) -> Unit,
        val onError: (String) -> Unit,
        val onProgress: (Int) -> Unit,
    )

    @Volatile private var pending: Pending? = null

    /** `SystemClock.elapsedRealtime()` when the current preparation started. */
    @Volatile private var startedAt = 0L

    /**
     * Cancels a preparation that never turns into a stream. A magnet with no reachable tracker
     * emits neither `onStreamReady` nor `onStreamError`, so without this the UI waits forever.
     */
    private val prepareTimeout = object : Runnable {
        override fun run() {
            val p = pending ?: return
            // Re-arm if the handler fired a touch early rather than cancelling a live stream.
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (!TorrentWatchdog.expired(startedAt, SystemClock.elapsedRealtime())) {
                main.postDelayed(this, (TorrentWatchdog.TIMEOUT_MS - elapsed).coerceAtLeast(1L))
                return
            }
            pending = null
            runCatching { engine?.stopStream() }
            main.post { p.onError(TorrentWatchdog.message()) }
        }
    }

    /** True once the native engine has been initialised successfully. */
    @Volatile var available: Boolean = false
        private set

    @Synchronized
    fun ensureInit(context: Context) {
        if (engine != null) { available = true; return }
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "torrents").apply { mkdirs() }
        val options = TorrentOptions.Builder()
            .saveLocation(dir)
            .removeFilesAfterStop(true)
            .maxConnections(200)
            .maxActiveDHT(200)
            .maxDownloadSpeed(0)
            .maxUploadSpeed(0)
            .build()
        engine = TorrentStream.init(options)
        available = true
    }

    /** Warm the engine up off the UI thread (called from Application.onCreate). */
    fun warmUp(context: Context) {
        Thread {
            runCatching { ensureInit(context.applicationContext) }
        }.apply { isDaemon = true }.start()
    }

    /**
     * Start streaming [source] (must carry an info hash). [onReady] is delivered on the main
     * thread with a playable local URL; [onError] likewise on failure.
     */
    fun stream(
        context: Context,
        source: StreamSource,
        onReady: (String) -> Unit,
        onError: (String) -> Unit,
        onProgress: (Int) -> Unit = {},
    ) {
        val magnet = source.magnetUri
        if (magnet == null) { onError("This stream has no torrent info."); return }
        try {
            ensureInit(context.applicationContext)
        } catch (t: Throwable) {
            available = false
            onError("Torrent engine unavailable: ${t.message ?: t.javaClass.simpleName}")
            return
        }
        val e = engine ?: run { onError("Torrent engine unavailable."); return }
        pending = Pending(onReady, onError, onProgress)
        startedAt = SystemClock.elapsedRealtime()
        main.removeCallbacks(prepareTimeout)
        main.postDelayed(prepareTimeout, TorrentWatchdog.TIMEOUT_MS)
        // The listener is a singleton, so drop any previous registration before re-adding it —
        // otherwise every playback leaks another copy and callbacks fire N times.
        runCatching { e.removeListener(listener) }
        e.addListener(listener)
        runCatching { e.startStream(magnet) }.onFailure {
            pending = null
            main.removeCallbacks(prepareTimeout)
            onError(it.message ?: "Couldn't start the torrent.")
        }
    }

    fun stop() {
        pending = null
        main.removeCallbacks(prepareTimeout)
        runCatching { engine?.stopStream() }
    }

    private fun postReady(url: String) {
        val p = pending ?: return
        pending = null
        main.removeCallbacks(prepareTimeout)
        main.post { p.onReady(url) }
    }

    private fun postError(msg: String) {
        val p = pending ?: return
        pending = null
        main.removeCallbacks(prepareTimeout)
        main.post { p.onError(msg) }
    }

    private val listener = object : TorrentListener {
        override fun onStreamPrepared(torrent: Torrent) {}
        override fun onStreamStarted(torrent: Torrent) {}

        override fun onStreamReady(torrent: Torrent) {
            val url = runCatching { engine?.currentTorrentUrl }.getOrNull()
            if (!url.isNullOrBlank()) postReady(url) else postError("Torrent stream URL unavailable.")
        }

        override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
            val p = pending ?: return
            main.post { p.onProgress(status.bufferProgress) }
        }

        override fun onStreamError(torrent: Torrent, e: Exception) {
            postError(e.message ?: "Torrent error.")
        }

        override fun onStreamStopped() {}
    }
}
