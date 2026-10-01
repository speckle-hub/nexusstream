package com.novastream.app.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import com.novastream.app.data.model.StreamSource
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
        // The listener is a singleton, so drop any previous registration before re-adding it —
        // otherwise every playback leaks another copy and callbacks fire N times.
        runCatching { e.removeListener(listener) }
        e.addListener(listener)
        runCatching { e.startStream(magnet) }.onFailure {
            pending = null
            onError(it.message ?: "Couldn't start the torrent.")
        }
    }

    fun stop() {
        pending = null
        runCatching { engine?.stopStream() }
    }

    private fun postReady(url: String) {
        val p = pending ?: return
        pending = null
        main.post { p.onReady(url) }
    }

    private fun postError(msg: String) {
        val p = pending ?: return
        pending = null
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
