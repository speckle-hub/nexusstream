package com.novastream.app.data.repo

import com.novastream.app.data.local.AddonStore
import com.novastream.app.data.model.Addon
import com.novastream.app.data.model.AddonKind
import com.novastream.app.data.model.CloudStreamExt
import com.novastream.app.data.model.MangaExt
import com.novastream.app.data.model.RemoteRepo
import com.novastream.app.data.remote.AniyomiRepo
import com.novastream.app.data.remote.CloudStreamRepo
import com.novastream.app.data.remote.KeiyoushiRepo
import com.novastream.app.data.remote.StremioClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Manages Stremio addons and the three external provider repositories. */
class AddonRepository(private val store: AddonStore) {

    val addons: Flow<List<Addon>> = store.addons
    val repos: Flow<List<RemoteRepo>> = store.repos
    val cloudStreamInstalled: Flow<List<CloudStreamExt>> = store.cloudStreamInstalled
    val mangaInstalled: Flow<List<MangaExt>> = store.mangaInstalled

    // ---- Stremio addons -----------------------------------------------------

    suspend fun install(url: String): Result<Addon> = withContext(Dispatchers.IO) {
        runCatching {
            val addon = StremioClient.fetchManifest(url.trim())
            val current = store.addons.first().toMutableList()
            current.removeAll { it.id == addon.id }
            current.add(addon)
            store.saveAddons(current)
            addon
        }
    }

    suspend fun installMany(urls: List<String>): List<Addon> {
        val installed = mutableListOf<Addon>()
        for (u in urls) install(u).onSuccess { installed.add(it) }
        return installed
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val current = store.addons.first().toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) {
            current[idx] = current[idx].copy(enabled = enabled)
            store.saveAddons(current)
        }
    }

    suspend fun remove(id: String) {
        val current = store.addons.first().toMutableList()
        current.removeAll { it.id == id }
        store.saveAddons(current)
    }

    suspend fun update(id: String): Result<Addon> = withContext(Dispatchers.IO) {
        runCatching {
            val current = store.addons.first()
            val existing = current.firstOrNull { it.id == id } ?: error("not installed")
            val fresh = StremioClient.fetchManifest(existing.transportUrl)
            val list = current.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            list[idx] = fresh.copy(enabled = existing.enabled)
            store.saveAddons(list)
            fresh
        }
    }

    suspend fun updateAll() {
        store.addons.first().forEach { update(it.id) }
    }

    suspend fun ensureDefaults() {
        // Provider repositories are cheap to seed (they only power the Add-on Manager browser) and
        // are merged in for existing installs too, so no one has to hunt for Phisher/Hexated/etc.
        ensureDefaultRepos()
        val current = store.addons.first()
        if (current.isEmpty()) {
            installMany(AddonStore.Defaults.stremioAddons.distinct())
            return
        }
        // Migration: drop add-ons hosted on retired/dead domains and re-seed the current stream
        // providers so existing installs keep a working playback source. Also make sure the
        // first-class stream providers (Torrentio, TPB+) are present even mid-failure.
        val retired = AddonStore.Defaults.retiredHosts
        val removed = current.filter { addon -> retired.any { addon.transportUrl.contains(it) } }
        if (removed.isNotEmpty()) store.saveAddons(current - removed.toSet())
        val present = (if (removed.isEmpty()) current else store.addons.first()).map { it.transportUrl }
        val missing = AddonStore.Defaults.streamAddons.filter { url -> present.none { it.contains(hostOf(url)) } }
        if (missing.isNotEmpty()) installMany(missing.distinct())
    }

    /**
     * Merge any missing default provider repositories into the stored list, after dropping any
     * [AddonStore.Defaults.retiredRepoUrls] (dead seed URLs) so an upgraded install ends up with
     * the live mirror instead of a 404ing entry.
     */
    suspend fun ensureDefaultRepos() {
        val stored = store.repos.first().toMutableList()
        var changed = false
        val retired = AddonStore.Defaults.retiredRepoUrls
        if (retired.isNotEmpty()) {
            val before = stored.size
            stored.removeAll { repo -> retired.any { it == repo.url } }
            if (stored.size != before) changed = true
        }
        AddonStore.Defaults.repos.forEach { repo ->
            if (stored.none { it.url == repo.url }) {
                stored.add(repo)
                changed = true
            }
        }
        if (changed) store.saveRepos(stored)
    }

    /** "torrentio" from "https://torrentio.strem.fun/manifest.json" — used for duplicate checks. */
    private fun hostOf(url: String): String = url
        .substringAfter("://", url)
        .substringBefore('/')
        .substringBefore(':')

    // ---- Repos --------------------------------------------------------------

    suspend fun addRepo(repo: RemoteRepo) {
        val list = store.repos.first().toMutableList()
        if (list.none { it.url == repo.url }) {
            list.add(repo)
            store.saveRepos(list)
        }
    }

    suspend fun removeRepo(url: String) {
        val list = store.repos.first().toMutableList()
        list.removeAll { it.url == url }
        store.saveRepos(list)
    }

    // ---- Provider extension catalogues -------------------------------------

    suspend fun fetchCloudStream(repoUrl: String): List<CloudStreamExt> =
        withContext(Dispatchers.IO) { runCatching { CloudStreamRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun fetchAniyomi(repoUrl: String): List<MangaExt> =
        withContext(Dispatchers.IO) { runCatching { AniyomiRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun fetchKeiyoushi(repoUrl: String): List<MangaExt> =
        withContext(Dispatchers.IO) { runCatching { KeiyoushiRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun installCloudStream(ext: CloudStreamExt) {
        val list = store.cloudStreamInstalled.first().toMutableList()
        list.removeAll { it.url == ext.url }
        list.add(ext)
        store.saveCloudStream(list)
    }

    suspend fun installMangaExt(ext: MangaExt) {
        val list = store.mangaInstalled.first().toMutableList()
        list.removeAll { it.pkg == ext.pkg }
        list.add(ext)
        store.saveMangaExt(list)
    }

    suspend fun uninstallCloudStream(url: String) {
        val list = store.cloudStreamInstalled.first().toMutableList()
        list.removeAll { it.url == url }
        store.saveCloudStream(list)
    }

    suspend fun uninstallMangaExt(pkg: String) {
        val list = store.mangaInstalled.first().toMutableList()
        list.removeAll { it.pkg == pkg }
        store.saveMangaExt(list)
    }

    /**
     * Refresh every configured provider repository and warm the on-disk cache. Returns the number
     * of repositories that returned a non-empty catalogue. Used by the background worker and the
     * Add-on Manager's manual sync.
     */
    suspend fun syncAllRepos(): Int {
        val repositories = store.repos.first()
        var synced = 0
        for (repo in repositories) {
            val catalogue = when (repo.kind) {
                AddonKind.CLOUDSTREAM -> fetchCloudStream(repo.url)
                AddonKind.ANIYOMI -> fetchAniyomi(repo.url)
                AddonKind.KEIYOUSHI -> fetchKeiyoushi(repo.url)
                AddonKind.STREMIO -> emptyList()
            }
            if (catalogue.isNotEmpty()) synced++
        }
        return synced
    }

    /** All enabled Stremio addons that declare a given resource. */
    suspend fun enabledWithResource(resource: String): List<Addon> =
        store.addons.first().filter { it.enabled && (it.resources.isEmpty() || it.resources.contains(resource)) }

    suspend fun enabledAddons(): List<Addon> = store.addons.first().filter { it.enabled }
}
