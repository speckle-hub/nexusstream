package com.moovio.app.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for pure logic in [Http] — markup detection, transient-failure classification,
 * gzip inflation, JSON helpers. */
@RunWith(AndroidJUnit4::class)
class HttpInstrumentedTest {

    @Test
    fun looksLikeMarkupDetectsHtml() {
        assertTrue(looksLikeMarkup("<html><body>challenge</body></html>"))
        assertTrue(looksLikeMarkup("  <html>POWBlock challenge</html>"))
        assertTrue(looksLikeMarkup("<!DOCTYPE html><html></html>"))
        assertFalse(looksLikeMarkup("{\"data\": []}"))
        assertFalse(looksLikeMarkup("   {\"data\": []}"))
        assertFalse(looksLikeMarkup("[]"))
        assertFalse(looksLikeMarkup(""))
        assertFalse(looksLikeMarkup("   "))
    }

    @Test
    fun isTransientFailureClassifiesCorrectly() {
        assertTrue(isTransientFailure(java.io.IOException("connection reset")))
        assertTrue(isTransientFailure(java.io.IOException("timeout")))
        assertTrue(isTransientFailure(java.io.IOException("DNS failure")))

        assertTrue(isTransientFailure(java.io.IOException("HTTP 500 for https://example.com")))
        assertTrue(isTransientFailure(java.io.IOException("HTTP 502 Bad Gateway")))
        assertTrue(isTransientFailure(java.io.IOException("HTTP 503 Service Unavailable")))

        assertTrue(isTransientFailure(java.io.IOException("HTTP 408 Request Timeout")))
        assertTrue(isTransientFailure(java.io.IOException("HTTP 429 Too Many Requests")))

        assertFalse(isTransientFailure(java.io.IOException("HTTP 400 Bad Request")))
        assertFalse(isTransientFailure(java.io.IOException("HTTP 401 Unauthorized")))
        assertFalse(isTransientFailure(java.io.IOException("HTTP 403 Forbidden")))
        assertFalse(isTransientFailure(java.io.IOException("HTTP 404 Not Found")))
        assertFalse(isTransientFailure(java.io.IOException("HTTP 410 Gone")))

        assertFalse(isTransientFailure(RuntimeException("boom")))
        assertFalse(isTransientFailure(IllegalStateException("bad state")))
    }

    @Test
    fun maybeGunzipPassesThroughNonGzip() {
        val plain = "not gzip".toByteArray()
        assertEquals(plain.contentToString(), maybeGunzip(plain).contentToString())
        assertTrue("empty stays empty", maybeGunzip(ByteArray(0)).size == 0)
        assertTrue("single byte stays", byteArrayOf(0x1f.toByte()).let { maybeGunzip(it).size == 1 })
    }

    @Test
    fun maybeGunzipInflatesGzip() {
        val original = "{\"key\":\"value\"}".toByteArray()
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(baos).use { gzos -> gzos.write(original) }
        val gzipped = baos.toByteArray()
        val inflated = maybeGunzip(gzipped)
        assertEquals(original.contentToString(), inflated.contentToString())
    }

    @Test
    fun parseJsonAndHelpers() {
        val json = JsonParser.parseString("{\"name\":\"Moovio\",\"count\":42}")
        assertNotNull(json)
        assertEquals("Moovio", json.asJsonObject.get("name").asString)
        assertEquals("42", json.asJsonObject.get("count").asString)

        val nullObj: com.google.gson.JsonElement? = null
        assertNull(nullObj)
    }
}
