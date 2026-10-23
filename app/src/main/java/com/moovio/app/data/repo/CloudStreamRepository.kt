package com.moovio.app.data.repo

import android.content.Context
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.MainAPI
import com.moovio.app.data.cloudstream.CloudStreamInstalled
import com.moovio.app.data.cloudstream.CloudStreamRuntime
import com.moovio.app.data.cloudstream.CloudStreamSources
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.model.CloudStreamExt
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.Video
import com.moovio.app.data.remote.httpGetBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Installs, persists and runs CloudStream `.cs3` provider plugins.
 *
 * Installing downloads the plugin file into app-private storage and loads it through
 * [CloudStreamRuntime], which registers the plugin's [MainAPI] providers. From then on those
 * providers can search, resolve details and return real playable extractor links (see
 * [CloudStreamSources]) exactly like the built-in/Stremio sources do.
 */
class CloudStreamRepository(
    private val store: AddonStore,
    private val context: Context,
) {

    /** Plugins the user has installed (persisted; reloaded on launch). */
    val installed: Flow<List<CloudStreamInstalled>> = store.cloudStreamPlugins

    /** Repositories are downloaded into this sandbox; nothing outside app-private storage is loaded. */
    private val pluginDir: File get() = File(context.filesDir, "cloudstream").apply { mkdirs() }

    private fun fileKey(ext: CloudStreamExt): String =
        (ext.internalName ?: ext.name).replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "plugin" }

    /**
     * Download + load [ext]. Returns the persisted descriptor on success. The plugin file is
     * rewritten every install so a version bump on the repo always takes effect.
     */
    suspend fun install(ext: CloudStreamExt): Result<CloudStreamInstalled> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(pluginDir, "${fileKey(ext)}.cs3")
            val bytes = httpGetBytes(ext.url)
            require(bytes.isNotEmpty()) { "Empty plugin download" }
            target.writeBytes(bytes)

            CloudStreamRuntime.load(context, target).getOrThrow()

            val descriptor = CloudStreamInstalled(
                url = ext.url,
                name = ext.name,
                version = ext.version,
                internalName = ext.internalName,
                filePath = target.absolutePath,
                nsfw = ext.nsfw,
            )
            val list = store.cloudStreamPlugins.first().filterNot { it.url == ext.url } + descriptor
            store.saveCloudStreamPlugins(list)
            descriptor
        }
    }

    /** Unload the plugin, delete its file, and forget it. */
    suspend fun uninstall(url: String) = withContext(Dispatchers.IO) {
        val list = store.cloudStreamPlugins.first()
        list.firstOrNull { it.url == url }?.filePath?.let { path ->
            runCatching { CloudStreamRuntime.unload(File(path)) }
            runCatching { File(path).delete() }
        }
        store.saveCloudStreamPlugins(list.filterNot { it.url == url })
    }

    /** Reload every installed plugin (called once at app start). Failures are contained per plugin. */
    suspend fun reloadAll() = withContext(Dispatchers.IO) {
        runCatching { CloudStreamApp.initialize(context) }
        store.cloudStreamPlugins.first().forEach { descriptor ->
            val path = descriptor.filePath ?: return@forEach
            val file = File(path)
            if (file.exists() && !CloudStreamRuntime.isLoaded(path)) {
                runCatching { CloudStreamRuntime.load(context, file) }
            }
        }
    }

    fun isLoaded(filePath: String): Boolean = CloudStreamRuntime.isLoaded(filePath)

    /** All providers currently contributed by loaded plugins. */
    fun providers(): List<MainAPI> = CloudStreamRuntime.providers()

    /**
     * Providers split by the repository they came from: [includeNsfw] false drops every provider
     * installed from an adult repo (CXXX), which is what keeps those out of general search while
     * still making them reachable from the NSFW area.
     *
     * The NSFW flag lives on the persisted plugin descriptor, so a plugin whose flag is unknown
     * (installed before the field existed) counts as SFW — the safe default for a *content* gate
     * is debatable, but this is a visibility filter, and defaulting to hidden would silently make
     * previously working providers disappear.
     */
    suspend fun providers(includeNsfw: Boolean): List<MainAPI> {
        if (includeNsfw) return providers()
        val nsfwPaths = runCatching { store.cloudStreamPlugins.first() }
            .getOrDefault(emptyList())
            .filter { it.nsfw }
            .mapNotNull { it.filePath }
            .toSet()
        if (nsfwPaths.isEmpty()) return providers()
        return providers().filterNot { it.sourcePlugin in nsfwPaths }
    }

    /** Resolve a provider from the `cloudstream:<provider>` addon id carried by a [MediaItem]. */
    fun providerFor(addonId: String?): MainAPI? =
        CloudStreamSources.providerNameOf(addonId)?.let { name ->
            CloudStreamRuntime.providerByName(name)
        }

    /** Streams for a CloudStream item opened from the app (movie `video == null`, or an episode). */
    suspend fun streamsFor(item: MediaItem, video: Video? = null): List<StreamSource> {
        val provider = providerFor(item.addonId) ?: return emptyList()
        return CloudStreamSources.streams(provider, item, video)
    }

    /**
     * Bonus links for a *title* that isn't a CloudStream item: ask every loaded provider for it,
     * keep the best title match, then resolve that provider's streams. Movies only — series need an
     * episode selected first, which the detail screen supplies through [streamsFor].
     */
    suspend fun titleStreams(title: String, type: MediaType): List<StreamSource> {
        if (type != MediaType.MOVIE) return emptyList()
        if (title.isBlank()) return emptyList()
        val out = ArrayList<StreamSource>()
        for (provider in providers()) {
            val match = runCatching {
                CloudStreamSources.bestMatch(CloudStreamSources.search(provider, title), title)
            }.getOrNull() ?: continue
            runCatching { out.addAll(CloudStreamSources.streams(provider, match, null)) }
        }
        return out
    }
}
