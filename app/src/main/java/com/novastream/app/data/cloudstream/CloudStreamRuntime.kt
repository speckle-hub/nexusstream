package com.novastream.app.data.cloudstream

import android.content.Context
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.novastream.app.data.remote.Http
import dalvik.system.PathClassLoader
import java.io.File

/** A CloudStream `.cs3` plugin the user has installed, persisted so it reloads on next launch. */
data class CloudStreamInstalled(
    /** Remote URL the plugin was downloaded from. */
    val url: String,
    val name: String,
    val version: String? = null,
    val internalName: String? = null,
    /** Absolute path to the downloaded `.cs3` inside app-private storage. */
    val filePath: String? = null,
)

/**
 * Loads CloudStream `.cs3` provider plugins **in-process** using a [PathClassLoader] whose parent is
 * the app classloader. Because the app ships the CloudStream `library` artifact, providers resolve
 * their `com.lagradost.cloudstream3.*` superclasses (`BasePlugin`, `MainAPI`, `app`, …) and can run
 * exactly as they would inside CloudStream itself. When a plugin's `load()` runs it self-registers
 * its [MainAPI] providers, which we then read back from [APIHolder].
 *
 * This mirrors CloudStream's own `PluginManager.loadPlugin`, minus the parts that only make sense
 * for the full CloudStream app (notifications, safe-mode, repo sync).
 */
object CloudStreamRuntime {

    /** file path -> loaded plugin instance. */
    private val loaded = LinkedHashMap<String, BasePlugin>()

    fun load(context: Context, file: File): Result<BasePlugin> = runCatching {
        CloudStreamApp.initialize(context)
        require(file.exists()) { "Plugin file not found: ${file.absolutePath}" }
        // Android 14+ refuses to load a *writable* dex from app storage; CloudStream marks the
        // plugin read-only before loading for the same reason. Best-effort — older devices ignore it.
        runCatching { file.setReadOnly() }

        val loader = PathClassLoader(file.absolutePath, context.classLoader)
        val manifestJson = loader.getResourceAsStream("manifest.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("Plugin has no manifest.json")
        val manifest = Http.gson.fromJson(manifestJson, BasePlugin.Manifest::class.java)
        val className = manifest?.pluginClassName?.takeIf { it.isNotBlank() }
            ?: error("Plugin manifest has no pluginClassName")

        val clazz = loader.loadClass(className)
        val instance = clazz.getDeclaredConstructor().newInstance()
        val plugin = instance as? BasePlugin
            ?: error("$className does not extend BasePlugin")

        plugin.filename = file.absolutePath
        // Registers the plugin's MainAPI providers into APIHolder as a side effect.
        plugin.load()
        loaded[file.absolutePath] = plugin
        plugin
    }

    /** Unload [file] and forget every provider it registered. Safe to call for an unloaded file. */
    fun unload(file: File) {
        val plugin = loaded.remove(file.absolutePath) ?: return
        val path = plugin.filename
        runCatching { plugin.beforeUnload() }
        APIHolder.allProviders.filter { it.sourcePlugin == path }
            .forEach { runCatching { APIHolder.removePluginMapping(it) } }
        APIHolder.allProviders.removeAll { it.sourcePlugin == path }
    }

    fun unloadAll() {
        loaded.keys.toList().forEach { unload(File(it)) }
    }

    fun isLoaded(filePath: String): Boolean = loaded.containsKey(filePath)

    /** Every provider currently registered by a loaded plugin. */
    fun providers(): List<MainAPI> = APIHolder.allProviders.toList()

    /** Providers registered by the plugin at [filePath]. */
    fun providersOf(filePath: String): List<MainAPI> =
        providers().filter { it.sourcePlugin == filePath }

    fun providerByName(name: String): MainAPI? = providers().firstOrNull { it.name == name }
}
