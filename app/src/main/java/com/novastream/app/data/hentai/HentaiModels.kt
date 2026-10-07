package com.novastream.app.data.hentai

import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.Video

/**
 * One playable episode discovered by a built-in hentai source.
 *
 * [url] is the absolute episode page URL, and [toVideo] encodes it (together with [sourceId])
 * as the [Video] id so a later stream lookup is a single page fetch instead of repeating the
 * title search. [series] is the owning title and doubles as the grouping key and the display
 * fallback when the site only wrote "Episode 1".
 */
data class HentaiEpisode(
    /** Owning source id — also the [StreamSource.addonId] of the streams it resolves to. */
    val sourceId: String,
    /** Series name, used to group episodes of one title and as a display fallback. */
    val series: String,
    /** Episode page title exactly as the site wrote it — kept verbatim for relevance scoring. */
    val title: String,
    /** Absolute episode page URL. */
    val url: String,
    /** Episode number when the site exposes one. */
    val episode: Int? = null,
    val poster: String? = null,
) {

    /**
     * Label for the episode row. The UI prefixes "E{n} · " itself, so this strips a leading or
     * trailing "Episode N" and the site's "English" / "English Subbed" tag, falling back to the
     * series name when nothing descriptive is left (HentaiMama titles episodes with the number
     * alone).
     */
    fun displayTitle(): String {
        val cleaned = title
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removeSuffix("English Subbed")
            .removeSuffix("English")
            .replace(Regex("""\s*\bep(?:isode)?\.?\s*\d+\s*$""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""^\s*\bep(?:isode)?\.?\s*\d+\s*""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s*[\-·:]+\s*$"""), " ")
            .trim()
        return cleaned.ifBlank { series.ifBlank { title } }
    }

    /**
     * Episode id for [Video]: `hentai:<sourceId>:<episode page url>`. The `hentai:` prefix is
     * what tells [com.novastream.app.data.repo.StreamRepository] to resolve this from the built-in
     * sources instead of asking a Stremio add-on to interpret a URL as an add-on id.
     */
    fun toVideo(): Video = Video(
        id = videoId,
        title = displayTitle(),
        season = null,
        episode = episode,
        thumbnail = poster,
    )

    val videoId: String get() = "$VIDEO_ID_PREFIX$sourceId:$url"

    companion object {
        const val VIDEO_ID_PREFIX = "hentai:"

        /** Inverse of [videoId] — null when [id] was not produced by a built-in source. */
        fun fromVideoId(id: String?): Pair<String, String>? {
            val raw = id ?: return null
            if (!raw.startsWith(VIDEO_ID_PREFIX)) return null
            val rest = raw.removePrefix(VIDEO_ID_PREFIX)
            val split = rest.indexOf(':')
            if (split <= 0 || split == rest.length - 1) return null
            return rest.substring(0, split) to rest.substring(split + 1)
        }
    }
}

/** Episode list plus a human-readable error per source that failed. */
data class HentaiEpisodes(
    val episodes: List<HentaiEpisode>,
    val errors: List<String> = emptyList(),
)

/** Built-in streams plus a human-readable error per source that failed. */
data class HentaiStreams(
    val streams: List<StreamSource>,
    val errors: List<String> = emptyList(),
)
