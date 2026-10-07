package com.novastream.app.data.repo

import com.novastream.app.data.model.StreamSource

/**
 * Pure ranking helpers for Stremio stream lists.
 *
 * "Prefer the most reliable sources first" needs a deterministic, testable order: known-good
 * add-ons (Torrentio and friends) lead, then higher quality, then healthier swarms (more seeders).
 * Kept Android-free so it can be unit-tested.
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
     * Seeder count advertised in a stream's text. Torrentio renders it as `👤 1234`; other add-ons
     * write `1234 seeders` or `Seeds: 1234`. Returns null when nothing plausible is present.
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
     */
    fun rank(
        streams: List<StreamSource>,
        preferredHosts: List<String> = emptyList(),
        reliableFirst: Boolean = true,
    ): List<StreamSource> = streams
        .distinctBy { key(it) }
        .sortedWith(
            compareBy<StreamSource> {
                if (reliableFirst) preferredIndex(it, preferredHosts) else 0
            }
                .thenByDescending { qualityRank(it.quality) }
                .thenByDescending { it.seeders ?: -1 },
        )

    private fun key(stream: StreamSource): String =
        stream.url ?: stream.infoHash ?: stream.externalUrl ?: stream.ytId
        ?: "${stream.addonId ?: ""}:${stream.name ?: ""}:${stream.title ?: ""}"

    private val SEEDER_PATTERNS = listOf(
        Regex("""👤\s*([\d.,]+)"""),
        Regex("""([\d.,]+)\s*seeders?""", RegexOption.IGNORE_CASE),
        Regex("""seeds?:?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
    )
}
