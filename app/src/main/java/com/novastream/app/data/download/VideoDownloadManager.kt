package com.novastream.app.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.remote.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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
)

/** Stable, collision-resistant download id from the item key + stream URL. */
fun videoDownloadId(itemKey: String, url: String): String =
    UUID.nameUUIDFromBytes("$itemKey|$url".toByteArray()).toString()

/**
 * Wraps Media3's [DownloadManager]: enqueue/pause/resume/remove go through the framework (which
 * handles progressive MP4, HLS — including alternate audio, byte-range and `#EXT-X-KEY` playlists —
 * and DASH reliably), while this class maps [Download]s to a reactive [StateFlow] for the UI and
 * derives download speed/ETA from successive progress callbacks.
 */
@UnstableApi
class VideoDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<VideoDownload>>(emptyList())
    val downloads: StateFlow<List<VideoDownload>> = _downloads.asStateFlow()

    private val items = LinkedHashMap<String, VideoDownload>()
    private val lock = Any()
    private val samples = HashMap<String, Pair<Long, Long>>() // id -> (bytes, timestampMs)

    private val listener = object : DownloadManager.Listener {
        override fun onInitialized(downloadManager: DownloadManager) = refresh()

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) = onChanged(download)

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            synchronized(lock) { items.remove(download.request.id) }
            publish()
        }
    }

    init {
        runCatching {
            DownloadManagerProvider.downloadManager(context).addListener(listener)
            refresh()
        }
    }

    // ---- Public API ----------------------------------------------------------

    fun isDownloaded(itemKey: String): Boolean =
        _downloads.value.any { it.itemKey == itemKey && it.status == VideoDownloadStatus.COMPLETED }

    fun enqueue(item: MediaItem, url: String, title: String, subtitle: String?, kind: VideoKind) {
        val id = videoDownloadId(item.key, url)
        // Ignore re-taps while a download is active/completed (Media3 rejects duplicate ids).
        if (_downloads.value.any { it.id == id && it.status != VideoDownloadStatus.FAILED }) return

        val meta = VideoDownloadMeta(
            itemJson = Http.gson.toJson(item),
            title = title,
            subtitle = subtitle,
            poster = item.poster,
            url = url,
        )
        val builder = DownloadRequest.Builder(id, Uri.parse(url))
            .setData(Http.gson.toJson(meta).toByteArray())
        // Nudge Media3 to the HLS downloader; progressive streams are inferred from the URI.
        if (kind == VideoKind.HLS) builder.setMimeType(MimeTypes.APPLICATION_M3U8)

        runCatching {
            DownloadService.sendAddDownload(
                context,
                NovaDownloadService::class.java,
                builder.build(),
                /* foreground = */ true,
            )
        }
    }

    fun pause(id: String) {
        runCatching { DownloadManagerProvider.downloadManager(context).setStopReason(id, STOP_REASON_PAUSED) }
    }

    fun resume(id: String) {
        runCatching { DownloadManagerProvider.downloadManager(context).setStopReason(id, Download.STOP_REASON_NONE) }
    }

    fun delete(id: String) {
        runCatching {
            DownloadService.sendRemoveDownload(context, NovaDownloadService::class.java, id, false)
        }
        synchronized(lock) { items.remove(id) }
        publish()
    }

    // ---- Internals -----------------------------------------------------------

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
        val id = download.request.id
        val now = System.currentTimeMillis()
        val bytes = download.bytesDownloaded
        val prev = samples[id]
        val speed = if (prev != null && now > prev.second && download.state == Download.STATE_DOWNLOADING) {
            ((bytes - prev.first) * 1000L / (now - prev.second)).coerceAtLeast(0L)
        } else 0L
        samples[id] = bytes to now
        val eta = if (speed > 0 && download.contentLength > bytes) {
            (download.contentLength - bytes) / speed
        } else 0L

        val ui = download.toUi(speed, eta)
        synchronized(lock) { items[id] = ui }
        publish()
    }

    private fun publish() {
        _downloads.value = synchronized(lock) { items.values.sortedByDescending { it.updatedAt } }
    }

    private fun Download.toUi(speed: Long, eta: Long): VideoDownload {
        val meta = request.data?.let {
            runCatching { Http.gson.fromJson(String(it), VideoDownloadMeta::class.java) }.getOrNull()
        }
        val itemKey = meta?.itemJson?.let {
            runCatching { Http.gson.fromJson(it, MediaItem::class.java)?.key }.getOrNull()
        } ?: request.id
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

    private fun failureText(reason: Int): String = when (reason) {
        Download.FAILURE_REASON_UNKNOWN -> "Download failed"
        else -> "Download failed ($reason)"
    }

    private companion object {
        /** Any non-zero stop reason pauses; Media3 reserves 0 for "not stopped". */
        const val STOP_REASON_PAUSED = 1
    }
}
