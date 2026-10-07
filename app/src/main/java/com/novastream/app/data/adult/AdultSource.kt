package com.novastream.app.data.adult

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * A modular built-in adult content provider.
 *
 * Adding or removing a site is a one-line change in [AdultSources] — the repository and UI only
 * ever talk to this interface. Every method may throw [AdultSourceException] (or any exception);
 * the aggregating repository isolates failures per source.
 */
interface AdultSource {
    /** Stable id, also used as the [com.novastream.app.data.model.MediaItem.addonId] for its items. */
    val id: String

    /** Human-readable name shown in row titles and UI. */
    val name: String

    /** Home rows: Popular / Trending / Latest / popular categories. */
    suspend fun home(): List<AdultRow>

    /** Adult-only keyword search. */
    suspend fun search(query: String): List<AdultVideo>

    /** Category / tag listing. */
    suspend fun categories(): List<AdultCategory>

    /** Videos inside a category. */
    suspend fun category(category: AdultCategory): List<AdultVideo>

    /** Full detail (metadata + playable streams + related) for a video page URL. */
    suspend fun detail(url: String): AdultDetail
}

/**
 * Fetch each listing independently so one bad row doesn't lose the rest; only surface an error if
 * *every* row failed (which is what a block/outage looks like).
 */
internal suspend fun adultRows(
    source: AdultSource,
    defs: List<Pair<String, String>>,
    fetch: suspend (String) -> List<AdultVideo>,
): List<AdultRow> = coroutineScope {
    val jobs = defs.map { (title, url) -> title to async { runCatching { fetch(url) } } }
    val rows = ArrayList<AdultRow>()
    var lastError: Exception? = null
    for ((title, job) in jobs) {
        job.await()
            .onSuccess { if (it.isNotEmpty()) rows.add(AdultRow("${source.name} \u00b7 $title", source.id, it)) }
            .onFailure { lastError = it as? Exception ?: Exception(it) }
    }
    if (rows.isEmpty() && lastError != null) throw lastError!!
    rows
}

/**
 * Build additional rows from the source's top categories. These double as the "Categories" rows
 * on Real 18+ and keep the tab populated even when a site's section pages are rate-limited.
 */
internal suspend fun adultCategoryRows(
    source: AdultSource,
    max: Int = 3,
    fetchCategory: suspend (AdultCategory) -> List<AdultVideo>,
): List<AdultRow> = coroutineScope {
    val categories = runCatching { source.categories() }.getOrDefault(emptyList()).take(max)
    val jobs = categories.map { category -> category to async { runCatching { fetchCategory(category) } } }
    val rows = ArrayList<AdultRow>()
    for ((category, job) in jobs) {
        val videos = job.await().getOrDefault(emptyList())
        if (videos.isNotEmpty()) rows.add(AdultRow("${source.name} \u00b7 ${category.name}", source.id, videos))
    }
    rows
}

/**
 * Build a source's home from two passes that run **concurrently**: the section listings and the
 * top-category rows. Serialising them (listings, then categories, then their pages) is what made
 * Real 18+ feel slow — now the tab fills as soon as the first pass lands.
 */
internal suspend fun adultHome(
    source: AdultSource,
    defs: List<Pair<String, String>>,
    fetchList: suspend (String) -> List<AdultVideo>,
): List<AdultRow> = coroutineScope {
    val listings = async { runCatching { adultRows(source, defs, fetchList) } }
    val categories = async { runCatching { adultCategoryRows(source) { source.category(it) } } }
    val rows = listings.await().getOrDefault(emptyList<AdultRow>()) +
        categories.await().getOrDefault(emptyList<AdultRow>())
    if (rows.isEmpty()) {
        val err = listings.await().exceptionOrNull()
        throw (err as? Exception) ?: AdultSourceException("No content available right now")
    }
    rows
}

// ---- Shared parse helpers -----------------------------------------------------

internal val ADULT_DOT: Set<RegexOption> =
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)

internal fun adultRx(pattern: String): Regex = Regex(pattern, ADULT_DOT)

/** First capture group of [pattern], trimmed, or null when absent/blank. */
internal fun String.capture(pattern: String, group: Int = 1): String? =
    adultRx(pattern).find(this)?.groupValues?.getOrNull(group)?.trim()?.takeIf { it.isNotEmpty() }

/** Unescapes the handful of HTML entities seen in titles and URLs. */
internal fun adultUnescape(s: String?): String? = s
    ?.replace("&amp;", "&")
    ?.replace("&#039;", "'")
    ?.replace("&apos;", "'")
    ?.replace("&quot;", "\"")
    ?.replace("&lt;", "<")
    ?.replace("&gt;", ">")
    ?.trim()

/** Unescapes JSON's `\/` and `\u0026` that these pages embed inside JS blobs. */
internal fun adultUnescapeJson(s: String?): String? = s
    ?.replace("\\/", "/")
    ?.replace("\\u0026", "&")
    ?.replace("\\\"", "\"")

