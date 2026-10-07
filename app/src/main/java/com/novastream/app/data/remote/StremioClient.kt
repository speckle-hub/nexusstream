package com.novastream.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.novastream.app.data.model.Addon
import com.novastream.app.data.model.AddonKind
import com.novastream.app.data.model.CatalogDef
import com.novastream.app.data.model.ExtraDef
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import java.net.URLEncoder

/**
 * Full implementation of the Stremio add-on protocol.
 * https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md
 */
object StremioClient {

    private fun base(transportUrl: String): String =
        transportUrl.trimEnd('/').let { if (it.endsWith("manifest.json")) it.removeSuffix("/manifest.json") else it }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    // ---- manifest -----------------------------------------------------------

    suspend fun fetchManifest(transportUrl: String): Addon {
        val url = base(transportUrl) + "/manifest.json"
        val json = parseJson(httpGet(url)).asJsonObject
        return parseManifest(json, base(transportUrl))
    }

    fun parseManifest(json: JsonObject, transportUrl: String): Addon {
        val types = json.getAsJsonArray("types")?.map { it.asString } ?: emptyList()
        // resources may be plain strings ("stream") or objects ({"name":"stream","types":[…],
        // "idPrefixes":[…]}). Collect the names, and union every declared idPrefixes (manifest-level
        // and per-resource) so the request router knows which ids this add-on understands.
        val resources = mutableListOf<String>()
        val prefixes = LinkedHashSet<String>()
        json.getAsJsonArray("resources")?.forEach { el ->
            when {
                el.isJsonPrimitive -> resources.add(el.asString)
                el.isJsonObject -> {
                    val o = el.asJsonObject
                    o.get("name")?.asString?.let { resources.add(it) }
                    o.getAsJsonArray("idPrefixes")?.forEach { p ->
                        if (p.isJsonPrimitive) prefixes.add(p.asString)
                    }
                }
            }
        }
        json.getAsJsonArray("idPrefixes")?.forEach { p ->
            if (p.isJsonPrimitive) prefixes.add(p.asString)
        }

        val catalogs = mutableListOf<CatalogDef>()
        json.getAsJsonArray("catalogs")?.forEach { el ->
            if (!el.isJsonObject) return@forEach
            val c = el.asJsonObject
            val extras = mutableListOf<ExtraDef>()
            c.getAsJsonArray("extra")?.forEach { e ->
                if (!e.isJsonObject) return@forEach
                val eo = e.asJsonObject
                val opts = eo.getAsJsonArray("options")?.map { it.asString } ?: emptyList()
                extras.add(ExtraDef(eo.get("name")?.asString ?: "", eo.get("isRequired")?.asBoolean ?: false, opts))
            }
            catalogs.add(
                CatalogDef(
                    type = c.get("type")?.asString ?: "movie",
                    id = c.get("id")?.asString ?: "",
                    name = c.get("name")?.asString ?: "Catalog",
                    genres = c.getAsJsonArray("genres")?.map { it.asString } ?: emptyList(),
                    extra = extras,
                )
            )
        }

        val id = json.get("id")?.asString ?: transportUrl.hashCode().toString()
        val name = json.get("name")?.asString ?: "Unknown addon"
        val desc = json.get("description")?.asString
        val nsfw = isNsfw(name, desc, id)
        return Addon(
            id = id,
            name = name,
            transportUrl = transportUrl,
            description = desc,
            version = json.get("version")?.asString,
            logo = json.get("logo")?.asString,
            types = types,
            resources = resources.filter { it.isNotBlank() },
            idPrefixes = prefixes.filter { it.isNotBlank() },
            catalogs = catalogs,
            enabled = true,
            nsfw = nsfw,
            kind = AddonKind.STREMIO,
        )
    }

    fun isNsfw(name: String?, desc: String?, id: String?): Boolean {
        val hay = listOfNotNull(name, desc, id).joinToString(" ").lowercase()
        return listOf("nsfw", "porn", "xxx", "18+", "adult", "hentai", "erotic", "jav").any { hay.contains(it) }
    }

    // ---- catalog ------------------------------------------------------------

    suspend fun fetchCatalog(
        addon: Addon,
        type: String,
        catalogId: String,
        extra: Map<String, String> = emptyMap(),
    ): List<MediaItem> {
        val extraPath = if (extra.isEmpty()) "" else "/" + extra.entries
            .filter { it.value.isNotBlank() }
            .joinToString("&") { "${it.key}=${enc(it.value)}" }
        val url = "${base(addon.transportUrl)}/catalog/$type/$catalogId$extraPath.json"
        val root = parseJson(httpGet(url)).asJsonObject
        val metas = root.getAsJsonArray("metas") ?: return emptyList()
        return metas.mapNotNull { parseMetaItem(it, addon) }
    }

