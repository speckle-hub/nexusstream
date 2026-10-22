package com.moovio.app.di

import android.content.Context
import com.moovio.app.data.download.MangaDownloadManager
import com.moovio.app.data.download.VideoDownloadManager
import com.moovio.app.data.ext.ExtensionRepository
import com.moovio.app.data.ext.ExtensionStore
import com.moovio.app.data.integrations.BackupManager
import com.moovio.app.data.integrations.ScrobbleManager
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.local.LibraryStore
import com.moovio.app.data.local.MetadataCache
import com.moovio.app.data.local.SettingsStore
import com.moovio.app.data.repo.AddonRepository
import com.moovio.app.data.repo.CatalogRepository
import com.moovio.app.data.repo.CloudStreamRepository
import com.moovio.app.data.repo.MetadataRepository
import com.moovio.app.data.repo.StreamRepository

/** Lightweight manual dependency container (no annotation processing required). */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val addonStore = AddonStore(context)
    val libraryStore = LibraryStore(context)
    val cache = MetadataCache(context)

    // Dynamic extension execution engine (DEX/APK loading, sandboxed to app-private storage).
    val extensionStore = ExtensionStore(context)
    val extensionRepository = ExtensionRepository(extensionStore, context, settings)

    // Offline download engines: manga chapter images and video streams.
    val mangaDownloadManager = MangaDownloadManager(context)
    val videoDownloadManager = VideoDownloadManager(context)

    val addonRepository = AddonRepository(addonStore)

    // Runs real CloudStream .cs3 provider plugins in-process (see data/cloudstream).
    val cloudStreamRepository = CloudStreamRepository(addonStore, context.applicationContext)

    val catalogRepository = CatalogRepository(addonRepository, settings, cloudStreamRepository)
    val metadataRepository = MetadataRepository(addonRepository, cloudStreamRepository)
    val streamRepository = StreamRepository(addonRepository, cloudStreamRepository)

    // Phase 12 integrations.
    val scrobbleManager = ScrobbleManager(settings)
    val backupManager = BackupManager(context.applicationContext, libraryStore, settings)
}
