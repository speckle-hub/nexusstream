package com.moovio.app.data.remote

/**
 * Query shaping for [MangaDexClient], kept pure so the rating rules can be unit-tested.
 *
 * MangaDex answers an unsatisfiable `contentRating[]` filter with **HTTP 200 and an empty
 * `data` array**, so "no chapters" and "wrong rating filter" look identical once parsed. Two
 * measured facts drive the rules below (probed 2026-10-23 against `/manga/{id}/feed`):
 *
 *  - asking for `safe` only returns 0 chapters on an adult series, while asking for
 *    suggestive + erotica + pornographic returns its chapters — so the NSFW feed must ask for
 *    the non-safe ratings explicitly and must never include `safe`;
 *  - **omitting `contentRating[]` behaves like `safe`** (an unfiltered feed of the same adult
 *    series also returned 0), so "retry without the filter" is not a usable fallback.
 */
object MangaDexQuery {

    /** Content ratings a general (SFW) listing may show. */
    val SAFE = listOf("safe", "suggestive")

    /** NSFW catalogue listings are strictly erotica + pornographic (§2ah). */
    val NSFW = listOf("erotica", "pornographic")

    /**
     * Ratings used for an NSFW **chapter feed**. A series listed under erotica/pornographic can
     * still have chapters rated `suggestive`, and a feed only contains chapters matching the
     * requested ratings — so the NSFW feed asks for all three non-safe ratings. `safe` is never
     * part of an NSFW query; including it is what emptied the NSFW Manga chapter list.
     */
    val NSFW_FEED = listOf("suggestive", "erotica", "pornographic")

    /** Ratings for a catalogue listing / search. */
    fun ratings(nsfw: Boolean): List<String> = if (nsfw) NSFW else SAFE

    /** Ratings for a chapter feed. */
    fun feedRatings(nsfw: Boolean): List<String> = if (nsfw) NSFW_FEED else SAFE

    /** `contentRating[]` query params for [ratings]. */
    fun ratingParams(ratings: List<String>): List<Pair<String, String>> =
        ratings.map { "contentRating[]" to it }

    /**
     * Rating sets to try for a chapter feed, in order, stopping at the first non-empty answer.
     *
     * An NSFW title asks once (its own ratings). A SFW title that comes back empty is retried
     * once with the NSFW set, which recovers an adult series whose [MediaItem][com.moovio.app.data.model.MediaItem]
     * lost its `NSFW_MANGA` type (e.g. restored from an older library entry) — the unfiltered
     * request cannot do that, because it defaults to the SFW ratings.
     */
    fun feedAttempts(nsfw: Boolean): List<List<String>> =
        if (nsfw) listOf(NSFW_FEED) else listOf(SAFE, NSFW_FEED)
}
