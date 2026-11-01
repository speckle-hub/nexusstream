package com.moovio.app

import com.moovio.app.data.remote.MangaDexClient
import com.moovio.app.data.remote.UA
import com.moovio.app.data.remote.isBrowserLikeUserAgent
import com.moovio.app.data.remote.looksLikeMarkup
import com.moovio.app.data.remote.mangaDexUserAgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "No manga found. Check your connection" bug (v1.11.4).
 *
 * MangaDex fronts `api.mangadex.org` and `uploads.mangadex.org` with a POWBlock proof-of-work
 * check that answers a **browser-shaped User-Agent** with `HTTP 200` and an HTML page. The app's
 * default UA is browser-shaped, so every MangaDex call returned HTML, every row fetcher threw while
 * parsing it, and the Manga tab came back empty — on a perfectly good connection.
 */
class MangaDexIdentityTest {

    // ---- isBrowserLikeUserAgent ------------------------------------------------

    @Test
    fun `the app's default user agent counts as a browser impersonation`() {
        // This is the UA that broke MangaDex: it was sent on every request by Http.globalIdentity().
        assertTrue(isBrowserLikeUserAgent(UA))
        assertTrue(isBrowserLikeUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 Moovio/1.0"))
        assertTrue(isBrowserLikeUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/121.0"))
        assertTrue(isBrowserLikeUserAgent("mozilla/5.0 lowercase still counts"))
    }

    @Test
    fun `descriptive client user agents are not browsers`() {
        assertFalse(isBrowserLikeUserAgent(MangaDexClient.USER_AGENT))
        assertFalse(isBrowserLikeUserAgent("okhttp/4.12.0"))
        assertFalse(isBrowserLikeUserAgent("curl/8.4.0"))
        assertFalse(isBrowserLikeUserAgent("MangaDex-Client/1.0"))
        assertFalse(isBrowserLikeUserAgent(""))
    }

    // ---- mangaDexUserAgent ----------------------------------------------------

    @Test
    fun `a browser user agent is replaced for mangadex`() {
        val replaced = mangaDexUserAgent("Mozilla/5.0 (Linux; Android 14) Chrome/120.0 Mobile Safari/537.36")
        assertEquals(MangaDexClient.USER_AGENT, replaced)
        assertFalse(isBrowserLikeUserAgent(replaced))
    }

    @Test
    fun `a blank user agent falls back to the mangadex identity`() {
        assertEquals(MangaDexClient.USER_AGENT, mangaDexUserAgent(""))
        assertEquals(MangaDexClient.USER_AGENT, mangaDexUserAgent("   "))
    }

    @Test
    fun `a custom non browser user agent is respected`() {
        // Settings → Network can still spoof a client identity; only browser impersonation is
        // overridden, because that is what MangaDex's bot check reacts to.
        assertEquals("MyCustomClient/2.0", mangaDexUserAgent("MyCustomClient/2.0"))
    }

    @Test
    fun `the mangadex identity never contains a mozilla token`() {
        assertFalse(MangaDexClient.USER_AGENT.contains("ozilla", ignoreCase = true))
        assertTrue(MangaDexClient.USER_AGENT.startsWith("Moovio/"))
    }

    // ---- looksLikeMarkup ------------------------------------------------------

    @Test
    fun `the mangadex challenge page is recognised as markup`() {
        val challenge = """
            <!doctype html>
            <html lang="en">
            <head><title>MangaDex - Browser verification...</title></head>
            <body><pre id=c>r53F5hk1Ljdz9KjUJ2fSl5S1PA8EtQoK</pre></body>
            </html>
        """.trimIndent()
        assertTrue(looksLikeMarkup(challenge))
        assertTrue(looksLikeMarkup("<!doctype html><html></html>"))
        assertTrue(looksLikeMarkup("  \n\t<html><body>hi</body></html>"))
        assertTrue(looksLikeMarkup("<pre id=c>payload</pre>"))
    }

    @Test
    fun `a real api payload is not markup`() {
        assertFalse(looksLikeMarkup("""{"result":"ok","response":"collection","data":[]}"""))
        assertFalse(looksLikeMarkup("""  {"data":[{"id":"x"}]}"""))
        assertFalse(looksLikeMarkup("[]"))
        assertFalse(looksLikeMarkup(""))
    }
}
