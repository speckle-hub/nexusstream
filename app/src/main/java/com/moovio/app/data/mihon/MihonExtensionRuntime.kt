package com.moovio.app.data.mihon

import android.content.Context
import dalvik.system.DexFile
import dalvik.system.PathClassLoader
import java.io.File
import java.lang.reflect.Modifier

/**
 * Loads Aniyomi / Mihon extension APKs **in-process** using a [PathClassLoader] whose parent is the
 * app classloader.
 *
 * Unlike CloudStream (which ships its `library` artifact so providers resolve `com.lagradost.*`
 * superclasses at compile time), tachiyomi extensions implement `eu.kanade.tachiyomi.*` source APIs
 * that this app cannot reference at compile time (the `extensions-lib` artifact requires
 * AGP ≥ 8.9 / compileSdk 37, see roadmap §2ba). Instead of a compile-time dependency, this loader
 * uses **pure reflection**:
 *
 * 1. Scans the APK's DEX for classes whose superclass chain contains a tachiyomi source base class
 *    (`Source`, `SingletonSource`, `OnlineSource`, `AnimeSource`, `MangaSource`, …).
 * 2. Instantiates each matching class via its no-arg constructor.
 * 3. Calls source methods (`getLatestManga`, `searchManga`, `getChapterList`, `getPageList`, …)
 *    reflectively, and reads data-object fields (`Manga.url`, `Chapter.url`, `Page.page_url`, …)
 *    reflectively.
 *
 * Every call is wrapped in [runCatching], so a broken or incompatible extension can never crash
 * the app — it is simply absent from search, detail and streams.
 */
object MihonExtensionRuntime {

    /** ID prefix for extension sources, mirroring CloudStream's `cloudstream:`. */
    const val ADDON_PREFIX = "mihon:"

    /**
     * True when the tachiyomi source APIs (`eu.kanade.tachiyomi.source.Source`) are on this build's
     * classpath. They are deliberately absent until the toolchain migration in `build.gradle.kts`
     * is done, so every installed extension APK can be scanned but no source can be instantiated.
     * The Add-on Manager reads this to explain the state instead of promising "sources live".
     */
    val canExecuteSources: Boolean = runCatching {
        Class.forName("eu.kanade.tachiyomi.source.Source")
    }.isSuccess

    /** Simple names of tachiyomi source base classes we recognise by walking the superclass chain. */
    private val SOURCE_BASE_SIMPLE_NAMES = setOf(
        "Source", "SingletonSource", "OnlineSource",
        "AnimeSource", "MangaSource",
    )

    /** APK path → loaded sources + their classloader. */
    private val loaded = LinkedHashMap<String, LoadedApk>()

    /** `mihon:<source-name>` for a loaded source instance. */
    fun addonId(source: Any): String {
        val name = reflectString(source, "name")
            ?: reflectInvoke(source, "getName") as? String
            ?: source.javaClass.simpleName
        return "$ADDON_PREFIX$name"
    }

    fun sourceNameOf(addonId: String?): String? =
        addonId?.takeIf { it.startsWith(ADDON_PREFIX) }?.removePrefix(ADDON_PREFIX)

    /**
     * Load an extension APK. Scans its DEX for tachiyomi source classes, instantiates them and
     * returns the list of source instances. Failures are contained per APK.
     */
    fun load(context: Context, file: File): Result<List<Any>> = runCatching {
        require(file.exists()) { "Extension APK not found: ${file.absolutePath}" }
        runCatching { file.setReadOnly() }

        val loader = PathClassLoader(file.absolutePath, context.classLoader)
        val sources = findAndInstantiateSources(loader, file)
        require(sources.isNotEmpty()) { "No tachiyomi source class found in ${file.name}" }

        loaded[file.absolutePath] = LoadedApk(loader, sources)
        sources
    }

    /** Unload every source from [filePath] and forget it. Safe to call for an unloaded path. */
    fun unload(filePath: String) {
        loaded.remove(filePath)
    }

    fun unloadAll() {
        loaded.clear()
    }

    fun isLoaded(filePath: String): Boolean = loaded.containsKey(filePath)

