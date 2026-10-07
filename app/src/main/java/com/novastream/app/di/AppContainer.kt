package com.novastream.app.di

import android.content.Context
import com.novastream.app.data.download.MangaDownloadManager
import com.novastream.app.data.download.VideoDownloadManager
import com.novastream.app.data.ext.ExtensionRepository
import com.novastream.app.data.ext.ExtensionStore
import com.novastream.app.data.integrations.BackupManager
import com.novastream.app.data.integrations.ScrobbleManager
import com.novastream.app.data.local.AddonStore
import com.novastream.app.data.local.LibraryStore
import com.novastream.app.data.local.MetadataCache
import com.novastream.app.data.local.SettingsStore
import com.novastream.app.data.repo.AddonRepository
import com.novastream.app.data.repo.CatalogRepository
import com.novastream.app.data.repo.MetadataRepository
import com.novastream.app.data.repo.StreamRepository

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
    val catalogRepository = CatalogRepository(addonRepository, settings)
    val metadataRepository = MetadataRepository(addonRepository)
    val streamRepository = StreamRepository(addonRepository)

    // Phase 12 integrations.
    val scrobbleManager = ScrobbleManager(settings)
    val backupManager = BackupManager(context.applicationContext, libraryStore, settings)
}
