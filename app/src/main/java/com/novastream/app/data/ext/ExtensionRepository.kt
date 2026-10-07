package com.novastream.app.data.ext

import android.content.Context
import com.novastream.app.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-backed store of installed extension descriptors.
 *
 * Reads and writes go through [ExtensionCodec], which sanitizes **per entry** — a malformed (or
 * partially written) record used to decode into an [ExtensionDescriptor] with null fields, and the
 * Sources dashboard crashed the moment it tried to render one. A single bad entry is now dropped
 * while every usable entry survives, and the read can never throw.
 */
class ExtensionStore(context: Context) {

    private val file = File(context.filesDir, "extensions.json")

    fun list(): List<ExtensionDescriptor> = runCatching {
        if (!file.exists()) emptyList() else ExtensionCodec.decode(file.readText())
    }.getOrDefault(emptyList())

    fun save(list: List<ExtensionDescriptor>) {
        runCatching { file.writeText(ExtensionCodec.encode(list)) }
    }

    fun add(descriptor: ExtensionDescriptor) {
        save(list().filterNot { it.id == descriptor.id } + descriptor)
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }
}

/**
 * Installs, loads, and unloads external extensions. Copies an incoming extension file into
 * app-private storage (the loader's trust root) before loading it.
 */
class ExtensionRepository(
    private val store: ExtensionStore,
    private val context: Context,
    private val settings: SettingsStore,
) {

    /** Installed descriptors, never throwing and never carrying a null field (see [ExtensionCodec]). */
    fun installed(): List<ExtensionDescriptor> =
        runCatching { store.list() }.getOrDefault(emptyList())

    fun loadedExtensions(): List<NexusExtension> = ExtensionHost.all()

    /**
     * Re-load every installed extension (e.g. on app start), **except** those the user disabled in
     * the Sources dashboard — otherwise the disable switch was session-only and every restart
     * silently re-enabled every extension.
     *
     * Cold-boot resilience: failures are contained **per extension** *and* retried once, and the
     * descriptor read itself is retried once, because both are transient far more often than they
     * are real (the dex opt directory may still be settling after an update, storage may not be
     * ready yet). A source that still fails to load is simply absent this session — it never takes
     * the other sources (or the app) down with it, and this function never throws.
     */
    suspend fun reloadAll(): List<NexusExtension> = withContext(Dispatchers.IO) {
        val disabled = runCatching { settings.disabledSources.first() }.getOrDefault(emptyList())
        val descriptors = runCatching { store.list() }.getOrElse {
            delay(READ_RETRY_DELAY_MS)
            runCatching { store.list() }.getOrDefault(emptyList())
        }
        val loaded = LinkedHashMap<String, NexusExtension>()
        descriptors.filterNot { it.id in disabled }.forEach { descriptor ->
            var plugin = ExtensionHost.load(descriptor, context.filesDir).getOrNull()
            if (plugin == null) {
                delay(READ_RETRY_DELAY_MS)
                plugin = ExtensionHost.load(descriptor, context.filesDir).getOrNull()
            }
            if (plugin != null) loaded[descriptor.id] = plugin
        }
        loaded.values.toList()
    }

    private companion object {
        /** Pause before the single retry of a cold-boot read/load. */
        const val READ_RETRY_DELAY_MS = 300L
    }

    /**
     * Import an extension file from [source] into private storage, then load it.
     * Returns the loaded plugin on success.
     */
    suspend fun install(
        id: String,
        name: String,
        version: String?,
        entryClass: String,
        source: File,
    ): Result<NexusExtension> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "extensions").apply { mkdirs() }
            val target = File(dir, "${id}.apk")
            source.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            val descriptor = ExtensionDescriptor(
                id = id,
                name = name,
                version = version,
                entryClass = entryClass,
                apkPath = target.absolutePath,
            )
            val plugin = ExtensionHost.load(descriptor, context.filesDir).getOrThrow()
            store.add(descriptor)
            plugin
        }
    }

    /** Unload and forget an extension; deletes its stored file. */
    fun remove(id: String) {
        ExtensionHost.unload(id)
        store.list().firstOrNull { it.id == id }?.let { runCatching { File(it.apkPath).delete() } }
        store.remove(id)
    }
}
