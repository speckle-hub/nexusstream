package com.lagradost.cloudstream3.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * The JSON codec used for CloudStream provider settings.
 *
 * Providers are compiled against the CloudStream *app*, so their preference helpers are inlined
 * into their dex — including the *read* path, which uses upstream's Jackson mapper. Values this
 * app writes therefore have to be readable by that mapper:
 *
 *  - upstream's mapper registers the Jackson Kotlin module and ignores unknown properties;
 *  - Gson (the app's other codec) omits null fields, which makes a Jackson/Kotlin read fail with
 *    `MissingKotlinParameterException` — the provider's setting is then silently lost.
 *
 * The library's own mapper is internal API, so an equivalent instance is built here.
 */
object ProviderSettingsCodec {

    /** Jackson mapper configured exactly like the CloudStream library's own. */
    val mapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    /** Serializes [value]; falls back to a `null` literal when the value cannot be written. */
    fun encode(value: Any?): String = runCatching {
        mapper.writeValueAsString(value)
    }.getOrElse { "null" }

    /** Reads a value back; returns null when the payload is unreadable. */
    fun <T> decode(json: String, valueType: Class<T>): T? = runCatching {
        mapper.readValue(json, valueType)
    }.getOrNull()
}
