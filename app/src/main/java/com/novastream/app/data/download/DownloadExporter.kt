package com.novastream.app.data.download

import android.content.Context
import android.provider.DocumentsContract
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheSpan
import com.novastream.app.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.RandomAccessFile

/**
 * Copies a completed Media3 download out of the app-private playback cache into the user's chosen
 * SAF folder (Settings → Download folder).
 *
 * Media3's [androidx.media3.datasource.cache.SimpleCache] can only be rooted at a real
 * [java.io.File], so a `content://` tree cannot *be* the cache. The setting is therefore honoured
 * as an **export destination**: once a download finishes, its cached bytes are streamed into a
 * document in that folder. This is entirely best-effort — a missing folder, a revoked permission
 * or an unrecognised cache key simply leaves the download in app storage, where it stays playable
 * offline.
 */
@UnstableApi
object DownloadExporter {

    /**
     * Write the cached media for [cacheKey] into the configured tree as [displayName].
     * Returns true only when bytes were actually written.
     */
    suspend fun export(
        context: Context,
        settings: SettingsStore,
        cacheKey: String,
        displayName: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val treeUri = runCatching { settings.storageUri.first() }.getOrNull().orEmpty()
        if (treeUri.isBlank()) return@withContext false
        val tree = runCatching { android.net.Uri.parse(treeUri) }.getOrNull() ?: return@withContext false

        val cache = runCatching { DownloadManagerProvider.cache(context) }.getOrNull()
            ?: return@withContext false
        val spans = runCatching {
            cache.getCachedSpans(cacheKey).filter { it.isCached }.sortedBy { it.position }
        }.getOrDefault(emptyList())
        if (spans.isEmpty()) return@withContext false

        runCatching {
            val resolver = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeDocId)
            val document = DocumentsContract.createDocument(resolver, parent, mimeFor(displayName), displayName)
                ?: return@runCatching false
            resolver.openOutputStream(document)?.use { out ->
                spans.forEach { copySpan(it, out) }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    /** Copy one cache span (a byte range inside [CacheSpan.file]) to [out]. */
    private fun copySpan(span: CacheSpan, out: OutputStream) {
        val file = span.file ?: return
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(span.position)
                val available = file.length() - span.position
                val remaining = if (span.length == C.LENGTH_UNSET.toLong()) available
                else minOf(span.length, available)
                val buffer = ByteArray(64 * 1024)
                var left = remaining
                while (left > 0) {
                    val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    left -= read
                }
            }
        }
    }

    /** A MIME type SAF providers accept for the exported file (kept generic to avoid rejections). */
    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v", "mov" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "ts" -> "video/mp2t"
        else -> "application/octet-stream"
    }
}
