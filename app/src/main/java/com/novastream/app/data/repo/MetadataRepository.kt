package com.novastream.app.data.repo

import com.novastream.app.data.adult.AdultDetail
import com.novastream.app.data.adult.AdultRepository
import com.novastream.app.data.adult.AdultSource
import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.adult.adultTitlesMatch
import com.novastream.app.data.hentai.HentaiRepository
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.AniListClient
import com.novastream.app.data.remote.JikanClient
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.data.remote.StremioClient
import com.novastream.app.data.remote.TmdbClient
import kotlinx.coroutines.withTimeout

/** Resolves full metadata for any MediaItem regardless of its source. */
class MetadataRepository(private val addonRepo: AddonRepository) {

    /**
     * Resolve the best available metadata for an item. Every candidate source is attempted in
     * priority order; a failing/throwing source never aborts the lookup, and the most complete
     * non-empty result wins. This guarantees the detail screen always has *something* to show.
     */
    suspend fun detail(item: MediaItem): MetaDetail? {
        // Built-in adult sources own their metadata entirely — no TMDB/AniList for Real 18+.
        AdultSources.byId(item.addonId)?.let { source ->
            val adult = runCatching { AdultRepository.detail(item.id, source) }.getOrNull()
            if (adult != null) return adultMeta(source, item, adult)
        }
        val candidates = candidateSources(item)
        var best: MetaDetail? = null
        var bestScore = -1
        for (source in candidates) {
            val d = runCatching {
                // Bound every NSFW-anime candidate: these are normally sub-second, and a hung
                // AniList/Jikan call must degrade to the next source instead of stalling the page.
                if (item.type == MediaType.NSFW_ANIME) withTimeout(NSFW_SOURCE_TIMEOUT_MS) { source() }
                else source()
            }.getOrNull() ?: continue
            if (d.name.isBlank()) continue
            // Never substitute a different title for the one the user picked. A fallback source
            // (Jikan's loose `?q=` search above all) can return a plausible-but-unrelated adult
            // entry; a candidate that isn't recognisably this title is skipped outright instead
            // of being allowed to win on metadata score.
            if (item.type == MediaType.NSFW_ANIME && !sameTitle(item, d)) continue
            val score = score(d, item)
            if (score > bestScore) { best = d; bestScore = score }
            // A fully-loaded result (rich metadata + episodes) is good enough — stop early.
            if (score >= 100) break
            // AniList is the fast primary NSFW-anime source: once it returns a *complete* record
            // (a synopsis) there is no reason to pay for the slower Jikan fallback.
            if (item.type == MediaType.NSFW_ANIME && !d.description.isNullOrBlank()) break
        }
        // Real 18+ titles must never surface a "couldn't load" error: fall back to the
        // catalog item itself (title/poster/backdrop) so the page still renders and plays.
        if (best == null && item.type == MediaType.REAL) return fallbackDetail(item)
        // Same guarantee for NSFW anime: if both AniList and Jikan failed, build the detail from
        // what the built-in hentai sources know (poster/series/episodes), falling back to the
        // catalog item itself. The page still renders and plays instead of hard-failing.
        if (best == null && item.type == MediaType.NSFW_ANIME) return hentaiFallback(item)
        return best
    }

    /**
     * Last-resort NSFW-anime detail: the catalog item's own metadata, enriched with whatever the
     * built-in hentai sources can provide (series/poster + an episode list) so the page never
     * hard-fails. Cached in [HentaiRepository], and
     * `DetailViewModel` skips its own episode fill when [MetaDetail.videos] is already populated.
     */
    private suspend fun hentaiFallback(item: MediaItem): MetaDetail {
        val base = fallbackDetail(item)
        val episodes = runCatching { HentaiRepository.episodes(item.title) }.getOrNull()?.episodes.orEmpty()
        val head = episodes.firstOrNull() ?: return base
        // The page keeps the title and artwork the user actually selected — only the episode list
        // is borrowed from the scrapers. This used to *rename* the page to whatever series the
        // source search returned (`name = head.series`), which is how "Boku to Misaki-sensei"
        // opened as a completely different title. [HentaiRepository] only returns groups that
        // match the query; the check here documents (and enforces) the same rule at the seam.
        if (!adultTitlesMatch(item.title, head.series)) return base
        return base.copy(
            poster = base.poster ?: head.poster,
            background = base.background ?: head.poster ?: base.poster,
            videos = episodes.map { it.toVideo() },
            totalEpisodes = episodes.size,
        )
    }

