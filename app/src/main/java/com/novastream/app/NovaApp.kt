package com.novastream.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.novastream.app.data.download.DownloadManagerProvider
import com.novastream.app.data.local.LibraryStore
import com.novastream.app.data.remote.Http
import com.novastream.app.data.remote.TorrentStreamer
import com.novastream.app.data.sync.NewContentWorker
import com.novastream.app.data.sync.RepoSyncWorker
import com.novastream.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NovaApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    /**
     * App-lifetime IO scope. Used for fire-and-forget persistence (e.g. saving playback progress)
     * that must survive the caller's Activity being destroyed.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Number of activities currently started — reaches 0 only when the whole app is backgrounded. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Give the shared HTTP layer access to the on-disk metadata cache.
        Http.cache = container.cache
        // Keep the cache in sync with the "Cache metadata" setting (initial value + live toggles).
        collectSafely(container.settings.cacheMeta) { Http.cacheEnabled = it }
        // Phase 12: mirror the custom HTTP identity + incognito flag into the shared layers.
        Http.customUserAgent = container.settings.customUserAgent.value
        Http.customReferer = container.settings.customReferer.value
        Http.customCookie = container.settings.customCookie.value
        LibraryStore.incognito = container.settings.incognito.value
        collectSafely(container.settings.customUserAgent) { Http.customUserAgent = it }
        collectSafely(container.settings.customReferer) { Http.customReferer = it }
        collectSafely(container.settings.customCookie) { Http.customCookie = it }
        collectSafely(container.settings.incognito) { LibraryStore.incognito = it }
        // Seed default add-ons on first launch. Best-effort, but retried once so a cold boot on a
        // flaky connection still ends up with working defaults instead of an empty Add-on Manager.
        appScope.launch {
            if (runCatching { container.addonRepository.ensureDefaults() }.isFailure) {
                delay(3_000)
                runCatching { container.addonRepository.ensureDefaults() }
            }
        }
        // Load any previously installed dynamic extensions (honouring the disabled list).
        appScope.launch {
            runCatching { container.extensionRepository.reloadAll() }
        }
        // Apply the user's max-parallel-downloads setting to Media3, and keep it live on change.
        collectSafely(container.settings.maxParallel) {
            DownloadManagerProvider.setMaxParallel(this@NovaApp, it)
        }
        // Warm up the in-app torrent engine so Real 18+ / P2P streams play instantly.
        TorrentStreamer.warmUp(this)
        registerNsfwAutoLock()
        scheduleRepoSync()
        scheduleNewContentCheck()
    }

    /**
     * Collects [flow] for the life of the app scope without letting an exception escape.
     *
     * `appScope` has no `CoroutineExceptionHandler`, so a single bad emission (the Media3 cache
     * failing to rebuild, a DataStore read hiccup) reaches the default handler and takes the whole
     * app down — usually right after a cold boot. Cold-boot collectors go through here instead: a
     * failed value is skipped and the flow keeps collecting.
     */
    private fun <T> collectSafely(flow: Flow<T>, onChange: (T) -> Unit) {
        appScope.launch { flow.collect { value -> runCatching { onChange(value) } } }
    }

    /** Periodically probe for new feed content and post a notification when enabled. */
    private fun scheduleNewContentCheck() {
        val request = PeriodicWorkRequestBuilder<NewContentWorker>(6, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        runCatching {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                NewContentWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    /** Enqueue the periodic extension-repository index refresh (network-constrained, 12 h). */
    private fun scheduleRepoSync() {
        val request = PeriodicWorkRequestBuilder<RepoSyncWorker>(12, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        runCatching {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                RepoSyncWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    /**
     * Re-lock NSFW whenever the app leaves the foreground. Counting started activities (rather
     * than reacting to a single Activity's onStop) avoids re-locking when we simply open the
     * player or a second screen on top of the current one.
     */
    private fun registerNsfwAutoLock() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                if (startedActivities > 0) startedActivities--
                if (startedActivities == 0) {
                    appScope.launch { runCatching { container.settings.setNsfwUnlocked(false) } }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** Explicit Coil disk + memory cache so previously loaded artwork shows offline. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
