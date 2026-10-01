package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.model.CloudStreamExt

/** Parser for CloudStream 3 extension repositories (repo.json). */
object CloudStreamRepo {

    /**
     * Fetch a CloudStream repo. Supports both a bare plugin array and the repo manifest shape
     * `{ name, pluginLists: [ "<url to plugins.json>" ] }` (the phisher98 layout), where the real
     * provider array lives one hop away. Inline `extensions` / `data` / `plugins` arrays also work.
     */
    suspend fun fetch(repoUrl: String): List<CloudStreamExt> {
        val root = runCatching { parseJson(httpGetCached(repoUrl)) }.getOrNull() ?: return emptyList()
        val out = ArrayList<CloudStreamExt>()

        fun addArray(el: com.google.gson.JsonElement?) {
            el?.takeIf { it.isJsonArray }?.asJsonArray?.forEach {
                if (it.isJsonObject) parse(it.asJsonObject)?.let(out::add)
            }
        }

        when {
            root.isJsonArray -> addArray(root)
            root.isJsonObject -> {
                val o = root.asJsonObject
                addArray(o.getAsJsonArray("extensions"))
                addArray(o.getAsJsonArray("data"))
                addArray(o.getAsJsonArray("plugins"))
                // Manifest repos point at plugin-list URLs; fetch each and parse its array.
                o.getAsJsonArray("pluginLists")?.forEach { el ->
                    val url = el.takeIf { it.isJsonPrimitive }?.asString
                    if (!url.isNullOrBlank() && url != repoUrl) addArray(runCatching { parseJson(httpGetCached(url)) }.getOrNull())
                }
            }
        }
        return out.distinctBy { it.url }
    }

    /** Parses a single CloudStream provider entry. Internal so it can be unit-tested. */
    internal fun parse(o: JsonObject): CloudStreamExt? {
        val name = o.get("name")?.asString ?: return null
        val url = o.get("url")?.asString
            ?: o.get("apkUrl")?.asString
            ?: o.get("apk")?.asString
            ?: return null
        return CloudStreamExt(
            name = name,
            url = url,
            description = o.get("description")?.asString,
            icon = o.get("icon")?.asString ?: o.get("iconUrl")?.asString,
            version = o.get("version")?.takeIf { it.isJsonPrimitive }?.asString,
            language = o.get("language")?.asString,
            internalName = o.get("internalName")?.asString,
            status = o.get("status")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1,
            tvTypes = o.getAsJsonArray("tvTypes")?.map { it.asString } ?: emptyList(),
        )
    }
}
