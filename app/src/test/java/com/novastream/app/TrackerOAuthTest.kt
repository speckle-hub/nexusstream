package com.novastream.app

import com.novastream.app.data.integrations.OAuthProvider
import com.novastream.app.data.integrations.authorizeUrl
import com.novastream.app.data.integrations.base64UrlNoPad
import com.novastream.app.data.integrations.parseAccessToken
import com.novastream.app.data.integrations.pkceChallenge
import com.novastream.app.data.integrations.randomVerifier
import com.novastream.app.data.integrations.tokenExchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 30 tracker OAuth builders, PKCE math and token parsing (pure JVM). */
class TrackerOAuthTest {

    @Test
    fun `trakt authorize url carries client id and state`() {
        val url = OAuthProvider.TRAKT.authorizeUrl("my-id", "trakt:abc123")
        assertTrue(url.startsWith("https://trakt.tv/oauth/authorize?"))
        assertTrue(url.contains("client_id=my-id"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("state=trakt%3Aabc123"))
        assertTrue(url.contains("redirect_uri=novastream%3A%2F%2Foauth"))
    }

    @Test
    fun `anilist authorize url carries client id and state`() {
        val url = OAuthProvider.ANILIST.authorizeUrl("42", "anilist:xyz")
        assertTrue(url.startsWith("https://anilist.co/api/v2/oauth/authorize?"))
        assertTrue(url.contains("client_id=42"))
        assertTrue(url.contains("state=anilist%3Axyz"))
    }

    @Test
    fun `mal authorize url carries the pkce challenge`() {
        val url = OAuthProvider.MAL.authorizeUrl("cid", "mal:s1", codeChallenge = "CHALLENGE")
        assertTrue(url.startsWith("https://myanimelist.net/v1/oauth2/authorize?"))
        assertTrue(url.contains("code_challenge=CHALLENGE"))
        assertTrue(url.contains("code_challenge_method=S256"))
    }

    @Test
    fun `pkce challenge matches the rfc 7636 vector`() {
        // RFC 7636 Appendix B: verifier -> challenge.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun `base64url has no padding or unsafe chars`() {
        val encoded = base64UrlNoPad(ByteArray(48) { it.toByte() })
        // 48 bytes -> 64 chars, no padding needed; alphabet is URL-safe.
        assertEquals(64, encoded.length)
        assertTrue(encoded.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `random verifier is 64 url-safe chars`() {
        val verifier = randomVerifier()
        assertEquals(64, verifier.length)
        assertTrue(verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `trakt exchange is a json post with the secret`() {
        val req = tokenExchange(OAuthProvider.TRAKT, "CODE", "id", "secret", null)
        assertEquals("https://api.trakt.tv/oauth/token", req.url)
        assertNull(req.formFields)
        val body = req.jsonBody.orEmpty()
        assertTrue(body.contains("\"code\":\"CODE\""))
        assertTrue(body.contains("\"client_secret\":\"secret\""))
        assertTrue(body.contains("authorization_code"))
    }

    @Test
    fun `mal exchange is a form post with the verifier`() {
        val req = tokenExchange(OAuthProvider.MAL, "CODE", "id", null, "VERIFIER")
        assertEquals("https://myanimelist.net/v1/oauth2/token", req.url)
        assertNull(req.jsonBody)
        assertEquals(
            mapOf(
                "client_id" to "id",
                "code" to "CODE",
                "code_verifier" to "VERIFIER",
                "grant_type" to "authorization_code",
                "redirect_uri" to "novastream://oauth",
            ),
            req.formFields,
        )
    }

    @Test
    fun `access token parses and rejects junk`() {
        assertEquals("abc.def", parseAccessToken("""{"access_token":"abc.def","token_type":"Bearer"}"""))
        assertNull(parseAccessToken("""{"error":"invalid_grant"}"""))
        assertNull(parseAccessToken("not json"))
        assertNull(parseAccessToken("""{"access_token":""}"""))
    }

    @Test
    fun `provider keys round-trip`() {
        assertEquals(OAuthProvider.TRAKT, OAuthProvider.fromKey("trakt"))
        assertEquals(OAuthProvider.ANILIST, OAuthProvider.fromKey("anilist"))
        assertEquals(OAuthProvider.MAL, OAuthProvider.fromKey("mal"))
        assertNull(OAuthProvider.fromKey("kitsu"))
        assertNull(OAuthProvider.fromKey(null))
    }
}
