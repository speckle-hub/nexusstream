package com.moovio.app.data.repo

import com.moovio.app.data.model.StreamSource

/**
 * Pure ranking helpers for Stremio stream lists.
 *
 * "Prefer the most reliable sources first" needs a deterministic, testable order: caller-supplied
 * preferred add-ons lead (none are preferred by default), then higher quality, then healthier
 * swarms (more seeders). Kept Android-free so it can be unit-tested.
 */
object StreamRanking {

    /** Quality tiers, highest first. Unknown quality sorts below everything recognised. */
    fun qualityRank(quality: String?): Int = when (quality) {
        "4K" -> 5
        "1440p" -> 4
        "1080p" -> 3
        "720p" -> 2
        "480p" -> 1
        "360p" -> 0
        else -> -1
    }

    /**
     * Seeder count advertised in a stream's text. Add-ons render it as `👤 1234`,
     * `1234 seeders` or `Seeds: 1234`. Returns null when nothing plausible is present.
     */
    fun seedersOf(vararg texts: String?): Int? {
        val hay = texts.filterNotNull().joinToString(" ")
        if (hay.isBlank()) return null
        SEEDER_PATTERNS.forEach { regex ->
            val m = regex.find(hay) ?: return@forEach
            val n = m.groupValues.getOrNull(1)?.replace(",", "")?.replace(".", "")?.toIntOrNull()
            if (n != null && n in 0..10_000_000) return n
        }
        return null
    }

    /** Position of a stream's add-on in the preferred list (lower = more reliable), else large. */
    fun preferredIndex(stream: StreamSource, preferredHosts: List<String>): Int {
        val hay = listOfNotNull(stream.addonName, stream.addonId).joinToString(" ").lowercase()
        val idx = preferredHosts.indexOfFirst { host -> hay.contains(host.lowercase()) }
        return if (idx >= 0) idx else Int.MAX_VALUE / 2
    }

    /**
     * De-duplicate and order a stream list: preferred add-ons first, then quality, then seeders.
     *
     * With [directFirst], directly playable media URLs (`.mp4`/`.m3u8`/…) outrank
     * embed/player pages at equal preference and quality — an embed that still has
     * to survive another hop is less likely to play than a file URL.
     */
    fun rank(
        streams: List<StreamSource>,
        preferredHosts: List<String> = emptyList(),
        reliableFirst: Boolean = true,
        directFirst: Boolean = false,
    ): List<StreamSource> = streams
        .distinctBy { key(it) }
        .sortedWith(
            compareBy<StreamSource> {
                if (reliableFirst) preferredIndex(it, preferredHosts) else 0
            }
                .thenByDescending { qualityRank(it.quality) }
                .thenByDescending { if (directFirst) isDirectMediaUrl(it.url ?: it.externalUrl) else true }
                .thenByDescending { it.seeders ?: -1 },
        )

    /** True when [url] points at a media file rather than an embed/player page. */
    fun isDirectMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("http")) return false
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".mp4") || path.endsWith(".m3u8") ||
            path.endsWith(".mkv") || path.endsWith(".webm") ||
            path.endsWith(".m4v") || path.endsWith(".mov")
    }

    /**
     * Auto-play pick: the highest-ranked stream that can actually be played
     * (direct URL, YouTube id or torrent hash — dead/empty entries never win).
     * Reliability order first (preferred hosts like Torrentio and the known-good
     * built-ins), then quality, then direct-over-embed, then seeders — so Play
     * starts the best available source immediately with no manual picking.
     */
    fun bestPlayable(
        streams: List<StreamSource>,
        preferredHosts: List<String> = emptyList(),
    ): StreamSource? = rank(
        streams.filter { it.playableUrl != null || it.ytId != null || !it.infoHash.isNullOrBlank() },
        preferredHosts,
        directFirst = true,
    ).firstOrNull()

    private fun key(stream: StreamSource): String =
        stream.url ?: stream.infoHash ?: stream.externalUrl ?: stream.ytId
        ?: "${stream.addonId ?: ""}:${stream.name ?: ""}:${stream.title ?: ""}"

    private val SEEDER_PATTERNS = listOf(
        Regex("""👤\s*([\d.,]+)"""),
        Regex("""([\d.,]+)\s*seeders?""", RegexOption.IGNORE_CASE),
        Regex("""seeds?:?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
    )
}
