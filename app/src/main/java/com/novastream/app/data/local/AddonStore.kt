package com.novastream.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.reflect.TypeToken
import com.novastream.app.data.model.Addon
import com.novastream.app.data.model.CloudStreamExt
import com.novastream.app.data.model.MangaExt
import com.novastream.app.data.model.RemoteRepo
import com.novastream.app.data.remote.Http
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists installed Stremio addons, provider repos, and installed extensions. */
class AddonStore(private val context: Context) {

    private val K_ADDONS = stringPreferencesKey("installed_addons")
    private val K_REPOS = stringPreferencesKey("remote_repos")
    private val K_CS = stringPreferencesKey("installed_cloudstream")
    private val K_MANGA = stringPreferencesKey("installed_manga_ext")

    private inline fun <reified T> decode(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Http.gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val addons: Flow<List<Addon>> = context.dataStore.data.map { decode<Addon>(it[K_ADDONS]) }
    val repos: Flow<List<RemoteRepo>> = context.dataStore.data.map {
        val stored = decode<RemoteRepo>(it[K_REPOS])
        if (stored.isEmpty()) Defaults.repos else stored
    }
    val cloudStreamInstalled: Flow<List<CloudStreamExt>> = context.dataStore.data.map { decode<CloudStreamExt>(it[K_CS]) }
    val mangaInstalled: Flow<List<MangaExt>> = context.dataStore.data.map { decode<MangaExt>(it[K_MANGA]) }

    suspend fun saveAddons(list: List<Addon>) = context.dataStore.edit { it[K_ADDONS] = Http.gson.toJson(list) }
    suspend fun saveRepos(list: List<RemoteRepo>) = context.dataStore.edit { it[K_REPOS] = Http.gson.toJson(list) }
    suspend fun saveCloudStream(list: List<CloudStreamExt>) = context.dataStore.edit { it[K_CS] = Http.gson.toJson(list) }
    suspend fun saveMangaExt(list: List<MangaExt>) = context.dataStore.edit { it[K_MANGA] = Http.gson.toJson(list) }

    object Defaults {
        val repos: List<RemoteRepo> = listOf(
            RemoteRepo("CloudStream (phisher98)", "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json", com.novastream.app.data.model.AddonKind.CLOUDSTREAM),
            RemoteRepo("Aniyomi (yuzono)", "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json", com.novastream.app.data.model.AddonKind.ANIYOMI),
            RemoteRepo("Mihon / Keiyoushi", "https://github.com/keiyoushi/extensions/raw/repo/index.pb", com.novastream.app.data.model.AddonKind.KEIYOUSHI),
        )
        /** Metadata catalogs (posters, descriptions, episode lists). */
        val metadataAddons: List<String> = listOf(
            "https://v3-cinemeta.strem.io/manifest.json",
        )

        /** Stream providers so playback works out of the box (torrent engines are bundled). */
        val streamAddons: List<String> = listOf(
            "https://torrentio.strem.fun/manifest.json",
            "https://thepiratebay-plus.strem.fun/manifest.json",
        )

        /** Subtitle providers. */
        val subtitleAddons: List<String> = listOf(
            "https://opensubtitles-v3.strem.io/manifest.json",
        )

        val stremioAddons: List<String> = metadataAddons + streamAddons + subtitleAddons

        /**
         * Hosts that used to be seeded but are now dead. Existing installs are pruned of these
         * on startup and the current stream providers are re-seeded in their place.
         */
        val retiredHosts: List<String> = listOf(
            "v3-chill.strem.io",
        )
    }
}
