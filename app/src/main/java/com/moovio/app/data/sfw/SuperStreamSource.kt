package com.moovio.app.data.sfw

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.moovio.app.data.adult.AdultHttp
import com.moovio.app.data.adult.adultPlayHeaders
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.remote.str
import java.net.URLEncoder

/**
 * SuperStream built-in source (movies + TV, StreamPlay-style app API).
 *
 * Unlike the HTML indexes, SuperStream-family backends expose a JSON app API:
 * a `Search` module returns title records (`id`, `title`, `poster`, `year`,
 * `type`), a detail call returns seasons/episodes, and a link call returns the
 * playable file list (`{file, label}`). Every hop is JSON-parsed (no markup
 * scraping), and each candidate API host is tried with per-host isolation.
 */
object SuperStreamSource : SfwSource {

    override val id = "superstream"
    override val name = "SuperStream"

    private val HOSTS = listOf(
        "https://showbox.shegu.net",
        "https://superstream.onl",
    )

    private fun base(): String = HOSTS.first()

    override suspend fun home(): List<SfwRow> {
        val rows = ArrayList<SfwRow>()
        var lastError: Exception? = null
        for ((title, module, kind) in listOf(
            Triple("Trending Movies", "TrendingMovies", MediaType.MOVIE),
            Triple("Trending TV", "TrendingTV", MediaType.SERIES),
        )) {
            val titles = runCatching { parseTitles(api(module, null), kind) }
                .onFailure { lastError = it as? Exception ?: Exception(it) }
                .getOrDefault(emptyList())
            if (titles.isNotEmpty()) rows.add(SfwRow("$name · $title", id, kind, titles))
        }
        if (rows.isEmpty() && lastError != null) throw lastError!!
        return rows
    }

    override suspend fun search(query: String): List<SfwTitle> =
        runCatching { parseTitles(api("Search", query), MediaType.MOVIE) }
            .getOrDefault(emptyList())

    override suspend fun detail(url: String): SfwDetail {
        val recordId = Regex("""/(?:movie|tv|detail)/(\w+)""").find(url)?.groupValues?.get(1)
            ?: Regex("""[?&]id=(\w+)""").find(url)?.groupValues?.get(1)
            ?: throw SfwSourceException("Unrecognised SuperStream url")
        val json = api("Detail", recordId)
        val data = runCatching {
            JsonParser.parseString(json).asJsonObject.get("data")?.asJsonObject
        }.getOrNull() ?: throw SfwSourceException("SuperStream detail unavailable")
        val title = data.str("title")?.takeIf { it.isNotBlank() } ?: "SuperStream title"
        val kind = if (looksLikeSeriesRecord(data)) MediaType.SERIES else MediaType.MOVIE
        val episodes = parseEpisodes(data, title)
        val streams = if (kind == MediaType.MOVIE) {
            parseLinks(data.get("links") ?: data.get("sources")).map { (file, label) ->
                fileStream(file, label)
            }
        } else emptyList()
        return SfwDetail(
            title = SfwTitle(id, url, title, kind, data.str("poster")?.takeIf { it.startsWith("http") }),
            description = data.str("description")?.takeIf { it.isNotBlank() },
            episodes = episodes,
            streams = streams,
        )
    }

    override suspend fun streamsForEpisode(episode: SfwEpisode): List<StreamSource> {
        val json = api("EpisodeLinks", episode.url.substringAfterLast('/'))
        val data = runCatching {
            JsonParser.parseString(json).asJsonObject
        }.getOrNull() ?: return emptyList()
        return parseLinks(data.get("data") ?: data.get("links")).map { (file, label) ->
            fileStream(file, label)
        }
    }

    // ---- API + parsing (internal so tests pin the contract without network) ----

    internal suspend fun api(module: String, arg: String?): String {
        var lastError: Exception? = null
        for (host in HOSTS) {
            val url = buildString {
                append(host).append("/api/v1/?module=").append(module)
                if (!arg.isNullOrBlank()) append("&q=").append(URLEncoder.encode(arg, "UTF-8"))
            }
            try {
                return AdultHttp.get(url, referer = host)
            } catch (e: Exception) {
                lastError = e as? Exception ?: Exception(e)
            }
        }
        throw lastError ?: SfwSourceException("SuperStream unreachable")
    }

    /** Parse a title list response: root `data[]` with `id/title/poster/year/type`. */
    internal fun parseTitles(json: String, kind: MediaType): List<SfwTitle> {
        val data = runCatching {
            JsonParser.parseString(json).asJsonObject.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return data.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val title = o.str("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val recordId = o.str("id") ?: o.get("id")?.takeIf { it.isJsonPrimitive }?.asString
            ?: return@mapNotNull null
            SfwTitle(
                sourceId = id,
                url = "${base()}/detail/$recordId",
                title = title,
                kind = if (looksLikeSeriesRecord(o)) MediaType.SERIES else kind,
                poster = o.str("poster")?.takeIf { it.startsWith("http") },
                year = o.str("year")?.take(4)?.takeIf { it[0] == '1' || it[0] == '2' },
            )
        }.distinctBy { it.url }
    }

    internal fun parseEpisodes(data: JsonObject, series: String): List<SfwEpisode> {
        val seasons = data.get("seasons")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        val out = ArrayList<SfwEpisode>()
        for (s in seasons) {
            val so = s.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val seasonNum = so.get("season_number")?.takeIf { it.isJsonPrimitive }?.asInt
                ?: so.str("season")?.toIntOrNull() ?: continue
            val eps = so.get("episodes")?.takeIf { it.isJsonArray }?.asJsonArray ?: continue
            for (e in eps) {
                val eo = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val epNum = eo.get("episode_number")?.takeIf { it.isJsonPrimitive }?.asInt
                    ?: eo.str("episode")?.toIntOrNull() ?: continue
                val linkId = eo.str("id") ?: eo.str("link") ?: continue
                out.add(
                    SfwEpisode(
                        sourceId = id,
                        series = series,
                        title = eo.str("title")?.takeIf { it.isNotBlank() } ?: "Episode $epNum",
                        url = "${base()}/episode/$linkId",
                        season = seasonNum,
                        episode = epNum,
                    )
                )
            }
        }
        return out
    }

    /** Parse a file list: `[{file, label}]` (or a bare string array) into pairs. */
    internal fun parseLinks(node: com.google.gson.JsonElement?): List<Pair<String, String?>> {
        val arr = node?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            if (el.isJsonPrimitive) {
                val file = el.asString.takeIf { it.startsWith("http") } ?: return@mapNotNull null
                file to null
            } else {
                val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val file = o.str("file")?.takeIf { it.startsWith("http") } ?: return@mapNotNull null
                file to o.str("label")
            }
        }.distinctBy { it.first }
    }

    private fun fileStream(file: String, label: String?): StreamSource = StreamSource(
        url = file,
        name = name,
        addonId = id,
        addonName = name,
        quality = SfwEmbeds.normalizeLabel(label) ?: SfwEmbeds.qualityOf(file),
        headers = adultPlayHeaders(base()),
    )

    internal fun looksLikeSeriesRecord(o: JsonObject): Boolean {
        val type = o.str("type").orEmpty().lowercase()
        if ("tv" in type || "series" in type || "show" in type) return true
        if ("movie" in type || "film" in type) return false
        return o.has("seasons")
    }
}
