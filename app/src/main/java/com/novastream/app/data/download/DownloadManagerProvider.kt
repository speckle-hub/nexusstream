package com.novastream.app.data.download

import android.content.Context
import android.os.Environment
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.offline.DownloaderFactory
import androidx.media3.exoplayer.offline.WritableDownloadIndex
import androidx.media3.exoplayer.scheduler.Requirements
import com.novastream.app.data.remote.Http
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
 *
 * Downloads are built through [HeaderAwareDownloaderFactory] so each request carries the stream's
 * own HTTP headers (Referer / Cookie / UA) — the player already did this, but the downloader did
 * not, which is why header-gated hosts failed offline.
 */
@UnstableApi
object DownloadManagerProvider {

    /** Cache ceiling for downloaded media (separate from the image/metadata caches). */
    private const val MAX_CACHE_BYTES = 4L * 1024 * 1024 * 1024 // 4 GB

    /** Retries per download before it is marked failed (mirror failover then takes over). */
    private const val MIN_RETRY_COUNT = 4

    /** Fallback concurrency when the user's max-parallel setting hasn't been applied yet. */
    private const val DEFAULT_MAX_PARALLEL = 3

    /**
     * The user's max-parallel-downloads setting, mirrored here so a newly-created [Holder] starts
     * with it. Kept in sync from `NovaApp`, which collects `SettingsStore.maxParallel`.
     */
    @Volatile
    private var maxParallel: Int = DEFAULT_MAX_PARALLEL

    private val executor: ExecutorService by lazy { Executors.newFixedThreadPool(4) }

    private class Holder(context: Context) {
        val database = StandaloneDatabaseProvider(context)
        val dir: File = resolveCacheDir(context)
        val cache = SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            database,
        )
        val downloadIndex: WritableDownloadIndex = DefaultDownloadIndex(database)
        val downloadManager = DownloadManager(
            context,
            downloadIndex,
            HeaderAwareDownloaderFactory(cache, executor),
        ).apply {
            setMaxParallelDownloads(maxParallel)
            setMinRetryCount(MIN_RETRY_COUNT)
            setRequirements(Requirements(Requirements.NETWORK))
        }
    }

    /**
     * Apply the user's "Max parallel downloads" setting. Safe to call before or after the manager
     * exists: it is stored for future holders and pushed onto the live [DownloadManager] when one
     * already exists.
     */
    fun setMaxParallel(context: Context, value: Int) {
        val clamped = value.coerceIn(1, 6)
        maxParallel = clamped
        runCatching { downloadManager(context).setMaxParallelDownloads(clamped) }
    }

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

    /** Seeds a [Holder] if none exists yet, without forcing cache creation any other way. */
    fun ensureInitialized(context: Context) {
        holder(context)
    }

    fun cache(context: Context): SimpleCache = holder(context).cache

    /**
     * The directory the Media3 cache actually lives in, after the writability probe. Used by the
     * orphan scanner so it inspects the real cache and not a stale path.
     */
    fun cacheDir(context: Context): File = holder(context).dir

    /**
     * Resolve the cache directory, degrading gracefully when a location cannot be written:
     *
     *  1. app-private storage (`filesDir`) — the historic location, so existing downloads survive
     *     the upgrade;
     *  2. app-specific external storage (`getExternalFilesDir`) — no runtime permission needed, but
     *     can be unmounted/unavailable;
     *  3. the internal cache dir as a last resort.
     *
     * SimpleCache is constructed against a fixed directory, so this only runs once per process.
     */
    private fun resolveCacheDir(context: Context): File {
        val primary = File(context.filesDir, "downloads/media")
        if (isWritable(primary)) return primary
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { ext ->
            val external = File(ext, "media")
            if (isWritable(external)) return external
        }
        val fallback = File(context.cacheDir, "downloads/media")
        if (isWritable(fallback)) return fallback
        return primary
    }

    private fun isWritable(dir: File): Boolean = try {
        if (!dir.exists() && !dir.mkdirs()) {
            false
        } else {
            val probe = File(dir, ".write_probe")
            probe.writeText("ok")
            probe.delete()
            true
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Creates a [Downloader] per request whose upstream HTTP source sends that request's headers.
 *
 * Media3's [DownloadRequest] has no header field, so per-download headers are carried in the
 * request's `data` payload (see [VideoDownloadMeta]) and decoded here. A fresh
 * [DefaultDownloaderFactory] is built per download because the header map is baked into the
 * upstream `DataSource.Factory`; this still shares the single process-wide [Cache].
 *
 * Routing through a custom [DownloaderFactory] (rather than a single global data source) means HLS
 * segments, key requests and progressive range requests all inherit the same Referer/Cookie the
 * player sends — not just the first playlist URL.
 */
@UnstableApi
private class HeaderAwareDownloaderFactory(
    private val cache: Cache,
    private val executor: ExecutorService,
) : DownloaderFactory {

    override fun createDownloader(request: DownloadRequest): Downloader {
        val meta = decodeDownloadMeta(request)
        val headers = downloadRequestHeaders(
            streamHeaders = meta?.headers ?: emptyMap(),
            customUserAgent = Http.customUserAgent,
            customReferer = Http.customReferer,
            customCookie = Http.customCookie,
        )
        val upstream = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DefaultDownloaderFactory(cacheDataSource, executor).createDownloader(request)
    }
}

/**
 * Decode the [VideoDownloadMeta] stored in a request's `data`. Returns null when the payload is
 * absent or unreadable (e.g. a download created before this schema existed).
 */
internal fun decodeDownloadMeta(request: DownloadRequest): VideoDownloadMeta? =
    request.data?.let {
        runCatching { Http.gson.fromJson(String(it), VideoDownloadMeta::class.java) }.getOrNull()
    }

/** Convenience for the [Download] wrapper. */
internal fun Download.meta(): VideoDownloadMeta? = decodeDownloadMeta(request)
