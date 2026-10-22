package com.moovio.app

import com.moovio.app.data.integrations.certificateFingerprint
import com.moovio.app.data.integrations.constantTimeEquals
import com.moovio.app.data.integrations.isTrustedSignature
import com.moovio.app.data.integrations.oauthState
import com.moovio.app.data.integrations.parseOAuthState
import com.moovio.app.data.integrations.randomNonce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OAuth redirect hardening and APK signature verification (pure JVM).
 *
 * The `moovio://oauth` redirect uses a custom scheme, so any app on the device can fire it. Without
 * a nonce the callback would happily exchange whatever authorization code it was handed and write
 * the resulting token into the user's tracker list.
 */
class OAuthAndSignatureSecurityTest {

    @Test
    fun `nonce is random and long enough`() {
        val a = randomNonce()
        val b = randomNonce()
        assertNotEquals(a, b)
        assertEquals(32, a.length)
        assertTrue(a.all { it in "0123456789abcdef" })
    }

    @Test
    fun `state round trips through the parser`() {
        val nonce = randomNonce()
        val state = oauthState(com.moovio.app.data.integrations.OAuthProvider.TRAKT, nonce)
        assertEquals("trakt:$nonce", state)
        val parsed = parseOAuthState(state)
        assertEquals(com.moovio.app.data.integrations.OAuthProvider.TRAKT, parsed?.provider)
        assertEquals(nonce, parsed?.nonce)
    }

    @Test
    fun `malformed states are rejected`() {
        assertNull(parseOAuthState(null))
        assertNull(parseOAuthState(""))
        assertNull(parseOAuthState("   "))
        assertNull(parseOAuthState("trakt"))
        assertNull(parseOAuthState("trakt:"))
        assertNull(parseOAuthState("unknown-provider:abc"))
        assertNull(parseOAuthState(":abc"))
    }

    @Test
    fun `constant time comparison matches only the same value`() {
        assertTrue(constantTimeEquals("abc123", "abc123"))
        assertFalse(constantTimeEquals("abc123", "abc124"))
        assertFalse(constantTimeEquals("abc123", "abc1234"))
        assertFalse(constantTimeEquals("abc123", null))
        assertTrue(constantTimeEquals(null, null))
    }

    @Test
    fun `certificate fingerprint is stable and differs per certificate`() {
        val cert = ByteArray(64) { it.toByte() }
        val other = ByteArray(64) { (it + 1).toByte() }
        val fp = certificateFingerprint(cert)
        assertEquals(fp, certificateFingerprint(cert))
        assertNotEquals(fp, certificateFingerprint(other))
        assertEquals(64, fp.length)
    }

    @Test
    fun `signature trust requires every candidate certificate to be trusted`() {
        val trusted = setOf("aa", "bb")
        assertTrue(isTrustedSignature(setOf("aa"), trusted))
        assertTrue(isTrustedSignature(setOf("aa", "bb"), trusted))
        assertFalse(isTrustedSignature(setOf("aa", "cc"), trusted))
        assertFalse(isTrustedSignature(setOf("cc"), trusted))
        // An unsigned archive is never trusted.
        assertFalse(isTrustedSignature(emptySet(), trusted))
    }
}
