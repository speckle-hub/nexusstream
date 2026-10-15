package com.novastream.app.data.sfw

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.remote.str
import java.net.URLEncoder

/**
 * AnimePahe built-in anime source.
 *
 * AnimePahe exposes a JSON API: `?m=search&q=` returns anime-level records
 * (session, title, poster, episode count), `?m=release&id={animeId}&sort=episode_asc`
 * pages the episode list, and each episode resolves through a `pahe.win` window
 * into a `kwik` embed whose packed script hides the `.m3u8` (unpacked by
 * [SfwEmbeds.jsUnpackAll]). Search is natively anime-level, so [searchTitles] is
 * overridden to skip the episode expansion the default would do.
 */
object AnimePaheSource : SfwAnimeSource {

    override val id = "animepahe"
    override val name = "AnimePahe"

    private const val BASE = "https://animepahe.ru"
    private const val API = "https://animepahe.ru/api"

    override suspend fun searchTitles(query: String): List<SfwAnimeTitle> =
        parseSearch(AdultHttp.get("$API?m=search&q=${URLEncoder.encode(query.trim(), "UTF-8")}", referer = BASE))

    override suspend fun search(query: String): List<SfwAnimeEpisode> {
        val titles = runCatching { searchTitles(query) }.getOrDefault(emptyList())
        if (titles.isEmpty()) return emptyList()
        val out = ArrayList<SfwAnimeEpisode>()
        for (title in titles.take(MAX_SERIES)) {
            out += runCatching { episodesOf(title) }.getOrDefault(emptyList())
        }
        return out.distinctBy { it.url }
    }

    override suspend fun streams(episode: SfwAnimeEpisode): List<StreamSource> {
        val play = AdultHttp.get(episode.url, referer = BASE)
        // The play page links one pahe_win window per quality/server.
        val windows = paheWindows(play, episode.url)
        for (window in windows.take(3)) {
            val streams = runCatching {
                val embed = AdultHttp.get(window, referer = episode.url)
                val unpacked = SfwEmbeds.jsUnpackAll(embed)
                SfwEmbeds.toStreams(
                    SfwEmbeds.mediaUrls(unpacked),
                    id,
                    name,
                    SfwEmbeds.rootOf(window).ifBlank { window },
                )
            }.getOrDefault(emptyList())
            if (streams.isNotEmpty()) return streams
        }
        return emptyList()
    }

    // ---- Parsing (internal so tests pin the contract without network) ----

    private const val MAX_SERIES = 3

    /** Anime-level records from `?m=search`: root `data[]`. */
    internal fun parseSearch(json: String): List<SfwAnimeTitle> {
        val data = runCatching {
            JsonParser.parseString(json).asJsonObject.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return data.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val title = o.str("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val session = o.str("session") ?: return@mapNotNull null
            SfwAnimeTitle(
                sourceId = id,
                url = "$BASE/anime/$session",
                title = title,
                poster = o.str("poster")?.takeIf { it.startsWith("http") },
            )
        }.distinctBy { it.url }
    }

    internal suspend fun episodesOf(title: SfwAnimeTitle): List<SfwAnimeEpisode> {
        val animeId = title.url.substringAfterLast('/')
        val out = ArrayList<SfwAnimeEpisode>()
        var page = 1
        while (page <= MAX_PAGES) {
            val json = AdultHttp.get("$API?m=release&id=$animeId&sort=episode_asc&page=$page", referer = BASE)
            val batch = parseRelease(json, title)
            if (batch.isEmpty()) break
            out += batch
            if (batch.size < PAGE_SIZE) break
            page++
        }
        return out
    }

    /** Episode records from `?m=release`: root `data[]` with `episode` + `session`. */
    internal fun parseRelease(json: String, title: SfwAnimeTitle): List<SfwAnimeEpisode> {
        val data = runCatching {
            JsonParser.parseString(json).asJsonObject.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
        }.getOrNull() ?: return emptyList()
        return data.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val epNum = o.get("episode")?.takeIf { it.isJsonPrimitive }?.asInt ?: return@mapNotNull null
            val session = o.str("session") ?: return@mapNotNull null
            val animeSession = title.url.substringAfterLast('/')
            SfwAnimeEpisode(
                sourceId = id,
                series = title.title,
                title = o.str("title")?.takeIf { it.isNotBlank() } ?: "Episode $epNum",
                url = "$BASE/play/$animeSession/$session",
                episode = epNum,
                poster = title.poster,
            )
        }.distinctBy { it.url }
    }

    /** pahe_win watch windows on a play page (`https://pahe.win/e/…`). */
    internal fun paheWindows(html: String, pageUrl: String): List<String> =
        Regex("""https?://pahe\.(?:win|to|lol)/[ehi]/[^"'\s<>]+""").findAll(html.replace("\\/", "/"))
            .map { it.value }
            .distinct()
            .toList()
            .ifEmpty {
                // Fallback: any watch-window anchor on the page.
                SfwEmbeds.iframeSrcs(html, SfwEmbeds.rootOf(pageUrl).ifBlank { pageUrl })
            }

    private const val MAX_PAGES = 10
    private const val PAGE_SIZE = 30
}
