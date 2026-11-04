package com.moovio.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moovio.app.data.local.ScrobbleConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [SettingsStore] — DataStore read/write needs a real Android Context. */
@RunWith(AndroidJUnit4::class)
class SettingsStoreInstrumentedTest {

    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store = SettingsStore(context)
    }

    @Test
    fun themeReadWriteRoundTrips() = runBlocking {
        store.setTheme("light")
        assertEquals("light", store.theme.value)
        store.setTheme("dark")
        assertEquals("dark", store.theme.value)
    }

    @Test
    fun accentReadWriteRoundTrips() = runBlocking {
        store.setAccent("emerald")
        assertEquals("emerald", store.accent.value)
    }

    @Test
    fun nsfwLockReadWriteRoundTrips() = runBlocking {
        store.setNsfwLock(true)
        assertTrue(store.nsfwLock.value)
        store.setNsfwLock(false)
        assertFalse(store.nsfwLock.value)
    }

    @Test
    fun nsfwPinReadWriteRoundTrips() = runBlocking {
        store.setNsfwPin("1234")
        assertEquals("1234", store.nsfwPin.value)
        store.setNsfwPin(null)
        assertNull(store.nsfwPin.value)
    }

    @Test
    fun homeOrderReadWriteRoundTrips() = runBlocking {
        store.setHomeOrder(listOf("Home", "Search", "Browse"))
        assertEquals(listOf("Home", "Search", "Browse"), store.homeOrder.value)
        store.setHomeOrder(emptyList<String>())
        assertTrue(store.homeOrder.value.isEmpty())
    }

    @Test
    fun disabledSourcesReadWriteRoundTrips() = runBlocking {
        store.setDisabledSources(listOf("spankbang", "youporn"))
        assertEquals(listOf("spankbang", "youporn"), store.disabledSources.value)
        store.setDisabledSources(emptyList<String>())
        assertTrue(store.disabledSources.value.isEmpty())
    }

    @Test
    fun customIdentityReadWriteRoundTrips() = runBlocking {
        store.setCustomUserAgent("TestUA/1.0")
        store.setCustomReferer("https://example.com")
        store.setCustomCookie("session=abc")
        assertEquals("TestUA/1.0", store.customUserAgent.value)
        assertEquals("https://example.com", store.customReferer.value)
        assertEquals("session=abc", store.customCookie.value)
    }

    @Test
    fun maxParallelClamped() = runBlocking {
        store.setMaxParallel(0)
        assertTrue("max parallel clamps to 1", store.maxParallel.value == 1)
        store.setMaxParallel(99)
        assertTrue("max parallel clamps to 6", store.maxParallel.value == 6)
        store.setMaxParallel(4)
        assertTrue("max parallel keeps 4", store.maxParallel.value == 4)
    }

    @Test
    fun lowSpaceMbClamped() = runBlocking {
        store.setLowSpaceMb(0)
        assertTrue("low space mb keeps 0", store.lowSpaceMb.value == 0)
        store.setLowSpaceMb(99999)
        assertTrue("low space mb clamps to 16384", store.lowSpaceMb.value == 16384)
        store.setLowSpaceMb(1024)
        assertTrue("low space mb keeps 1024", store.lowSpaceMb.value == 1024)
    }

    @Test
    fun scrobbleSnapshotReturnsAllProviders() = runBlocking {
        store.setScrobbleTrakt(true)
        store.setToken("trakt", "trakt-token-123")
        store.setScrobbleAniList(false)
        store.setToken("anilist", "")

        val snapshot = store.scrobbleSnapshot()
        assertNotNull(snapshot)
        assertTrue("snapshot covers all 5 scrobble providers", snapshot.size == 5)

        val trakt = snapshot.find { it.provider == "trakt" }
        assertNotNull(trakt)
        assertTrue(trakt!!.active)
        assertEquals("trakt-token-123", trakt!!.token)

        val anilist = snapshot.find { it.provider == "anilist" }
        assertNotNull(anilist)
        assertFalse(anilist!!.active)
    }

    @Test
    fun oauthNonceIsConsumedOnce() = runBlocking {
        store.setOAuthNonce("abc-123")
        assertEquals("abc-123", store.takeOAuthNonce())
        assertNull(store.takeOAuthNonce())
    }

    @Test
    fun oauthCredentialsRoundTrip() = runBlocking {
        store.setOAuthCredentials("client-id", "client-secret")
        assertEquals("client-id", store.oauthClientIdOnce())
        assertEquals("client-secret", store.oauthClientSecretOnce())
    }

    @Test
    fun recordSearchCapsAndDedups() = runBlocking {
        store.recordSearch("naruto")
        store.recordSearch("one piece")
        store.recordSearch("naruto")
        store.recordSearch("a")

        val recents = store.recentSearches.value
        assertEquals(listOf("naruto", "one piece"), recents)
    }

    @Test
    fun anime4KPresetValidatesKnownValues() = runBlocking {
        store.setAnime4KPreset("sharpen")
        assertEquals("sharpen", store.anime4KPreset.value)
        store.setAnime4KPreset("unknown")
        assertEquals("off", store.anime4KPreset.value)
    }

    @Test
    fun anime4KGpuTierValidatesKnownValues() = runBlocking {
        store.setAnime4KGpuTier("high")
        assertEquals("high", store.anime4KGpuTier.value)
        store.setAnime4KGpuTier("unknown")
        assertEquals("mid", store.anime4KGpuTier.value)
    }

    @Test
    fun scrobbleConfigActiveRequiresEnabledAndToken() {
        assertTrue(ScrobbleConfig("trakt", true, "tok").active)
        assertFalse(ScrobbleConfig("trakt", true, "").active)
        assertFalse(ScrobbleConfig("trakt", false, "tok").active)
        assertFalse(ScrobbleConfig("trakt", false, "").active)
    }
}
