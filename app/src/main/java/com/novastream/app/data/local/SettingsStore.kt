package com.novastream.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        val SUBS_ENABLED = booleanPreferencesKey("subs_enabled")
        val NSFW_LOCK = booleanPreferencesKey("nsfw_lock")
        val NSFW_PIN = stringPreferencesKey("nsfw_pin")
        val NSFW_BIOMETRIC = booleanPreferencesKey("nsfw_biometric")
        val NSFW_UNLOCKED = booleanPreferencesKey("nsfw_unlocked")
        val AUTO_NSFW_FROM_ADDONS = booleanPreferencesKey("auto_nsfw")
        val CACHE_META = booleanPreferencesKey("cache_meta")
        val LAST_TAB = intPreferencesKey("last_tab")
    }

    val theme: StateFlow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "dark" }.stateIn(scope, SharingStarted.Eagerly, "dark")
    val accent: StateFlow<String> = context.dataStore.data.map { it[Keys.ACCENT] ?: "violet" }.stateIn(scope, SharingStarted.Eagerly, "violet")
    val playerExternal: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.PLAYER_EXTERNAL] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)
    val preferredQuality: StateFlow<String> = context.dataStore.data.map { it[Keys.PREFERRED_QUALITY] ?: "Auto" }.stateIn(scope, SharingStarted.Eagerly, "Auto")
    val autoplay: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val subsEnabled: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.SUBS_ENABLED] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val nsfwLock: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_LOCK] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)
    val nsfwPin: StateFlow<String?> = context.dataStore.data.map { it[Keys.NSFW_PIN] }.stateIn(scope, SharingStarted.Eagerly, null)
    val nsfwBiometric: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_BIOMETRIC] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)
    val nsfwUnlocked: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.NSFW_UNLOCKED] ?: false }.stateIn(scope, SharingStarted.Eagerly, false)
    val autoNsfwFromAddons: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_NSFW_FROM_ADDONS] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val cacheMeta: StateFlow<Boolean> = context.dataStore.data.map { it[Keys.CACHE_META] ?: true }.stateIn(scope, SharingStarted.Eagerly, true)
    val lastTab: StateFlow<Int> = context.dataStore.data.map { it[Keys.LAST_TAB] ?: 0 }.stateIn(scope, SharingStarted.Eagerly, 0)

    suspend fun setTheme(v: String) = context.dataStore.edit { it[Keys.THEME] = v }
    suspend fun setAccent(v: String) = context.dataStore.edit { it[Keys.ACCENT] = v }
    suspend fun setPlayerExternal(v: Boolean) = context.dataStore.edit { it[Keys.PLAYER_EXTERNAL] = v }
    suspend fun setPreferredQuality(v: String) = context.dataStore.edit { it[Keys.PREFERRED_QUALITY] = v }
    suspend fun setAutoplay(v: Boolean) = context.dataStore.edit { it[Keys.AUTOPLAY] = v }
    suspend fun setSubsEnabled(v: Boolean) = context.dataStore.edit { it[Keys.SUBS_ENABLED] = v }
    suspend fun setNsfwLock(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_LOCK] = v }
    suspend fun setNsfwPin(v: String?) = context.dataStore.edit { if (v == null) it.remove(Keys.NSFW_PIN) else it[Keys.NSFW_PIN] = v }
    suspend fun setNsfwBiometric(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_BIOMETRIC] = v }
    suspend fun setNsfwUnlocked(v: Boolean) = context.dataStore.edit { it[Keys.NSFW_UNLOCKED] = v }
    suspend fun setAutoNsfwFromAddons(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_NSFW_FROM_ADDONS] = v }
    suspend fun setCacheMeta(v: Boolean) = context.dataStore.edit { it[Keys.CACHE_META] = v }
    suspend fun setLastTab(v: Int) = context.dataStore.edit { it[Keys.LAST_TAB] = v }
}
