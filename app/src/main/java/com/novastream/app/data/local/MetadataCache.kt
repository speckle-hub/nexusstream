package com.novastream.app.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Simple on-disk cache for metadata responses.
 *
 * Files are named from a SHA-256 digest of the cache key: the old `String.hashCode()` naming could
 * collide, so two different URLs could serve each other's cached body. Total size is capped and the
 * oldest files are evicted on write, so the cache can't grow without bound.
 */
class MetadataCache(private val context: Context) {

    private val dir: File by lazy {
        File(context.cacheDir, "metadata").apply { if (!exists()) mkdirs() }
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        // 32 hex chars (128 bits) is plenty to avoid collisions while keeping filenames short.
        val name = digest.take(16).joinToString("") { "%02x".format(it) }
        return File(dir, "$name.json")
    }

    suspend fun get(key: String, maxAgeMs: Long = 6 * 60 * 60 * 1000L): String? = withContext(Dispatchers.IO) {
        val f = fileFor(key)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) f.readText() else null
    }

    suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            fileFor(key).writeText(value)
            trim()
        } catch (_: Exception) { }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /** Evict the least-recently-written files until the cache fits within [MAX_BYTES]. */
    private fun trim() {
        val files = dir.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { f ->
            if (total <= MAX_BYTES) return
            total -= f.length()
            f.delete()
        }
    }

    private companion object {
        /** Upper bound on the on-disk metadata cache. */
        const val MAX_BYTES = 64L * 1024 * 1024
    }
}
