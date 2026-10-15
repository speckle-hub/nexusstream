package com.novastream.app.data.sfw

import com.novastream.app.data.adult.adultQueryTokens
import com.novastream.app.data.adult.adultRelevanceScore
import com.novastream.app.data.adult.adultTitlesMatch
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.model.StreamSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * A modular built-in movie/TV provider.
 *
 * Metadata stays with TMDB, so a source only answers: *what titles match this
 * query*, *what rows fill the catalog*, *which episodes exist for this series page*
 * and *where does this title/episode actually play*. Resolver-only sources (Vidsrc
 * family: TMDB-id-keyed embeds with no search of their own) implement just
 * [streamsForTmdb] and keep the list defaults.
 *
 * Every method may throw; [SfwRepository] isolates failures per source so one dead
 * site degrades to a notice while the others keep resolving. Adding or removing a
 * site is a one-line change in [SfwSources].
 */
interface SfwSource {
    /** Stable id, also used as the `addonId` of its items and streams. */
    val id: String

    /** Human-readable name shown in rows and the stream list. */
    val name: String

    /** True when this source only ever serves movies (YTS). */
    val moviesOnly: Boolean get() = false

    /** Home rows: Popular / Latest / Trending where the site offers them. */
    suspend fun home(): List<SfwRow> = emptyList()

    /** Title-level keyword search. */
    suspend fun search(query: String): List<SfwTitle> = emptyList()

    /**
     * Full detail for a title page URL: metadata, episode list (series) and
     * playable streams (movies resolve them here directly).
     */
    suspend fun detail(url: String): SfwDetail = SfwDetail(
        title = SfwTitle(sourceId = id, url = url, title = "", kind = MediaType.MOVIE),
    )

    /** Playable streams for an episode page produced by [detail]. */
    suspend fun streamsForEpisode(episode: SfwEpisode): List<StreamSource> =
        detail(episode.url).streams

    /**
     * Direct resolution by TMDB id (Vidsrc family): movie, or one series episode
     * when [season]/[episode] are present. Default unsupported.
     */
    suspend fun streamsForTmdb(tmdbId: String, season: Int?, episode: Int?): List<StreamSource> =
        emptyList()
}

/**
 * A modular built-in anime provider. The app gets anime *metadata* from AniList,
 * so a source only answers *which episodes exist for this title* and *where does
 * this episode actually play* — the same contract as
 * [com.novastream.app.data.hentai.HentaiSource], SFW edition.
 *
 * Every method may throw; [SfwAnimeRepository] isolates failures per source.
 * Adding or removing a site is a one-line change in [SfwAnimeSources].
 */
interface SfwAnimeSource {
    /** Stable id, also used as the `addonId` of its items and streams. */
    val id: String

    /** Human-readable name shown in the stream list. */
    val name: String

    /**
     * Episode-level results for [query], most relevant first. Series-indexed sites
     * expand the top matches into episodes before returning.
     */
    suspend fun search(query: String): List<SfwAnimeEpisode>

    /** Playable streams for one episode page returned by [search]. */
    suspend fun streams(episode: SfwAnimeEpisode): List<StreamSource>

    /**
     * Series-level results for catalog search rows. The default groups [search]
     * hits by series (first poster wins); API sources with native anime-level
     * search (AnimePahe) override this to skip the episode expansion.
     */
    suspend fun searchTitles(query: String): List<SfwAnimeTitle> = coroutineScope {
        val hits = runCatching { search(query) }.getOrDefault(emptyList())
        hits.groupBy { it.series.trim().lowercase() }
            .mapNotNull { (_, group) ->
                val head = group.firstOrNull() ?: return@mapNotNull null
                SfwAnimeTitle(
                    sourceId = id,
                    url = group.firstNotNullOfOrNull { seriesUrlOf(it) } ?: head.url,
                    title = head.series,
                    poster = group.firstNotNullOfOrNull { it.poster },
                )
            }
    }

    /** Series page backing an episode — defaults to the episode page itself. */
    fun seriesUrlOf(episode: SfwAnimeEpisode): String? = null
}

// ---- Shared SFW helpers -------------------------------------------------------

/** Significant query keywords (shared adult tokenizer: filler-safe, ranking-ready). */
internal fun sfwTokens(query: String): List<String> = adultQueryTokens(query)

/** True when [title] matches at least one query keyword (empty query matches all). */
internal fun sfwTitleMatches(title: String, tokens: List<String>): Boolean {
    if (tokens.isEmpty()) return true
    val lower = title.lowercase()
    return tokens.any { lower.contains(it) }
}

/** Keyword overlap score used to rank the most relevant titles first. */
internal fun sfwRelevance(title: String, tokens: List<String>): Int =
    adultRelevanceScore(title, tokens)

/**
 * Strict same-title gate (shared adult matcher: significant-token overlap, so a
 * loose upstream search can never substitute a different title).
 */
internal fun sfwSameTitle(expected: String, actual: String): Boolean =
    adultTitlesMatch(expected, actual)

/**
 * Season/episode from a Stremio-style series id (`tt123:2:5` → 2/5), an `S02E05`
 * marker, or a trailing episode number (anime-style, season left null).
 */
internal fun sfwSeasonEpisode(videoId: String?, itemId: String?): Pair<Int?, Int?> {
    val raw = videoId?.takeIf { it.isNotBlank() } ?: return null to null
    if (raw == itemId) return null to null
    Regex(""":(\d{1,3}):(\d{1,4})$""").find(raw)?.let {
        val s = it.groupValues[1].toIntOrNull()
        val e = it.groupValues[2].toIntOrNull()
        if (s != null && e != null && s in 1..99 && e in 1..9999) return s to e
    }
    Regex("""[Ss](\d{1,2})[Ee](\d{1,3})""").find(raw)?.let {
        val s = it.groupValues[1].toIntOrNull()
        val e = it.groupValues[2].toIntOrNull()
        if (s != null && e != null) return s to e
    }
    return null to Regex("""(\d{1,4})$""").find(raw)?.groupValues?.get(1)
        ?.toIntOrNull()?.takeIf { it in 1..9999 }
}

/**
 * Fetch each listing independently so one bad row doesn't lose the rest; only
 * surface an error when *every* row failed (which is what a block/outage looks like).
 */
internal suspend fun sfwRows(
    source: SfwSource,
    type: MediaType,
    defs: List<Pair<String, String>>,
    fetch: suspend (String) -> List<SfwTitle>,
): List<SfwRow> = coroutineScope {
    val jobs = defs.map { (title, url) -> title to async { runCatching { fetch(url) } } }
    val rows = ArrayList<SfwRow>()
    var lastError: Exception? = null
    for ((title, job) in jobs) {
        job.await()
            .onSuccess { if (it.isNotEmpty()) rows.add(SfwRow("${source.name} · $title", source.id, type, it)) }
            .onFailure { lastError = it as? Exception ?: Exception(it) }
    }
    if (rows.isEmpty() && lastError != null) throw lastError!!
    rows
}