    fun parseMetaItem(el: JsonElement, addon: Addon?): MediaItem? {
        if (!el.isJsonObject) return null
        val o = el.asJsonObject
        val id = o.get("id")?.asString ?: return null
        val rawType = o.get("type")?.asString ?: addon?.types?.firstOrNull() ?: "movie"
        val type = mapType(rawType, addon)
        return MediaItem(
            id = id,
            type = type,
            title = o.get("name")?.asString ?: id,
            poster = o.get("poster")?.asString,
            backdrop = o.get("background")?.asString,
            year = o.get("releaseInfo")?.asString,
            rating = o.get("imdbRating")?.asString?.toDoubleOrNull(),
            description = o.get("description")?.asString,
            genres = o.getAsJsonArray("genres")?.map { it.asString } ?: emptyList(),
            addonId = addon?.id,
            catalogId = null,
            addonName = addon?.name,
            // Keep the exact declared type so meta/stream requests reuse it verbatim.
            stremioType = rawType,
        )
    }

    /** Map a Stremio "type" to our logical MediaType, honouring NSFW addon context. */
    fun mapType(rawType: String, addon: Addon?): MediaType {
        val t = rawType.lowercase()
        if (addon?.nsfw == true) {
            return when (t) {
                "anime", "hentai" -> MediaType.NSFW_ANIME
                "manga", "comic" -> MediaType.NSFW_MANGA
                else -> MediaType.REAL
            }
        }
        return when (t) {
            "movie" -> MediaType.MOVIE
            "series", "tv" -> MediaType.SERIES
            "anime" -> MediaType.ANIME
            "manga", "comic" -> MediaType.MANGA
            else -> MediaType.MOVIE
        }
    }

    // ---- meta ---------------------------------------------------------------

    suspend fun fetchMeta(addon: Addon, type: String, id: String): MetaDetail? {
        val url = "${base(addon.transportUrl)}/meta/$type/${enc(id)}.json"
        val root = parseJson(httpGet(url)).asJsonObject
        val meta = root.getAsJsonObject("meta") ?: return null
        return parseMeta(meta, addon)
    }

    fun parseMeta(o: JsonObject, addon: Addon?): MetaDetail {
        val videos = mutableListOf<Video>()
        o.getAsJsonArray("videos")?.forEach { el ->
            if (!el.isJsonObject) return@forEach
            val v = el.asJsonObject
            videos.add(
                Video(
                    id = v.get("id")?.asString ?: return@forEach,
                    title = v.get("title")?.asString ?: v.get("name")?.asString,
                    season = v.get("season")?.asInt,
                    episode = v.get("episode")?.asInt ?: v.get("number")?.asInt,
                    thumbnail = v.get("thumbnail")?.asString,
                    released = v.get("released")?.asString,
                    overview = v.get("overview")?.asString,
                )
            )
        }
        val cast = o.getAsJsonArray("cast")?.map { it.asString } ?: emptyList()
        val directors = o.getAsJsonArray("director")?.map { it.asString } ?: emptyList()
        return MetaDetail(
            id = o.get("id")?.asString ?: "",
            type = o.get("type")?.asString ?: "movie",
            name = o.get("name")?.asString ?: "",
            poster = o.get("poster")?.asString,
            background = o.get("background")?.asString,
            logo = o.get("logo")?.asString,
            description = o.get("description")?.asString,
            releaseInfo = o.get("releaseInfo")?.asString,
            imdbRating = o.get("imdbRating")?.asString,
            runtime = o.get("runtime")?.asString,
            genres = o.getAsJsonArray("genres")?.map { it.asString } ?: emptyList(),
            cast = cast,
            director = directors,
            videos = videos,
            addonId = addon?.id,
            raw = o,
        )
    }

    // ---- streams ------------------------------------------------------------

    suspend fun fetchStreams(addon: Addon, type: String, id: String): List<StreamSource> {
        val url = "${base(addon.transportUrl)}/stream/$type/${enc(id)}.json"
        val root = parseJson(httpGet(url)).asJsonObject
        val streams = root.getAsJsonArray("streams") ?: return emptyList()
        return streams.mapNotNull { parseStream(it, addon) }
    }

