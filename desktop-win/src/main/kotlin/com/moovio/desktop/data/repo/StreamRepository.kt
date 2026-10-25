package com.moovio.desktop.data.repo

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.remote.TtlCache
import com.moovio.desktop.domain.StreamRanking
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import com.moovio.desktop.domain.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class StreamResult(
    val sources: List<StreamSource>,
    val failures: List<AddonFailure> = emptyList(),
) {
    val best: StreamSource? get() = StreamRanking.bestPlayable(sources)
}

/**
 * Queries every enabled stream add-on that accepts the content's type/id, in parallel.
 * Each add-on has its own timeout and error bucket; partial results are always returned.
 */
class StreamRepository(
    private val stremio: StremioClient,
    private val cinemeta: CinemetaClient,
    private val addonStore: AddonStore,
) {
    private val imdbCache = TtlCache<String, String>(ttlMs = 60 * 60_000)

    suspend fun resolve(
        media: MediaItem,
        detail: MetaDetail? = null,
        season: Int? = null,
        episode: Int? = null,
        includeAdult: Boolean = false,
    ): StreamResult = withContext(Dispatchers.IO) {
        val requestId = resolveStremioId(media, detail, season, episode)
        val type = media.type.stremioType
        val addons = eligibleAddons(type, requestId.id, includeAdult)
        val failures = java.util.Collections.synchronizedList(mutableListOf<AddonFailure>())

        val sources = coroutineScope {
            addons.map { addon ->
                async {
                    val result = withTimeoutOrNull(if (addon.builtIn) 8_000 else 12_000) {
                        runCatching { stremio.streams(addon, type, requestId.id) }
                    }
                    when {
                        result == null -> {
                            failures.add(AddonFailure(addon.name, "timed out"))
                            emptyList()
                        }
                        result.isFailure -> {
                            failures.add(
                                AddonFailure(addon.name, result.exceptionOrNull()?.message ?: "request failed"),
                            )
                            emptyList()
                        }
                        else -> result.getOrThrow().map {
                            it.copy(addonId = addon.id, addonName = addon.name)
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        // Only allow adult streams when the gated area (or an explicit adult flow) asked for them.
        val filtered = sources.filter { stream ->
            val addon = addons.firstOrNull { it.id == stream.addonId }
            includeAdult || addon?.adult != true
        }

        StreamResult(StreamRanking.rank(filtered), failures.toList())
    }

    private fun eligibleAddons(type: String, id: String, includeAdult: Boolean): List<Addon> =
        addonStore.current.filter { addon ->
            addon.enabled &&
                addon.supportsStreams &&
                (includeAdult || !addon.adult) &&
                (addon.types.isEmpty() || addon.types.any { it.equals(type, true) }) &&
                (addon.idPrefixes.isEmpty() || addon.idPrefixes.any { id.startsWith(it) })
        }

    /**
     * Stream add-ons key off IMDb/kitsu ids. AniList cards only carry `anilist:<id>`, so resolve
     * an IMDb id from the detail payload or a title search before asking for streams.
     */
    private suspend fun resolveStremioId(
        media: MediaItem,
        detail: MetaDetail?,
        season: Int?,
        episode: Int?,
    ): StremioId {
        val base = when {
            detail?.imdbId != null -> detail.imdbId
            media.imdbId != null -> media.imdbId
            media.id.startsWith("tt") -> media.id
            media.id.startsWith("anilist:") -> resolveImdbForAnime(media, detail)
            else -> media.id
        }

        val isSeries = media.type != MediaType.MOVIE
        val id = if (isSeries && season != null && episode != null) {
            detail?.episodes
                ?.firstOrNull { ep -> ep.season == season && ep.episode == episode }
                ?.id
                ?.takeIf { it.contains(":") && season != 0 }
                ?: "$base:$season:$episode"
        } else {
            base
        }
        return StremioId(id)
    }

    private suspend fun resolveImdbForAnime(media: MediaItem, detail: MetaDetail?): String {
        val key = media.name.lowercase() + "|" + (media.year ?: 0)
        imdbCache.get(key)?.let { return it.ifBlank { media.id } }
        val candidates = runCatching {
            cinemeta.search(MediaType.SERIES, media.name) + cinemeta.search(MediaType.MOVIE, media.name)
        }.getOrDefault(emptyList())
        val normalized = normalize(media.name)
        val match = candidates.firstOrNull { normalize(it.name) == normalized && (media.year == null || it.year == media.year) }
            ?: candidates.firstOrNull { normalize(it.name) == normalized }
            ?: candidates.firstOrNull()
        val imdb = match?.id
        imdbCache.put(key, imdb.orEmpty())
        return imdb ?: media.id
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private data class StremioId(val id: String)
}