    /** True when [d] is recognisably the detail of [item] rather than a different title. */
    private fun sameTitle(item: MediaItem, d: MetaDetail): Boolean =
        adultTitlesMatch(item.title, d.name) || d.altTitles.any { adultTitlesMatch(item.title, it) }

    /** Maps a built-in adult source result onto the normal detail model. */
    private fun adultMeta(source: AdultSource, item: MediaItem, detail: AdultDetail): MetaDetail {
        val v = detail.video
        val uploader = detail.uploader ?: v.uploader
        val synopsis = detail.description ?: buildString {
            if (!uploader.isNullOrBlank()) append("Uploaded by $uploader\n")
            if (!v.views.isNullOrBlank()) append("${v.views} views\n")
            if (!v.duration.isNullOrBlank()) append("Duration ${v.duration}")
        }.trim().ifBlank { null }
        return MetaDetail(
            id = item.id,
            type = "real",
            name = v.title.ifBlank { item.title },
            poster = v.thumbnail ?: item.poster,
            background = v.thumbnail ?: item.backdrop ?: item.poster,
            description = synopsis,
            runtime = v.duration,
            cast = listOfNotNull(uploader),
            addonId = source.id,
            recommendations = detail.related.map { it.toMediaItem(source.name) },
            castMembers = listOfNotNull(
                uploader?.let { com.novastream.app.data.model.CastMember(name = it, character = "Uploader") }
            ),
        )
    }

    /** Minimal detail synthesized from a catalog item when no add-on meta is available. */
    private fun fallbackDetail(item: MediaItem): MetaDetail = MetaDetail(
        id = item.id,
        type = item.stremioType ?: "movie",
        name = item.title.ifBlank { "Unknown" },
        poster = item.poster,
        background = item.backdrop ?: item.poster,
        description = item.description,
        releaseInfo = item.year,
        imdbRating = item.rating?.let { String.format("%.1f", it) },
        genres = item.genres,
        addonId = item.addonId,
    )

    /** Ordered list of metadata sources to try for this item. */
    private fun candidateSources(item: MediaItem): List<suspend () -> MetaDetail?> {
        val list = mutableListOf<suspend () -> MetaDetail?>()
        // Real 18+ content comes exclusively from installed NSFW Stremio add-ons.
        // Never touch TMDB / AniList / Jikan (or any regular metadata API) for these titles.
        if (item.type == MediaType.REAL) {
            list += { stremioMeta(item) }
            return list
        }
        when (item.addonId) {
            "tmdb" -> {
                list += { TmdbClient.details(item.type, item.id, item.title) }
                // Wrong-namespace correction (movie <-> tv share id space).
                list += { TmdbClient.details(alt(item.type), item.id, item.title) }
                // Stremio fallback (e.g. Cinemeta) via a resolved IMDb id.
                list += { tmdbViaImdb(item.id) }
                list += { stremioMeta(item) }
            }
            "anilist" -> {
                list += { AniListClient.details(item.id) }
                // Jikan by title is the NSFW-anime fallback when AniList fails or returns a thin
                // record (AniList's id cannot be translated back to MAL without that same call).
                if (item.type == MediaType.NSFW_ANIME) list += { JikanClient.detailsByTitle(item.title) }
            }
            "mangadex" -> {
                list += { MangaDexClient.details(item.id) }
            }
            "jikan" -> {
                list += { JikanClient.details(item.id) }
                list += { AniListClient.details(item.id, byMal = true) }
            }
            else -> {
                // Stremio addon item: try the owning addon, then Cinemeta, then TMDB.
                list += { stremioMeta(item) }
                list += { tmdbViaImdb(item.id) }
                if (item.id.all { it.isDigit() }) {
                    list += { TmdbClient.details(MediaType.SERIES, item.id, item.title) }
                    list += { TmdbClient.details(MediaType.MOVIE, item.id, item.title) }
                }
            }
        }
        // Universal last-resort fallbacks.
        if (item.id.startsWith("tt")) list += { tmdbViaImdb(item.id) }
        return list
    }

    private fun alt(type: MediaType): MediaType = if (type == MediaType.MOVIE) MediaType.SERIES else MediaType.MOVIE