    fun parseStream(el: JsonElement, addon: Addon?): StreamSource? {
        if (!el.isJsonObject) return null
        val o = el.asJsonObject
        val url = o.get("url")?.asString
        val infoHash = o.get("infoHash")?.asString
        val ytId = o.get("ytId")?.asString
        val externalUrl = o.get("externalUrl")?.asString
        if (url == null && infoHash == null && ytId == null && externalUrl == null) return null
        val name = o.get("name")?.asString
        val sources = o.getAsJsonArray("sources")?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString } ?: emptyList()
        // behaviorHints (Stremio protocol): proxyHeaders.request carries headers the CDN needs
        // (Referer/Cookie/User-Agent) — without them a hotlink-protected stream silently fails —
        // and filename is a better display title than the add-on's generic label. `notWebReady` is
        // intentionally ignored: it only warns *web* players, and this is a native ExoPlayer app.
        val hints = o.getAsJsonObject("behaviorHints")
        val proxyHeaders = hints?.getAsJsonObject("proxyHeaders")
            ?.getAsJsonObject("request")
            ?.entrySet()
            ?.mapNotNull { (k, v) -> if (v.isJsonPrimitive) k to v.asString else null }
            ?.toMap()
            .orEmpty()
        val filename = hints?.get("filename")?.takeIf { it.isJsonPrimitive }?.asString
        val title = o.get("title")?.asString ?: filename
        return StreamSource(
            url = url,
            ytId = ytId,
            infoHash = infoHash,
            fileIdx = o.get("fileIdx")?.takeIf { it.isJsonPrimitive }?.asInt,
            externalUrl = externalUrl,
            title = title,
            name = name,
            description = o.get("description")?.asString,
            quality = detectQuality(title, name, o.get("description")?.asString),
            addonId = addon?.id,
            addonName = addon?.name,
            isTorrent = infoHash != null,
            sources = sources,
            headers = proxyHeaders,
        )
    }

    private fun detectQuality(vararg texts: String?): String? {
        val hay = texts.filterNotNull().joinToString(" ").lowercase()
        return when {
            hay.contains("2160") || hay.contains("4k") -> "4K"
            hay.contains("1440") -> "1440p"
            hay.contains("1080") -> "1080p"
            hay.contains("720") -> "720p"
            hay.contains("480") -> "480p"
            hay.contains("360") -> "360p"
            else -> null
        }
    }

    // ---- subtitles ----------------------------------------------------------

    suspend fun fetchSubtitles(addon: Addon, type: String, id: String, extra: Map<String, String> = emptyMap()): List<SubtitleTrack> {
        val extraPath = if (extra.isEmpty()) "" else "/" + extra.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val url = "${base(addon.transportUrl)}/subtitles/$type/${enc(id)}$extraPath.json"
        return try {
            val root = parseJson(httpGet(url)).asJsonObject
            val subs = root.getAsJsonArray("subtitles") ?: return emptyList()
            subs.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val o = el.asJsonObject
                val u = o.get("url")?.asString ?: return@mapNotNull null
                SubtitleTrack(
                    id = o.get("id")?.asString ?: u,
                    url = u,
                    lang = o.get("lang")?.asString ?: "unknown",
                    addonName = addon.name,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---- search helper ------------------------------------------------------

    suspend fun search(addon: Addon, type: String, catalogId: String, query: String): List<MediaItem> =
        fetchCatalog(addon, type, catalogId, mapOf("search" to query))

    /**
     * Build the list of catalogs from an addon that match a section type.
     *
     * NSFW add-ons advertise non-standard catalog types ("porn", "Porn", "hentai", "jav", ...),
     * so for those we resolve each catalog's type through [mapType] instead of matching a fixed set.
     */
    fun catalogsFor(addon: Addon, section: MediaType): List<CatalogDef> {
        if (addon.nsfw) {
            return addon.catalogs.filter { mapType(it.type, addon) == section }
        }
        val wanted = when (section) {
            MediaType.MOVIE -> setOf("movie")
            MediaType.SERIES -> setOf("series", "tv")
            MediaType.ANIME -> setOf("anime")
            MediaType.MANGA -> setOf("manga", "comic")
            else -> emptySet()
        }
        return addon.catalogs.filter { it.type.lowercase() in wanted }
    }

    fun JsonArray?.toList(): List<JsonElement> = this?.toList() ?: emptyList()

    // ---- id routing (idPrefixes) -------------------------------------------

    /**
     * True when an add-on declaring [idPrefixes] will accept [id]. An empty list means the add-on
     * did not restrict ids (the Stremio default), so anything is accepted.
     */
    fun acceptsId(id: String, idPrefixes: List<String>): Boolean =
        idPrefixes.isEmpty() || idPrefixes.any { id.startsWith(it) }

    /**
     * Alternative (type, id) forms for a request, most-specific first, before the historical
     * default pair. Two cases the bare id gets wrong:
     *  - a `tt…:S:E` episode id sent under the `anime` type is really a *series* request;
     *  - an AniList item is keyed by a bare numeric id, but Stremio anime add-ons address titles by
     *    namespace, so `anilist:<id>` is offered.
     * Pure so it can be unit-tested without a network call.
     */
    fun candidateRequests(
        defaultType: String,
        baseId: String,
        isAnime: Boolean,
        videoId: String?,
    ): List<Pair<String, String>> {
        val id = videoId ?: baseId
        val out = LinkedHashSet<Pair<String, String>>()
        if (id.startsWith("tt") && id.contains(':')) out.add("series" to id)
        if (isAnime && id == baseId && baseId.isNotBlank() && baseId.all { it.isDigit() }) {
            out.add("anime" to "anilist:$baseId")
        }
        out.add(defaultType to id)
        return out.toList()
    }

    /**
     * The (type, id) pair to send to an add-on, honouring its `idPrefixes`.
     *
     * [default] is used verbatim when the add-on declares no prefixes (so behaviour is unchanged
     * for those). Otherwise the first candidate the add-on accepts wins; when nothing matches we
     * fall back to [default] rather than dropping the add-on entirely.
     */
    fun chooseRequest(
        default: Pair<String, String>,
        candidates: List<Pair<String, String>>,
        idPrefixes: List<String>,
    ): Pair<String, String> {
        if (idPrefixes.isEmpty()) return default
        return candidates.firstOrNull { acceptsId(it.second, idPrefixes) } ?: default
    }
}
