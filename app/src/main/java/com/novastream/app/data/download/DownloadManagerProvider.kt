package com.novastream.app.data.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import com.novastream.app.data.remote.UA
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Process-wide holder for the Media3 offline stack — the [SimpleCache], its database, and the
 * [DownloadManager].
 *
 * Both the download [NovaDownloadService] and the player must share **one** cache instance (a
 * second one over the same directory throws), so everything is created together, once, behind a
 * double-checked lock and cached against the application context.
 */
@UnstableApi
object DownloadManagerProvider {

    /** Cache ceiling for downloaded media (separate from the image/metadata caches). */
    private const val MAX_CACHE_BYTES = 4L * 1024 * 1024 * 1024 // 4 GB

    private class Holder(context: Context) {
        val database = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(
            File(context.filesDir, "downloads/media"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            database,
        )
        val downloadManager = DownloadManager(
            context,
            database,
            cache,
            DefaultHttpDataSource.Factory()
                .setUserAgent(UA)
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true),
            executor,
        ).apply {
            setMaxParallelDownloads(3)
            setRequirements(Requirements(Requirements.NETWORK))
        }
    }

    private val executor: ExecutorService by lazy { Executors.newFixedThreadPool(4) }

    @Volatile
    private var holder: Holder? = null

    private fun holder(context: Context): Holder {
        holder?.let { return it }
        synchronized(this) {
            holder?.let { return it }
            return Holder(context.applicationContext).also { holder = it }
        }
    }

    fun downloadManager(context: Context): DownloadManager = holder(context).downloadManager

    fun cache(context: Context): SimpleCache = holder(context).cache
}
