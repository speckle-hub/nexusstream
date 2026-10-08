package com.novastream.app.data.download

import android.content.Context
import com.novastream.app.data.remote.Http
import com.novastream.app.data.remote.httpGetBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Lifecycle of a single downloaded chapter. */
enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

/**
 * A queued/downloaded manga chapter.
 *
 * The remote [pages] list is persisted alongside the images so a download can resume after the
 * app is restarted, and so the reader can rebuild the ordered local page list from disk.
 */
data class MangaDownload(
    val mangaId: String,
    val mangaTitle: String,
    val chapterId: String,
    val chapterTitle: String,
    val cover: String? = null,
    val pages: List<String> = emptyList(),
    val total: Int = 0,
    val downloaded: Int = 0,
    val bytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * True when the owning title is NSFW. Kept (as the trailing field, so older `meta.json`
     * snapshots simply default to false) because `MangaDownload` carries no `MediaType` and the
     * Library's NSFW filter would otherwise have no way to tell an adult chapter from a regular
     * one.
     */
    val nsfw: Boolean = false,
) {
    /** Stable key across manga and chapter. */
    val key: String get() = "$mangaId::$chapterId"

    val progress: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/**
 * Background manga-chapter downloader.
 *
 * Images are written to `files/downloads/manga/{mangaId}/{chapterId}/` in app-private storage,
 * with an `index.json` listing the ordered page filenames (what the reader reads for offline
 * playback) and a `meta.json` describing the download itself (what [loadFromDisk] restores).
 *
 * Concurrency: each chapter runs in its own job on a small app scope; pause cancels the job and
 * keeps the partial files, so resume only fetches the pages that are still missing.
 */
class MangaDownloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, MangaDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, MangaDownload>> = _downloads.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()
    private val paused = ConcurrentHashMap.newKeySet<String>()

    init { runCatching { loadFromDisk() } }

    // ---- Paths ---------------------------------------------------------------

    private fun root(): File = File(context.filesDir, "downloads/manga").apply { mkdirs() }

    fun chapterDir(mangaId: String, chapterId: String): File = File(root(), "$mangaId/$chapterId")

    // ---- Public API ----------------------------------------------------------

    /** Queue (or re-queue) a chapter for download. No-op if it is already completed. */
    fun enqueue(download: MangaDownload) {
        if (_downloads.value[download.key]?.status == DownloadStatus.COMPLETED) return
        val seeded = if (download.total > 0) download else download.copy(total = download.pages.size)
        update(download.key) { seeded.copy(status = DownloadStatus.QUEUED, error = null) }
        persistMeta(seeded)
        start(download.key)
    }

    fun pause(key: String) {
        paused.add(key)
        jobs[key]?.cancel()
        jobs.remove(key)
        update(key) { it.copy(status = DownloadStatus.PAUSED) }
        _downloads.value[key]?.let { persistMeta(it) }
    }

    fun resume(key: String) {
        paused.remove(key)
        start(key)
    }

    fun delete(key: String) {
        jobs[key]?.cancel()
        jobs.remove(key)
        paused.remove(key)
        _downloads.value[key]?.let { chapterDir(it.mangaId, it.chapterId).deleteRecursively() }
        _downloads.update { it - key }
    }

    fun retry(key: String) {
        paused.remove(key)
        start(key)
    }

    /** Ordered local page files for a chapter, or null when it isn't downloaded. */
    fun localPages(mangaId: String, chapterId: String): List<File>? {
        val dir = chapterDir(mangaId, chapterId)
        val index = File(dir, INDEX)
        if (!index.exists()) return null
        val names = runCatching {
            Http.gson.fromJson(index.readText(), Array<String>::class.java)?.toList()
        }.getOrNull() ?: return null
        val files = names.map { File(dir, it) }.filter { it.exists() && it.length() > 0L }
        return files.takeIf { it.isNotEmpty() }
    }

    fun isDownloaded(mangaId: String, chapterId: String): Boolean =
        _downloads.value["$mangaId::$chapterId"]?.status == DownloadStatus.COMPLETED ||
            localPages(mangaId, chapterId) != null

    // ---- Internals -----------------------------------------------------------

    private fun start(key: String) {
        jobs[key]?.cancel()
        jobs[key] = scope.launch {
            val d = _downloads.value[key] ?: return@launch
            if (d.pages.isEmpty()) {
                update(key) { it.copy(status = DownloadStatus.FAILED, error = "No pages to download") }; notifyFailed(_downloads.value[key]?.mangaTitle ?: "Chapter", "No pages to download")
                persistMeta(_downloads.value[key] ?: d)
                return@launch
            }
            val dir = chapterDir(d.mangaId, d.chapterId).apply { mkdirs() }
            update(key) { it.copy(status = DownloadStatus.DOWNLOADING, error = null) }

            val names = mangaPageFileNames(d.pages)
            var done = 0
            var bytes = 0L
            var lastBytes = 0L
            var lastTick = System.currentTimeMillis()

            for (i in d.pages.indices) {
                if (!isActive || paused.contains(key)) {
                    update(key) { it.copy(status = DownloadStatus.PAUSED) }
                    _downloads.value[key]?.let { persistMeta(it) }
                    return@launch
                }
                val file = File(dir, names[i])
                if (!file.exists() || file.length() == 0L) {
                    val data = runCatching { httpGetBytes(d.pages[i]) }.getOrNull()
                    if (data == null || data.isEmpty()) {
                        update(key) { it.copy(status = DownloadStatus.FAILED, error = "Page ${i + 1} failed") }; notifyFailed(_downloads.value[key]?.mangaTitle ?: "Chapter", "Page ${i + 1} failed")
                        _downloads.value[key]?.let { persistMeta(it) }
                        return@launch
                    }
                    file.writeBytes(data)
                }
                done++
                bytes += file.length()

                val now = System.currentTimeMillis()
                if (now - lastTick >= 500) {
                    val speed = ((bytes - lastBytes) * 1000L) / (now - lastTick).coerceAtLeast(1L)
                    lastBytes = bytes
                    lastTick = now
                    update(key) {
                        it.copy(downloaded = done, bytes = bytes, speedBytesPerSec = speed, status = DownloadStatus.DOWNLOADING)
                    }
                }
            }

            writeIndex(dir, names)
            update(key) {
                it.copy(
                    downloaded = done,
                    total = d.pages.size,
                    bytes = bytes,
                    speedBytesPerSec = 0L,
                    status = DownloadStatus.COMPLETED,
                )
            }
            _downloads.value[key]?.let { persistMeta(it) }
        }
    }

    private fun writeIndex(dir: File, names: List<String>) {
        runCatching { File(dir, INDEX).writeText(Http.gson.toJson(names)) }
    }

    private fun persistMeta(d: MangaDownload) {
        runCatching {
            val dir = chapterDir(d.mangaId, d.chapterId).apply { mkdirs() }
            File(dir, META).writeText(Http.gson.toJson(d))
        }
    }

    private fun loadFromDisk() {
        val dirs = root().listFiles()?.flatMap { it.listFiles()?.toList() ?: emptyList() } ?: return
        val loaded = HashMap<String, MangaDownload>()
        for (dir in dirs) {
            val meta = File(dir, META)
            if (!meta.exists()) continue
            val d = runCatching { Http.gson.fromJson(meta.readText(), MangaDownload::class.java) }.getOrNull() ?: continue
            // A download interrupted by process death should come back paused, not stuck.
            val restored = if (d.status == DownloadStatus.DOWNLOADING || d.status == DownloadStatus.QUEUED) {
                d.copy(status = DownloadStatus.PAUSED)
            } else d
            loaded[restored.key] = restored
        }
        if (loaded.isNotEmpty()) _downloads.value = loaded
    }

    private fun notifyFailed(title: String, error: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, "Download failed for $title: $error", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun update(key: String, transform: (MangaDownload) -> MangaDownload) {
        _downloads.update { current ->
            val existing = current[key] ?: return@update current
            current + (key to transform(existing))
        }
    }

    private companion object {
        const val INDEX = "index.json"
        const val META = "meta.json"
    }
}

/**
 * Deterministic, order-preserving local filenames for a chapter's pages (`page_0000.jpg`), so the
 * reader can rebuild the exact sequence from the directory listing / index.
 */
fun mangaPageFileNames(pages: List<String>): List<String> {
    val allowed = setOf("jpg", "jpeg", "png", "webp", "gif")
    return pages.mapIndexed { index, url ->
        val ext = url.substringBefore('?').substringAfterLast('.', "jpg").lowercase()
            .takeIf { it in allowed } ?: "jpg"
        "page_%04d.%s".format(index, ext)
    }
}
