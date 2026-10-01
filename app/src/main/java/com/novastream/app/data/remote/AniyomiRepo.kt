package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.model.MangaExt
import com.novastream.app.data.model.MangaSource

/** Parser for Aniyomi extension repositories (index.min.json). */
object AniyomiRepo {

    suspend fun fetch(repoUrl: String): List<MangaExt> {
        val root = parseJson(httpGet(repoUrl))
        val arr = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.getAsJsonArray("extensions")
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull { if (it.isJsonObject) parse(it.asJsonObject) else null }
    }

    private fun parse(o: JsonObject): MangaExt? {
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
            apk = apk,
            icon = o.get("icon")?.asString,
            version = o.get("version")?.takeIf { it.isJsonPrimitive }?.asString,
            lang = o.get("lang")?.asString,
            nsfw = nsfw,
            sources = sources,
        )
    }
}
