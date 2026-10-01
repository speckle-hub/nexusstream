package com.novastream.app.data.ext

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** File-backed store of installed extension descriptors. */
class ExtensionStore(context: Context) {

    private val file = File(context.filesDir, "extensions.json")

    fun list(): List<ExtensionDescriptor> = runCatching {
        if (!file.exists()) emptyList()
        else Gson().fromJson(file.readText(), Array<ExtensionDescriptor>::class.java)?.toList() ?: emptyList()
    }.getOrDefault(emptyList())

    fun save(list: List<ExtensionDescriptor>) {
        runCatching { file.writeText(Gson().toJson(list)) }
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
) {

    fun installed(): List<ExtensionDescriptor> = store.list()

    fun loadedExtensions(): List<NexusExtension> = ExtensionHost.all()

    /** Re-load every installed extension (e.g. on app start). Failures are dropped per extension. */
    suspend fun reloadAll(): List<NexusExtension> = withContext(Dispatchers.IO) {
        store.list().mapNotNull { ExtensionHost.load(it, context.filesDir).getOrNull() }
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
