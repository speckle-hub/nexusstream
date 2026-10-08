package com.novastream.app.data.integrations

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.novastream.app.data.remote.Http
import com.novastream.app.data.remote.httpGet
import com.novastream.app.data.remote.obj
import com.novastream.app.data.remote.parseJson
import com.novastream.app.data.remote.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

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
 * Downloads a release APK into the app cache so it can be handed to the system installer.
 * Returns the file on success, `null` on any failure.
 */
suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
    runCatching {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "nexusstream-update.apk")
        val request = Request.Builder().url(url).build()
        Http.client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val stream = resp.body?.byteStream() ?: error("Empty response")
            stream.use { input -> out.outputStream().use { input.copyTo(it) } }
        }
        out
    }.getOrNull()
}

/** Launches the system APK installer for a downloaded update via a [FileProvider] URI. */
fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
