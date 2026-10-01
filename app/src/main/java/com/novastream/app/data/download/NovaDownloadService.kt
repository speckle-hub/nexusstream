package com.novastream.app.data.download

import android.app.Notification
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
 * [DownloadNotificationHelper] builds the progress notification (Media3 handles the "tap to open"
 * and per-download actions itself via the service's intents).
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

    override fun getDownloadManager(): DownloadManager =
        DownloadManagerProvider.downloadManager(this)

    /** No background scheduler — downloads run while the service is alive/foreground. */
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notificationId: Int): Notification =
        notifier.buildProgressNotification(
            /* context = */ this,
            /* smallIcon = */ R.drawable.ic_download_notification,
            /* contentIntent = */ null,
            /* message = */ getString(R.string.app_name),
            downloads,
            // No requirements are reported as unmet here; the DownloadManager's own
            // setRequirements(...) gates scheduling, and we keep the notification copy simple.
            /* notMetRequirements = */ 0,
        )

    companion object {
        const val CHANNEL_ID = "video_downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 4801
    }
}
