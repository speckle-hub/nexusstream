package com.moovio.desktop.data.remote

import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.net.arrayOrNull
import com.moovio.desktop.data.net.bool
import com.moovio.desktop.data.net.int
import com.moovio.desktop.data.net.obj
import com.moovio.desktop.data.net.objectOrNull
import com.moovio.desktop.data.net.parseJsonObject
import com.moovio.desktop.data.net.plainDescription
import com.moovio.desktop.data.net.str
import com.moovio.desktop.data.net.stringMap
import com.moovio.desktop.data.net.strings
import com.moovio.desktop.domain.StreamRanking
import com.moovio.desktop.domain.model.Addon
import com.moovio.desktop.domain.model.AddonCatalog
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.MetaDetail
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.domain.model.SubtitleTrack
import java.net.URLEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Implementation of the Stremio add-on protocol (manifest / catalog / meta / stream).
 *
 * All parsing is defensive: third-party add-ons frequently omit fields or return slightly
 * malformed JSON, and one bad add-on must never break the whole page.
 */
class StremioClient(private val http: Http) {

    fun base(transportUrl: String): String {
        val trimmed = transportUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/manifest.json", true)) trimmed.dropLast("/manifest.json".length) else trimmed
    }

    /** Path-safe encoding: colons are significant in episode ids, so keep them literal. */
    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("%3A", ":")
            .replace("%3a", ":")

    suspend fun fetchManifest(manifestUrl: String): Addon {
        val base = base(manifestUrl)
        val json = parseJsonObject(http.getText("$base/manifest.json"))
        return parseManifest(json, base, manifestUrl)
    }

    fun parseManifest(json: JsonObject, base: String, manifestUrl: String): Addon {
        val resources = mutableListOf<String>()
        val prefixes = LinkedHashSet<String>()
        json.arrayOrNull("resources")?.forEach { element ->
            when {
                element is kotlinx.serialization.json.JsonPrimitive -> resources.add(element.content)
                else -> {
                    val resource = element.obj() ?: return@forEach
                    resource.str("name")?.let { resources.add(it) }
                    resource.arrayOrNull("idPrefixes")?.strings()?.let { prefixes.addAll(it) }
                }
            }
        }
        json.arrayOrNull("idPrefixes")?.strings()?.let { prefixes.addAll(it) }

        val catalogs = json.arrayOrNull("catalogs")?.mapNotNull { element ->
            val catalog = element.obj() ?: return@mapNotNull null
            val type = catalog.str("type") ?: return@mapNotNull null
            val id = catalog.str("id") ?: return@mapNotNull null
            val name = catalog.str("name") ?: id.replaceFirstChar(Char::uppercase)
            val genres = catalog.arrayOrNull("genres").strings()
            val searchable = catalog.arrayOrNull("extra")?.any {
                it.obj().str("name").equals("search", true)
            } == true
            AddonCatalog(type = type, id = id, name = name, genres = genres, searchable = searchable)
        } ?: emptyList()

        return Addon(
            manifestUrl = manifestUrl,
            transportUrl = base,
            id = json.str("id") ?: base,
            name = json.str("name") ?: base,
            version = json.str("version").orEmpty(),
            description = json.str("description").plainDescription().orEmpty(),
            logo = json.str("logo"),
            types = json.arrayOrNull("types").strings(),
            resources = resources.distinct(),
            idPrefixes = prefixes.toList(),
            catalogs = catalogs,
            adult = json.indicatesAdult(),
            builtIn = false,
        )
    }

    /** Adult only when the manifest says so (Stremio's `behaviorHints.adult`/`nsfw` or `flags`) —
     * never inferred from the add-on's kind, catalogs or name. */
    private fun JsonObject.indicatesAdult(): Boolean {
        if (bool("adult") == true || bool("nsfw") == true) return true
        return listOf("behaviorHints", "flags").any { key ->
            val hints = objectOrNull(key) ?: return@any false
            hints.bool("adult") == true || hints.bool("nsfw") == true
        }
    }

    suspend fun catalog(
        addon: Addon,
        type: String,
        catalogId: String,
        skip: Int = 0,
        search: String? = null,
        genre: String? = null,
    ): List<MediaItem> {
        val extras = buildList {
            search?.takeIf { it.isNotBlank() }?.let { add("search=${enc(it)}") }
            genre?.takeIf { it.isNotBlank() }?.let { add("genre=${enc(it)}") }
            if (skip > 0 && search.isNullOrBlank()) add("skip=$skip")
        }
        val path = buildString {
            append("/catalog/").append(type).append('/').append(catalogId)
            if (extras.isNotEmpty()) append('/').append(extras.joinToString("&"))
            append(".json")
        }
        val json = parseJsonObject(http.getText(addon.transportUrl + path))
        return json.arrayOrNull("metas")?.mapNotNull { element ->
            val meta = element.obj() ?: return@mapNotNull null
            parseCatalogItem(meta, addon)
        } ?: emptyList()
    }

