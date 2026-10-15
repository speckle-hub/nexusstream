package com.novastream.app.data.sfw

import com.google.gson.JsonParser
import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str
import java.net.URLEncoder

/**
 * YTS built-in source (movies only).
 *
 * YTS exposes a public JSON API, which makes it the most reliable movie source:
 * `list_movies.json` powers home rows (`sort_by=like_count` / `download_count`) and
 * keyword search (`query_term=`), while `movie_details.json?with_images=false&with_cast=false`
 * returns the torrent list. Torrents resolve to [StreamSource.infoHash] entries with
 * real quality labels and seeder counts, so the existing torrent playback path
 * ([com.novastream.app.ui.screens.detail.DetailScreen.play]) and the seeder-aware
 * ranking both work unchanged.
 */
object YtsSource : SfwSource {

    override val id = "yts"
    override val name = "YTS"
    override val moviesOnly = true

    private const val BASE = "https://yts.mx"
    private const val API = "https://yts.mx/api/v2"

    override suspend fun home(): List<SfwRow> = sfwRows(
        this,
        MediaType.MOVIE,
        defs = listOf(
            "Trending" to apiList(query = null, sort = "like_count"),
            "Popular" to apiList(query = null, sort = "download_count"),
            "Latest" to apiList(query = null, sort = "date_added"),
        ),
    ) { url -> parseList(AdultHttp.get(url, referer = BASE)) }

    override suspend fun search(query: String): List<SfwTitle> =
        parseList(AdultHttp.get(apiList(query = query, sort = "like_count"), referer = BASE))

    override suspend fun detail(url: String): SfwDetail {
        val idNum = Regex("""(?:movie_id=|/movies?/[^/]*?(\d+))""").find(url)
            ?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: Regex("""(\d+)""").findAll(url).lastOrNull()?.value
            ?: throw SfwSourceException("Unrecognised YTS url")
        val json = AdultHttp.get("$API/movie_details.json?movie_id=$idNum&with_images=false&with_cast=false", referer = BASE)
        val movie = runCatching {
            JsonParser.parseString(json).asJsonObject.get("data")?.asJsonObject?.get("movie")?.asJsonObject
        }.getOrNull() ?: throw SfwSourceException("YTS detail unavailable")
        val title = parseMovie(movie)?.copy(url = url) ?: throw SfwSourceException("YTS detail unavailable")
        return SfwDetail(
            title = title,
            description = movie.str("description_full")?.takeIf { it.isNotBlank() },
            poster = title.poster,
            streams = parseTorrents(movie),
        )
    }

    override suspend fun streamsForEpisode(episode: SfwEpisode): List<StreamSource> =
        detail(episode.url).streams

    internal fun apiList(query: String?, sort: String, page: Int = 1, limit: Int = 20): String {
        val q = if (query.isNullOrBlank()) "" else "query_term=" + URLEncoder.encode(query, "UTF-8") + "&"
        return "$API/list_movies.json?${q}sort_by=$sort&order_by=desc&limit=$limit&page=$page&minimum_rating=0"
    }

    /** Parse a `list_movies.json` response: root `data.movies[]`. */
    internal fun parseList(json: String): List<SfwTitle> {
        val movies = runCatching {
            JsonParser.parseString(json).asJsonObject
                .get("data")?.takeIf { it.isJsonObject }?.asJsonObject
                ?.get("movies")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return movies.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject?.let { o -> parseMovie(o) } }
            .distinctBy { it.url }
    }

    internal fun parseMovie(o: com.google.gson.JsonObject): SfwTitle? {
        val title = o.str("title_english")?.takeIf { it.isNotBlank() }
            ?: o.str("title")?.takeIf { it.isNotBlank() } ?: return null
        val idNum = o.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        return SfwTitle(
            sourceId = id,
            url = "$BASE/movies/$idNum",
            title = title,
            kind = MediaType.MOVIE,
            poster = o.str("medium_cover_image")?.takeIf { it.startsWith("http") },
            year = o.get("year")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it != "0" },
            rating = o.get("rating")?.takeIf { it.isJsonPrimitive }?.asDouble,
        )
    }

    /** Torrent list of a `movie_details.json` movie object → infoHash streams. */
    internal fun parseTorrents(movie: com.google.gson.JsonObject): List<StreamSource> {
        val torrents = movie.get("torrents")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return torrents.mapNotNull { el ->
            val t = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val hash = t.str("hash")?.takeIf { it.length in 32..64 } ?: return@mapNotNull null
            val quality = t.str("quality")
            StreamSource(
                infoHash = hash.uppercase(),
                title = listOfNotNull(quality, t.str("type")).joinToString(" ").ifBlank { null },
                name = name,
                addonId = id,
                addonName = name,
                quality = quality?.let { SfwEmbeds.normalizeLabel(it) },
                seeders = t.get("seeds")?.takeIf { it.isJsonPrimitive }?.asInt,
                isTorrent = true,
            )
        }.distinctBy { it.infoHash }
    }
}
