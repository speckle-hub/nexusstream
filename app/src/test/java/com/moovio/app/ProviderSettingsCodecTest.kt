package com.moovio.app

import com.lagradost.cloudstream3.utils.ProviderSettingsCodec
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The provider-settings codec must stay compatible with the CloudStream library's own mapper,
 * because the *read* path is inlined into every provider's dex and uses Jackson.
 *
 * The concrete failure this guards against: Gson omits null fields, so a value written with Gson
 * and read back with Jackson/Kotlin throws `MissingKotlinParameterException` — and the provider's
 * setting disappears without any error.
 */
class ProviderSettingsCodecTest {

    private data class ProviderSetting(
        val label: String,
        val count: Int,
        val extra: String? = null,
    )

    @Test
    fun `round trips a kotlin data class`() {
        val value = ProviderSetting("Quality", 3, "hd")
        val json = ProviderSettingsCodec.encode(value)
        val back = ProviderSettingsCodec.decode(json, ProviderSetting::class.java)
        assertEquals(value, back)
    }

    @Test
    fun `a null field survives the round trip`() {
        // The Gson-vs-Jackson bug: Gson would drop `extra` entirely, and Jackson/Kotlin would
        // then fail on the missing parameter.
        val value = ProviderSetting("Quality", 1, null)
        val json = ProviderSettingsCodec.encode(value)
        assertTrue("null field must be written explicitly: $json", json.contains("extra"))
        val back = ProviderSettingsCodec.decode(json, ProviderSetting::class.java)
        assertNotNull(back)
        assertEquals(1, back!!.count)
        assertNull(back.extra)
    }

    @Test
    fun `unknown properties are ignored like upstream`() {
        val json = """{"label":"x","count":2,"futureField":true}"""
        val back = ProviderSettingsCodec.decode(json, ProviderSetting::class.java)
        assertNotNull(back)
        assertEquals("x", back!!.label)
        assertEquals(2, back.count)
    }

    @Test
    fun `garbage payloads decode to null instead of throwing`() {
        assertNull(ProviderSettingsCodec.decode("not json at all", ProviderSetting::class.java))
        assertNull(ProviderSettingsCodec.decode("", ProviderSetting::class.java))
    }

    @Test
    fun `gson would have dropped the null field the provider needs`() {
        // Documents *why* the codec is Jackson: Gson serialises the same object without the null
        // field, which is exactly what made the provider's read fail.
        val value = ProviderSetting("Quality", 1, null)
        val gsonJson = Gson().toJson(value)
        assertTrue("Gson drops null fields: $gsonJson", !gsonJson.contains("extra"))
        assertNotNull(ProviderSettingsCodec.decode(ProviderSettingsCodec.encode(value), ProviderSetting::class.java))
    }
}
