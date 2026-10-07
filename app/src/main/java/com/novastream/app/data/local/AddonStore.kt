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
        /**
         * The app ships source-neutral: no provider repositories and no stream providers are
         * preinstalled. Only legal metadata (Cinemeta) and subtitle (OpenSubtitles) add-ons are
         * seeded; users add their own repositories and add-ons in the Add-on Manager.
         */
        val repos: List<RemoteRepo> = emptyList()

        /** Metadata catalogs (posters, descriptions, episode lists). */
        val metadataAddons: List<String> = listOf(
            "https://v3-cinemeta.strem.io/manifest.json",
        )

        /** No stream providers are seeded; users install their own via the Add-on Manager. */
        val streamAddons: List<String> = emptyList()

        /** Subtitle providers. */
        val subtitleAddons: List<String> = listOf(
            "https://opensubtitles-v3.strem.io/manifest.json",
        )

        val stremioAddons: List<String> = metadataAddons + streamAddons + subtitleAddons

        /**
         * Legal, configuration-free add-ons offered as one-tap (re)installs in the Add-on
         * Manager, so a user who removes one of the seeded add-ons can get it back.
         */
        val recommendedStreamAddons: List<Pair<String, String>> = listOf(
            "Cinemeta" to "https://v3-cinemeta.strem.io/manifest.json",
            "OpenSubtitles v3" to "https://opensubtitles-v3.strem.io/manifest.json",
        )

        /**
         * Reliability order for stream ranking: streams from these add-ons are surfaced first.
         * Empty by default — the app ships no preferred providers, so ranking falls back to
         * quality, then seeders. Matched case-insensitively against the add-on name/id.
         */
        val preferredStreamHosts: List<String> = emptyList()

        /**
         * Hosts that used to be seeded but must no longer be: existing installs are pruned of
         * these on startup. Includes the stream providers removed in the source-neutrality pass,
         * so an upgraded install matches a fresh one.
         */
        val retiredHosts: List<String> = listOf(
            "v3-chill.strem.io",
            "torrentio.strem.fun",
            "thepiratebay-plus.strem.fun",
            "mediafusion.elfhosted.com",
        )

        /**
         * Provider-repo URLs that used to be seeded. [com.novastream.app.data.repo.AddonRepository.ensureDefaultRepos]
         * prunes these from existing installs — both dead URLs (the old Hexated org) and every
         * repository removed in the source-neutrality pass.
         */
        val retiredRepoUrls: List<String> = listOf(
            "https://raw.githubusercontent.com/hexated/cloudstream-extensions-hexated/builds/repo.json",
            "https://raw.githubusercontent.com/JoeTinnySpace/cloudstream-extensions-hexated/builds/repo.json",
            "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json",
            "https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json",
            "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json",
            "https://raw.githubusercontent.com/aniyomiorg/aniyomi-extensions/repo/index.min.json",
            "https://github.com/keiyoushi/extensions/raw/repo/index.pb",
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
        )
    }
}
