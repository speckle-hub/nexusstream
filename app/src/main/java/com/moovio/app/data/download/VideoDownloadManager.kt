package com.moovio.app.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.moovio.app.MoovioApp
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.remote.Http
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
import kotlin.text.Charsets
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
    /** True when the owning title is adult/NSFW — kept isolated from SFW downloads in the Library. */
    val nsfw: Boolean = false,
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
    /** True when the owning title is adult/NSFW — isolated from SFW downloads in the Library. */
    val nsfw: Boolean = false,
)

/** Stable, collision-resistant download id from the item key + stream URL. */
fun videoDownloadId(itemKey: String, url: String): String =
    UUID.nameUUIDFromBytes("$itemKey|$url".toByteArray()).toString()

/** Parses a download URL, returning null when it has no scheme (and is therefore not downloadable). */
internal fun parseDownloadUrl(url: String): Uri? =
    Uri.parse(url).takeIf { it.scheme != null }

/** File extensions Media3 classifies as adaptive (HLS / DASH / SmoothStreaming) from the URI alone. */
private val ADAPTIVE_EXTENSIONS = listOf(".m3u8", ".mpd", ".ism", ".isml")

/**
 * True when Media3 will treat this request as a **progressive** download — the only content type
 * that accepts a custom cache key.
 *
 * `DownloadRequest` asserts `customCacheKey must be null for type: <contentType>` for HLS, DASH and
 * SmoothStreaming, and it infers the type from the mime type when one is given, otherwise from the
 * URI's last path segment (mirroring `Util.inferContentTypeForUriAndMimeType`). This helper mirrors
 * exactly that inference so the key is attached only where Media3 accepts it.
 *
 * Pure and Android-free so the rule is unit-tested (`VideoDownloadManagerTest`).
 */
