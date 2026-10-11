package com.novastream.app.data.integrations

import com.google.gson.JsonParser
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Tracker OAuth plumbing (Phase 30).
 *
 * The app ships no client secrets, so each provider is authorized with credentials the user
 * pastes from their own app registration (Settings → Scrobbling → OAuth credentials): the
 * Settings screen opens the provider's authorize page in the browser, the `novastream://oauth`
 * redirect lands in the callback activity, and the code is exchanged for a token that is stored
 * like a pasted one. Pasted tokens keep working as a fallback for users who skip OAuth entirely.
 *
 * Everything in this file is pure JVM (no Android APIs) so the URL builders, PKCE math and
 * response parsing are unit-tested (`TrackerOAuthTest`).
 */
enum class OAuthProvider(val key: String) {
    TRAKT("trakt"),
    ANILIST("anilist"),
    MAL("mal");

    companion object {
        fun fromKey(key: String?): OAuthProvider? = entries.firstOrNull { it.key == key }
    }
}

/** Deep-link redirect registered in the manifest; must match the provider app registration. */
const val OAUTH_REDIRECT_URI = "novastream://oauth"

/**
 * Browser authorize URL for this provider. [state] must encode the provider (`"trakt:…"`) so the
 * callback knows which token endpoint to hit. [codeChallenge] is MyAnimeList-only (PKCE S256 —
 * see [pkceChallenge]).
 */
fun OAuthProvider.authorizeUrl(clientId: String, state: String, codeChallenge: String? = null): String {
    val redirect = urlEncode(OAUTH_REDIRECT_URI)
    val st = urlEncode(state)
    return when (this) {
        OAuthProvider.TRAKT ->
            "https://trakt.tv/oauth/authorize?response_type=code" +
                "&client_id=${urlEncode(clientId)}&redirect_uri=$redirect&state=$st"
        OAuthProvider.ANILIST ->
            "https://anilist.co/api/v2/oauth/authorize?client_id=${urlEncode(clientId)}" +
                "&response_type=code&redirect_uri=$redirect&state=$st"
        OAuthProvider.MAL -> {
            val challenge = urlEncode(requireNotNull(codeChallenge) { "MAL needs a PKCE challenge" })
            "https://myanimelist.net/v1/oauth2/authorize?response_type=code" +
                "&client_id=${urlEncode(clientId)}&code_challenge=$challenge" +
                "&code_challenge_method=S256&state=$st&redirect_uri=$redirect"
        }
    }
}

/** Where an authorization [code] is exchanged, and whether the endpoint wants JSON or a form. */
data class TokenExchange(
    val url: String,
    /** JSON body, or null when [formFields] applies. */
    val jsonBody: String?,
    /** Form fields, or null when [jsonBody] applies. */
    val formFields: Map<String, String>?,
)

/**
 * Token-exchange request for an authorization [code]. MyAnimeList takes a form (with the PKCE
 * [codeVerifier]); Trakt and AniList take JSON (with the [clientSecret]).
 */
fun tokenExchange(
    provider: OAuthProvider,
    code: String,
    clientId: String,
    clientSecret: String?,
    codeVerifier: String?,
): TokenExchange = when (provider) {
    OAuthProvider.TRAKT -> TokenExchange(
        url = "https://api.trakt.tv/oauth/token",
        jsonBody = """{"code":"$code","client_id":"$clientId","client_secret":"${clientSecret.orEmpty()}",""" +
            """"redirect_uri":"$OAUTH_REDIRECT_URI","grant_type":"authorization_code"}""",
        formFields = null,
    )
    OAuthProvider.ANILIST -> TokenExchange(
        url = "https://anilist.co/api/v2/oauth/token",
        jsonBody = """{"grant_type":"authorization_code","client_id":"$clientId","client_secret":"${clientSecret.orEmpty()}",""" +
            """"redirect_uri":"$OAUTH_REDIRECT_URI","code":"$code"}""",
        formFields = null,
    )
    OAuthProvider.MAL -> TokenExchange(
        url = "https://myanimelist.net/v1/oauth2/token",
        jsonBody = null,
        formFields = mapOf(
            "client_id" to clientId,
            "code" to code,
            "code_verifier" to requireNotNull(codeVerifier) { "MAL needs the PKCE verifier" },
            "grant_type" to "authorization_code",
            "redirect_uri" to OAUTH_REDIRECT_URI,
        ),
    )
}

/** Extract the bearer token from a token-endpoint response, or null when absent/unparseable. */
fun parseAccessToken(responseJson: String): String? = runCatching {
    JsonParser.parseString(responseJson).asJsonObject
        .get("access_token")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
}.getOrNull()

/** PKCE S256 challenge for a [codeVerifier] (RFC 7636 §4.2). */
fun pkceChallenge(codeVerifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
    return base64UrlNoPad(digest)
}

/** Random PKCE verifier: 64 URL-safe characters (RFC 7636 §4.1 allows 43–128). */
fun randomVerifier(): String {
    val bytes = ByteArray(48)
    SecureRandom().nextBytes(bytes)
    return base64UrlNoPad(bytes)
}

/** Base64url without padding, hand-rolled so it stays pure-JVM (no Android Base64). */
fun base64UrlNoPad(bytes: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val out = StringBuilder()
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
        out.append(alphabet[(b0 shr 2) and 0x3F])
        out.append(alphabet[((b0 and 0x03) shl 4) or ((b1 shr 4) and 0x0F)])
        if (i + 1 < bytes.size) out.append(alphabet[((b1 and 0x0F) shl 2) or ((b2 shr 6) and 0x03)])
        if (i + 2 < bytes.size) out.append(alphabet[b2 and 0x3F])
        i += 3
    }
    return out.toString()
}

private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
