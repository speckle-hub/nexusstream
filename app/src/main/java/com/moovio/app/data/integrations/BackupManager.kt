package com.moovio.app.data.integrations

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.moovio.app.data.local.LibraryStore
import com.moovio.app.data.local.ScrobbleConfig
import com.moovio.app.data.local.SettingsStore
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.WatchEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * A portable snapshot of the user's app state. Deliberately a plain data class so Gson can
 * serialise it without reflection tricks, and so a future schema change is an additive field.
 *
 * Note: scrobble tokens, custom cookies and headers are included so a restore re-authenticates the
 * integrations. That is precisely why the file is written encrypted — see [BackupManager].
 *
 * `version` 2 = AES-256-GCM container (ISSUE-15). Version 1 files were plaintext JSON and are still
 * importable, but only after the user confirms it.
 */
data class BackupPayload(
    val version: Int = 2,
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
 * What an import found in the picked file, *before* anything is written into the app's stores.
 *
 * Splitting "read and identify" from "apply" is what makes the legacy path safe: the UI can look at
 * an unencrypted file, tell the user it holds tracker tokens in the clear, and only then decide to
 * restore it.
 */
sealed class BackupInspection {
    abstract val payload: BackupPayload

    /** AES-256-GCM container written by this version of the app. */
    class Encrypted(override val payload: BackupPayload) : BackupInspection()

    /** A pre-ISSUE-15 export: plain JSON, secrets included, written before backups were encrypted. */
    class LegacyPlaintext(override val payload: BackupPayload) : BackupInspection()
}

/**
 * Exports/imports [BackupPayload] to a user-chosen document (SAF [Uri]).
 *
 * SAF is used rather than a hard-coded path so the file can live on a real `.json` in Downloads,
 * an SD card, or a cloud provider the user has mounted.
 *
 * **ISSUE-15:** exports are AES-256-GCM encrypted with a key held in the Android KeyStore
 * ([AndroidBackupKey]), so a backup file that leaks — synced to a cloud drive, mailed, left in a
 * shared folder — no longer contains readable Trakt/AniList tokens or cookies. The trade-off is that
 * the file is bound to this install: it cannot be decrypted on another device. Imports therefore
 * still accept the old plaintext format, but only through [importFrom] with `allowPlaintext = true`,
 * which the Settings screen sets after an explicit confirmation dialog.
 */
class BackupManager(
    private val context: Context,
    private val library: LibraryStore,
    private val settings: SettingsStore,
    private val cipher: BackupCipher = BackupCipher { AndroidBackupKey.get() },
) {

    /** Encrypts the current state into [uri]. Returns the number of favourites + history entries. */
    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = snapshot()
            val plain = Gson().toJson(payload).toByteArray()
            val encrypted = cipher.encrypt(plain)
            // "wt" truncates: re-exporting over an existing file used to leave the tail of the
            // previous, larger backup on the end, which then failed to decrypt or parse.
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(encrypted) }
                ?: error("Could not open the selected file for writing")
            payload.favorites.size + payload.history.size
        }
    }

    /** Reads [uri] and reports whether it is an encrypted container or a legacy plaintext export. */
    suspend fun inspect(uri: Uri): Result<BackupInspection> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Could not open the selected file")
            if (bytes.isEmpty()) error("The selected file is empty")
            if (BackupFormat.isEncrypted(bytes)) {
                BackupInspection.Encrypted(decode(cipher.decrypt(bytes)))
            } else {
                // Anything without our magic bytes is treated as the v1 JSON export. A file that is
                // neither will fail to parse below and report that instead of importing garbage.
                BackupInspection.LegacyPlaintext(decode(bytes))
            }
        }
    }

    /**
     * Restores [uri]. Refuses a plaintext backup unless [allowPlaintext] is set, so an unencrypted
     * file can never be restored by accident.
     */
    suspend fun importFrom(uri: Uri, allowPlaintext: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val inspection = inspect(uri).getOrThrow()
            if (inspection is BackupInspection.LegacyPlaintext && !allowPlaintext) {
                error(PLAINTEXT_NEEDS_CONFIRMATION)
            }
            val payload = inspection.payload
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

    /** Everything a backup carries, read once so export and its size report agree. */
    private suspend fun snapshot() = BackupPayload(
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

    private fun decode(bytes: ByteArray): BackupPayload {
        val text = bytes.decodeToString()
        return Gson().fromJson(text, BackupPayload::class.java)
            ?: error("Not a valid Moovio backup")
    }

    companion object {
        /** Returned when an unencrypted file is imported without confirmation; the UI matches on it. */
        const val PLAINTEXT_NEEDS_CONFIRMATION = "This backup is not encrypted"
    }
}
