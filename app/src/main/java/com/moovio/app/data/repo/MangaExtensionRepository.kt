package com.moovio.app.data.repo

import android.content.Context
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.remote.httpGetBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Installs Aniyomi / Mihon extension APKs.
 *
 * The extension index only carries metadata; the APK itself has to be downloaded before anything
 * can use it. This repository resolves `<repo dir>/apk/<name>` (the parsers hand over absolute
 * URLs), stores the file in app-private storage and records the descriptor — including the
 * [MangaExt.nsfw] flag, the version code used for update detection, and the download path.
 *
 * ### What this does *not* do
 * It does not execute the extension. Aniyomi/Mihon extensions implement the
 * `eu.kanade.tachiyomi.*` source APIs, which this app does not ship a runtime for yet (the
 * CloudStream runtime is the only execution engine). Installing here means "downloaded, known and
 * togglable"; the source runner is the next phase (see roadmap §2ba).
 */
class MangaExtensionRepository(
    private val context: Context,
    private val store: AddonStore,
    private val addons: AddonRepository,
) {

    /** Every downloaded extension descriptor (enabled + disabled). */
    val installed: Flow<List<MangaExt>> = store.mangaInstalled

    private val dir: File get() = File(context.filesDir, "manga-ext").apply { mkdirs() }

    /**
     * Download [ext] and persist it as installed. Re-installing the same package replaces the
     * descriptor (and the file), which is how an update is applied.
     */
    suspend fun install(ext: MangaExt): Result<MangaExt> = withContext(Dispatchers.IO) {
        runCatching {
            require(ext.pkg.isNotBlank()) { "Extension has no package name" }
            require(ext.apk.startsWith("https://") || ext.apk.startsWith("http://")) {
                "Extension has no download URL"
            }
            val target = File(dir, safeName(ext.pkg) + ".apk")
            val bytes = httpGetBytes(ext.apk)
            require(bytes.size > MIN_APK_BYTES) { "Downloaded extension looks empty" }
            target.writeBytes(bytes)
            val descriptor = ext.copy(apkPath = target.absolutePath, disabled = false)
            addons.installMangaExt(descriptor)
            descriptor
        }
    }

    /** Enable / disable an installed extension (persisted; survives restarts). */
    suspend fun setEnabled(pkg: String, enabled: Boolean) = addons.setMangaExtEnabled(pkg, enabled)

    /** Remove the descriptor and delete the downloaded APK. */
    suspend fun uninstall(pkg: String) = withContext(Dispatchers.IO) {
        runCatching {
            store.mangaInstalled.first().firstOrNull { it.pkg == pkg }?.apkPath?.let { path ->
                runCatching { File(path).delete() }
            }
        }
        addons.uninstallMangaExt(pkg)
    }

    /** True when [candidate] (fresh from the index) is newer than what is installed. */
    fun updateAvailable(installed: MangaExt?, candidate: MangaExt): Boolean =
        isUpdateAvailable(installed, candidate)

    /** Strip anything that could escape the extension directory. */
    private fun safeName(pkg: String): String =
        pkg.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "extension" }

    companion object {
        /** Smallest plausible extension APK; anything less is an error page, not a download. */
        private const val MIN_APK_BYTES = 2048

        /**
         * Update policy, kept pure so it is testable without a Context.
         *
         * Only an *installed* extension can be updated (an APK on disk), and only when both sides
         * carry a version code — without one the index gives us nothing to compare, and guessing
         * from the version string could offer a "downgrade" as an update.
         */
        fun isUpdateAvailable(installed: MangaExt?, candidate: MangaExt): Boolean {
            if (installed == null || installed.apkPath.isNullOrBlank()) return false
            val installedCode = installed.versionCode ?: return false
            val candidateCode = candidate.versionCode ?: return false
            return candidateCode > installedCode
        }
    }
}
