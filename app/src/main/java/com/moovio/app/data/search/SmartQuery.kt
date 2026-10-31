package com.moovio.app.data.search

import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import java.util.Calendar

/** How a compiled query wants its results ordered. */
enum class SmartSort { RELEVANCE, RATING, RECENT }

/**
 * A user's typed query, compiled into filters.
 *
 * [text] is what actually goes to the providers: the words that are *not* filters. "recent action"
 * leaves "action" behind. A query made only of filters ("high rating") has no subject, so [text]
 * falls back to the genre keyword and, when there is none, is empty — the view model then publishes
 * nothing rather than asking a provider for its whole catalog.
 */
data class SmartQuery(
    val text: String,
    val type: MediaType? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRating: Double? = null,
    val genre: String? = null,
    val sort: SmartSort = SmartSort.RELEVANCE,
) {

    /** True when the query carried at least one filter, i.e. the typed words meant more than text. */
    val hasFilters: Boolean
        get() = type != null || yearFrom != null || yearTo != null ||
            minRating != null || genre != null || sort != SmartSort.RELEVANCE

    /**
     * Applies the client-side half of the query to a provider result set.
     *
     * Only the *type* also reaches the network as a search parameter (the catalog repository already
     * searches per type). Year, rating and genre are applied here, on the merged result, because
     * add-on catalogs do not accept those parameters — narrowing locally is honest, whereas
     * pretending to send them would silently do nothing.
     *
     * An item with no year is dropped when a year filter is active: it cannot be shown to match, and
     * keeping it would flood a "2024" search with everything undated.
     */
    fun applyTo(items: List<MediaItem>): List<MediaItem> {
        val filtered = items.filter { item ->
            val year = item.year?.toIntOrNull()
            (type == null || item.type == type) &&
                (yearFrom == null || (year != null && year >= yearFrom)) &&
                (yearTo == null || (year != null && year <= yearTo)) &&
                (minRating == null || (item.rating ?: 0.0) >= minRating) &&
                (genre == null || matchesGenre(item.genres, genre))
        }
        return when (sort) {
            SmartSort.RATING -> filtered.sortedByDescending { it.rating ?: 0.0 }
            SmartSort.RECENT -> filtered.sortedByDescending { it.year.orEmpty() }
            SmartSort.RELEVANCE -> filtered
        }
    }

    /**
     * One quiet line describing what was understood, or null when the query was plain text.
     * Example: `Anime · 2024 · rated 7+ · best rated first`.
     */
    fun summary(): String? {
        if (!hasFilters) return null
        val parts = buildList {
            type?.let { add(it.label) }
            when {
                yearFrom != null && yearTo != null ->
                    add(if (yearFrom == yearTo) "$yearFrom" else "$yearFrom–$yearTo")
                yearFrom != null -> add("$yearFrom and newer")
                yearTo != null -> add("up to $yearTo")
            }
            genre?.let { add(it) }
            minRating?.let { add("rated ${trimRating(it)}+") }
            when (sort) {
                SmartSort.RATING -> add("best rated first")
                SmartSort.RECENT -> add("newest first")
                SmartSort.RELEVANCE -> Unit
            }
        }
        return parts.joinToString(" · ").ifBlank { null }
    }

    private fun trimRating(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    private fun matchesGenre(genres: List<String>, wanted: String): Boolean {
        val aliases = GENRE_ALIASES[wanted] ?: listOf(wanted.lowercase().filter { it.isLetterOrDigit() })
        return genres.any { g ->
            val normalized = g.lowercase().filter { it.isLetterOrDigit() }
            aliases.any { alias -> normalized.contains(alias) }
        }
    }
}

/** Genre words the parser recognises, mapped to the label shown in [SmartQuery.summary]. */
private val GENRE_WORDS: Map<String, String> = mapOf(
    "action" to "Action",
    "comedy" to "Comedy",
    "drama" to "Drama",
    "horror" to "Horror",
    "romance" to "Romance",
    "thriller" to "Thriller",
    "fantasy" to "Fantasy",
    "mystery" to "Mystery",
    "documentary" to "Documentary",
    "animation" to "Animation",
    "animated" to "Animation",
    "adventure" to "Adventure",
    "crime" to "Crime",
    "family" to "Family",
    "musical" to "Music",
    "western" to "Western",
    "historical" to "History",
    "scifi" to "Science Fiction",
)

/**
 * Spellings an item's own genre list may use for each canonical genre.
 *
 * Add-ons label the same genre "Sci-Fi", "Science Fiction" and "scifi", so comparison is done on a
 * normalised (lowercase, letters+digits only) form matched against every known spelling.
 */
private val GENRE_ALIASES: Map<String, List<String>> = mapOf(
    "Science Fiction" to listOf("sciencefiction", "scifi"),
    "Animation" to listOf("animation", "animated"),
    "Music" to listOf("music", "musical"),
    "History" to listOf("history", "historical"),
)

private val TYPE_WORDS: Map<String, MediaType> = mapOf(
    "movie" to MediaType.MOVIE,
    "movies" to MediaType.MOVIE,
    "film" to MediaType.MOVIE,
    "films" to MediaType.MOVIE,
    "tv" to MediaType.SERIES,
    "series" to MediaType.SERIES,
    "show" to MediaType.SERIES,
    "shows" to MediaType.SERIES,
    "anime" to MediaType.ANIME,
    "manga" to MediaType.MANGA,
    "comic" to MediaType.MANGA,
    "comics" to MediaType.MANGA,
)

/** Phrases that mean "give me the best ones", not a title to look up. */
private val HIGH_RATING_PHRASES = listOf(
    "high rating", "highly rated", "highly-rated", "top rated", "best rated",
    "well rated", "great rating", "good rating", "best", "top",
)

/** Words that mean a recency window rather than a title. */
private val RECENT_WORDS = setOf("recent", "recently", "latest", "new", "newest")

/** Words that mean "older than the modern era". */
private val CLASSIC_WORDS = setOf("classic", "classics", "old", "older", "retro")

/** Filter vocabulary that must never end up in the text sent to a provider. */
private val NOISE_WORDS = setOf("rating", "ratings", "rated", "imdb", "score", "scored")

/** Default floor for "high rating": 7.0 is where a catalog stops calling something merely average. */
private const val HIGH_RATING_FLOOR = 7.0

/** Everything pre-1990 reads as "classic" for the purposes of this filter. */
private const val CLASSIC_LAST_YEAR = 1989

/** How far back "recent" reaches. Two years still covers last season's releases. */
private const val RECENT_YEARS = 2

/**
 * Turns a typed query into [SmartQuery] — offline, allocation-light and side-effect free.
 *
 * Runs before the debounced search in `SearchViewModel`, so the 300 ms pipeline and its generation
 * guard are untouched: the parser only changes *what* is searched and how the result is narrowed.
 *
 * Deliberately narrow: it recognises type words, genres, a year or a decade, a rating floor and
 * "recent"/"classic"/"best". It does not attempt to understand sentences, because a parser that
 * half-understands "that show with the boat" would strip the words the provider actually needs.
 *
 * A four-digit token only counts as a year when it could be one (`<= currentYear + 1`), so
 * "Blade Runner 2049" stays a title instead of becoming a filter for a film that does not exist yet.
 */
object SmartSearchParser {

    /** Parses [raw]. [currentYear] is injectable so relative filters are deterministic in tests. */
    fun parse(raw: String, currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)): SmartQuery {
        var working = " " + raw.lowercase().replace(Regex("\\s+"), " ").trim() + " "

        // Multi-word phrases first, so "high rating" is never left behind as "high" + "rating".
        HIGH_RATING_PHRASES.forEach { phrase -> working = working.replace(" $phrase ", " minrating ") }
        working = working
            .replace("science fiction", " scifi ")
            .replace("sci-fi", " scifi ")
            .replace("sci fi", " scifi ")
            .replace("this year", " recent ")

        val tokens = working.split(Regex("[^a-z0-9.]+")).filter { it.isNotBlank() }
        val latestYear = currentYear + 1

        var type: MediaType? = null
        var genre: String? = null
        var minRating: Double? = null
        var explicitYear: Int? = null
        var yearFrom: Int? = null
        var yearTo: Int? = null
        var sort = SmartSort.RELEVANCE
        val leftovers = mutableListOf<String>()

        for (token in tokens) {
            // --- explicit score, e.g. "rating 8.5" ---------------------------------------------
            val asScore = token.toDoubleOrNull()
            if (asScore != null && token.contains('.') && asScore in 0.0..10.0) {
                if (working.contains(" minrating ") || working.contains(" rating ") || working.contains(" imdb ")) {
                    minRating = asScore
                    if (sort == SmartSort.RELEVANCE) sort = SmartSort.RATING
                    continue
                }
            }

            // --- decade, e.g. "2020s" -----------------------------------------------------------
            if (token.length == 5 && token.endsWith("s")) {
                val start = token.take(4).toIntOrNull()
                if (start != null && start in 1900..latestYear) {
                    yearFrom = start
                    yearTo = start + 9
                    continue
                }
            }

            // --- single year, e.g. "2024" -------------------------------------------------------
            val asYear = token.toIntOrNull()
            if (asYear != null && asYear in 1900..latestYear) {
                val seen = explicitYear
                if (seen != null) {
                    // Two years read as a range ("2010 2015").
                    yearFrom = minOf(seen, asYear)
                    yearTo = maxOf(seen, asYear)
                    explicitYear = null
                } else {
                    explicitYear = asYear
                    yearFrom = asYear
                    yearTo = asYear
                }
                continue
            }

            when {
                token == "minrating" -> {
                    minRating = maxOf(minRating ?: 0.0, HIGH_RATING_FLOOR)
                    sort = SmartSort.RATING
                }
                token in RECENT_WORDS -> {
                    yearFrom = maxOf(yearFrom ?: 0, currentYear - RECENT_YEARS)
                    if (sort == SmartSort.RELEVANCE) sort = SmartSort.RECENT
                }
                token in CLASSIC_WORDS -> {
                    yearTo = minOf(yearTo ?: CLASSIC_LAST_YEAR, CLASSIC_LAST_YEAR)
                }
                TYPE_WORDS.containsKey(token) -> type = TYPE_WORDS[token]
                GENRE_WORDS.containsKey(token) -> genre = GENRE_WORDS[token]
                token in NOISE_WORDS -> Unit
                else -> leftovers += token
            }
        }

        // "high rating 8" / "rating 7": a bare number right after the rating words is the floor.
        if (working.contains(" minrating ") || working.contains(" rating ")) {
            val next = working.substringAfter(" minrating ", "")
                .ifBlank { working.substringAfter(" rating ", "") }
                .trim()
                .substringBefore(' ')
                .toDoubleOrNull()
            if (next != null && next in 0.0..10.0) {
                minRating = maxOf(minRating ?: 0.0, next)
                if (sort == SmartSort.RELEVANCE) sort = SmartSort.RATING
            }
        }

        val text = leftovers.joinToString(" ").trim()
        return SmartQuery(
            text = text.ifBlank { genre?.lowercase().orEmpty() },
            type = type,
            yearFrom = yearFrom,
            yearTo = yearTo,
            minRating = minRating,
            genre = genre,
            sort = sort,
        )
    }
}
