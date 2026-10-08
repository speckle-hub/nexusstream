package com.novastream.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_settings")

/** App-wide user preferences. */
class SettingsStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private object Keys {
        val THEME = stringPreferencesKey("theme")                 // dark | light | system
        val ACCENT = stringPreferencesKey("accent")
        val PLAYER_EXTERNAL = booleanPreferencesKey("player_external")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val AUTOPLAY = booleanPreferencesKey("autoplay")
        // Automatically jump past the heuristic/chapter intro window when playback reaches it.
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val SUBS_ENABLED = booleanPreferencesKey("subs_enabled")
        val NSFW_LOCK = booleanPreferencesKey("nsfw_lock")
        val NSFW_PIN = stringPreferencesKey("nsfw_pin")
        val NSFW_BIOMETRIC = booleanPreferencesKey("nsfw_biometric")
        val NSFW_UNLOCKED = booleanPreferencesKey("nsfw_unlocked")
        val AUTO_NSFW_FROM_ADDONS = booleanPreferencesKey("auto_nsfw")
        // Opt-in, set from inside the isolated NSFW Hub: when false (default) NSFW add-ons are
        // never consulted by the general Search screen or the Home feed.
        val NSFW_GLOBAL_SEARCH = booleanPreferencesKey("nsfw_global_search")
        val CACHE_META = booleanPreferencesKey("cache_meta")
        val LAST_TAB = intPreferencesKey("last_tab")
        // Bumped whenever the bottom-bar tab list changes shape. The persisted tab is stored as an
        // *index*, so reordering or removing a tab silently points a returning user at the wrong
        // destination. On a mismatch the saved index is ignored and the app starts on Home.
        val TAB_LAYOUT_VERSION = intPreferencesKey("tab_layout_version")
        // Newline-joined recent search terms (DataStore has no string-list type). Stored here so the
        // Search screen can offer them back instead of showing a dead "Start typing" empty state.
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")

        // ---- Phase 11: customization & player/reader engine ----
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        // Subtitle rendering (applied to the Media3 PlayerView's SubtitleView).
        val SUB_SCALE = floatPreferencesKey("sub_scale")            // 0.6 .. 2.0
        val SUB_COLOR = stringPreferencesKey("sub_color")           // white | yellow | cyan | green
        val SUB_BG = floatPreferencesKey("sub_bg")                  // 0 .. 1 background opacity
        val SUB_OFFSET = floatPreferencesKey("sub_offset")          // 0 .. 0.3 bottom padding fraction
        // Caption timing correction in ms (-10000 .. 10000). Phase 15.
        val SUB_SYNC_MS = intPreferencesKey("sub_sync_ms")
        // Player
        val PLAYER_SPEED = floatPreferencesKey("player_speed")      // 0.5 .. 2.0
        val AUDIO_BOOST = booleanPreferencesKey("audio_boost")
        val AUDIO_NORMALIZE = booleanPreferencesKey("audio_normalize")
        // Double-tap seek distance (ms), configurable from Settings -> Player.
        val DOUBLE_TAP_MS = intPreferencesKey("double_tap_ms")
        // Remembered in-player screen brightness (0..1); -1 = follow the system.
        val PLAYER_BRIGHTNESS = floatPreferencesKey("player_brightness")
        // Manga reader
        val MANGA_INVERT = booleanPreferencesKey("manga_invert")
        val MANGA_GRAYSCALE = booleanPreferencesKey("manga_grayscale")
        val MANGA_CROP = booleanPreferencesKey("manga_crop")
        val MANGA_DOUBLE = booleanPreferencesKey("manga_double")
        val MANGA_ZOOM_LOCK = booleanPreferencesKey("manga_zoom_lock")
        val MANGA_VOLUME_KEYS = booleanPreferencesKey("manga_volume_keys")
        // Warm / sepia reading filter alongside invert + grayscale.
        val MANGA_WARM = booleanPreferencesKey("manga_warm")
        // Newline-joined "mangaId::chapterId=page" resume markers.
        val MANGA_PROGRESS = stringPreferencesKey("manga_progress")