    /**
     * Fetch a Stremio addon's meta. Prefers the owning addon, then (for NSFW content) other
     * NSFW addons, then any meta-capable addon. The item's own declared [MediaItem.stremioType]
     * is reused verbatim, because NSFW addons use non-standard types ("Porn", "hentai", ...).
     */
    private suspend fun stremioMeta(item: MediaItem): MetaDetail? {
        val addons = addonRepo.enabledAddons()
        val stremioType = item.stremioType ?: when (item.type) {
            MediaType.MOVIE -> "movie"
            MediaType.SERIES -> "series"
            MediaType.ANIME -> "anime"
            MediaType.NSFW_ANIME -> "anime"
            MediaType.NSFW_MANGA -> "manga"
            else -> "movie"
        }
        val metaCapable = addons.filter { it.resources.isEmpty() || it.resources.contains("meta") }
        val ordered = when {
            item.type == MediaType.REAL -> metaCapable.filter { it.nsfw }
            else -> metaCapable
        }.sortedByDescending { it.id == item.addonId }
        val isAnime = item.type == MediaType.ANIME || item.type == MediaType.NSFW_ANIME
        val candidates = StremioClient.candidateRequests(stremioType, item.id, isAnime, null)
        for (addon in ordered) {
            val (aType, aId) = StremioClient.chooseRequest(stremioType to item.id, candidates, addon.idPrefixes)
            val d = runCatching { StremioClient.fetchMeta(addon, aType, aId) }.getOrNull()
            if (d != null && d.name.isNotBlank()) return d
        }
        return null
    }

    /** Resolve an IMDb id to TMDB and return its rich detail (seasons, cast, trailer, ...). */
    private suspend fun tmdbViaImdb(imdbId: String): MetaDetail? {
        val found = TmdbClient.findByImdb(imdbId) ?: return null
        return TmdbClient.details(found.first, found.second)
    }

    /** Higher is better. Rewards rich metadata and, for series, a populated episode list. */
    private fun score(d: MetaDetail, item: MediaItem): Int {
        var s = 0
        if (d.name.isNotBlank()) s += 5
        if (!d.description.isNullOrBlank()) s += 10
        if (d.genres.isNotEmpty()) s += 5
        if (!d.poster.isNullOrBlank()) s += 3
        if (!d.background.isNullOrBlank()) s += 2
        if (!d.imdbRating.isNullOrBlank()) s += 2
        if (d.castMembers.isNotEmpty()) s += 4
        if (d.recommendations.isNotEmpty()) s += 3
        if (d.trailerUrl != null) s += 2
        if (d.status != null) s += 1
        val isSeries = item.type == MediaType.SERIES || item.type == MediaType.ANIME ||
            item.type == MediaType.NSFW_ANIME || d.type == "series"
        if (isSeries) {
            if (d.videos.isNotEmpty()) s += 50
            if (d.totalEpisodes != null) s += 10
        }
        return s
    }

    /** Episodes for a series/anime detail page. */
    suspend fun episodes(item: MediaItem, detail: MetaDetail): List<Video> {
        if (detail.videos.isNotEmpty()) return detail.videos
        return emptyList()
    }

    suspend fun tmdbSeason(tmdbId: String, season: Int): List<Video> {
        val imdb = TmdbClient.imdbId(MediaType.SERIES, tmdbId)
        return TmdbClient.seasonEpisodes(tmdbId, season, imdb)
    }

    /** Related content for the detail page: prefer the source's own recommendations, fall back to genre. */
    suspend fun related(item: MediaItem, detail: MetaDetail): List<MediaItem> {
        if (detail.recommendations.isNotEmpty()) {
            return detail.recommendations.filter { it.id != item.id }.distinctBy { it.key }.take(20)
        }
        val genre = detail.genres.firstOrNull()
        return runCatching {
            when (item.type) {
                MediaType.MOVIE -> TmdbClient.discoverMovies()
                MediaType.SERIES -> TmdbClient.discoverTv()
                MediaType.ANIME -> AniListClient.byGenre(genre ?: "Action")
                // Bounded like every other NSFW-anime hop: the related rail must never be the
                // thing that keeps the detail spinner up.
                MediaType.NSFW_ANIME -> withTimeout(NSFW_SOURCE_TIMEOUT_MS) { AniListClient.nsfwByGenre(genre ?: "Ecchi") }
                MediaType.MANGA -> MangaDexClient.popular()
                MediaType.NSFW_MANGA -> MangaDexClient.search("", nsfw = true, limit = 20)
                else -> emptyList()
            }.filter { it.id != item.id }.distinctBy { it.key }.take(20)
        }.getOrDefault(emptyList())
    }

    private companion object {
        /** Bounds one NSFW-anime metadata source (AniList / Jikan) so a hung call can't stall the page. */
        const val NSFW_SOURCE_TIMEOUT_MS = 8_000L
    }
}
