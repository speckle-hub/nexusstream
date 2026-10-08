package com.novastream.app.data.remote

import com.google.gson.JsonObject
import com.novastream.app.data.adult.adultTitlesMatch
import com.novastream.app.data.model.CastMember
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.MetaDetail
import kotlinx.coroutines.withTimeout

/** Jikan (MyAnimeList) client — fallback NSFW anime metadata source. */
object JikanClient {
    private const val BASE = "https://api.jikan.moe/v4"

    /** Jikan is a *fallback*: bound it so a 504 can't stall the detail page or a search. */
    private const val DETAIL_TIMEOUT_MS = 8_000L

    private fun item(o: JsonObject): MediaItem? {
        val id = o.get("mal_id")?.asInt?.toString() ?: return null
        val title = o.get("title_english")?.takeIf { !it.isJsonNull }?.asString
            ?: o.get("title")?.asString ?: return null
        val images = o.getAsJsonObject("images")?.getAsJsonObject("jpg")
        return MediaItem(
            id = id,
            type = MediaType.NSFW_ANIME,
            title = title,
            poster = images?.get("large_image_url")?.asString ?: images?.get("image_url")?.asString,
            year = o.getAsJsonObject("aired")?.getAsJsonObject("prop")?.getAsJsonObject("from")?.get("year")?.takeIf { !it.isJsonNull }?.asInt?.toString(),
            rating = o.get("score")?.takeIf { !it.isJsonNull }?.asDouble,
            description = o.get("synopsis")?.takeIf { !it.isJsonNull }?.asString,
            addonId = "jikan",
            addonName = "Jikan (MAL)",
        )
    }

    private suspend fun listRaw(path: String): List<JsonObject> {
        val root = parseJson(httpGet("$BASE$path")).asJsonObject
        val arr = root.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
    }

    private suspend fun list(path: String): List<MediaItem> = listRaw(path).mapNotNull { item(it) }

    /** Per-page fail-safe: a 504 on page 2 must not discard page 1's results. */
    private suspend fun listSafe(path: String): List<MediaItem> =
        runCatching { list(path) }.getOrDefault(emptyList())

    private suspend fun listAdultSafe(path: String): List<MediaItem> =
        runCatching { listAdult(path) }.getOrDefault(emptyList())

    /** Adult entries by MAL rating: "Rx - Hentai" or "R+ - Mild Nudity". */
    private fun isAdult(o: JsonObject): Boolean {
        val r = o.get("rating")?.asString?.lowercase() ?: return false
        return r.startsWith("rx") || r.startsWith("r+")
    }

    private suspend fun listAdult(path: String): List<MediaItem> =
        listRaw(path).filter { isAdult(it) }.mapNotNull { item(it) }

    /**
     * Jikan intermittently returns 504 when MyAnimeList is unreachable — retry once.
     */
    private suspend fun withRetry(block: suspend () -> List<MediaItem>): List<MediaItem> {
        val first = runCatching { block() }.getOrDefault(emptyList())
        if (first.isNotEmpty()) return first
        kotlinx.coroutines.delay(700)
        return runCatching { block() }.getOrDefault(emptyList())
    }

    /**
     * Jikan caps each page at 25, so an adult search pages twice (up to 50) to return as much as
     * the API allows instead of stopping at the first page. Each page is fail-safe on its own, so
     * one flaky page never voids the other.
     */
    suspend fun nsfwSearch(query: String): List<MediaItem> {
        val enc = java.net.URLEncoder.encode(query, "UTF-8")
        val strict = withRetry {
            listSafe("/anime?q=$enc&rating=rx&sfw=false&limit=25") +
                listSafe("/anime?q=$enc&rating=rx&sfw=false&limit=25&page=2")
        }
        if (strict.isNotEmpty()) return strict
        // Fallback when the filtered endpoint is down: search normally, keep adult-rated entries.
        return withRetry {
            listAdultSafe("/anime?q=$enc&limit=25") +
                listAdultSafe("/anime?q=$enc&limit=25&page=2")
        }
    }

    suspend fun nsfwTop(): List<MediaItem> {
        val strict = withRetry { list("/top/anime?filter=bypopularity&rating=rx&sfw=false&limit=25") }
        if (strict.isNotEmpty()) return strict
        // Fallback: page through the general top list and filter adult entries client-side.
        return withRetry {
            listAdult("/top/anime?page=1&limit=25") + listAdult("/top/anime?page=2&limit=25")
        }
    }

    // ---- Full detail (used as the NSFW-anime metadata fallback) --------------

