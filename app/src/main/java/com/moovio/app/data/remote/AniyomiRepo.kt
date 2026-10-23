package com.moovio.app.data.remote

import com.google.gson.JsonObject
import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.model.MangaSource

/** Parser for Aniyomi extension repositories (index.min.json). */
object AniyomiRepo {

    suspend fun fetch(repoUrl: String): List<MangaExt> {
        val root = parseJson(httpGetCached(repoUrl))
        val arr = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.getAsJsonArray("extensions")
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull { if (it.isJsonObject) parse(it.asJsonObject, repoUrl) else null }
    }

    /**
     * Resolve an index `apk` value to a download URL.
     *
     * Aniyomi/Mihon indices carry the APK as a **file name**: `aniyomi-all.x-v14.4.apk`. The files
     * live next to the index in `<repo dir>/apk/`, e.g.
     * `https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json` →
     * `…/anime-repo/repo/apk/aniyomi-all.x-v14.4.apk`. Using the raw value as a URL (what happened
     * before) 404s on every download, and resolving it against the index directory also 404s —
     * both were verified live on 2026-10-23.
     */
    internal fun resolveApk(repoUrl: String, apk: String): String {
        val value = apk.trim()
        if (value.isBlank()) return ""
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val dir = repoUrl.substringBeforeLast('/', missingDelimiterValue = "")
            .ifBlank { return value }
        return "$dir/apk/$value"
    }

    private fun parse(o: JsonObject, repoUrl: String): MangaExt? {
        val name = o.get("name")?.asString ?: return null
        val pkg = o.get("pkg")?.asString ?: o.get("packageName")?.asString ?: return null
        val apk = o.get("apk")?.asString ?: o.get("url")?.asString ?: return null
        val nsfwEl = o.get("nsfw")
        val nsfw = when {
            nsfwEl == null -> false
            nsfwEl.isJsonPrimitive && nsfwEl.asJsonPrimitive.isBoolean -> nsfwEl.asBoolean
            nsfwEl.isJsonPrimitive -> nsfwEl.asInt == 1
            else -> false
        }
        val sources = mutableListOf<MangaSource>()
        o.getAsJsonArray("sources")?.forEach { s ->
            if (!s.isJsonObject) return@forEach
            val so = s.asJsonObject
            sources.add(
                MangaSource(
                    id = so.get("id")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L,
                    name = so.get("name")?.asString ?: "",
                    lang = so.get("lang")?.asString,
                    baseUrl = so.get("baseUrl")?.asString,
                )
            )
        }
        return MangaExt(
            name = name,
            pkg = pkg,
            apk = resolveApk(repoUrl, apk),
            icon = o.get("icon")?.asString,
            version = o.get("version")?.takeIf { it.isJsonPrimitive }?.asString,
            lang = o.get("lang")?.asString,
            nsfw = nsfw,
            sources = sources,
            versionCode = o.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong,
            repoUrl = repoUrl,
        )
    }
}