internal fun supportsCustomCacheKey(url: String, mimeType: String? = null): Boolean {
    if (mimeType != null) {
        return mimeType != MimeTypes.APPLICATION_M3U8 &&
            mimeType != MimeTypes.APPLICATION_MPD &&
            mimeType != MimeTypes.APPLICATION_SS
    }
    val lastSegment = url.substringBefore('?').substringBefore('#')
        .trimEnd('/').substringAfterLast('/').lowercase()
    return ADAPTIVE_EXTENSIONS.none { lastSegment.endsWith(it) }
}

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

    /**
     * Last transport error per download id, straight from Media3's `finalException`. The persisted
     * [Download] only carries a numeric reason, which is useless in a toast ("Download failed (4)");
     * this keeps the readable cause ("Response code: 403") for the row and the notification.
     */
    private val lastErrors = HashMap<String, String>()

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
            val reason = finalException?.let { describeError(it) }
            onChanged(download, reason)
            if (download.state == Download.STATE_FAILED) {
                // A backup mirror takes over silently: only a download with no untried alternate is
                // reported to the user. Previously every failed attempt raised a toast, even when a
                // mirror replaced it a moment later and the download then completed.
                val retrying = maybeFailover(download)
                if (!retrying) {
                    val title = items[download.request.id]?.title ?: "Download"
                    val error = reason ?: items[download.request.id]?.error
                    val msg = if (!error.isNullOrBlank()) "Download failed for $title: $error" else "Download failed for $title"
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            if (download.state == Download.STATE_COMPLETED) maybeExport(download)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            synchronized(lock) {
                items.remove(download.request.id)
                samples.remove(download.request.id)
                lastErrors.remove(download.request.id)
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
        nsfw: Boolean = false,
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
            nsfw = nsfw,
        )
        runCatching {
            sendAdd(buildRequest(id, url, kind, meta))
        }.onFailure { exception ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Download failed for $title: ${exception.message ?: exception.javaClass.simpleName}",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            }
        }
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
            DownloadService.sendRemoveDownload(context, MoovioDownloadService::class.java, id, true)
        }
        synchronized(lock) {
            items.remove(id)
            samples.remove(id)
            lastErrors.remove(id)
        }
        publish()
    }

    /**
     * Remove every download (and its cached media) belonging to [itemKey]. Used by the
     * "auto-delete watched downloads" feature once playback passes 90%.
     *
     * The removal goes through [DownloadManager.removeDownload] instead of
     * `DownloadService.sendRemoveDownload(..., foreground = true)`: the service variant has to
     * *start* the download service, which Android 12+ (API 31) refuses when the app is in the
     * background — and this call happens exactly then (from `onPause`/`onStop` of the player). The
     * exception was swallowed, so watched downloads were never reclaimed.
     */
    fun deleteDownloadsForItem(itemKey: String) {
        scope.launch {
            val manager = runCatching { DownloadManagerProvider.downloadManager(context) }.getOrNull()
            val ids = runCatching {
                val out = ArrayList<String>()
                manager?.downloadIndex?.getDownloads()?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val d = cursor.download
                        if (d.meta()?.let { itemKeyOf(it) } == itemKey) out += d.request.id
                    }
                }
                out
            }.getOrDefault(emptyList())
            ids.forEach { id ->
                runCatching { manager?.removeDownload(id) }
                // Progressive downloads are cached under the download id as the cache key, so the
                // bytes can be released right away. (HLS stores one key per segment; those files
                // are reclaimed by the orphan sweep in `clearTemporaryFiles`.)
                runCatching { DownloadManagerProvider.cache(context).removeResource(id) }
                synchronized(lock) {
                    items.remove(id)
                    samples.remove(id)
                    lastErrors.remove(id)
                }
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

    /**
     * Reclaims orphaned cache chunks when free space is under the user's floor.
     *
     * Called at app start (see `MoovioApp`). Deliberately narrow: only chunks no `SimpleCache` index
     * entry references are deleted, so a completed or in-progress download is never touched — the
     * bytes removed are ones the user already cannot play. Returns the bytes reclaimed.
     */
    suspend fun sweepOrphansIfLowOnSpace(thresholdMb: Int): Long = withContext(Dispatchers.IO) {
        if (thresholdMb <= 0) return@withContext 0L
        val cache = runCatching { DownloadManagerProvider.cache(context) }.getOrNull()
            ?: return@withContext 0L
        val orphans = runCatching { orphanFiles(cache) }.getOrDefault(emptyList())
        val free = runCatching { DownloadManagerProvider.cacheDir(context).usableSpace }.getOrDefault(-1L)
        val limitBytes = thresholdMb.toLong() * 1024L * 1024L
        if (!DownloadCacheMaintenance.shouldSweep(free, limitBytes, orphans.size)) return@withContext 0L
        var freed = 0L
        orphans.forEach { file ->
            val size = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) freed += size
        }
        freed
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
                MoovioDownloadService::class.java,
                request,
                /* foreground = */ true,
            )
        }.recover { exception ->
            if (exception::class.simpleName == "ForegroundServiceStartNotAllowedException") {
                DownloadService.sendAddDownload(
                    context,
                    MoovioDownloadService::class.java,
                    request,
                    /* foreground = */ false,
                )
            } else {
                throw exception
            }
        }
    }

    private fun buildRequest(id: String, url: String, kind: VideoKind, meta: VideoDownloadMeta): DownloadRequest {
        val uri = parseDownloadUrl(url)
            ?: throw IllegalArgumentException("Invalid download URL: $url")
        val mimeType = if (kind == VideoKind.HLS) MimeTypes.APPLICATION_M3U8 else null
        val builder = DownloadRequest.Builder(id, uri)
        // A custom cache key is only legal on *progressive* downloads. Media3's DownloadRequest
        // asserts `customCacheKey must be null for type: <contentType>` for HLS/DASH/SmoothStreaming,
        // and setting it unconditionally here made every adaptive download throw while the request
        // was still being built — it never reached the queue, so the only symptom was a failure
        // toast a moment after "Download started".
        if (supportsCustomCacheKey(url, mimeType)) builder.setCustomCacheKey(id)
        builder.setData(
            runCatching { Http.gson.toJson(meta).toByteArray(Charsets.UTF_8) }
                .getOrDefault(byteArrayOf())
        )
        if (mimeType != null) builder.setMimeType(mimeType)
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

    private fun onChanged(download: Download, error: String? = null) {
        if (applyDownload(download, error)) publish()
        // Media3's listener reliably reports state transitions (queued -> downloading -> done), so
        // this is where the byte/percent ticker is (re)armed; the ticker keeps the UI live between
        // those transitions, since the listener does not emit continuous byte progress.
        if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
            ensureProgressTicker()
        }
        // State failures are handled by the listener's STATE_FAILED branch, which also has the
        // `finalException`; failover must run exactly once per failure, not once per callback path.
    }

    /**
     * Recompute one download's UI snapshot, deriving speed/ETA from the previous sample, and store
     * it. Returns true only when something the row actually renders changed, so callers can skip
     * no-op [StateFlow] emissions.
     */
    private fun applyDownload(download: Download, error: String? = null): Boolean {
        val id = download.request.id
        val now = System.currentTimeMillis()
        val bytes = download.bytesDownloaded
        return synchronized(lock) {
            if (download.state == Download.STATE_FAILED) {
                error?.let { lastErrors[id] = it }
            } else {
                lastErrors.remove(id)
            }
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
     * A download finished: copy it into the user's chosen export folder (Settings → Download
     * folder), when one is configured. Skipped silently otherwise; the download remains playable
     * offline from the app cache either way.
     */
    private fun maybeExport(download: Download) {
        val id = download.request.id
        if (exported.contains(id)) return
        val app = context.applicationContext as? MoovioApp ?: return
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
    private fun maybeFailover(download: Download): Boolean {
        val meta = decodeDownloadMeta(download.request) ?: return false
        if (meta.alternates.isEmpty()) return false
        val itemKey = itemKeyOf(meta) ?: return false
        val attempted = meta.attempted.toSet() + meta.url
        val candidates = (listOf(meta.url) + meta.alternates).distinct()
        val next = candidates.firstOrNull { it !in attempted } ?: return false
        val failedId = download.request.id
        val nextId = videoDownloadId(itemKey, next)
        val nextMeta = meta.copy(url = next, attempted = attempted.toList())
        val kind = if (next.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE

        scope.launch {
            runCatching {
                // removeCache = true so a dead mirror's partial chunks don't linger on disk.
                DownloadService.sendRemoveDownload(context, MoovioDownloadService::class.java, failedId, true)
            }
            synchronized(lock) { items.remove(failedId) }
            publish()
            // A mirror that cannot even be turned into a request must not crash the IO scope; the
            // dead entry is already gone from the list, so the failure is simply dropped.
            runCatching { sendAdd(buildRequest(nextId, next, kind, nextMeta)) }
        }
        // A replacement is on its way, so the failure is not reported to the user.
        return true
    }

    /** Readable one-liner for a Media3/HTTP exception (`Response code: 403`, `Connection reset`…). */
    private fun describeError(error: Throwable): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        return message ?: error.javaClass.simpleName
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
            // The listener's `finalException` message when we have one this session, otherwise the
            // numeric Media3 reason — a bare "(4)" tells the user nothing.
            error = if (state == Download.STATE_FAILED) {
                lastErrors[request.id] ?: failureText(failureReason)
            } else null,
            updatedAt = updateTimeMs,
            nsfw = meta?.nsfw ?: false,
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
        val base = meta.title.replace(Regex("[^A-Za-z0-9 ._-]"), "_").trim().ifBlank { "Moovio download" }
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