        // ---- Phase 12: integrations, extensions, downloads & security ----
        val INCOGNITO = booleanPreferencesKey("incognito")
        val FLAG_SECURE = booleanPreferencesKey("flag_secure")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        // Newline-joined home-row titles in display order (empty = default order).
        val HOME_ORDER = stringPreferencesKey("home_order")
        // Newline-joined extension ids the user has disabled.
        val DISABLED_SOURCES = stringPreferencesKey("disabled_sources")
        // Global custom HTTP identity applied to every outbound request + stream playback.
        val CUSTOM_UA = stringPreferencesKey("custom_ua")
        val CUSTOM_REFERER = stringPreferencesKey("custom_referer")
        val CUSTOM_COOKIE = stringPreferencesKey("custom_cookie")
        // Downloads
        val MAX_PARALLEL = intPreferencesKey("max_parallel")
        val AUTO_DOWNLOAD_NEXT = booleanPreferencesKey("auto_download_next")
        val AUTO_DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("auto_download_wifi_only")
        // Delete a finished download once playback passes 90% (Phase 15).
        val AUTO_DELETE_WATCHED = booleanPreferencesKey("auto_delete_watched")
        val STORAGE_URI = stringPreferencesKey("storage_uri")
        // Updates
        val UPDATE_REPO = stringPreferencesKey("update_repo")
        val NOTIFY_NEW_CONTENT = booleanPreferencesKey("notify_new_content")
        // Scrobbling (token non-empty = provider enabled).
        val SCROBBLE_TRAKT = booleanPreferencesKey("scrobble_trakt")
        val SCROBBLE_ANILIST = booleanPreferencesKey("scrobble_anilist")
        val SCROBBLE_MAL = booleanPreferencesKey("scrobble_mal")
        val SCROBBLE_KITSU = booleanPreferencesKey("scrobble_kitsu")
        val SCROBBLE_SIMKL = booleanPreferencesKey("scrobble_simkl")
        val TOKEN_TRAKT = stringPreferencesKey("token_trakt")
        val TOKEN_ANILIST = stringPreferencesKey("token_anilist")
        val TOKEN_MAL = stringPreferencesKey("token_mal")
        val TOKEN_KITSU = stringPreferencesKey("token_kitsu")
        val TOKEN_SIMKL = stringPreferencesKey("token_simkl")
    }

    val theme: StateFlow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "dark" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "dark")
    val accent: StateFlow<String> = context.dataStore.data.map { it[Keys.ACCENT] ?: "violet" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "violet")
    val playerExternal: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.PLAYER_EXTERNAL] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val preferredQuality: StateFlow<String> = context.dataStore.data.map { it[Keys.PREFERRED_QUALITY] ?: "Auto" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "Auto")
    val autoplay: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val subsEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SUBS_ENABLED] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val autoSkipIntro: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SKIP_INTRO] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val nsfwLock: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_LOCK] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val nsfwPin: StateFlow<String?> = context.dataStore.data.map { it[Keys.NSFW_PIN] }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
    val nsfwBiometric: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_BIOMETRIC] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val nsfwUnlocked: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_UNLOCKED] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val autoNsfwFromAddons: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_NSFW_FROM_ADDONS] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val nsfwGlobalSearch: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_GLOBAL_SEARCH] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val cacheMeta: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.CACHE_META] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val lastTab: StateFlow<Int> = context.dataStore.data.map { it[Keys.LAST_TAB] ?: 0 }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    // ---- Phase 11 ----
    val dynamicColor: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val amoledBlack: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED_BLACK] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val subtitleScale: StateFlow<Float> = context.dataStore.data.map { it[Keys.SUB_SCALE] ?: 1f }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 1f)
    val subtitleColor: StateFlow<String> = context.dataStore.data.map { it[Keys.SUB_COLOR] ?: "white" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "white")
    val subtitleBg: StateFlow<Float> = context.dataStore.data.map { it[Keys.SUB_BG] ?: 0.4f }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0.4f)
    val subtitleOffset: StateFlow<Float> = context.dataStore.data.map { it[Keys.SUB_OFFSET] ?: 0.05f }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0.05f)
    val subtitleSyncMs: StateFlow<Int> = context.dataStore.data.map { it[Keys.SUB_SYNC_MS] ?: 0 }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)
    val playerSpeed: StateFlow<Float> = context.dataStore.data.map { it[Keys.PLAYER_SPEED] ?: 1f }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 1f)
    val audioBoost: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_BOOST] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val audioNormalize: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_NORMALIZE] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val doubleTapMs: StateFlow<Int> = context.dataStore.data.map { it[Keys.DOUBLE_TAP_MS] ?: 10_000 }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 10_000)
    val playerBrightness: StateFlow<Float> = context.dataStore.data.map { it[Keys.PLAYER_BRIGHTNESS] ?: -1f }.stateIn(scope, SharingStarted.WhileSubscribed(5000), -1f)
    val mangaInvert: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_INVERT] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val mangaGrayscale: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_GRAYSCALE] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val mangaCrop: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_CROP] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val mangaDoublePage: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_DOUBLE] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val mangaZoomLock: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_ZOOM_LOCK] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val mangaVolumeKeys: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_VOLUME_KEYS] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val mangaWarm: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.MANGA_WARM] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    // ---- Phase 12 ----
    val incognito: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.INCOGNITO] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val flagSecure: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.FLAG_SECURE] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val appLock: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val homeOrder: StateFlow<List<String>> = context.dataStore.data
        .map { it[Keys.HOME_ORDER]?.split('\n')?.filter { s -> s.isNotBlank() }.orEmpty() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val disabledSources: StateFlow<List<String>> = context.dataStore.data
        .map { it[Keys.DISABLED_SOURCES]?.split('\n')?.filter { s -> s.isNotBlank() }.orEmpty() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customUserAgent: StateFlow<String> = context.dataStore.data.map { it[Keys.CUSTOM_UA] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val customReferer: StateFlow<String> = context.dataStore.data.map { it[Keys.CUSTOM_REFERER] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val customCookie: StateFlow<String> = context.dataStore.data.map { it[Keys.CUSTOM_COOKIE] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val maxParallel: StateFlow<Int> = context.dataStore.data.map { it[Keys.MAX_PARALLEL] ?: 3 }.stateIn(scope, SharingStarted.WhileSubscribed(5000), 3)
    val autoDownloadNext: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DOWNLOAD_NEXT] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val autoDownloadWifiOnly: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DOWNLOAD_WIFI_ONLY] ?: true }.stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    val autoDeleteWatched: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DELETE_WATCHED] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val storageUri: StateFlow<String> = context.dataStore.data.map { it[Keys.STORAGE_URI] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val updateRepo: StateFlow<String> = context.dataStore.data.map { it[Keys.UPDATE_REPO] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val notifyNewContent: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_NEW_CONTENT] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleTrakt: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SCROBBLE_TRAKT] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleAniList: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SCROBBLE_ANILIST] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleMal: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SCROBBLE_MAL] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleKitsu: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SCROBBLE_KITSU] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val scrobbleSimkl: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SCROBBLE_SIMKL] ?: false }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
    val tokenTrakt: StateFlow<String> = context.dataStore.data.map { it[Keys.TOKEN_TRAKT] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val tokenAniList: StateFlow<String> = context.dataStore.data.map { it[Keys.TOKEN_ANILIST] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val tokenMal: StateFlow<String> = context.dataStore.data.map { it[Keys.TOKEN_MAL] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val tokenKitsu: StateFlow<String> = context.dataStore.data.map { it[Keys.TOKEN_KITSU] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")
    val tokenSimkl: StateFlow<String> = context.dataStore.data.map { it[Keys.TOKEN_SIMKL] ?: "" }.stateIn(scope, SharingStarted.WhileSubscribed(5000), "")

    /** Most-recent-first list of past searches, capped at [MAX_RECENT]. */
    val recentSearches: StateFlow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[Keys.RECENT_SEARCHES]
                ?.split('\n')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun setTheme(v: String) = context.dataStore.edit { it[Keys.THEME] = v }
    suspend fun setAccent(v: String) = context.dataStore.edit { it[Keys.ACCENT] = v }
    suspend fun setPlayerExternal(v: Boolean) = context.dataStore.edit { it[Keys.PLAYER_EXTERNAL] = v }
    suspend fun setPreferredQuality(v: String) = context.dataStore.edit { it[Keys.PREFERRED_QUALITY] = v }
    suspend fun setAutoplay(v: Boolean) = context.dataStore.edit { it[Keys.AUTOPLAY] = v }
    suspend fun setAutoSkipIntro(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_SKIP_INTRO] = v }
    suspend fun setSubsEnabled(v: Boolean) = context.dataStore.edit { it[Keys.SUBS_ENABLED] = v }
    suspend fun setNsfwLock(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_LOCK] = v }
    suspend fun setNsfwPin(v: String?) = context.dataStore.edit { if (v == null) it.remove(Keys.NSFW_PIN) else it[Keys.NSFW_PIN] = v }
    suspend fun setNsfwBiometric(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_BIOMETRIC] = v }
    suspend fun setNsfwUnlocked(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_UNLOCKED] = v }
    suspend fun setAutoNsfwFromAddons(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_NSFW_FROM_ADDONS] = v }
    suspend fun setNsfwGlobalSearch(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_GLOBAL_SEARCH] = v }
    suspend fun setCacheMeta(v: Boolean) = context.dataStore.edit { it[Keys.CACHE_META] = v }
    suspend fun setLastTab(v: Int) = context.dataStore.edit { it[Keys.LAST_TAB] = v }

    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = v }
    suspend fun setAmoledBlack(v: Boolean) = context.dataStore.edit { it[Keys.AMOLED_BLACK] = v }
    suspend fun setSubtitleScale(v: Float) = context.dataStore.edit { it[Keys.SUB_SCALE] = v.coerceIn(0.6f, 2f) }
    suspend fun setSubtitleColor(v: String) = context.dataStore.edit { it[Keys.SUB_COLOR] = v }
    suspend fun setSubtitleBg(v: Float) = context.dataStore.edit { it[Keys.SUB_BG] = v.coerceIn(0f, 1f) }
    suspend fun setSubtitleOffset(v: Float) = context.dataStore.edit { it[Keys.SUB_OFFSET] = v.coerceIn(0f, 0.3f) }
    suspend fun setSubtitleSyncMs(v: Int) = context.dataStore.edit { it[Keys.SUB_SYNC_MS] = v.coerceIn(-10_000, 10_000) }
    suspend fun setPlayerSpeed(v: Float) = context.dataStore.edit { it[Keys.PLAYER_SPEED] = v.coerceIn(0.5f, 2f) }
    suspend fun setDoubleTapMs(v: Int) = context.dataStore.edit { it[Keys.DOUBLE_TAP_MS] = v.coerceIn(1_000, 60_000) }
    suspend fun setPlayerBrightness(v: Float) = context.dataStore.edit { it[Keys.PLAYER_BRIGHTNESS] = v.coerceIn(-1f, 1f) }
    suspend fun setAudioBoost(v: Boolean) = context.dataStore.edit { it[Keys.AUDIO_BOOST] = v }
    suspend fun setAudioNormalize(v: Boolean) = context.dataStore.edit { it[Keys.AUDIO_NORMALIZE] = v }
    suspend fun setMangaInvert(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_INVERT] = v }
    suspend fun setMangaGrayscale(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_GRAYSCALE] = v }
    suspend fun setMangaCrop(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_CROP] = v }
    suspend fun setMangaDoublePage(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_DOUBLE] = v }
    suspend fun setMangaZoomLock(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_ZOOM_LOCK] = v }
    suspend fun setMangaVolumeKeys(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_VOLUME_KEYS] = v }
    suspend fun setMangaWarm(v: Boolean) = context.dataStore.edit { it[Keys.MANGA_WARM] = v }

    suspend fun setIncognito(v: Boolean) = context.dataStore.edit { it[Keys.INCOGNITO] = v }
    suspend fun setFlagSecure(v: Boolean) = context.dataStore.edit { it[Keys.FLAG_SECURE] = v }
    suspend fun setAppLock(v: Boolean) = context.dataStore.edit { it[Keys.APP_LOCK] = v }
    suspend fun setHomeOrder(order: List<String>) = context.dataStore.edit { it[Keys.HOME_ORDER] = order.joinToString("\n") }
    suspend fun setDisabledSources(ids: List<String>) = context.dataStore.edit { it[Keys.DISABLED_SOURCES] = ids.joinToString("\n") }
    suspend fun setCustomUserAgent(v: String) = context.dataStore.edit { it[Keys.CUSTOM_UA] = v }
    suspend fun setCustomReferer(v: String) = context.dataStore.edit { it[Keys.CUSTOM_REFERER] = v }
    suspend fun setCustomCookie(v: String) = context.dataStore.edit { it[Keys.CUSTOM_COOKIE] = v }
    suspend fun setMaxParallel(v: Int) = context.dataStore.edit { it[Keys.MAX_PARALLEL] = v.coerceIn(1, 6) }
    suspend fun setAutoDownloadNext(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_DOWNLOAD_NEXT] = v }
    suspend fun setAutoDownloadWifiOnly(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_DOWNLOAD_WIFI_ONLY] = v }
    suspend fun setAutoDeleteWatched(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_DELETE_WATCHED] = v }
    suspend fun setStorageUri(v: String) = context.dataStore.edit { it[Keys.STORAGE_URI] = v }
    suspend fun setUpdateRepo(v: String) = context.dataStore.edit { it[Keys.UPDATE_REPO] = v }
    suspend fun setNotifyNewContent(v: Boolean) = context.dataStore.edit { it[Keys.NOTIFY_NEW_CONTENT] = v }
    suspend fun setScrobbleTrakt(v: Boolean) = context.dataStore.edit { it[Keys.SCROBBLE_TRAKT] = v }
    suspend fun setScrobbleAniList(v: Boolean) = context.dataStore.edit { it[Keys.SCROBBLE_ANILIST] = v }
    suspend fun setScrobbleMal(v: Boolean) = context.dataStore.edit { it[Keys.SCROBBLE_MAL] = v }
    suspend fun setScrobbleKitsu(v: Boolean) = context.dataStore.edit { it[Keys.SCROBBLE_KITSU] = v }
    suspend fun setScrobbleSimkl(v: Boolean) = context.dataStore.edit { it[Keys.SCROBBLE_SIMKL] = v }
    suspend fun setToken(provider: String, v: String) = context.dataStore.edit { prefs ->
        when (provider) {
            "trakt" -> prefs[Keys.TOKEN_TRAKT] = v
            "anilist" -> prefs[Keys.TOKEN_ANILIST] = v
            "mal" -> prefs[Keys.TOKEN_MAL] = v
            "kitsu" -> prefs[Keys.TOKEN_KITSU] = v
            "simkl" -> prefs[Keys.TOKEN_SIMKL] = v
        }
    }

    /** One-shot snapshot of every scrobble provider (enabled + token), for the sync client. */
    suspend fun scrobbleSnapshot(): List<ScrobbleConfig> {
        val prefs = context.dataStore.data.first()
        fun cfg(provider: String, enabledKey: Preferences.Key<Boolean>, tokenKey: Preferences.Key<String>) =
            ScrobbleConfig(provider, prefs[enabledKey] ?: false, prefs[tokenKey].orEmpty())
        return listOf(
            cfg("trakt", Keys.SCROBBLE_TRAKT, Keys.TOKEN_TRAKT),
            cfg("anilist", Keys.SCROBBLE_ANILIST, Keys.TOKEN_ANILIST),
            cfg("mal", Keys.SCROBBLE_MAL, Keys.TOKEN_MAL),
            cfg("kitsu", Keys.SCROBBLE_KITSU, Keys.TOKEN_KITSU),
            cfg("simkl", Keys.SCROBBLE_SIMKL, Keys.TOKEN_SIMKL),
        )
    }

    /** Persist the last-read page for a chapter (used to restore precise position). */
    suspend fun setMangaProgress(chapterKey: String, page: Int) = context.dataStore.edit { prefs ->
        val map = prefs[Keys.MANGA_PROGRESS]
            ?.split('\n')
            ?.mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
            }
            ?.toMap()
            .orEmpty()
            .toMutableMap()
        // Remove before re-inserting so an *updated* chapter moves to the end of the (insertion
        // ordered) map. Updating in place left frequently-read chapters at their old position,
        // where `takeLast(MAX_MANGA_PROGRESS)` could evict the very chapters read most.
        map.remove(chapterKey)
        map[chapterKey] = page.toString()
        // Keep the map bounded so it can't grow without limit.
        val trimmed = map.entries.toList().takeLast(MAX_MANGA_PROGRESS)
        prefs[Keys.MANGA_PROGRESS] = trimmed.joinToString("\n") { (k, v) -> "$k=$v" }
    }

    /** One-shot read of the saved page for a chapter (`null` when never opened). */
    suspend fun mangaProgress(chapterKey: String): Int? {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.MANGA_PROGRESS]
            ?.split('\n')
            ?.firstOrNull { it.startsWith("$chapterKey=") }
            ?.substringAfter('=')
            ?.toIntOrNull()
    }

    /**
     * One-shot read of the persisted tab index, for cold-boot restore. Reading the DataStore
     * directly (rather than the [lastTab] StateFlow, whose initial value is always 0) avoids
     * mistaking "not loaded yet" for "Home" and lets the restore run exactly once.
     */
    /**
     * One-shot read of the persisted tab index, for cold-boot restore. Reading the DataStore
     * directly (rather than the [lastTab] StateFlow, whose initial value is always 0) avoids
     * mistaking "not loaded yet" for "Home" and lets the restore run exactly once.
     *
     * Returns [FALLBACK_TAB] when the stored index predates the current tab layout — the Phase 9
     * restructure folded six tabs into five and reordered them, so an index saved by an older
     * install would otherwise resolve to a different tab than the one the user last used.
     */
    suspend fun lastTabSnapshot(): Int {
        val prefs = context.dataStore.data.first()
        if (prefs[Keys.TAB_LAYOUT_VERSION] != CURRENT_TAB_LAYOUT_VERSION) return FALLBACK_TAB
        return prefs[Keys.LAST_TAB] ?: FALLBACK_TAB
    }

    /** Records the tab layout the persisted [LAST_TAB] index was captured against. */
    suspend fun stampTabLayoutVersion() = context.dataStore.edit {
        it[Keys.TAB_LAYOUT_VERSION] = CURRENT_TAB_LAYOUT_VERSION
    }

    /**
     * Push [term] to the front of the recent-search list, de-duplicating case-insensitively and
     * keeping only the newest [MAX_RECENT] entries.
     */
    suspend fun recordSearch(term: String) {
        val query = term.trim()
        if (query.length < MIN_SEARCH_LENGTH) return
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.RECENT_SEARCHES]
                ?.split('\n')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            val updated = buildList {
                add(query)
                existing.forEach { if (!it.equals(query, ignoreCase = true)) add(it) }
            }.take(MAX_RECENT)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() = context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }

    companion object {
        /** Newest-first cap on remembered searches. */
        const val MAX_RECENT = 8

        /** Below this, a "search" is just a keystroke, so nothing is worth remembering. */
        const val MIN_SEARCH_LENGTH = 2

        /**
         * Incremented on every bottom-bar tab list change: 1 = the original six tabs
         * (Home/Manga/NSFW/Search/Library/Settings), 2 = the five-tab Browse layout.
         */
        const val CURRENT_TAB_LAYOUT_VERSION = 2

        /** Tab index meaning "no saved tab" — always Home. */
        const val FALLBACK_TAB = 0

        /** Cap on remembered manga reading positions. */
        const val MAX_MANGA_PROGRESS = 200
    }
}

/** One scrobble provider's enabled/token state. */
data class ScrobbleConfig(
    val provider: String,
    val enabled: Boolean,
    val token: String,
) {
    /** Ready to sync only when enabled *and* a token has been pasted in. */
    val active: Boolean get() = enabled && token.isNotBlank()
}