    private fun parseCatalogItem(json: JsonObject, addon: Addon): MediaItem? {
        val id = json.str("id") ?: return null
        val name = json.str("name") ?: return null
        val type = when (json.str("type")) {
            "movie" -> MediaType.MOVIE
            "anime" -> MediaType.ANIME
            "manga" -> MediaType.MANGA
            else -> MediaType.SERIES
        }
        return MediaItem(
            id = id,
            type = type,
            name = name,
            poster = json.str("poster"),
            background = json.str("background") ?: json.str("banner"),
            logo = json.str("logo"),
            year = yearOf(json.str("releaseInfo")),
            rating = (json.str("imdbRating") ?: json.str("rating"))?.toDoubleOrNull(),
            description = json.str("description").plainDescription(),
            genres = json.arrayOrNull("genres").strings(),
            releaseInfo = json.str("releaseInfo"),
            addonId = addon.id,
            adult = addon.adult,
        )
    }

    suspend fun meta(addon: Addon, type: String, id: String): MetaDetail? {
        val json = runCatching {
            parseJsonObject(http.getText("${addon.transportUrl}/meta/$type/${enc(id)}.json"))
        }.getOrNull() ?: return null
        val meta = json["meta"].obj() ?: return null
        val name = meta.str("name") ?: return null
        val videos = meta.arrayOrNull("videos")?.mapNotNull { element ->
            val video = element.obj() ?: return@mapNotNull null
            val videoId = video.str("id") ?: return@mapNotNull null
            val season = video.int("season") ?: 1
            val number = video.int("number") ?: video.int("episode") ?: return@mapNotNull null
            com.moovio.desktop.domain.model.Episode(
                id = videoId,
                season = season,
                episode = number,
                title = video.str("name") ?: video.str("title") ?: "Episode $number",
                thumbnail = video.str("thumbnail"),
                released = video.str("released"),
                description = video.str("overview").plainDescription(),
            )
        }?.sortedWith(compareBy({ it.season }, { it.episode })) ?: emptyList()

        return MetaDetail(
            id = meta.str("id") ?: id,
            type = when (type) {
                "movie" -> MediaType.MOVIE
                "anime" -> MediaType.ANIME
                else -> MediaType.SERIES
            },
            name = name,
            poster = meta.str("poster"),
            background = meta.str("background"),
            logo = meta.str("logo"),
            description = meta.str("description").plainDescription(),
            year = yearOf(meta.str("releaseInfo")),
            releaseInfo = meta.str("releaseInfo"),
            rating = (meta.str("imdbRating") ?: meta.str("rating"))?.toDoubleOrNull(),
            runtime = meta.str("runtime"),
            genres = meta.arrayOrNull("genres").strings(),
            cast = meta.arrayOrNull("cast").strings(),
            episodes = videos,
            adult = addon.adult,
        )
    }

    suspend fun streams(addon: Addon, type: String, id: String): List<StreamSource> {
        val json = parseJsonObject(http.getText("${addon.transportUrl}/stream/$type/${enc(id)}.json"))
        val streams = json.arrayOrNull("streams") ?: return emptyList()
        return streams.mapNotNull { element ->
            val stream = element.obj() ?: return@mapNotNull null
            val url = stream.str("url")
            val infoHash = stream.str("infoHash")
            val external = stream.str("externalUrl")
            if (url == null && infoHash == null && external == null) return@mapNotNull null
            val hints = stream.objectOrNull("behaviorHints")
            val headers = hints.stringMap("proxyHeaders").ifEmpty {
                hints.objectOrNull("proxyHeaders")?.stringMap("request").orEmpty()
            } + hints.stringMap("headers")
            val subtitles = stream.arrayOrNull("subtitles")?.mapNotNull { sub ->
                val track = sub.obj() ?: return@mapNotNull null
                val subUrl = track.str("url") ?: return@mapNotNull null
                SubtitleTrack(
                    url = subUrl,
                    lang = track.str("lang") ?: "und",
                    label = track.str("label") ?: track.str("lang") ?: "Subtitles",
                )
            } ?: emptyList()
            StreamSource(
                url = url,
                infoHash = infoHash,
                fileIdx = stream.int("fileIdx"),
                externalUrl = external,
                name = stream.str("name"),
                title = stream.str("title"),
                description = stream.str("description"),
                seeders = StreamRanking.seedersOf(stream.str("title"), stream.str("description")),
                addonId = addon.id,
                addonName = addon.name,
                headers = headers,
                subtitles = subtitles,
            )
        }.let { StreamRanking.rank(it) }
    }
}
