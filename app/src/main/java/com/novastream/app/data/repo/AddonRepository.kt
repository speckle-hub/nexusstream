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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Manages Stremio addons and any user-added external provider repositories. */
class AddonRepository(private val store: AddonStore) {

    /**
     * Phase 34: serializes every read-modify-write on the stored lists. Rapid toggling
     * (enable/disable) from the Add-on Manager used to interleave `first()` + `saveAddons()`,
     * losing writes and surfacing NPE / ConcurrentModificationException crashes.
     */
    private val mutationMutex = Mutex()

    val addons: Flow<List<Addon>> = store.addons
    val repos: Flow<List<RemoteRepo>> = store.repos
    val cloudStreamInstalled: Flow<List<CloudStreamExt>> = store.cloudStreamInstalled
    val mangaInstalled: Flow<List<MangaExt>> = store.mangaInstalled

    // ---- Stremio addons -----------------------------------------------------

    suspend fun install(url: String): Result<Addon> = withContext(Dispatchers.IO) {
        runCatching {
            val addon = StremioClient.fetchManifest(url.trim())
            mutationMutex.withLock {
                val current = runCatching { store.addons.first() }.getOrDefault(emptyList()).toMutableList()
                current.removeAll { it.id == addon.id }
                current.add(addon)
                store.saveAddons(current.toList())
            }
            addon
        }
    }

    suspend fun installMany(urls: List<String>): List<Addon> {
        val installed = mutableListOf<Addon>()
        for (u in urls) install(u).onSuccess { installed.add(it) }
        return installed
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        runCatching {
            mutationMutex.withLock {
                val current = store.addons.first().toMutableList()
                val idx = current.indexOfFirst { runCatching { it.id == id }.getOrDefault(false) }
                if (idx >= 0) {
                    current[idx] = runCatching { current[idx].copy(enabled = enabled) }.getOrNull() ?: return@withLock
                    store.saveAddons(current.toList())
                }
            }
        }
    }

    suspend fun remove(id: String) {
        runCatching {
            mutationMutex.withLock {
                val current = runCatching { store.addons.first() }.getOrDefault(emptyList()).toMutableList()
                current.removeAll { runCatching { it.id == id }.getOrDefault(false) }
                store.saveAddons(current.toList())
            }
        }
    }

    suspend fun update(id: String): Result<Addon> = withContext(Dispatchers.IO) {
        runCatching {
            val current = runCatching { store.addons.first() }.getOrDefault(emptyList())
            val existing = current.firstOrNull { runCatching { it.id == id }.getOrDefault(false) } ?: error("not installed")
            val fresh = StremioClient.fetchManifest(existing.transportUrl)
            mutationMutex.withLock {
                val list = runCatching { store.addons.first() }.getOrDefault(emptyList()).toMutableList()
                val idx = list.indexOfFirst { runCatching { it.id == id }.getOrDefault(false) }
                if (idx >= 0) {
                    list[idx] = runCatching { fresh.copy(enabled = existing.enabled) }.getOrNull() ?: fresh
                    store.saveAddons(list.toList())
                }
            }
            fresh
        }
    }

    suspend fun updateAll() {
        // Phase 34: snapshot ids first — iterating the live list while update() rewrites the
        // store was the ConcurrentModificationException path.
        val ids = runCatching { store.addons.first().mapNotNull { runCatching { it.id }.getOrNull() } }
            .getOrDefault(emptyList()).toList()
        ids.forEach { runCatching { update(it) } }
    }

    suspend fun ensureDefaults() {
        // Prune any retired/dead seed repos from existing installs (the app no longer seeds
        // provider repositories; users add their own in the Add-on Manager).
        ensureDefaultRepos()
        val current = runCatching { store.addons.first() }.getOrDefault(emptyList())
        if (current.isEmpty()) {
            installMany(AddonStore.Defaults.stremioAddons.distinct())
            return
        }
        // Migration: drop add-ons hosted on retired domains (dead hosts, and the stream
        // providers removed in the source-neutrality pass), then re-seed any missing default
        // add-ons (metadata + subtitles only) so existing installs keep those working.
        val retired = AddonStore.Defaults.retiredHosts
        val removed = current.filter { addon ->
            runCatching { retired.any { addon.transportUrl.contains(it) } }.getOrDefault(false)
        }
        if (removed.isNotEmpty()) {
            mutationMutex.withLock {
                val fresh = runCatching { store.addons.first() }.getOrDefault(emptyList())
                store.saveAddons((fresh - removed.toSet()).toList())
            }
        }
        val present = runCatching {
            (if (removed.isEmpty()) current else store.addons.first()).map { it.transportUrl }
        }.getOrDefault(emptyList())
        val missing = AddonStore.Defaults.streamAddons.filter { url -> present.none { it.contains(hostOf(url)) } }
        if (missing.isNotEmpty()) installMany(missing.distinct())
    }

