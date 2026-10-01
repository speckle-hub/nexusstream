package com.novastream.app.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.novastream.app.data.download.VideoDownloadContract.CHANNEL_ID
import com.novastream.app.data.download.VideoDownloadContract.KEY_BYTES
import com.novastream.app.data.download.VideoDownloadContract.KEY_ETA
import com.novastream.app.data.download.VideoDownloadContract.KEY_ID
import com.novastream.app.data.download.VideoDownloadContract.KEY_KIND
import com.novastream.app.data.download.VideoDownloadContract.KEY_PROGRESS
import com.novastream.app.data.download.VideoDownloadContract.KEY_SPEED
import com.novastream.app.data.download.VideoDownloadContract.KEY_TITLE
import com.novastream.app.data.download.VideoDownloadContract.KEY_TOTAL
import com.novastream.app.data.download.VideoDownloadContract.KEY_URL
import com.novastream.app.data.download.VideoDownloadContract.NOTIFICATION_ID
import com.novastream.app.data.remote.Http
import com.novastream.app.data.remote.UA
import com.novastream.app.data.remote.httpGet
import com.novastream.app.data.remote.httpGetBytes
import kotlinx.coroutines.CancellationException
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Downloads one video (progressive MP4 or HLS playlist) to app-private storage.
 *
 * Runs as a WorkManager foreground job so it survives the app being backgrounded, reporting live
 * progress (percent / bytes / speed / ETA) through `setProgress` + the notification. Progressive
 * downloads resume with an HTTP `Range` request; HLS downloads skip segments already on disk.
 */
class VideoDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private var lastTick = 0L
    private var lastBytes = 0L

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val kind = runCatching { VideoKind.valueOf(inputData.getString(KEY_KIND) ?: "PROGRESSIVE") }
            .getOrDefault(VideoKind.PROGRESSIVE)
        val title = inputData.getString(KEY_TITLE) ?: "Download"
        val dir = File(applicationContext.filesDir, "downloads/video/$id").apply { mkdirs() }

        ensureChannel()
        runCatching { setForeground(buildForeground(0, title, 0L, 0L)) }

        return try {
            val bytes = if (kind == VideoKind.HLS) downloadHls(url, dir, title)
            else downloadProgressive(url, dir, title)
            report(100, bytes, if (bytes > 0) bytes else 0L, title, force = true)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(workDataOf(VideoDownloadContract.KEY_ERROR to (t.message ?: "Download failed")))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForeground(0, inputData.getString(KEY_TITLE) ?: "Download", 0L, 0L)

    // ---- Progressive (MP4 / direct URL) --------------------------------------

    private suspend fun downloadProgressive(url: String, dir: File, title: String): Long {
        val file = File(dir, "video.mp4")
        val existing = if (file.exists()) file.length() else 0L

        val builder = Request.Builder().url(url).get().header("User-Agent", UA)
        if (existing > 0) builder.header("Range", "bytes=$existing-")

        Http.client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty response body")
            val append = response.code == 206 && existing > 0
            val contentLength = body.contentLength()
            val total = when {
                append -> existing + contentLength
                contentLength > 0 -> contentLength
                else -> -1L
            }
            var written = if (append) existing else 0L

            body.byteStream().use { input ->
                FileOutputStream(file, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped) throw CancellationException("Download cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        val percent = if (total > 0) ((written * 100) / total).toInt().coerceIn(0, 99) else 0
                        report(percent, written, if (total > 0) total else 0L, title)
                    }
                }
            }
            return written
        }
    }

    // ---- HLS (.m3u8) ---------------------------------------------------------

    private suspend fun downloadHls(url: String, dir: File, title: String): Long {
        val master = httpGet(url)
        val mediaUrl = if (HlsPlaylist.isMaster(master)) HlsPlaylist.pickVariant(master, url) ?: url else url
        val media = if (mediaUrl != url) httpGet(mediaUrl) else master

        val uris = HlsPlaylist.segmentUris(media, mediaUrl)
        if (uris.isEmpty()) throw IOException("No HLS segments found")
        val names = HlsPlaylist.segmentNames(uris)

        var bytes = 0L
        for (i in uris.indices) {
            if (isStopped) throw CancellationException("Download cancelled")
            val segment = File(dir, names[i])
            if (!segment.exists() || segment.length() == 0L) {
                segment.writeBytes(httpGetBytes(uris[i]))
            }
            bytes += segment.length()
            val percent = (((i + 1) * 100) / uris.size).coerceIn(0, 99)
            report(percent, bytes, 0L, title)
        }
        File(dir, "local.m3u8").writeText(HlsPlaylist.localPlaylist(media, mediaUrl, names))
        return bytes
    }

    // ---- Progress + notification --------------------------------------------

    private suspend fun report(progressPercent: Int, bytes: Long, totalBytes: Long, title: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && lastTick != 0L && now - lastTick < 500) return
        val dt = (now - lastTick).coerceAtLeast(1L)
        val speed = if (lastTick == 0L) 0L else ((bytes - lastBytes) * 1000L) / dt
        val eta = if (speed > 0 && totalBytes > bytes) (totalBytes - bytes) / speed else 0L
        lastBytes = bytes
        lastTick = now

        setProgress(
            workDataOf(
                KEY_PROGRESS to progressPercent,
                KEY_BYTES to bytes,
                KEY_TOTAL to totalBytes,
                KEY_SPEED to speed,
                KEY_ETA to eta,
            )
        )
        runCatching { setForeground(buildForeground(progressPercent, title, bytes, totalBytes)) }
    }

    private fun buildForeground(progress: Int, title: String, bytes: Long, total: Long): ForegroundInfo {
        val clamped = progress.coerceIn(0, 100)
        val status = buildString {
            if (clamped in 1..99) append("$clamped% · ")
            append(formatBytes(bytes))
            if (total > bytes && total > 0) append(" / ${formatBytes(total)}")
        }
        val cancelIntent = PendingIntent.getBroadcast(
            applicationContext,
            inputData.getString(KEY_ID)?.hashCode() ?: NOTIFICATION_ID,
            Intent(applicationContext, VideoDownloadActionReceiver::class.java)
                .setAction(VideoDownloadActionReceiver.ACTION_CANCEL)
                .putExtra(KEY_ID, inputData.getString(KEY_ID)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, clamped, clamped <= 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

/** Handles the notification's "Cancel" action by cancelling the corresponding unique work. */
class VideoDownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL) return
        val id = intent.getStringExtra(KEY_ID) ?: return
        runCatching {
            WorkManager.getInstance(context)
                .cancelUniqueWork(VideoDownloadContract.workName(id))
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.novastream.app.action.CANCEL_DOWNLOAD"
    }
}
