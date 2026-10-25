package com.moovio.desktop.data.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val MoovioJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

fun parseJsonObject(text: String): JsonObject =
    MoovioJson.parseToJsonElement(text) as? JsonObject
        ?: error("Expected a JSON object")

fun JsonElement?.obj(): JsonObject? = this as? JsonObject
fun JsonElement?.arr(): JsonArray? = this as? JsonArray

fun JsonObject?.str(key: String): String? {
    val element = this?.get(key) ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
}

fun JsonObject?.int(key: String): Int? = str(key)?.let { raw ->
    raw.toIntOrNull() ?: raw.toDoubleOrNull()?.toInt()
}

fun JsonObject?.long(key: String): Long? = str(key)?.toLongOrNull()

fun JsonObject?.double(key: String): Double? = str(key)?.toDoubleOrNull()

fun JsonObject?.bool(key: String): Boolean? {
    val element = this?.get(key) ?: return null
    return (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
}

fun JsonObject?.objectOrNull(key: String): JsonObject? = this?.get(key).obj()

fun JsonObject?.arrayOrNull(key: String): JsonArray? = this?.get(key).arr()

fun JsonArray?.strings(): List<String> =
    this?.mapNotNull { element ->
        (element as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    } ?: emptyList()

fun JsonObject?.stringMap(key: String): Map<String, String> {
    val objectValue = objectOrNull(key) ?: return emptyMap()
    return objectValue.mapNotNull { (k, v) ->
        (v as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { k to it }
    }.toMap()
}

/** Converts AniList / Stremio HTML-ish descriptions to readable plain text. */
fun String?.plainDescription(): String? = this
    ?.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
    ?.replace(Regex("""<[^>]+>"""), "")
    ?.replace("&quot;", "\"")
    ?.replace("&amp;", "&")
    ?.replace("&#39;", "'")
    ?.replace("&lt;", "<")
    ?.replace("&gt;", ">")
    ?.replace(Regex("""\n{3,}"""), "\n\n")
    ?.trim()
    ?.takeIf { it.isNotBlank() }