    /**
     * Merge any missing default provider repositories into the stored list, after dropping any
     * [AddonStore.Defaults.retiredRepoUrls]. The default repo list is empty (source-neutral
     * shipping), so in practice this only prunes the retired seed URLs from existing installs.
     */
    suspend fun ensureDefaultRepos() {
        mutationMutex.withLock {
            val stored = runCatching { store.repos.first() }.getOrDefault(emptyList()).toMutableList()
            var changed = false
            val retired = AddonStore.Defaults.retiredRepoUrls
            if (retired.isNotEmpty()) {
                val before = stored.size
                stored.removeAll { repo -> runCatching { retired.any { it == repo.url } }.getOrDefault(false) }
                if (stored.size != before) changed = true
            }
            AddonStore.Defaults.repos.forEach { repo ->
                if (stored.none { runCatching { it.url == repo.url }.getOrDefault(false) }) {
                    stored.add(repo)
                    changed = true
                }
            }
            if (changed) store.saveRepos(stored.toList())
        }
    }

    /** "example.com" from "https://example.com/manifest.json" — used for duplicate checks. */
    private fun hostOf(url: String): String = url
        .substringAfter("://", url)
        .substringBefore('/')
        .substringBefore(':')

    // ---- Repos --------------------------------------------------------------

    suspend fun addRepo(repo: RemoteRepo) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.repos.first() }.getOrDefault(emptyList()).toMutableList()
                if (list.none { runCatching { it.url == repo.url }.getOrDefault(false) }) {
                    list.add(repo)
                    store.saveRepos(list.toList())
                }
            }
        }
    }

    suspend fun removeRepo(url: String) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.repos.first() }.getOrDefault(emptyList()).toMutableList()
                list.removeAll { runCatching { it.url == url }.getOrDefault(false) }
                store.saveRepos(list.toList())
            }
        }
    }

    // ---- Provider extension catalogues -------------------------------------

    suspend fun fetchCloudStream(repoUrl: String): List<CloudStreamExt> =
        withContext(Dispatchers.IO) { runCatching { CloudStreamRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun fetchAniyomi(repoUrl: String): List<MangaExt> =
        withContext(Dispatchers.IO) { runCatching { AniyomiRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun fetchKeiyoushi(repoUrl: String): List<MangaExt> =
        withContext(Dispatchers.IO) { runCatching { KeiyoushiRepo.fetch(repoUrl) }.getOrDefault(emptyList()) }

    suspend fun installCloudStream(ext: CloudStreamExt) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.cloudStreamInstalled.first() }.getOrDefault(emptyList()).toMutableList()
                list.removeAll { runCatching { it.url == ext.url }.getOrDefault(false) }
                list.add(ext)
                store.saveCloudStream(list.toList())
            }
        }
    }

    suspend fun installMangaExt(ext: MangaExt) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.mangaInstalled.first() }.getOrDefault(emptyList()).toMutableList()
                list.removeAll { runCatching { it.pkg == ext.pkg }.getOrDefault(false) }
                list.add(ext)
                store.saveMangaExt(list.toList())
            }
        }
    }

    suspend fun uninstallCloudStream(url: String) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.cloudStreamInstalled.first() }.getOrDefault(emptyList()).toMutableList()
                list.removeAll { runCatching { it.url == url }.getOrDefault(false) }
                store.saveCloudStream(list.toList())
            }
        }
    }

    suspend fun uninstallMangaExt(pkg: String) {
        runCatching {
            mutationMutex.withLock {
                val list = runCatching { store.mangaInstalled.first() }.getOrDefault(emptyList()).toMutableList()
                list.removeAll { runCatching { it.pkg == pkg }.getOrDefault(false) }
                store.saveMangaExt(list.toList())
            }
        }
    }

    /**
     * Refresh every configured provider repository and warm the on-disk cache. Returns the number
     * of repositories that returned a non-empty catalogue. Used by the background worker and the
     * Add-on Manager's manual sync.
     */
    suspend fun syncAllRepos(): Int {
        val repositories = runCatching { store.repos.first() }.getOrDefault(emptyList()).toList()
        var synced = 0
        for (repo in repositories) {
            val kind = runCatching { repo.kind }.getOrNull() ?: continue
            val url = runCatching { repo.url }.getOrNull()?.takeIf { it.isNotBlank() } ?: continue
            val catalogue = when (kind) {
                AddonKind.CLOUDSTREAM -> fetchCloudStream(url)
                AddonKind.ANIYOMI -> fetchAniyomi(url)
                AddonKind.KEIYOUSHI -> fetchKeiyoushi(url)
                AddonKind.STREMIO -> emptyList()
            }
            if (catalogue.isNotEmpty()) synced++
        }
        return synced
    }

    /** All enabled Stremio addons that declare a given resource. */
    suspend fun enabledWithResource(resource: String): List<Addon> =
        runCatching { store.addons.first() }.getOrDefault(emptyList()).filter {
            runCatching { it.enabled && (it.resources.isEmpty() || it.resources.contains(resource)) }
                .getOrDefault(false)
        }.toList()

    suspend fun enabledAddons(): List<Addon> =
        runCatching { store.addons.first() }.getOrDefault(emptyList())
            .filter { runCatching { it.enabled }.getOrDefault(false) }.toList()
}
