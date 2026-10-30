package com.moovio.app.data.download

import java.io.File

/**
 * Pure helpers for spotting orphaned Media3 `SimpleCache` files.
 *
 * Media3 normally releases a download's cache spans when it fails or is removed, but a process
 * kill mid-download can leave the underlying chunk files on disk with no index entry pointing at
 * them. Those bytes are invisible to `SimpleCache.getCacheSpace()`,
 * invisible in the Downloads tab, and are never evicted — so over time the app bloats by hundreds
 * of MB. These helpers decide which on-disk files are orphans given the set of files the cache
 * index still references.
 */
object DownloadCacheMaintenance {

    /** SimpleCache bookkeeping files; never delete these. */
    private const val INDEX_PREFIX = "cached_content"

    /** Extensions the cache/downloader writes that can become orphaned after a crash. */
    private val CACHE_EXTENSIONS = setOf("exo", "tmp", "part", "vtt")

    /** The file names the cache index still references (from `CacheSpan.file` across all keys). */
    fun referencedNames(files: Iterable<File?>): Set<String> =
        files.filterNotNull().map { it.name }.toSet()

    /**
     * True when [file] is a cache chunk that no index entry references. The index/metadata files
     * are always kept, and only known cache extensions are considered (so unrelated files are
     * never touched).
     */
    fun isOrphan(file: File, referenced: Set<String>): Boolean {
        if (!file.isFile) return false
        val name = file.name
        if (name.startsWith(INDEX_PREFIX)) return false
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext !in CACHE_EXTENSIONS) return false
        return name !in referenced
    }

    /** All orphaned chunk files under [dir] (recursively). */
    fun orphans(dir: File?, referenced: Set<String>): List<File> {
        if (dir == null || !dir.isDirectory) return emptyList()
        return dir.walkTopDown().filter { isOrphan(it, referenced) }.toList()
    }

    /**
     * True when free space has fallen under the user's limit.
     *
     * A limit of zero or less means "off", and a negative free reading (some file systems report
     * that for a mount that has just gone away) is treated as unknown rather than as an emergency,
     * so a departed SD card never triggers a sweep.
     */
    fun isBelowLimit(freeBytes: Long, limitBytes: Long): Boolean =
        limitBytes > 0L && freeBytes in 0L until limitBytes

    /**
     * Whether an automatic sweep is worth running: only when space is short *and* there is something
     * to reclaim. Completed downloads are never touched — the sweep here is limited to chunks no
     * index entry references, i.e. bytes the user already cannot play.
     */
    fun shouldSweep(freeBytes: Long, limitBytes: Long, orphanCount: Int): Boolean =
        orphanCount > 0 && isBelowLimit(freeBytes, limitBytes)
}
