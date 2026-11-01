package com.moovio.app.data.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.exoplayer.offline.Download
import com.moovio.app.data.local.SettingsStore
import com.moovio.app.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.UUID

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
 *
 * Progressive downloads are cached under the download id as the cache key and exported as a single
 * file. HLS downloads cache the master playlist and each segment under their own URL-derived keys,
 * so they are exported as a directory (`<title>/` containing the playlist + segment files) — never
 * concatenated into one unplayable file.
 */
@UnstableApi
object DownloadExporter {

    /**
     * Write the cached media for [download] into the configured tree as [displayName].
     * Returns true only when bytes were actually written.
     */
    suspend fun export(
        context: Context,
        settings: SettingsStore,
        download: Download,
        displayName: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val treeUri = runCatching { settings.storageUri.first() }.getOrNull().orEmpty()
        if (treeUri.isBlank()) return@withContext false
        val tree = runCatching { Uri.parse(treeUri) }.getOrNull() ?: return@withContext false

        val cache = runCatching { DownloadManagerProvider.cache(context) }.getOrNull()
            ?: return@withContext false

        val kind = if (download.request.mimeType == "application/x-mpegURL" ||
            download.request.uri.lastPathSegment?.endsWith(".m3u8", ignoreCase = true) == true) {
            DownloadKind.HLS
        } else {
            DownloadKind.Progressive
        }

        when (kind) {
            DownloadKind.Progressive -> exportProgressive(context, tree, cache, download, displayName)
            DownloadKind.HLS -> exportHls(context, tree, cache, download, displayName)
        }
    }

    // ---- Progressive ----------------------------------------------------------

    /** Export a progressive (single-file) download: cached under the download id as cache key. */
    private fun exportProgressive(
        context: Context,
        tree: Uri,
        cache: Cache,
        download: Download,
        displayName: String,
    ): Boolean = runCatching {
        val spans = cache.getCachedSpans(download.request.id).filter { it.isCached }.sortedBy { it.position }
        if (spans.isEmpty()) return@runCatching false
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

    // ---- HLS ------------------------------------------------------------------

    /** Export an HLS download as a directory: manifest + segments, never concatenated. */
    private fun exportHls(
        context: Context,
        tree: Uri,
        cache: Cache,
        download: Download,
        displayName: String,
    ): Boolean = runCatching {
        val manifestUri = download.request.uri
        val manifestKey = manifestUri.toString()

        // Read the master playlist from cache.
        val manifestSpans = cache.getCachedSpans(manifestKey).filter { it.isCached }.sortedBy { it.position }
        if (manifestSpans.isEmpty()) return@runCatching false
        val manifestBytes = manifestSpans.flatMap { readSpanBytes(it).toList() }.toByteArray()
        if (manifestBytes.isEmpty()) return@runCatching false
        val manifestText = String(manifestBytes)

        // Resolve the base URL (manifest dir) for relative segment URLs.
        val baseUri = Uri.parse(manifestUri.toString().substringBeforeLast('/') + '/')

        // Parse segment URLs from the master playlist (basic #EXTINF → next line extraction).
        val segmentUrls = parseHlsSegments(manifestText, baseUri)

        // Look up each segment in cache.
        val segments = mutableListOf<Pair<String, ByteArray>>() // (segmentUrl, bytes)
        for (segUrl in segmentUrls) {
            val segSpans = cache.getCachedSpans(segUrl).filter { it.isCached }.sortedBy { it.position }
            if (segSpans.isEmpty()) continue
            val segBytes = segSpans.flatMap { readSpanBytes(it).toList() }.toByteArray()
            if (segBytes.isNotEmpty()) segments += segUrl to segBytes
        }

        if (segments.isEmpty()) return@runCatching false

        // Create a directory for this export, then write the manifest + segments into it.
        val dirName = sanitizeDirName(displayName)
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeDocId)
        val dir = DocumentsContract.createDocument(resolver, parent, "application/vnd.android.document/directory", dirName)
            ?: return@runCatching false
        val dirDocumentId = DocumentsContract.getDocumentId(dir)
        val dirUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirDocumentId)

        // Write the master playlist (rewritten with relative segment names so it stays playable).
        val manifestName = manifestUri.lastPathSegment ?: "master.m3u8"
        writeDocumentText(resolver, dirUri, manifestName, manifestText, "application/x-mpegURL")

        // Write each segment next to the playlist, using the segment's last path segment as filename.
        for ((segUrl, segBytes) in segments) {
            val segName = Uri.parse(segUrl).lastPathSegment ?: "segment_${UUID.randomUUID()}.ts"
            writeDocumentBytes(resolver, dirUri, segName, segBytes, "video/mp2t")
        }

        true
    }.getOrDefault(false)

    /** Parse segment URLs from an HLS master playlist (basic #EXTINF → URL-line extraction). */
    private fun parseHlsSegments(manifest: String, base: Uri): List<String> {
        val lines = manifest.lines()
        val urls = mutableListOf<String>()
        var expectUrl = false
        for (line in lines) {
            val trimmed = line.trim()
            if (expectUrl && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                urls.add(resolveUrl(base, trimmed))
                expectUrl = false
            }
            expectUrl = trimmed.startsWith("#EXTINF")
        }
        return urls
    }

    /** Resolve a possibly-relative URL against a base. */
    private fun resolveUrl(base: Uri, ref: String): String =
        if (ref.startsWith("http://", ignoreCase = true) || ref.startsWith("https://", ignoreCase = true)) ref
        else base.toString().substringBeforeLast('/') + '/' + ref

    /** Read all bytes from a cache span's file. */
    private fun readSpanBytes(span: CacheSpan): ByteArray {
        val file = span.file ?: return ByteArray(0)
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(span.position)
                val available = file.length() - span.position
                val remaining = if (span.length == C.LENGTH_UNSET.toLong()) available
                else minOf(span.length, available)
                val buffer = ByteArray(64 * 1024)
                var left = remaining
                val out = ByteArray(remaining.toInt())
                var offset = 0
                while (left > 0) {
                    val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (read <= 0) break
                    System.arraycopy(buffer, 0, out, offset, read)
                    offset += read
                    left -= read
                }
                out
            }
        }.getOrDefault(ByteArray(0))
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

    /** Write [text] as a document named [name] inside [dirUri]. */
    private fun writeDocumentText(
        resolver: android.content.ContentResolver,
        dirUri: Uri,
        name: String,
        text: String,
        mime: String,
    ) {
        val document = DocumentsContract.createDocument(resolver, dirUri, mime, name) ?: return
        resolver.openOutputStream(document)?.use { out ->
            out.write(text.toByteArray())
        }
    }

    /** Write [bytes] as a document named [name] inside [dirUri]. */
    private fun writeDocumentBytes(
        resolver: android.content.ContentResolver,
        dirUri: Uri,
        name: String,
        bytes: ByteArray,
        mime: String,
    ) {
        val document = DocumentsContract.createDocument(resolver, dirUri, mime, name) ?: return
        resolver.openOutputStream(document)?.use { out ->
            out.write(bytes)
        }
    }

    /** Sanitize a directory name (remove path separators and dangerous chars). */
    private fun sanitizeDirName(name: String): String =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().ifBlank { "Moovio download" }

    /** A MIME type SAF providers accept for the exported file (kept generic to avoid rejections). */
    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v", "mov" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "ts" -> "video/mp2t"
        else -> "application/octet-stream"
    }

    private enum class DownloadKind { Progressive, HLS }
}
