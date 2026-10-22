package com.moovio.app.data.integrations

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.moovio.app.data.remote.Http
import com.moovio.app.data.remote.httpGet
import com.moovio.app.data.remote.obj
import com.moovio.app.data.remote.parseJson
import com.moovio.app.data.remote.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import kotlin.math.roundToInt

/** A GitHub release that may carry a newer APK. */
data class ReleaseInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val apkUrl: String?,
    val pageUrl: String,
) {
    /** Compares two dotted version tags loosely (ignores a leading `v`). */
    fun isNewerThan(current: String): Boolean {
        fun parts(s: String) = s.trim().removePrefix("v").split('.', '-').mapNotNull { it.toIntOrNull() }
        val a = parts(tag)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/**
 * Checks a GitHub repository for a newer release.
 *
 * Unauthenticated GitHub API calls are rate-limited but fine for occasional checks. When the user
 * has not configured a repository the check falls back to [DEFAULT_REPO], so "Check for updates"
 * (and the launch-time check) work out of the box.
 */
object UpdateChecker {

    /**
     * Repository the updater queries when Settings has none configured. The in-app GitHub Release
     * for this repo carries `app-release.apk`, so the launch dialog can download and install it.
     */
    const val DEFAULT_REPO = "speckle-hub/nexusstream"

    /** The repository actually queried: the user's configured value, or [DEFAULT_REPO] when blank. */
    fun resolveRepo(configured: String): String = configured.trim().ifBlank { DEFAULT_REPO }

    suspend fun check(repo: String): Result<ReleaseInfo> = runCatching {
        val target = resolveRepo(repo)
        val json = httpGet(
            "https://api.github.com/repos/$target/releases/latest",
            mapOf("Accept" to "application/vnd.github+json"),
        )
        val root = parseJson(json)
        val tag = root.str("tag_name") ?: error("No release found")
        val assets = root.obj("assets")
        val apkUrl = assets
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.firstOrNull { el ->
                el.isJsonObject && el.asJsonObject.get("name")?.asString?.endsWith(".apk", true) == true
            }
            ?.asJsonObject
            ?.get("browser_download_url")
            ?.asString
        ReleaseInfo(
            tag = tag,
            name = root.str("name") ?: tag,
            notes = root.str("body").orEmpty(),
            apkUrl = apkUrl,
            pageUrl = "https://github.com/$target/releases/latest",
        )
    }
}

/**
 * A snapshot of an in-flight APK download: bytes written so far and the total when the server
 * reports one (`Content-Length` may be absent on chunked responses, hence nullable).
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?,
) {
    /** 0..1 when the total is known and positive, else null (caller shows indeterminate). */
    val fraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0 }
            ?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }

    /** 0..100 when the total is known, else null. */
    val percent: Int?
        get() = fraction?.let { (it * 100).roundToInt().coerceIn(0, 100) }
}

/** "38.3 MB" / "512 KB" / "128 B" — shared by the progress label and its unit tests. */
fun formatUpdateBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}

/**
 * One-line status for the update dialog, e.g. `Downloading update… 45% (18.2 MB / 40.5 MB)`.
 * Falls back to `Downloading update… 18.2 MB` when the server hides the total.
 */
fun downloadProgressLabel(progress: DownloadProgress?): String = when {
    progress == null -> "Downloading update…"
    progress.percent != null && progress.totalBytes != null ->
        "Downloading update… ${progress.percent}% " +
            "(${formatUpdateBytes(progress.downloadedBytes)} / ${formatUpdateBytes(progress.totalBytes)})"
    else -> "Downloading update… ${formatUpdateBytes(progress.downloadedBytes)}"
}

/**
 * Downloads a release APK into the app cache so it can be handed to the system installer,
 * reporting live progress through [onProgress] (invoked on the IO thread — callers writing to
 * Compose state must be on a thread-safe snapshot path, which plain `mutableStateOf` writes are).
 *
 * Progress is throttled to one emission per ~100 ms or per percent-point so the dialog recomposes
 * at most ~10×/s instead of per 8 KB chunk. Cancellation aborts the copy, deletes the partial file
 * and returns `null`; any other failure also returns `null` (callers show their own message).
 */
suspend fun downloadApkWithProgress(
    context: Context,
    url: String,
    onProgress: (DownloadProgress) -> Unit = {},
): File? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "moovio-update.apk")
        // A stale partial from a cancelled run must never be mistaken for a finished APK.
        if (out.exists()) out.delete()
        val request = Request.Builder().url(url).build()
        Http.client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val total = resp.body?.contentLength()?.takeIf { it > 0 }
            val stream = resp.body?.byteStream() ?: error("Empty response")
            var downloaded = 0L
            var lastEmitMs = 0L
            var lastPercent = -1
            onProgress(DownloadProgress(0, total))
            stream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        val percent = DownloadProgress(downloaded, total).percent ?: -1
                        if (now - lastEmitMs >= 100 || percent != lastPercent) {
                            lastEmitMs = now
                            lastPercent = percent
                            onProgress(DownloadProgress(downloaded, total))
                        }
                    }
                }
            }
            onProgress(DownloadProgress(downloaded, total))
        }
        out
    }.getOrElse {
        // Cancellation (or failure) leaves no partial behind; CancellationException is rethrown
        // by getOrElse's contract? No — runCatching captures it, so rethrow explicitly to keep
        // the caller's Job cancellable instead of swallowing it into a silent `null`.
        if (it is kotlinx.coroutines.CancellationException) {
            runCatching { File(File(context.cacheDir, "updates"), "moovio-update.apk").delete() }
            throw it
        }
        runCatching { outDelete(context) }
        null
    }
}

private fun outDelete(context: Context) {
    runCatching { File(File(context.cacheDir, "updates"), "moovio-update.apk").delete() }
}

/**
 * Downloads a release APK into the app cache so it can be handed to the system installer.
 * Returns the file on success, `null` on any failure. Progress-blind callers (e.g. the Settings
 * "Check for updates" row) use this; the launch-time dialog uses [downloadApkWithProgress].
 */
suspend fun downloadApk(context: Context, url: String): File? =
    downloadApkWithProgress(context, url)

/**
 * Launches the system APK installer for a downloaded update via a [FileProvider] URI.
 *
 * The archive is signature-checked first ([verifyApkSignature]): it must be the same package and
 * signed by a key this installation trusts. Without that check, a hijacked release could install
 * an arbitrary APK under the user's eyes — the system installer would refuse it *after* showing an
 * install prompt for a foreign package.
 *
 * @throws SecurityException when the archive fails verification.
 */
fun installApk(context: Context, file: File) {
    verifyApkSignature(context, file)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
