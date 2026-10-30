package com.moovio.app

import com.google.gson.Gson
import com.moovio.app.data.integrations.BackupCipher
import com.moovio.app.data.integrations.BackupFormat
import com.moovio.app.data.integrations.BackupPayload
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * ISSUE-15: an exported backup carries tracker tokens and cookies, so it must not be readable
 * without the install's KeyStore key — while a file written by an older, plaintext version still has
 * to be recognised as such (and never silently treated as an encrypted container).
 *
 * The cipher is plain JCE, so these run on the JVM with a generated AES key standing in for the
 * Android KeyStore one; only key custody is platform-bound, not the format or the crypto.
 */
class BackupCryptoTest {

    /** One key for the class: a fresh key per call would make decrypt fail by construction. */
    private val key: SecretKey by lazy { KeyGenerator.getInstance("AES").apply { init(256) }.generateKey() }

    private fun freshKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val plain = """{"version":2,"customCookie":"session=abc123","scrobble":[{"provider":"trakt","token":"t0ken"}]}"""
        .toByteArray()

    @Test
    fun `round trip recovers the exact payload`() {
        val cipher = BackupCipher { key }
        val blob = cipher.encrypt(plain)
        assertArrayEquals(plain, cipher.decrypt(blob))
    }

    @Test
    fun `container is marked and the secrets are not readable in it`() {
        val cipher = BackupCipher { key }
        val blob = cipher.encrypt(plain)

        assertTrue("encrypted blobs must carry the magic header", BackupFormat.isEncrypted(blob))
        assertEquals(BackupFormat.MAGIC.size + BackupFormat.IV_BYTES, blob.size - (plain.size + BackupFormat.TAG_BITS / 8))
        val asText = String(blob, Charsets.ISO_8859_1)
        assertFalse("tracker token leaked as plaintext", asText.contains("t0ken"))
        assertFalse("cookie leaked as plaintext", asText.contains("session=abc123"))
    }

    @Test
    fun `each export uses a fresh nonce so identical payloads differ`() {
        val cipher = BackupCipher { key }
        val a = cipher.encrypt(plain)
        val b = cipher.encrypt(plain)

        assertNotEquals(
            "GCM is broken if the same IV is reused under one key",
            a.copyOfRange(BackupFormat.MAGIC.size, BackupFormat.MAGIC.size + BackupFormat.IV_BYTES)
                .toList(),
            b.copyOfRange(BackupFormat.MAGIC.size, BackupFormat.MAGIC.size + BackupFormat.IV_BYTES).toList(),
        )
        assertArrayEquals(plain, cipher.decrypt(a))
        assertArrayEquals(plain, cipher.decrypt(b))
    }

    @Test
    fun `a flipped byte fails the authentication tag`() {
        val cipher = BackupCipher { key }
        val blob = cipher.encrypt(plain)
        blob[blob.size - 1] = (blob[blob.size - 1] + 1).toByte()

        try {
            cipher.decrypt(blob)
            fail("tampered ciphertext must not decrypt")
        } catch (expected: Exception) {
            // AEADBadTagException / IllegalBlockSizeException — either way, rejected.
        }
    }

    @Test
    fun `a different key cannot decrypt the file`() {
        val blob = BackupCipher { key }.encrypt(plain)
        try {
            BackupCipher { freshKey() }.decrypt(blob)
            fail("another install's key must not open this backup")
        } catch (expected: Exception) {
        }
    }

    @Test
    fun `legacy plaintext json is not mistaken for an encrypted container`() {
        val legacy = """{"version":1,"customCookie":"session=abc123"}""".toByteArray()
        assertFalse(BackupFormat.isEncrypted(legacy))
        assertFalse(BackupFormat.isEncrypted(ByteArray(0)))
        assertFalse("a truncated container must not be treated as valid", BackupFormat.isEncrypted(ByteArray(20)))
    }

    @Test
    fun `legacy plaintext still parses into a payload with its secrets`() {
        val legacy = """
            {"version":1,"exportedAt":1700000000000,"customCookie":"session=abc123",
             "customUserAgent":"UA/1.0","maxParallel":5,
             "scrobble":[{"provider":"trakt","token":"t0ken","enabled":true}]}
        """.trimIndent()

        val payload = Gson().fromJson(legacy, BackupPayload::class.java)

        assertEquals(1, payload.version)
        assertEquals("session=abc123", payload.customCookie)
        assertEquals("UA/1.0", payload.customUserAgent)
        assertEquals(5, payload.maxParallel)
        assertEquals("t0ken", payload.scrobble.single().token)
        assertTrue("the legacy file must be flagged as needing confirmation", payload.version < 2)
    }

    @Test
    fun `an encrypted payload keeps every field through the container`() {
        val payload = BackupPayload(
            customCookie = "session=abc123",
            maxParallel = 7,
            appLock = true,
        )
        val cipher = BackupCipher { key }
        val blob = cipher.encrypt(Gson().toJson(payload).toByteArray())
        val restored = Gson().fromJson(cipher.decrypt(blob).decodeToString(), BackupPayload::class.java)

        assertEquals(2, restored.version)
        assertEquals("session=abc123", restored.customCookie)
        assertEquals(7, restored.maxParallel)
        assertTrue(restored.appLock)
    }
}