    /** Every loaded extension source instance, across all APKs. */
    fun sources(): List<Any> = loaded.values.flatMap { it.sources }.toList()

    /** Sources loaded from the APK at [filePath]. */
    fun sourcesOf(filePath: String): List<Any> = loaded[filePath]?.sources ?: emptyList()

    // ---- DEX scanning -----------------------------------------------------------

    private fun findAndInstantiateSources(loader: PathClassLoader, file: File): List<Any> {
        val sources = mutableListOf<Any>()
        val dexFile = DexFile(file.absolutePath)
        try {
            val entries = dexFile.entries()
            while (entries.hasMoreElements()) {
                val rawName = entries.nextElement()
                // Skip inner / anonymous / synthetic classes (`$` in the name).
                if (rawName.contains('$')) continue
                val className = rawName.replace('/', '.')
                try {
                    val clazz = loader.loadClass(className)
                    if (!isTachiyomiSource(clazz)) continue
                    if (Modifier.isAbstract(clazz.modifiers) || Modifier.isInterface(clazz.modifiers)) continue
                    val ctor = clazz.getDeclaredConstructor()
                    ctor.isAccessible = true
                    val instance = ctor.newInstance()
                    sources += instance
                } catch (e: Exception) {
                    // Skip classes that can't be loaded or instantiated.
                }
            }
        } finally {
            runCatching { dexFile.close() }
        }
        return sources
    }

    /** True when [clazz] (or any superclass) has a simple name matching a tachiyomi source base. */
    private fun isTachiyomiSource(clazz: Class<*>): Boolean {
        var current: Class<*>? = clazz
        while (current != null) {
            val simpleName = current.simpleName ?: return false
            if (simpleName in SOURCE_BASE_SIMPLE_NAMES) return true
            current = current.superclass
        }
        return false
    }

    // ---- Reflection helpers ------------------------------------------------------

    /** Read a public `String` field from [obj]. */
    internal fun reflectString(obj: Any, fieldName: String): String? =
        runCatching {
            val field = obj.javaClass.getField(fieldName)
            field.get(obj) as? String
        }.getOrNull()

    /** Invoke a public no-arg method on [obj], returning its result. */
    internal fun reflectInvoke(obj: Any, methodName: String): Any? =
        runCatching {
            val method = obj.javaClass.getMethod(methodName)
            method.isAccessible = true
            method.invoke(obj)
        }.getOrNull()

    /** Invoke `methodName(int)` on [obj], returning its result. */
    internal fun reflectInvokeInt(obj: Any, methodName: String, value: Int): Any? =
        runCatching {
            val method = obj.javaClass.getMethod(methodName, Int::class.java)
            method.isAccessible = true
            method.invoke(obj, value)
        }.getOrNull()

    /** Invoke `methodName(String)` on [obj], returning its result. */
    internal fun reflectInvokeString(obj: Any, methodName: String, value: String): Any? =
        runCatching {
            val method = obj.javaClass.getMethod(methodName, String::class.java)
            method.isAccessible = true
            method.invoke(obj, value)
        }.getOrNull()

    /** Invoke `methodName(String, int)` on [obj], returning its result. */
    internal fun reflectInvokeStringInt(obj: Any, methodName: String, value: String, page: Int): Any? =
        runCatching {
            val method = obj.javaClass.getMethod(methodName, String::class.java, Int::class.java)
            method.isAccessible = true
            method.invoke(obj, value, page)
        }.getOrNull()

    /** Invoke `methodName(Any?)` on [obj], returning its result. */
    internal fun reflectInvokeAny(obj: Any, methodName: String, arg: Any?): Any? =
        runCatching {
            val method = obj.javaClass.getMethod(methodName, Any::class.java)
            method.isAccessible = true
            method.invoke(obj, arg)
        }.getOrNull()

    /** Read a public field of any type from [obj]. */
    internal fun reflectGetField(obj: Any, fieldName: String): Any? =
        runCatching {
            val field = obj.javaClass.getField(fieldName)
            field.get(obj)
        }.getOrNull()
}

private data class LoadedApk(
    val loader: PathClassLoader,
    val sources: List<Any>,
)
