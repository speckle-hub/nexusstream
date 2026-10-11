package com.novastream.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.novastream.app.BuildConfig
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import com.novastream.app.data.model.Video

/** TMDB metadata client (movies, TV, and anime catalogues). */
object TmdbClient {
    val API_KEY: String = BuildConfig.TMDB_API_KEY
    val isConfigured: Boolean get() = API_KEY.isNotBlank()
    private const val BASE = "https://api.themoviedb.org/3"
    const val IMG = "https://image.tmdb.org/t/p"

    fun poster(path: String?, size: String = "w500"): String? = path?.let { "$IMG/$size$it" }
    fun backdrop(path: String?, size: String = "w1280"): String? = path?.let { "$IMG/$size$it" }

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): JsonObject {
        if (API_KEY.isBlank()) throw java.io.IOException("TMDB_API_KEY is not configured (set it in local.properties)")
        val q = (params + mapOf("api_key" to API_KEY, "language" to "en-US"))
            .entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }
        val parsed = parseJson(httpGetCached("$BASE$path?$q"))
        if (!parsed.isJsonObject) throw java.io.IOException("Malformed TMDB response for $path: expected JSON object")
        return parsed.asJsonObject
    }

    private fun item(o: JsonObject, fallbackType: MediaType): MediaItem? {
        val id = o.get("id")?.takeIf { it.isJsonPrimitive }?.asInt?.toString() ?: return null
        val mt = o.get("media_type")?.takeIf { it.isJsonPrimitive }?.asString
        val type = when (mt) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.SERIES
            else -> fallbackType
        }
        val title = o.get("title")?.takeIf { it.isJsonPrimitive }?.asString
            ?: o.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        val date = o.get("release_date")?.takeIf { it.isJsonPrimitive }?.asString
            ?: o.get("first_air_date")?.takeIf { it.isJsonPrimitive }?.asString
        return MediaItem(
            id = id,
            type = type,
            title = title,
            poster = poster(o.get("poster_path")?.takeIf { it.isJsonPrimitive }?.asString),
            backdrop = backdrop(o.get("backdrop_path")?.takeIf { it.isJsonPrimitive }?.asString),
            year = date?.take(4),
            rating = o.get("vote_average")?.takeIf { it.isJsonPrimitive }?.asDouble,
            description = o.get("overview")?.takeIf { it.isJsonPrimitive }?.asString,
            addonId = "tmdb",
            addonName = "TMDB",
        )
    }

    private fun parseList(root: JsonObject, fallbackType: MediaType): List<MediaItem> {
        val arr = root.optJsonArray("results") ?: return emptyList()
        return arr.mapNotNull { if (it.isJsonObject) item(it.asJsonObject, fallbackType) else null }
    }

    suspend fun trending(type: String = "all", window: String = "week"): List<MediaItem> =
        parseList(get("/trending/$type/$window"), MediaType.MOVIE)

    suspend fun popularMovies(): List<MediaItem> = parseList(get("/movie/popular"), MediaType.MOVIE)
    suspend fun topRatedMovies(): List<MediaItem> = parseList(get("/movie/top_rated"), MediaType.MOVIE)
    suspend fun nowPlaying(): List<MediaItem> = parseList(get("/movie/now_playing"), MediaType.MOVIE)
    suspend fun upcoming(): List<MediaItem> = parseList(get("/movie/upcoming"), MediaType.MOVIE)
    suspend fun popularTv(): List<MediaItem> = parseList(get("/tv/popular"), MediaType.SERIES)
    suspend fun topRatedTv(): List<MediaItem> = parseList(get("/tv/top_rated"), MediaType.SERIES)
    suspend fun airingToday(): List<MediaItem> = parseList(get("/tv/airing_today"), MediaType.SERIES)

    suspend fun discoverMovies(genreId: String? = null, sort: String = "popularity.desc"): List<MediaItem> {
        val p = mutableMapOf("sort_by" to sort)
        if (genreId != null) p["with_genres"] = genreId
        return parseList(get("/discover/movie", p), MediaType.MOVIE)
    }

    suspend fun discoverTv(genreId: String? = null): List<MediaItem> =
        parseList(get("/discover/tv", if (genreId != null) mapOf("with_genres" to genreId) else emptyMap()), MediaType.SERIES)

    /**
     * General (SFW) search. `include_adult` is forced to false so pornographic titles can never
     * leak into the normal Search tab; NSFW content is reached only through the NSFW sections.
     */
    suspend fun search(query: String): List<MediaItem> =
        parseList(get("/search/multi", mapOf("query" to query, "include_adult" to "false")), MediaType.MOVIE)

    suspend fun searchMovies(query: String): List<MediaItem> =
        parseList(get("/search/movie", mapOf("query" to query)), MediaType.MOVIE)

    suspend fun searchTv(query: String): List<MediaItem> =
        parseList(get("/search/tv", mapOf("query" to query)), MediaType.SERIES)

    /** Anime-ish discovery: animation genre (16) on TMDB. */
    suspend fun discoverAnime(): List<MediaItem> =
        parseList(get("/discover/tv", mapOf("with_genres" to "16", "sort_by" to "popularity.desc")), MediaType.ANIME)

    /**
     * Anime search. TMDB ignores `with_genres` on /search/tv, so filter results down to the
     * animation genre (16) client-side instead of sending a parameter that does nothing.
     */
    suspend fun searchAnime(query: String): List<MediaItem> {
        val arr = get("/search/tv", mapOf("query" to query)).optJsonArray("results") ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val isAnimation = o.optJsonArray("genre_ids")?.any { it.isJsonPrimitive && it.asInt == 16 } ?: false
            if (isAnimation) item(o, MediaType.ANIME) else null
        }
    }

    /** Resolve the IMDb id for a TMDB item so Stremio stream addons can be queried. */
    suspend fun imdbId(type: MediaType, tmdbId: String): String? {
        val path = if (type == MediaType.MOVIE) "/movie/$tmdbId/external_ids" else "/tv/$tmdbId/external_ids"
        return try {
            get(path).get("imdb_id")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolve an IMDb id (tt...) to a TMDB (type, id) pair via /find.
     * TV is preferred over movies because a title that exists as both is almost always a series.
     */
    suspend fun findByImdb(imdbId: String): Pair<MediaType, String>? {
        if (!imdbId.startsWith("tt")) return null
        val root = runCatching { get("/find/$imdbId", mapOf("external_source" to "imdb_id")) }.getOrNull() ?: return null
        root.optJsonArray("tv_results")?.firstOrNull()?.takeIf { it.isJsonObject }
            ?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asInt
            ?.let { return MediaType.SERIES to it.toString() }
        root.optJsonArray("movie_results")?.firstOrNull()?.takeIf { it.isJsonObject }
            ?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asInt
            ?.let { return MediaType.MOVIE to it.toString() }
        return null
    }

    private fun normTitle(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun titleMatches(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        val na = normTitle(a); val nb = normTitle(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        return na == nb || na.startsWith(nb) || nb.startsWith(na)
    }

    /**
     * Robust detail fetch. TMDB ids are shared between the movie and TV namespaces, so a title
     * that is really a series can be looked up under the wrong endpoint (e.g. Dexter = tv 1405,
     * but movie 1405 = "Greed"). When an [expectedTitle] is supplied we verify the primary result
     * and transparently fall back to the alternate namespace if it matches better.
     */
    suspend fun details(type: MediaType, tmdbId: String, expectedTitle: String? = null): MetaDetail? {
        val primary = runCatching { detailsOnce(type, tmdbId) }.getOrNull()
        val altType = if (type == MediaType.MOVIE) MediaType.SERIES else MediaType.MOVIE

        // No usable primary result -> try the alternate namespace.
        if (primary == null || primary.name.isBlank()) {
            val alt = runCatching { detailsOnce(altType, tmdbId) }.getOrNull()
            if (alt != null && alt.name.isNotBlank()) return alt
            return primary
        }

        // Primary exists but the name is wrong for this title -> prefer the alternate if it matches.
        if (!expectedTitle.isNullOrBlank() && !titleMatches(primary.name, expectedTitle)) {
            val alt = runCatching { detailsOnce(altType, tmdbId) }.getOrNull()
            if (alt != null && titleMatches(alt.name, expectedTitle)) return alt
        }
        return primary
    }

    private suspend fun detailsOnce(type: MediaType, tmdbId: String): MetaDetail? {
        val isMovie = type == MediaType.MOVIE
        val path = if (isMovie) "/movie/$tmdbId" else "/tv/$tmdbId"
        val root = get(
            path,
            mapOf("append_to_response" to "credits,external_ids,videos,recommendations,alternative_titles"),
        )
        val title = root.get("title")?.takeIf { it.isJsonPrimitive }?.asString
            ?: root.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return null

        val castMembers = root.optJsonObject("credits")?.optJsonArray("cast")?.take(20)?.mapNotNull {
            if (!it.isJsonObject) return@mapNotNull null
            val o = it.asJsonObject
            val name = o.get("name")?.takeIf { n -> n.isJsonPrimitive }?.asString ?: return@mapNotNull null
            com.novastream.app.data.model.CastMember(
                name = name,
                character = o.get("character")?.takeIf { c -> !c.isJsonNull && c.isJsonPrimitive }?.asString,
                image = poster(o.get("profile_path")?.takeIf { p -> !p.isJsonNull && p.isJsonPrimitive }?.asString, "w185"),
            )
        } ?: emptyList()
        val cast = castMembers.map { it.name }

        val crew = root.optJsonObject("credits")?.optJsonArray("crew")?.filter {
            it.isJsonObject && it.asJsonObject.get("job")?.takeIf { j -> j.isJsonPrimitive }?.asString == "Director"
        }?.mapNotNull { it.asJsonObject.get("name")?.takeIf { n -> n.isJsonPrimitive }?.asString } ?: emptyList()

        val videos = mutableListOf<Video>()
        val imdb = root.optJsonObject("external_ids")?.get("imdb_id")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
        if (!isMovie) {
            val seasons = root.optJsonArray("seasons")
            seasons?.forEach { s ->
                if (!s.isJsonObject) return@forEach
                val so = s.asJsonObject
                val sn = so.get("season_number")?.takeIf { it.isJsonPrimitive }?.asInt ?: return@forEach
                if (sn <= 0) return@forEach
                videos.add(
                    Video(
                        id = "$imdb:$sn:1",
                        title = so.get("name")?.takeIf { it.isJsonPrimitive }?.asString,
                        season = sn,
                        episode = 1,
                        thumbnail = poster(so.get("poster_path")?.takeIf { it.isJsonPrimitive }?.asString, "w300"),
                    )
                )
            }
        }
        val runtime = root.get("runtime")?.takeIf { it.isJsonPrimitive }?.asInt
            ?: root.optJsonArray("episode_run_time")?.firstOrNull()?.takeIf { it.isJsonPrimitive }?.asInt

        // Trailer (prefer official YouTube trailers)
        val trailerUrl = root.optJsonObject("videos")?.optJsonArray("results")
            ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
            ?.firstOrNull { v ->
                v.get("site")?.takeIf { it.isJsonPrimitive }?.asString.equals("YouTube", true) &&
                    v.get("type")?.takeIf { it.isJsonPrimitive }?.asString in listOf("Trailer", "Teaser")
            }?.get("key")?.takeIf { it.isJsonPrimitive }?.asString?.let { "https://www.youtube.com/watch?v=$it" }

        // Alternative titles
        val altTitles = root.optJsonObject("alternative_titles")?.optJsonArray("titles")
            ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject?.get("title")?.takeIf { t -> t.isJsonPrimitive }?.asString }
            ?.filter { it.isNotBlank() && it != title }?.distinct()?.take(6) ?: emptyList()

        // Recommendations
        val recommendations = root.optJsonObject("recommendations")?.optJsonArray("results")
            ?.mapNotNull { r ->
                if (!r.isJsonObject) return@mapNotNull null
                val o = r.asJsonObject
                val rid = o.get("id")?.takeIf { it.isJsonPrimitive }?.asInt?.toString() ?: return@mapNotNull null
                val rtitle = o.get("title")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: o.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
                MediaItem(
                    id = rid,
                    type = if (isMovie) MediaType.MOVIE else MediaType.SERIES,
                    title = rtitle,
                    poster = poster(o.get("poster_path")?.takeIf { it.isJsonPrimitive }?.asString),
                    backdrop = backdrop(o.get("backdrop_path")?.takeIf { it.isJsonPrimitive }?.asString),
                    year = (o.get("release_date")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: o.get("first_air_date")?.takeIf { it.isJsonPrimitive }?.asString)?.take(4),
                    rating = o.get("vote_average")?.takeIf { it.isJsonPrimitive }?.asDouble,
                    description = o.get("overview")?.takeIf { it.isJsonPrimitive }?.asString,
                    addonId = "tmdb",
                    addonName = "TMDB",
                )
            } ?: emptyList()

        val status = root.get("status")?.takeIf { it.isJsonPrimitive }?.asString?.let {
            when (it) {
                "Released" -> "Released"
                "Returning Series" -> "Airing"
                "Ended" -> "Ended"
                "Canceled" -> "Cancelled"
                "In Production" -> "In Production"
                else -> it
            }
        }
        val totalEpisodes = root.get("number_of_episodes")?.takeIf { it.isJsonPrimitive }?.asInt
        val totalSeasons = root.get("number_of_seasons")?.takeIf { it.isJsonPrimitive }?.asInt

        return MetaDetail(
            id = imdb ?: tmdbId,
            type = if (isMovie) "movie" else "series",
            name = title,
            poster = poster(root.get("poster_path")?.takeIf { it.isJsonPrimitive }?.asString),
            background = backdrop(root.get("backdrop_path")?.takeIf { it.isJsonPrimitive }?.asString),
            description = root.get("overview")?.takeIf { it.isJsonPrimitive }?.asString,
            releaseInfo = (root.get("release_date")?.takeIf { it.isJsonPrimitive }?.asString
                ?: root.get("first_air_date")?.takeIf { it.isJsonPrimitive }?.asString)?.take(4),
            imdbRating = root.get("vote_average")?.takeIf { it.isJsonPrimitive }?.asDouble?.let { String.format("%.1f", it) },
            runtime = runtime?.let { "$it min" }
                ?: totalSeasons?.let { "$it season${if (it > 1) "s" else ""}" },
            genres = root.optJsonArray("genres")?.mapNotNull {
                if (it.isJsonObject) it.asJsonObject.get("name")?.takeIf { n -> n.isJsonPrimitive }?.asString else null
            } ?: emptyList(),
            cast = cast,
            director = crew,
            videos = videos,
            addonId = "tmdb",
            altTitles = altTitles,
            votes = root.get("vote_count")?.takeIf { it.isJsonPrimitive }?.asInt,
            status = status,
            totalEpisodes = totalEpisodes,
            trailerUrl = trailerUrl,
            castMembers = castMembers,
            recommendations = recommendations,
            raw = root,
        )
    }

    /** Fetch episodes for a specific season (for series detail pages). */
    suspend fun seasonEpisodes(tmdbId: String, season: Int, imdb: String?): List<Video> {
        val root = get("/tv/$tmdbId/season/$season")
        val arr = root.optJsonArray("episodes") ?: return emptyList()
        return arr.mapNotNull { e ->
            if (!e.isJsonObject) return@mapNotNull null
            val eo = e.asJsonObject
            val num = eo.get("episode_number")?.takeIf { it.isJsonPrimitive }?.asInt ?: return@mapNotNull null
            Video(
                id = "${imdb ?: tmdbId}:$season:$num",
                title = eo.get("name")?.takeIf { it.isJsonPrimitive }?.asString,
                season = season,
                episode = num,
                thumbnail = poster(eo.get("still_path")?.takeIf { it.isJsonPrimitive }?.asString, "w300"),
                overview = eo.get("overview")?.takeIf { it.isJsonPrimitive }?.asString,
                released = eo.get("air_date")?.takeIf { it.isJsonPrimitive }?.asString,
                runtimeMin = eo.get("runtime")?.takeIf { it.isJsonPrimitive }?.asInt,
            )
        }
    }
}

fun JsonObject.optJsonArray(key: String): JsonArray? =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray

fun JsonObject.optJsonObject(key: String): JsonObject? =
    get(key)?.takeIf { it.isJsonObject }?.asJsonObject
