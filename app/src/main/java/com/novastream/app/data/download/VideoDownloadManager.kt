package com.novastream.app.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.remote.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** How a remote stream is turned into an offline file. */
enum class VideoKind { PROGRESSIVE, HLS }

enum class VideoDownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED }

/**
 * A single video download (movie, episode, anime stream).
 *
 * [itemJson] keeps the original [MediaItem] so the offline player can be launched straight from
 * the downloads list with the same identity — which is what preserves watch history and progress.
 */
data class VideoDownload(
    val id: String,
    val itemKey: String,
    val itemJson: String,
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val poster: String? = null,
    val kind: VideoKind = VideoKind.PROGRESSIVE,
    val status: VideoDownloadStatus = VideoDownloadStatus.QUEUED,
    val progress: Float = 0f,
    val bytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val error: String? = null,
    val filePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Shared keys between [VideoDownloadManager] and [VideoDownloadWorker]. */
object VideoDownloadContract {
    const val KEY_ID = "dl_id"
    const val KEY_URL = "dl_url"
    const val KEY_KIND = "dl_kind"
    const val KEY_TITLE = "dl_title"
    const val KEY_PROGRESS = "dl_progress" // 0..100
    const val KEY_BYTES = "dl_bytes"
    const val KEY_TOTAL = "dl_total"
    const val KEY_SPEED = "dl_speed"
    const val KEY_ETA = "dl_eta"
    const val KEY_ERROR = "dl_error"

    const val TAG = "video-download"
    const val DL_TAG_PREFIX = "dl:"
    const val NOTIFICATION_ID = 4801
    const val CHANNEL_ID = "video_downloads"

    fun dlTag(id: String) = "$DL_TAG_PREFIX$id"
    fun workName(id: String) = "video-dl-$id"
    fun idFromTags(tags: Set<String>): String? =
        tags.firstOrNull { it.startsWith(DL_TAG_PREFIX) }?.removePrefix(DL_TAG_PREFIX)
}

/**
 * Owns the list of video downloads, their persisted index, and the WorkManager jobs that actually
 * move bytes. Live progress is read back from each job's [WorkInfo], so the UI always reflects the
 * worker even across process restarts.
 */
class VideoDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, VideoDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, VideoDownload>> = _downloads.asStateFlow()

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    init {
        runCatching { loadIndex() }
        observeWork()
    }

    // ---- Public API ----------------------------------------------------------

    /** Expected local media file for a download id (created once the job finishes). */
    fun fileFor(id: String): File? {
        val d = _downloads.value[id] ?: return null
        return d.filePath?.let { File(it).takeIf { f -> f.exists() } }
    }

    /** A completed offline file for an item key, used to prefer local playback. */
    fun localFileFor(itemKey: String): File? =
        _downloads.value.values
            .filter { it.itemKey == itemKey && it.status == VideoDownloadStatus.COMPLETED }
            .mapNotNull { it.filePath?.let { p -> File(p).takeIf { f -> f.exists() } } }
            .firstOrNull()

    fun enqueue(item: MediaItem, url: String, title: String, subtitle: String?, kind: VideoKind) {
        val id = stableId(item.key, url)
        val existing = _downloads.value[id]
        if (existing?.status == VideoDownloadStatus.COMPLETED) return
        val download = VideoDownload(
            id = id,
            itemKey = item.key,
            itemJson = Http.gson.toJson(item),
            url = url,
            title = title,
            subtitle = subtitle,
            poster = item.poster,
            kind = kind,
            status = VideoDownloadStatus.QUEUED,
            filePath = expectedPath(id, kind).absolutePath,
        )
        _downloads.value = _downloads.value + (id to download)
        saveIndex()
        enqueueWork(download)
    }

    fun pause(id: String) {
        workManager.cancelUniqueWork(VideoDownloadContract.workName(id))
        update(id) { it.copy(status = VideoDownloadStatus.PAUSED, speedBytesPerSec = 0L, etaSeconds = 0L) }
        saveIndex()
    }

    fun resume(id: String) {
        val d = _downloads.value[id] ?: return
        update(id) { it.copy(status = VideoDownloadStatus.QUEUED, error = null) }
        enqueueWork(d)
    }

    fun delete(id: String) {
        workManager.cancelUniqueWork(VideoDownloadContract.workName(id))
        File(root(), id).deleteRecursively()
        _downloads.value = _downloads.value - id
        saveIndex()
    }

    // ---- Internals -----------------------------------------------------------

    private fun enqueueWork(d: VideoDownload) {
        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(
                workDataOf(
                    VideoDownloadContract.KEY_ID to d.id,
                    VideoDownloadContract.KEY_URL to d.url,
                    VideoDownloadContract.KEY_KIND to d.kind.name,
                    VideoDownloadContract.KEY_TITLE to d.title,
                )
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag(VideoDownloadContract.TAG)
            .addTag(VideoDownloadContract.dlTag(d.id))
            .build()
        workManager.enqueueUniqueWork(
            VideoDownloadContract.workName(d.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun observeWork() {
        scope.launch {
            runCatching {
                workManager.getWorkInfosByTagFlow(VideoDownloadContract.TAG).collect { infos ->
                    infos.forEach { info -> applyWorkInfo(info) }
                }
            }
        }
    }

    private fun applyWorkInfo(info: WorkInfo) {
        val id = VideoDownloadContract.idFromTags(info.tags) ?: return
        val current = _downloads.value[id] ?: return
        val progressData = info.progress
        val status = when (info.state) {
            WorkInfo.State.RUNNING -> VideoDownloadStatus.DOWNLOADING
            WorkInfo.State.SUCCEEDED -> VideoDownloadStatus.COMPLETED
            WorkInfo.State.FAILED -> VideoDownloadStatus.FAILED
            WorkInfo.State.CANCELLED -> VideoDownloadStatus.PAUSED
            else -> VideoDownloadStatus.QUEUED
        }
        val updated = current.copy(
            status = status,
            progress = progressData.getInt(VideoDownloadContract.KEY_PROGRESS, (current.progress * 100).toInt()) / 100f,
            bytes = progressData.getLong(VideoDownloadContract.KEY_BYTES, current.bytes),
            totalBytes = progressData.getLong(VideoDownloadContract.KEY_TOTAL, current.totalBytes),
            speedBytesPerSec = if (status == VideoDownloadStatus.DOWNLOADING) {
                progressData.getLong(VideoDownloadContract.KEY_SPEED, current.speedBytesPerSec)
            } else 0L,
            etaSeconds = if (status == VideoDownloadStatus.DOWNLOADING) {
                progressData.getLong(VideoDownloadContract.KEY_ETA, current.etaSeconds)
            } else 0L,
            error = if (status == VideoDownloadStatus.FAILED) {
                info.outputData.getString(VideoDownloadContract.KEY_ERROR) ?: current.error
            } else current.error,
            updatedAt = System.currentTimeMillis(),
        )
        _downloads.value = _downloads.value + (id to updated)
        if (status == VideoDownloadStatus.COMPLETED || status == VideoDownloadStatus.FAILED) saveIndex()
    }

    private fun update(id: String, transform: (VideoDownload) -> VideoDownload) {
        val current = _downloads.value[id] ?: return
        _downloads.value = _downloads.value + (id to transform(current))
    }

    private fun expectedPath(id: String, kind: VideoKind): File {
        val dir = File(root(), id).apply { mkdirs() }
        return if (kind == VideoKind.HLS) File(dir, "local.m3u8") else File(dir, "video.mp4")
    }

    private fun root(): File = File(context.filesDir, "downloads/video").apply { mkdirs() }

    private fun indexFile(): File = File(root(), "index.json")

    private fun saveIndex() {
        runCatching { indexFile().writeText(Http.gson.toJson(_downloads.value.values.toList())) }
    }

    private fun loadIndex() {
        if (!indexFile().exists()) return
        val list = runCatching {
            Http.gson.fromJson(indexFile().readText(), Array<VideoDownload>::class.java)?.toList()
        }.getOrNull() ?: return
        // Anything mid-flight when the process died comes back paused; the worker job (if still
        // alive) will immediately flip it back to RUNNING via the WorkInfo observer.
        val normalized = list.map { d ->
            if (d.status == VideoDownloadStatus.DOWNLOADING || d.status == VideoDownloadStatus.QUEUED) {
                d.copy(status = VideoDownloadStatus.PAUSED)
            } else d
        }
        _downloads.value = normalized.associateBy { it.id }
    }

    private companion object {
        /** Stable, collision-resistant id from the item key + stream URL. */
        fun stableId(itemKey: String, url: String): String =
            UUID.nameUUIDFromBytes("$itemKey|$url".toByteArray()).toString()
    }
}
