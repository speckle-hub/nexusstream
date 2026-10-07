package com.novastream.app.data.download

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.novastream.app.R

/**
 * Runs Media3 [DownloadManager] work in a foreground service.
 *
 * Backed by [DownloadManagerProvider], which supplies the shared cache + manager. The Media3
 * [DownloadNotificationHelper] builds the progress/completion notification; we supply a content
 * intent so tapping it re-opens the app. Requires `POST_NOTIFICATIONS` on API 33+ (requested in
 * `MainActivity`) — without it the notification never reaches the shade.
 *
 * The base class ties its *foreground* notification to the service, so it disappears the moment the
 * queue drains and the service stops. [completionListener] posts a **separate, persistent**
 * completion notification (its own id, straight through the system `NotificationManager`) for every
 * finished download, so finished downloads stay visible in the shade until the user dismisses them.
 */
@UnstableApi
class NovaDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {

    private val notifier by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    /**
     * Posts a persistent completion notification whenever a download reaches `STATE_COMPLETED`.
     * Registered next to the service so it only runs while the service (the thing driving the
     * downloads) is alive; the notification it posts outlives the service.
     */
    private val completionListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_COMPLETED) postCompletionNotification(download)
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { getDownloadManager().addListener(completionListener) }
    }

    override fun onDestroy() {
        // The posted completion notifications deliberately stay behind — only the listener goes.
        runCatching { getDownloadManager().removeListener(completionListener) }
        super.onDestroy()
    }

    override fun getDownloadManager(): DownloadManager =
        DownloadManagerProvider.downloadManager(this)

    /** No background scheduler — downloads run while the service is alive/foreground. */
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notificationId: Int): Notification =
        notifier.buildProgressNotification(
            /* context = */ this,
            /* smallIcon = */ R.drawable.ic_download_notification,
            /* contentIntent = */ contentIntent(),
            /* message = */ getString(R.string.app_name),
            downloads,
            // No requirements are reported as unmet here; the DownloadManager's own
            // setRequirements(...) gates scheduling, and we keep the notification copy simple.
            /* notMetRequirements = */ 0,
        )

    /**
     * Build and post a completion notification for one finished download. Uses its own id (distinct
     * from the foreground id) so the base class stopping the service never cancels it, and titles it
     * with the download's name so several completions each read clearly in the shade.
     */
    private fun postCompletionNotification(download: Download) {
        // Media3's DownloadRequest has no title; ours travels in the request's data payload.
        val title = decodeDownloadMeta(download.request)?.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val notification = runCatching {
            notifier.buildDownloadCompletedNotification(
                /* context = */ this,
                /* smallIcon = */ R.drawable.ic_download_notification,
                /* contentIntent = */ contentIntent(),
                /* message = */ title,
            )
        }.getOrNull() ?: return
        runCatching {
            NotificationManagerCompat.from(this).notify(completionNotificationId(download), notification)
        }
    }

    /** Stable per-download notification id, offset clear of [FOREGROUND_NOTIFICATION_ID]. */
    private fun completionNotificationId(download: Download): Int =
        COMPLETION_NOTIFICATION_BASE + (download.request.id.hashCode() and 0x7ff)

    /** Tapping the download notification re-opens the app (its launcher activity). */
    private fun contentIntent(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    companion object {
        const val CHANNEL_ID = "video_downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 4801

        /** Per-download completion notification ids live here (…5000–7047), never the foreground id. */
        private const val COMPLETION_NOTIFICATION_BASE = 5000
    }
}