    /**
     * Full Jikan detail for a MAL id, shaped like every other [MetaDetail] the app renders.
     *
     * Jikan is the fallback metadata source for NSFW anime: when AniList is unreachable or only
     * returns a thin record, the detail page can still show a synopsis, poster, genres and studios
     * instead of a hard "Couldn't load details" error.
     */
    suspend fun details(malId: String): MetaDetail? {
        val raw = runCatching { withTimeout(DETAIL_TIMEOUT_MS) { httpGet("$BASE/anime/$malId/full") } }
            .getOrNull() ?: return null
        val data = runCatching { parseJson(raw).asJsonObject.getAsJsonObject("data") }.getOrNull()
            ?: return null
        return toDetail(malId, data)
    }

    /**
     * Resolve a title to a MAL entry, then fetch its full detail. Used when only a title is known
     * (an AniList id cannot be translated back to MAL without the AniList call we just failed on).
     */
    suspend fun detailsByTitle(title: String): MetaDetail? {
        val enc = java.net.URLEncoder.encode(title, "UTF-8")
        val candidates = runCatching {
            withTimeout(DETAIL_TIMEOUT_MS) { listRaw("/anime?q=$enc&limit=10&sfw=false") }
        }.getOrDefault(emptyList())
        if (candidates.isEmpty()) return null
        fun normalize(s: String): String =
            s.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
        fun titlesOf(o: JsonObject): List<String> = buildList {
            o.get("title_english")?.takeIf { !it.isJsonNull }?.asString?.let { add(it) }
            o.get("title")?.takeIf { !it.isJsonNull }?.asString?.let { add(it) }
            o.getAsJsonArray("titles")?.forEach { t ->
                if (t.isJsonObject) t.asJsonObject.get("title")?.asString?.let { add(it) }
            }
        }
        val want = normalize(title)
        // Exact (case/punctuation-insensitive) match first, then a shared-significant-word match.
        // Deliberately NO "first adult entry / first result" fallback: Jikan's loose `?q=` search
        // happily answers with a completely unrelated hentai title, and taking it is how the
        // detail page opened a different series than the one the user clicked. No credible match
        // means no detail from here — the caller falls back to what the user actually selected.
        val match = candidates.firstOrNull { c -> titlesOf(c).any { normalize(it) == want } }
            ?: candidates.firstOrNull { c -> titlesOf(c).any { adultTitlesMatch(title, it) } }
            ?: return null
        val id = match.get("mal_id")?.asInt?.toString() ?: return null
        return details(id)
    }

    private fun toDetail(malId: String, o: JsonObject): MetaDetail? {
        val title = o.get("title_english")?.takeIf { !it.isJsonNull }?.asString
            ?: o.get("title")?.takeIf { !it.isJsonNull }?.asString
            ?: return null
        val images = o.getAsJsonObject("images")?.getAsJsonObject("jpg")
        val poster = images?.get("large_image_url")?.asString
            ?: images?.get("image_url")?.asString
        val genres = o.getAsJsonArray("genres")?.mapNotNull { g ->
            if (g.isJsonObject) g.asJsonObject.get("name")?.asString else null
        } ?: emptyList()
        val studios = o.getAsJsonArray("studios")?.mapNotNull { s ->
            if (s.isJsonObject) s.asJsonObject.get("name")?.asString else null
        } ?: emptyList()
        val alt = buildList {
            o.get("title_japanese")?.takeIf { !it.isJsonNull }?.asString?.let { add(it) }
            o.getAsJsonArray("title_synonyms")?.forEach { s -> s.asString?.let { add(it) } }
            o.getAsJsonArray("titles")?.forEach { t ->
                if (t.isJsonObject) t.asJsonObject.get("title")?.asString?.let { add(it) }
            }
        }.filter { it.isNotBlank() && it != title }.distinct().take(6)
        val year = o.getAsJsonObject("aired")?.getAsJsonObject("prop")?.getAsJsonObject("from")
            ?.get("year")?.takeIf { !it.isJsonNull }?.asInt
        val episodes = o.get("episodes")?.takeIf { !it.isJsonNull }?.asInt
        return MetaDetail(
            id = malId,
            type = "anime",
            name = title,
            poster = poster,
            background = poster,
            description = o.get("synopsis")?.takeIf { !it.isJsonNull }?.asString,
            releaseInfo = year?.toString(),
            imdbRating = o.get("score")?.takeIf { !it.isJsonNull }?.asDouble?.let { String.format("%.1f", it) },
            runtime = o.get("duration")?.takeIf { !it.isJsonNull }?.asString,
            genres = genres,
            cast = studios,
            addonId = "jikan",
            altTitles = alt,
            votes = o.get("favorites")?.takeIf { !it.isJsonNull }?.asInt,
            status = o.get("status")?.takeIf { !it.isJsonNull }?.asString,
            totalEpisodes = episodes,
            castMembers = studios.map { CastMember(name = it, character = "Studio") },
        )
    }
}