/** `1094` -> `18:14`, `3725` -> `1:02:05`. */
internal fun adultDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Strips HTML tags and collapses whitespace — used to read text out of markup blocks. */
internal fun stripTags(s: String): String =
    s.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

/** Best-effort resolution label from a stream URL (`..._360p.mp4` -> `360p`). */
internal fun adultQualityFromUrl(url: String): String? =
    Regex("""(\d{3,4})p""").find(url)?.groupValues?.getOrNull(1)?.plus("p")

/**
 * Normalises a scraped media URL to an absolute `http(s)` URL: resolves protocol-relative
 * (`//host/path.mp4`) values, un-escapes JSON `\/` and drops a stray trailing backslash left over
 * from escaped JS strings. Returns null when the value isn't an absolute web URL at all.
 */
internal fun adultAbsoluteUrl(url: String?): String? {
    val raw = url?.trim()?.removeSuffix("\\")?.trim('"')?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val clean = raw.replace("\\/", "/")
    return when {
        clean.startsWith("https://") -> clean
        clean.startsWith("http://") -> clean
        clean.startsWith("//") -> "https:$clean"
        else -> null
    }
}

/**
 * Headers sent both by the player and by the pre-flight stream probe, so what we *list* is exactly
 * what the player will *request*.
 */
internal fun adultPlayHeaders(referer: String?, cookie: String? = null): Map<String, String> {
    val headers = LinkedHashMap<String, String>()
    headers["User-Agent"] = AdultHttp.BROWSER_UA
    headers["Accept"] = "*/*"
    if (!referer.isNullOrBlank()) headers["Referer"] = referer
    if (!cookie.isNullOrBlank()) headers["Cookie"] = cookie
    return headers
}

/** Value of `<meta property="og:…">`, matching either attribute order. */
internal fun String.ogContent(property: String): String? =
    capture("""<meta[^>]*property="$property"[^>]*content="([^"]+)""")
        ?: capture("""<meta[^>]*content="([^"]+)"[^>]*property="$property""")

/** Collapses runs of whitespace — titles pulled out of markup are usually multi-line. */
internal fun adultCollapse(s: String?): String? =
    s?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf { it.isNotEmpty() }

// ---- Search relevance -------------------------------------------------------

/**
 * Significant keywords from a search query: lower-cased, split on any non-letter/digit, and
 * limited to tokens of two or more characters. Filler words ("a", "of") and punctuation are
 * dropped so they cannot drag in unrelated results.
 */
internal fun adultQueryTokens(query: String): List<String> =
    query.lowercase()
        .split(Regex("""[^\p{L}\p{N}]+"""))
        .filter { it.length >= 2 }
        .distinct()

/**
 * True when [title] contains at least one query [token]. With no usable tokens (short/blank
 * query) everything matches, so the filter never wrongly empties the list.
 */
internal fun adultTitleMatches(title: String, tokens: List<String>): Boolean {
    if (tokens.isEmpty()) return true
    val lower = title.lowercase()
    return tokens.any { lower.contains(it) }
}

/** How many query [tokens] appear in [title] — used to rank the most relevant results first. */
internal fun adultRelevanceScore(title: String, tokens: List<String>): Int {
    if (tokens.isEmpty()) return 0
    val lower = title.lowercase()
    return tokens.count { lower.contains(it) }
}

/**
 * Words that identify no title on their own: articles, prepositions and the Japanese genitive
 * particle. They are kept by [adultQueryTokens] (for ranking), but they must never be enough to
 * declare two titles the *same* one — "to" is a substring of "Netokano", which is exactly how
 * "Boku to Misaki-sensei" ended up resolving to a different series.
 */
private val FILLER_TOKENS = setOf(
    "a", "an", "the", "to", "of", "in", "on", "at", "for", "and", "or", "is", "it",
    "by", "with", "from", "no", "de", "la", "le", "el", "lo", "ni", "wa", "wo", "ga",
)

/**
 * The identifying keywords of a query: [adultQueryTokens] minus the filler words. Falls back to
 * the raw tokens when the query is nothing but fillers, so a degenerate query stays permissive
 * instead of silently matching nothing.
 */
internal fun adultSignificantTokens(query: String): List<String> {
    val all = adultQueryTokens(query)
    return all.filterNot { it in FILLER_TOKENS }.ifEmpty { all }
}

/**
 * True when [actual] plausibly names the same title as [expected]: at least one *significant*
 * word of [expected] appears in [actual]. Punctuation, case and word order are irrelevant, and a
 * filler-only overlap ("to", "the", "no") never counts — so a loose upstream search result can
 * not be mistaken for the title the user picked.
 *
 * Returns false when [expected] carries no usable keywords at all: a caller deciding "is this
 * the same title?" must answer no when there is nothing to compare.
 */
internal fun adultTitlesMatch(expected: String, actual: String): Boolean {
    val tokens = adultSignificantTokens(expected)
    if (tokens.isEmpty()) return false
    val hay = actual.lowercase()
    return tokens.any { hay.contains(it) }
}
