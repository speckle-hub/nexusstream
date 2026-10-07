package com.novastream.app.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novastream.app.NovaApp
import com.novastream.app.R
import com.novastream.app.data.remote.Http
import java.io.File

/**
 * Periodic "new episodes / chapters" check (Phase 12).
 *
 * Pulls the current home feed, diffs the title set against what the last run saw, and posts a
 * notification when genuinely new items appear. The seen-set lives in a small JSON file rather than
 * DataStore, so the worker stays self-contained. No-ops entirely when the setting is off.
 */
class NewContentWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NovaApp ?: return Result.success()
        if (!app.container.settings.notifyNewContent.value) return Result.success()

        val rows = runCatching { app.container.catalogRepository.homeRows() }.getOrDefault(emptyList())
        val titles = rows.flatMap { it.items }.map { it.title }.filter { it.isNotBlank() }.distinct()
        if (titles.isEmpty()) return Result.success()

        val seenFile = File(applicationContext.filesDir, "notify_seen.json")
        val seen = runCatching {
            Http.gson.fromJson(seenFile.readText(), Array<String>::class.java)?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())
        val fresh = titles.filterNot { it in seen }

        // Only notify once we have a baseline, so the very first run doesn't claim "everything".
        if (seen.isNotEmpty() && fresh.isNotEmpty()) notify(fresh.size, fresh.take(3))
        runCatching { seenFile.writeText(Http.gson.toJson(titles.take(300))) }
        return Result.success()
    }

    private fun notify(count: Int, sample: List<String>) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New content",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_notification)
            .setContentTitle("$count new title${if (count == 1) "" else "s"} on your feed")
            .setContentText(sample.joinToString(" · "))
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        const val UNIQUE_NAME = "new_content_check"
        private const val CHANNEL_ID = "new_content"
        private const val NOTIFICATION_ID = 7301
    }
}
