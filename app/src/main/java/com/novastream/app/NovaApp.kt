package com.novastream.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.novastream.app.data.remote.Http
import com.novastream.app.data.remote.TorrentStreamer
import com.novastream.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Number of activities currently started — reaches 0 only when the whole app is backgrounded. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Give the shared HTTP layer access to the on-disk metadata cache.
        Http.cache = container.cache
        // Seed default addons on first launch.
        appScope.launch {
            runCatching { container.addonRepository.ensureDefaults() }
        }
        // Warm up the in-app torrent engine so Real 18+ / P2P streams play instantly.
        TorrentStreamer.warmUp(this)
        registerNsfwAutoLock()
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
