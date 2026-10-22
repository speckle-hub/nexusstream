package com.lagradost.cloudstream3.utils

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.json.JsonMapper
import com.moovio.app.data.remote.Http

/**
 * Minimal re-implementation of CloudStream's app-module `DataStore`.
 *
 * CloudStream providers are compiled against the CloudStream *app*, so many of them persist their
 * own settings through `CloudStreamApp.getKey(...)` / `DataStore.getSharedPrefs()`. Those inline
 * functions are baked into the provider's dex at compile time, which means the class
 * `com.lagradost.cloudstream3.utils.DataStore` must exist at runtime for the provider to link.
 *
 * This shim mirrors the upstream public surface (the parts providers actually call) and backs it
 * with a SharedPreferences file, so settings a provider writes survive across launches.
 *
 * **Codec:** values are serialized with a Jackson mapper configured exactly like the CloudStream
 * library's own (Kotlin module registered, unknown properties ignored). That matters: the
 * provider's dex has the *upstream* read path inlined, and upstream reads with Jackson, so a value
 * written with a different codec (the app's Gson) can be unreadable to the provider — Gson omits
 * null fields, which makes a Jackson/Kotlin read fail with `MissingKotlinParameterException` and
 * silently drop the provider's setting. Gson stays as a fallback for values Jackson cannot handle.
 */
object DataStore {

    const val PREFERENCES_NAME = "cloudstream_provider_settings"

    private fun getPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** CloudStream's per-provider preferences file. */
    fun Context.getSharedPrefs(): SharedPreferences = getPreferences(this)

    fun Context.getDefaultSharedPrefs(): SharedPreferences = getSharedPrefs()

    /** `"folder/path"` — the composite key providers use. */
    fun getFolderName(folder: String, path: String): String = "$folder/$path"

    fun Context.getKeys(folder: String): List<String> {
        val fixed = folder.trimEnd('/') + "/"
        return this.getSharedPrefs().all.keys.filter { it.startsWith(fixed) }
    }

    fun Context.removeKey(folder: String, path: String) {
        removeKey(getFolderName(folder, path))
    }

    fun Context.removeKey(path: String) {
        runCatching {
            val prefs = getSharedPrefs()
            if (prefs.contains(path)) prefs.edit().remove(path).apply()
        }
    }

    fun Context.removeKeys(folder: String): Int {
        val keys = getKeys(folder)
        return runCatching {
            val editor = getSharedPrefs().edit()
            keys.forEach { editor.remove(it) }
            editor.apply()
            keys.size
        }.getOrDefault(0)
    }

    fun Context.containsKey(path: String): Boolean = getSharedPrefs().contains(path)

    fun Context.containsKey(folder: String, path: String): Boolean = containsKey(getFolderName(folder, path))

    /**
     * The codec used for provider settings.
     *
     * It mirrors the CloudStream library's own mapper exactly: a [JsonMapper] with the Kotlin
     * module registered and `FAIL_ON_UNKNOWN_PROPERTIES` disabled — the same configuration the
     * upstream library builds in `MainAPI.getMapper()`. The library's mapper is not public API, so
     * the equivalent instance is built here instead of reaching into it.
     */
    private val mapper: JsonMapper = ProviderSettingsCodec.mapper

    /** Serializes with the upstream-compatible Jackson mapper, falling back to Gson. */
    @PublishedApi
    internal fun encode(value: Any?): String = runCatching {
        mapper.writeValueAsString(value)
    }.getOrElse {
        runCatching { Http.gson.toJson(value) }.getOrElse { "null" }
    }

    /** Reads back with the upstream-compatible Jackson mapper, falling back to Gson. */
    @PublishedApi
    internal fun <T> decode(json: String, valueType: Class<T>): T? = runCatching {
        mapper.readValue(json, valueType)
    }.getOrElse {
        runCatching { Http.gson.fromJson(json, valueType) }.getOrNull()
    }

    fun <T> Context.setKey(path: String, value: T) {
        runCatching {
            getSharedPrefs().edit().putString(path, encode(value)).apply()
        }
    }

    fun <T> Context.setKey(folder: String, path: String, value: T) = setKey(getFolderName(folder, path), value)

    fun <T : Any> Context.getKey(path: String, valueType: Class<T>): T? = runCatching {
        val json: String = getSharedPrefs().getString(path, null) ?: return null
        decode(json, valueType)
    }.getOrNull()

    inline fun <reified T : Any> Context.getKey(path: String, defVal: T?): T? = try {
        val json: String = getSharedPrefs().getString(path, null) ?: return defVal
        decode(json, T::class.java)
    } catch (_: Exception) {
        null
    }

    inline fun <reified T : Any> Context.getKey(path: String): T? = getKey(path, null)

    inline fun <reified T : Any> Context.getKey(folder: String, path: String): T? =
        getKey(getFolderName(folder, path), null)

    inline fun <reified T : Any> Context.getKey(folder: String, path: String, defVal: T?): T? =
        getKey(getFolderName(folder, path), defVal) ?: defVal
}
