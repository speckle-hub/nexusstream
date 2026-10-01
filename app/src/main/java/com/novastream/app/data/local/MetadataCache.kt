package com.novastream.app.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Simple on-disk JSON cache for metadata responses. */
class MetadataCache(private val context: Context) {

    private val dir: File by lazy {
        File(context.cacheDir, "metadata").apply { if (!exists()) mkdirs() }
    }

    private fun fileFor(key: String) = File(dir, key.hashCode().toString() + ".json")

    suspend fun get(key: String, maxAgeMs: Long = 6 * 60 * 60 * 1000L): String? = withContext(Dispatchers.IO) {
        val f = fileFor(key)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) f.readText() else null
    }

    suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            fileFor(key).writeText(value)
        } catch (_: Exception) { }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
