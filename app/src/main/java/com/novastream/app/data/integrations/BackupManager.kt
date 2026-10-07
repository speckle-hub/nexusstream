package com.novastream.app.data.integrations

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.novastream.app.data.local.LibraryStore
import com.novastream.app.data.local.ScrobbleConfig
import com.novastream.app.data.local.SettingsStore
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * A portable snapshot of the user's app state. Deliberately a plain data class so Gson can
 * serialise it without reflection tricks, and so a future schema change is an additive field.
 *
 * Note: scrobble tokens are included so a restore re-authenticates the integrations — treat the
 * exported `.json` as sensitive.
 */
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val favorites: List<MediaItem> = emptyList(),
    val history: List<WatchEntry> = emptyList(),
    val homeOrder: List<String> = emptyList(),
    val customUserAgent: String = "",
    val customReferer: String = "",
    val customCookie: String = "",
    val updateRepo: String = "",
    val incognito: Boolean = false,
    val flagSecure: Boolean = false,
    val appLock: Boolean = false,
    val maxParallel: Int = 3,
    val autoDownloadNext: Boolean = false,
    val scrobble: List<ScrobbleConfig> = emptyList(),
)

/**
 * Exports/imports [BackupPayload] to a user-chosen document (SAF [Uri]).
 *
 * SAF is used rather than a hard-coded path so the file can live on a real `.json` in Downloads,
 * an SD card, or a cloud provider the user has mounted.
 */
class BackupManager(
    private val context: Context,
    private val library: LibraryStore,
    private val settings: SettingsStore,
) {

    /** Returns the number of favourites + history entries written. */
    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupPayload(
                favorites = library.favorites.first(),
                history = library.history.first(),
                homeOrder = settings.homeOrder.first(),
                customUserAgent = settings.customUserAgent.first(),
                customReferer = settings.customReferer.first(),
                customCookie = settings.customCookie.first(),
                updateRepo = settings.updateRepo.first(),
                incognito = settings.incognito.first(),
                flagSecure = settings.flagSecure.first(),
                appLock = settings.appLock.first(),
                maxParallel = settings.maxParallel.first(),
                autoDownloadNext = settings.autoDownloadNext.first(),
                scrobble = settings.scrobbleSnapshot(),
            )
            val json = Gson().toJson(payload)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                ?: error("Could not open the selected file for writing")
            payload.favorites.size + payload.history.size
        }
    }

    /** Returns the number of favourites + history entries restored. */
    suspend fun importFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Could not open the selected file")
            val payload = Gson().fromJson(text, BackupPayload::class.java)
                ?: error("Not a valid NexusStream backup")
            library.restore(payload.favorites, payload.history)
            if (payload.homeOrder.isNotEmpty()) settings.setHomeOrder(payload.homeOrder)
            settings.setCustomUserAgent(payload.customUserAgent)
            settings.setCustomReferer(payload.customReferer)
            settings.setCustomCookie(payload.customCookie)
            if (payload.updateRepo.isNotBlank()) settings.setUpdateRepo(payload.updateRepo)
            settings.setIncognito(payload.incognito)
            settings.setFlagSecure(payload.flagSecure)
            settings.setAppLock(payload.appLock)
            settings.setMaxParallel(payload.maxParallel)
            settings.setAutoDownloadNext(payload.autoDownloadNext)
            payload.scrobble.forEach { cfg ->
                settings.setToken(cfg.provider, cfg.token)
                when (cfg.provider) {
                    "trakt" -> settings.setScrobbleTrakt(cfg.enabled)
                    "anilist" -> settings.setScrobbleAniList(cfg.enabled)
                    "mal" -> settings.setScrobbleMal(cfg.enabled)
                    "kitsu" -> settings.setScrobbleKitsu(cfg.enabled)
                    "simkl" -> settings.setScrobbleSimkl(cfg.enabled)
                }
            }
            payload.favorites.size + payload.history.size
        }
    }
}
