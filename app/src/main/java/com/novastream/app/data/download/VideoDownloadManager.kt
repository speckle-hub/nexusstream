package com.novastream.app.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.novastream.app.NovaApp
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.remote.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** How a stream is downloaded (Media3 infers the actual downloader from the URI/mime). */
enum class VideoKind { PROGRESSIVE, HLS }

enum class VideoDownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED }

/**
 * UI-facing snapshot of a Media3 download.
 *
 * The original [MediaItem] JSON and display fields are stored inside the Media3
 * [DownloadRequest]'s `data` payload (see [VideoDownloadMeta]), so the list can be rebuilt after a
 * restart without a separate index of our own — Media3 persists the downloads in its own database.
 */
data class VideoDownload(
    val id: String,
    val itemKey: String,
    val itemJson: String,
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val poster: String? = null,
    val status: VideoDownloadStatus = VideoDownloadStatus.QUEUED,
    val progress: Float = 0f,
    val bytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Extra fields Media3 does not model, serialized into `DownloadRequest.data`. */
data class VideoDownloadMeta(
    val itemJson: String,
    val title: String,
    val subtitle: String? = null,
    val poster: String? = null,
    val url: String,
    /** Stream HTTP headers (Referer / Cookie / UA) so downloads can reach header-gated hosts. */
    val headers: Map<String, String> = emptyMap(),
    /** Backup mirror URLs tried in order when this download fails (see [VideoDownloadManager]). */
    val alternates: List<String> = emptyList(),
    /** Mirror URLs already attempted for this item (including the primary), so failover never loops. */
    val attempted: List<String> = emptyList(),
)

/** Stable, collision-resistant download id from the item key + stream URL. */
fun videoDownloadId(itemKey: String, url: String): String =
    UUID.nameUUIDFromBytes("$itemKey|$url".toByteArray()).toString()

/** Completed vs in-progress vs orphaned bytes for the storage meter / cleanup UI. */
data class DownloadStorageStats(
    /** Bytes held by finished, indexed downloads. */
    val completedBytes: Long = 0L,
    /** Cache bytes not owned by a completed download (partial/failed chunks + playback cache). */
    val temporaryBytes: Long = 0L,
    /** Chunk files on disk that no cache index entry references (crashed downloads). */
    val orphanBytes: Long = 0L,
    /** Total bytes `SimpleCache` reports as occupied. */
    val cacheBytes: Long = 0L,
) {
    val reclaimableBytes: Long get() = temporaryBytes + orphanBytes
}

/**
 * Wraps Media3's [DownloadManager]: enqueue/pause/resume/remove go through the framework (which
 * handles progressive MP4, HLS — including alternate audio, byte-range and `#EXT-X-KEY` playlists —
 * and DASH reliably), while this class maps [Download]s to a reactive [StateFlow] for the UI and
 * derives download speed/ETA from successive progress callbacks.
 *
 * It also owns two things the raw framework does not: automatic **mirror failover** (a failed
 * download is retried against its alternate URLs) and **storage maintenance** (stats + orphan
 * cleanup).
 */
@UnstableApi
class VideoDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<VideoDownload>>(emptyList())
    val downloads: StateFlow<List<VideoDownload>> = _downloads.asStateFlow()

    private val items = LinkedHashMap<String, VideoDownload>()
    private val lock = Any()
    private val samples = HashMap<String, Pair<Long, Long>>() // id -> (bytes, timestampMs)

    /** Ensures only one progress ticker runs at a time (state changes and polling can race). */
    private val tickerRunning = AtomicBoolean(false)

    /** Completed downloads already copied to the user's export folder, so we never copy twice. */
    private val exported = java.util.Collections.synchronizedSet(HashSet<String>())

    private val listener = object : DownloadManager.Listener {
        override fun onInitialized(downloadManager: DownloadManager) = refresh()

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            onChanged(download)
            if (download.state == Download.STATE_FAILED) maybeFailover(download)
            if (download.state == Download.STATE_COMPLETED) maybeExport(download)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            synchronized(lock) {
                items.remove(download.request.id)
                samples.remove(download.request.id)
            }
            publish()
        }
    }

    init {
        runCatching {
            DownloadManagerProvider.downloadManager(context).addListener(listener)
            refresh()
        }
        // Pick up where a previously-interrupted download left off, before any new state change.
        ensureProgressTicker()
    }

    // ---- Public API ----------------------------------------------------------

    fun isDownloaded(itemKey: String): Boolean =
        _downloads.value.any { it.itemKey == itemKey && it.status == VideoDownloadStatus.COMPLETED }

    fun enqueue(
        item: MediaItem,
        url: String,
        title: String,
        subtitle: String?,
        kind: VideoKind,
        headers: Map<String, String> = emptyMap(),
        alternates: List<String> = emptyList(),
    ) {
        val id = videoDownloadId(item.key, url)
        // Ignore re-taps while a download is active/completed (Media3 rejects duplicate ids).
        if (_downloads.value.any { it.id == id && it.status != VideoDownloadStatus.FAILED }) return

        val meta = VideoDownloadMeta(
            itemJson = Http.gson.toJson(item),
            title = title,
            subtitle = subtitle,
            poster = item.poster,
            url = url,
            headers = headers,
            alternates = alternates,
        )
        sendAdd(buildRequest(id, url, kind, meta))
        // Start polling now so the row advances the moment the service begins downloading, rather
        // than waiting for the first listener callback (which may only arrive on state changes).
        ensureProgressTicker()
    }

    fun pause(id: String) {
        runCatching { DownloadManagerProvider.downloadManager(context).setStopReason(id, STOP_REASON_PAUSED) }
    }

    fun resume(id: String) {
        runCatching { DownloadManagerProvider.downloadManager(context).setStopReason(id, Download.STOP_REASON_NONE) }
        ensureProgressTicker()
    }

    fun delete(id: String) {
        runCatching {
            // removeCache = true: deleting a download must also release its cached media, otherwise
            // the bytes stay on disk with nothing in the Downloads tab pointing at them.
            DownloadService.sendRemoveDownload(context, NovaDownloadService::class.java, id, true)
        }
        synchronized(lock) {
            items.remove(id)
            samples.remove(id)
        }
        publish()
    }

    /**
     * Remove every download (and its cached media) belonging to [itemKey]. Used by the
     * "auto-delete watched downloads" feature once playback passes 90%.
     */
    fun deleteDownloadsForItem(itemKey: String) {
        scope.launch {
            val ids = runCatching {
                val out = ArrayList<String>()
                DownloadManagerProvider.downloadManager(context).downloadIndex.getDownloads().use { cursor ->
                    while (cursor.moveToNext()) {
                        val d = cursor.download
                        if (d.meta()?.let { itemKeyOf(it) } == itemKey) out += d.request.id
                    }
                }
                out
            }.getOrDefault(emptyList())
            ids.forEach { id ->
                runCatching {
                    DownloadService.sendRemoveDownload(context, NovaDownloadService::class.java, id, true)
                }
                synchronized(lock) { items.remove(id) }
            }
            if (ids.isNotEmpty()) publish()
        }
    }

    // ---- Storage maintenance -------------------------------------------------

    /** Completed vs temporary vs orphaned bytes for the Settings/Library storage meter. */
    suspend fun storageStats(): DownloadStorageStats = withContext(Dispatchers.IO) {
        val manager = runCatching { DownloadManagerProvider.downloadManager(context) }.getOrNull()
        val cache = runCatching { DownloadManagerProvider.cache(context) }.getOrNull()
        if (manager == null || cache == null) return@withContext DownloadStorageStats()

        var completed = 0L
        runCatching {
            manager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val d = cursor.download
                    if (d.state == Download.STATE_COMPLETED) completed += d.bytesDownloaded.coerceAtLeast(0)
                }
            }
        }
        val cacheBytes = runCatching { cache.getCacheSpace() }.getOrDefault(0L)
        val orphanBytes = runCatching { orphanFiles(cache).sumOf { it.length() } }.getOrDefault(0L)
        DownloadStorageStats(
            completedBytes = completed,
            // Anything in the cache that is not a finished download is partial/evictable.
            temporaryBytes = (cacheBytes - completed).coerceAtLeast(0L),
            orphanBytes = orphanBytes,
            cacheBytes = cacheBytes,
        )
    }

    /**
     * Delete temporary/incomplete and orphaned download data, keeping completed downloads.
     *
     *  - in-progress, paused, queued and failed downloads are removed so their cache spans are
     *    released by Media3;
     *  - chunk files left on disk that no index entry references (a process kill mid-download) are
     *    deleted directly.
     *
     * Returns the fresh stats after cleanup.
     */
    suspend fun clearTemporaryFiles(): DownloadStorageStats = withContext(Dispatchers.IO) {
        val manager = runCatching { DownloadManagerProvider.downloadManager(context) }.getOrNull()
        val cache = runCatching { DownloadManagerProvider.cache(context) }.getOrNull()
        if (manager == null || cache == null) return@withContext DownloadStorageStats()

        // 1. Drop non-completed downloads so their cached spans are released.
        val unfinished = runCatching {
            val ids = ArrayList<String>()
            manager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val d = cursor.download
                    if (d.state != Download.STATE_COMPLETED) ids += d.request.id
                }
            }
            ids
        }.getOrDefault(emptyList())
        unfinished.forEach { id -> runCatching { manager.removeDownload(id) } }

        // 2. Delete orphaned chunk files the index no longer points at.
        runCatching { orphanFiles(cache) }.getOrDefault(emptyList()).forEach { file ->
            runCatching { file.delete() }
        }

        // 3. Rebuild the in-memory list and report.
        refresh()
        storageStats()
    }

    /** Files in the cache dir that no `SimpleCache` span references. */
    private fun orphanFiles(cache: Cache) = DownloadCacheMaintenance.orphans(
        DownloadManagerProvider.cacheDir(context),
        DownloadCacheMaintenance.referencedNames(
            cache.keys.flatMap { key -> cache.getCachedSpans(key).map { it.file } },
        ),
    )

    // ---- Internals -----------------------------------------------------------

    private fun sendAdd(request: DownloadRequest) {
        runCatching {
            DownloadService.sendAddDownload(
                context,
                NovaDownloadService::class.java,
                request,
                /* foreground = */ true,
            )
        }
    }

    private fun buildRequest(id: String, url: String, kind: VideoKind, meta: VideoDownloadMeta): DownloadRequest {
        val builder = DownloadRequest.Builder(id, Uri.parse(url))
            .setData(Http.gson.toJson(meta).toByteArray())
        // Nudge Media3 to the HLS downloader; progressive streams are inferred from the URI.
        if (kind == VideoKind.HLS) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        return builder.build()
    }

    private fun refresh() {
        scope.launch {
            val list = runCatching {
                val cursor = DownloadManagerProvider.downloadManager(context).downloadIndex.getDownloads()
                cursor.use { c ->
                    val out = ArrayList<VideoDownload>()
                    while (c.moveToNext()) out.add(c.download.toUi(speed = 0L, eta = 0L))
                    out
                }
            }.getOrDefault(emptyList())
            synchronized(lock) {
                items.clear()
                list.forEach { items[it.id] = it }
            }
            publish()
        }
    }

    private fun onChanged(download: Download) {
        if (applyDownload(download)) publish()
        // Media3's listener reliably reports state transitions (queued -> downloading -> done), so
        // this is where the byte/percent ticker is (re)armed; the ticker keeps the UI live between
        // those transitions, since the listener does not emit continuous byte progress.
        if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
            ensureProgressTicker()
        }
        if (download.state == Download.STATE_FAILED) maybeFailover(download)
    }

    /**
     * Recompute one download's UI snapshot, deriving speed/ETA from the previous sample, and store
     * it. Returns true only when something the row actually renders changed, so callers can skip
     * no-op [StateFlow] emissions.
     */
    private fun applyDownload(download: Download): Boolean {
        val id = download.request.id
        val now = System.currentTimeMillis()
        val bytes = download.bytesDownloaded
        return synchronized(lock) {
            val prev = samples[id]
            val speed = if (prev != null && now > prev.second && download.state == Download.STATE_DOWNLOADING) {
                ((bytes - prev.first) * 1000L / (now - prev.second)).coerceAtLeast(0L)
            } else 0L
            samples[id] = bytes to now
            val eta = if (speed > 0 && download.contentLength > bytes) {
                (download.contentLength - bytes) / speed
            } else 0L

            val ui = download.toUi(speed, eta)
            val old = items[id]
            if (old != null && old.sameProgressAs(ui)) {
                false
            } else {
                items[id] = ui
                true
            }
        }
    }

    /** The fields a download row renders — used to avoid pointless emissions while polling. */
    private fun VideoDownload.sameProgressAs(other: VideoDownload): Boolean =
        status == other.status && progress == other.progress && bytes == other.bytes &&
            totalBytes == other.totalBytes && speedBytesPerSec == other.speedBytesPerSec &&
            etaSeconds == other.etaSeconds && error == other.error

    /**
     * Media3's [DownloadManager.Listener] does not guarantee continuous byte/percentage callbacks,
     * so a download row could sit at "0% · 0 B" until the download finished. While anything is
     * queued or downloading, this ticker polls the live [DownloadManager.getCurrentDownloads] every
     * [PROGRESS_POLL_MS] and republishes the snapshot, which is what makes the Library progress bar,
     * percentage and byte counter advance in real time. It stops once nothing is left to download.
     */
    private fun ensureProgressTicker() {
        if (!tickerRunning.compareAndSet(false, true)) return
        scope.launch {
            while (isActive) {
                val current = runCatching {
                    DownloadManagerProvider.downloadManager(context).currentDownloads
                }.getOrDefault(emptyList())
                var changed = false
                current.forEach { download -> if (applyDownload(download)) changed = true }
                if (changed) publish()
                val stillActive = current.any {
                    it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
                }
                if (!stillActive) break
                delay(PROGRESS_POLL_MS)
            }
            tickerRunning.set(false)
            // A download may have started while the ticker was winding down; re-arm if so.
            if (hasActiveDownloads()) ensureProgressTicker()
        }
    }

    private fun hasActiveDownloads(): Boolean = runCatching {
        DownloadManagerProvider.downloadManager(context).currentDownloads.any {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }
    }.getOrDefault(false)

    /**
     * A download failed: if it has an untried backup mirror, transparently replace it with the next
     * one instead of leaving a dead entry. The set of tried URLs travels in the request metadata, so
     * the fallback also survives a process restart.
     */
    /**
     * A download finished: copy it into the user's chosen export folder (Settings → Download
     * folder), when one is configured. Skipped silently otherwise; the download remains playable
     * offline from the app cache either way.
     */
    private fun maybeExport(download: Download) {
        val id = download.request.id
        if (exported.contains(id)) return
        val app = context.applicationContext as? NovaApp ?: return
        val meta = decodeDownloadMeta(download.request) ?: return
        scope.launch {
            val configured = runCatching { app.container.settings.storageUri.first() }
                .getOrNull().orEmpty().isNotBlank()
            if (!configured) return@launch
            if (!exported.add(id)) return@launch
            val ok = DownloadExporter.export(context, app.container.settings, id, exportFileName(meta))
            // Failed export (e.g. a transient provider error) is retried on the next completion event.
            if (!ok) exported.remove(id)
        }
    }

    /**
     * A download failed: if it has an untried backup mirror, transparently replace it with the next
     * one instead of leaving a dead entry. The set of tried URLs travels in the request metadata, so
     * the fallback also survives a process restart.
     */
    private fun maybeFailover(download: Download) {
        val meta = decodeDownloadMeta(download.request) ?: return
        if (meta.alternates.isEmpty()) return
        val itemKey = itemKeyOf(meta) ?: return
        val attempted = meta.attempted.toSet() + meta.url
        val candidates = (listOf(meta.url) + meta.alternates).distinct()
        val next = candidates.firstOrNull { it !in attempted } ?: return
        val failedId = download.request.id
        val nextId = videoDownloadId(itemKey, next)
        val nextMeta = meta.copy(url = next, attempted = attempted.toList())
        val kind = if (next.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE

        scope.launch {
            runCatching {
                // removeCache = true so a dead mirror's partial chunks don't linger on disk.
                DownloadService.sendRemoveDownload(context, NovaDownloadService::class.java, failedId, true)
            }
            synchronized(lock) { items.remove(failedId) }
            publish()
            sendAdd(buildRequest(nextId, next, kind, nextMeta))
        }
    }

    private fun publish() {
        _downloads.value = synchronized(lock) { items.values.sortedByDescending { it.updatedAt } }
    }

    private fun Download.toUi(speed: Long, eta: Long): VideoDownload {
        val meta = meta()
        val itemKey = meta?.let { itemKeyOf(it) } ?: request.id
        return VideoDownload(
            id = request.id,
            itemKey = itemKey,
            itemJson = meta?.itemJson ?: "",
            url = request.uri.toString(),
            title = meta?.title ?: (request.uri.lastPathSegment ?: "Download"),
            subtitle = meta?.subtitle,
            poster = meta?.poster,
            status = statusOf(state),
            progress = (percentDownloaded / 100f).coerceIn(0f, 1f),
            bytes = bytesDownloaded,
            totalBytes = contentLength,
            speedBytesPerSec = speed,
            etaSeconds = eta,
            error = if (state == Download.STATE_FAILED) failureText(failureReason) else null,
            updatedAt = updateTimeMs,
        )
    }

    private fun statusOf(state: Int): VideoDownloadStatus = when (state) {
        Download.STATE_DOWNLOADING -> VideoDownloadStatus.DOWNLOADING
        Download.STATE_COMPLETED -> VideoDownloadStatus.COMPLETED
        Download.STATE_FAILED -> VideoDownloadStatus.FAILED
        Download.STATE_STOPPED -> VideoDownloadStatus.PAUSED
        else -> VideoDownloadStatus.QUEUED
    }

    private fun failureText(reason: Int): String =
        if (reason == Download.FAILURE_REASON_UNKNOWN || reason == Download.FAILURE_REASON_NONE) {
            "Download failed"
        } else {
            "Download failed ($reason)"
        }

    private fun itemKeyOf(meta: VideoDownloadMeta): String? =
        runCatching { Http.gson.fromJson(meta.itemJson, MediaItem::class.java)?.key }.getOrNull()

    /** Human-friendly export filename: sanitized title plus the stream's extension. */
    private fun exportFileName(meta: VideoDownloadMeta): String {
        val ext = meta.url.substringBefore('?').substringAfterLast('.', "").lowercase()
            .takeIf { it in EXPORT_EXTENSIONS } ?: "mp4"
        val base = meta.title.replace(Regex("[^A-Za-z0-9 ._-]"), "_").trim().ifBlank { "NovaStream download" }
        return "$base.$ext"
    }

    private companion object {
        /** Any non-zero stop reason pauses; Media3 reserves 0 for "not stopped". */
        const val STOP_REASON_PAUSED = 1

        /** How often the progress ticker samples the DownloadManager while a download is active. */
        const val PROGRESS_POLL_MS = 500L

        /** Extensions we recognise when naming an exported download. */
        val EXPORT_EXTENSIONS = setOf("mp4", "m4v", "mov", "mkv", "webm", "ts", "avi")
    }
}
