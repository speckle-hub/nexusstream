package com.moovio.app.data.mihon

import android.content.Context
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.Video
import com.moovio.app.data.repo.MangaExtensionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Execution engine for Aniyomi / Mihon extension APKs.
 *
 * Builds on [MangaExtensionRepository] (which downloads APKs and persists descriptors) by adding
 * the runtime piece: loading APKs with [MihonExtensionRuntime] and adapting sources through
 * [MihonExtensionSources].
 *
 * ### NSFW gating
 * A `MangaExt.nsfw` flag (stamped from the repo it came from) gates which sections see the
 * extension: NSFW-flagged extensions only surface in `NSFW_ANIME` / `NSFW_MANGA` / `REAL`
 * sections, never in general search or Home. This mirrors the CloudStream `nsfw` handling.
 */
class MihonExtensionRepository(
    private val context: Context,
    private val store: AddonStore,
    private val mangaRepo: MangaExtensionRepository,
) {

    private val runtime = MihonExtensionRuntime

    /** Every downloaded extension descriptor (enabled + disabled), same as [MangaExtensionRepository.installed]. */
    val installed: Flow<List<MangaExt>> = mangaRepo.installed

    /** Loaded extension sources (enabled extensions with APKs on disk). */
    val loaded: Flow<List<Any>> = store.mangaInstalled.map { exts ->
        exts.filterNot { it.disabled }
            .filter { it.apkPath != null }
            .flatMap { ext ->
                runtime.sourcesOf(ext.apkPath!!)
            }
    }

    /**
     * Install [ext]: download its APK (delegated to [MangaExtensionRepository]), then load it
     * into the runtime so its sources become searchable / playable this session.
     */
    suspend fun install(ext: MangaExt): Result<MangaExt> = withContext(Dispatchers.IO) {
        val installed = mangaRepo.install(ext)
        installed.onSuccess { descriptor ->
            // Load the newly installed APK into the runtime.
            val file = File(descriptor.apkPath!!)
            runtime.load(context, file)
        }
        installed
    }

    /** Enable / disable an installed extension. Disabled extensions are not loaded. */
    suspend fun setEnabled(pkg: String, enabled: Boolean) = mangaRepo.setEnabled(pkg, enabled)

    /** Remove the extension: unload from runtime, delete APK, drop descriptor. */
    suspend fun uninstall(pkg: String) = withContext(Dispatchers.IO) {
        // Unload from runtime first.
        store.mangaInstalled.first().firstOrNull { it.pkg == pkg }?.apkPath?.let { path ->
            runtime.unload(path)
        }
        mangaRepo.uninstall(pkg)
    }

    /** Reload every installed, enabled extension APK into the runtime (called on app start). */
    suspend fun reloadAll(): List<Any> = withContext(Dispatchers.IO) {
        val exts = store.mangaInstalled.first()
        var sources = mutableListOf<Any>()
        exts.filterNot { it.disabled }
            .filter { it.apkPath != null }
            .forEach { ext ->
                val file = File(ext.apkPath!!)
                if (file.exists()) {
                    runtime.load(context, file).onSuccess { loadedSources ->
                        sources.addAll(loadedSources)
                    }
                }
            }
        sources.toList()
    }

    /** The loaded source instance whose display name matches [sourceName], or null. */
    suspend fun sourceFor(sourceName: String): Any? =
        store.mangaInstalled.first().firstOrNull { ext ->
            MihonExtensionSources.sourceName(ext) == sourceName
        }?.apkPath?.let { path ->
            runtime.sourcesOf(path).firstOrNull { s ->
                MihonExtensionSources.sourceName(s) == sourceName
            }
        }

    /** Every loaded source instance, across all installed extensions. */
    fun allSources(): List<Any> = runtime.sources()

    // ---- Source-level operations (delegated to MihonExtensionSources) ------------

    suspend fun latest(source: Any, page: Int = 1, isNsfw: Boolean = false): List<MediaItem> =
        MihonExtensionSources.latest(source, page).map { it.copy(type = MihonExtensionSources.mediaType(source, isNsfw)) }

    suspend fun popular(source: Any, page: Int = 1, isNsfw: Boolean = false): List<MediaItem> =
        MihonExtensionSources.popular(source, page).map { it.copy(type = MihonExtensionSources.mediaType(source, isNsfw)) }

    suspend fun search(
        source: Any,
        query: String,
        page: Int = 1,
        isNsfw: Boolean = false,
    ): List<MediaItem> =
        MihonExtensionSources.search(source, query, page).map { it.copy(type = MihonExtensionSources.mediaType(source, isNsfw)) }

    suspend fun details(source: Any, titleUrl: String): Map<String, Any?>? =
        MihonExtensionSources.details(source, titleUrl)

    suspend fun chapters(source: Any, titleUrl: String): List<Map<String, Any?>> =
        MihonExtensionSources.chapters(source, titleUrl)

    suspend fun pages(source: Any, chapterUrl: String): List<Map<String, Any?>> =
        MihonExtensionSources.pages(source, chapterUrl)

    fun chapterToVideo(chapter: Map<String, Any?>): Video =
        MihonExtensionSources.chapterToVideo(chapter)

    fun pageToStreamSource(page: Map<String, Any?>, source: Any): StreamSource =
        MihonExtensionSources.pageToStreamSource(page, source)

    fun sourceName(source: Any): String = MihonExtensionSources.sourceName(source)
    fun sourceMediaType(source: Any, isNsfw: Boolean): MediaType = MihonExtensionSources.mediaType(source, isNsfw)
    fun baseUrl(source: Any): String? = MihonExtensionSources.baseUrl(source)

    companion object {
        private const val MIN_APK_BYTES = 2048
    }
}
