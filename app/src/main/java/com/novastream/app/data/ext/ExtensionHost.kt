package com.novastream.app.data.ext

import dalvik.system.DexClassLoader
import java.io.File

/** A persisted description of an installed extension. */
data class ExtensionDescriptor(
    /** Stable id, also used as the plugin key. */
    val id: String,
    val name: String,
    val version: String?,
    /** Fully-qualified class name implementing [NexusExtension]. */
    val entryClass: String,
    /** Absolute path to the `.apk` / `.dex` / `.extension` file inside app-private storage. */
    val apkPath: String,
)

/**
 * Sandboxed loader + registry for external extensions.
 *
 * ### Trust model
 * An extension is arbitrary code running inside the app's process with the app's privileges, so
 * this is **not** a security boundary — only load extensions the user explicitly installed and
 * trusts. The "sandboxing" here is (a) files must live under the app's private storage, so no
 * world-writable path can be loaded, (b) code is loaded through an isolated [DexClassLoader]
 * whose optional-dex (optimized output) directory is app-private, and (c) every failure is
 * contained so a broken extension can never crash the app.
 *
 * Delegate class resolution is rooted at the host classloader, which is required so the loaded
 * class can be resolved as a [NexusExtension].
 */
object ExtensionHost {

    private val loaded = LinkedHashMap<String, NexusExtension>()

    /**
     * Load [descriptor] and instantiate its [NexusExtension] entry point.
     *
     * @param privateRoot the app's `filesDir`; the extension file must resolve inside it.
     */
    fun load(
        descriptor: ExtensionDescriptor,
        privateRoot: File,
        parent: ClassLoader? = null,
    ): Result<NexusExtension> = runCatching {
        val root = privateRoot.canonicalFile
        val apk = File(descriptor.apkPath).canonicalFile
        require(apk.exists()) { "Extension file not found: ${descriptor.apkPath}" }
        require(apk.absolutePath.startsWith(root.absolutePath)) {
            "Extension must live in app-private storage"
        }
        require(descriptor.entryClass.isNotBlank()) { "Missing entry class" }

        val optimized = File(root, "dex_opt").apply { mkdirs() }
        val loader = DexClassLoader(
            apk.absolutePath,
            optimized.absolutePath,
            null,
            parent ?: ExtensionHost::class.java.classLoader,
        )
        val clazz = Class.forName(descriptor.entryClass, true, loader)
        val instance = clazz.getDeclaredConstructor().newInstance()
        val plugin = instance as? NexusExtension
            ?: error("${descriptor.entryClass} does not implement NexusExtension")

        loaded[descriptor.id] = plugin
        plugin
    }

    fun get(id: String): NexusExtension? = loaded[id]

    fun all(): List<NexusExtension> = loaded.values.toList()

    fun unload(id: String) {
        loaded.remove(id)
    }

    fun clear() = loaded.clear()
}
