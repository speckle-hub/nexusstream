package com.moovio.desktop.domain

import com.moovio.desktop.domain.model.StreamSource

/**
 * Ranking policy for stream results. Reliability first (a direct URL beats a torrent hash),
 * then resolution, then swarm size. Kept free of network/UI code so it is unit-testable.
 */
object StreamRanking {

    private val qualityPattern =
        Regex("""(?:^|[^0-9])(2160|1440|1080|720|576|480|360)\s*p(?:\b|$)""", RegexOption.IGNORE_CASE)
    private val dimsPattern = Regex("""\b(3840|2560|1920|1280|854)\s*[x×]\s*(2160|1440|1080|720|480)\b""")
    private val fourKPattern = Regex("""\b(4k|uhd)\b""", RegexOption.IGNORE_CASE)
    private val seedersPrefix = Regex("""(?:👤|seeders?|seeds)\s*[:=]?\s*([\d,.]+)""", RegexOption.IGNORE_CASE)
    private val seedersSuffix = Regex("""([\d,.]+)\s*(?:seeders?|seeds)\b""", RegexOption.IGNORE_CASE)

    fun qualityOf(vararg texts: String?): String? {
        val haystack = texts.joinToString(" ") { it.orEmpty() }
        if (haystack.isBlank()) return null
        qualityPattern.find(haystack)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@let
            return "${n}p"
        }
        dimsPattern.find(haystack)?.let { m ->
            val height = m.groupValues[2].toIntOrNull() ?: return@let
            return "${if (height == 2160) 2160 else height}p".replace("2160p", "2160p")
        }
        if (fourKPattern.containsMatchIn(haystack)) return "2160p"
        return when {
            haystack.contains("bluray", true) || haystack.contains("blu-ray", true) -> "1080p"
            else -> null
        }
    }

    fun qualityRank(quality: String?): Int = when (quality?.lowercase()) {
        "2160p", "4k" -> 2160
        "1440p" -> 1440
        "1080p" -> 1080
        "720p" -> 720
        "576p" -> 576
        "480p" -> 480
        "360p" -> 360
        else -> 0
    }

    fun seedersOf(vararg texts: String?): Int? {
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            if (text.length >= 40) {
                // Long cached-availability tables from add-ons cram many counts together; only
                // trust an explicitly labelled figure near the start.
                val head = text.take(80)
                seedersPrefix.find(head)?.let { m ->
                    return m.groupValues[1].replace(",", "").toIntOrNull()
                }
                seedersSuffix.find(head)?.let { m ->
                    return m.groupValues[1].replace(",", "").toIntOrNull()
                }
                continue
            }
            seedersPrefix.find(text)?.let { m ->
                return m.groupValues[1].replace(",", "").toIntOrNull()
            }
            seedersSuffix.find(text)?.let { m ->
                return m.groupValues[1].replace(",", "").toIntOrNull()
            }
        }
        return null
    }

    /** Adds quality / seeders metadata if the add-on did not state them explicitly. */
    fun enrich(stream: StreamSource): StreamSource = stream.copy(
        quality = stream.quality ?: qualityOf(stream.title, stream.name, stream.description),
        seeders = stream.seeders ?: seedersOf(stream.title, stream.description),
    )

    /**
     * De-duplicate then order: direct playable URLs before torrents, then resolution, then seeders.
     * Add-on order is preserved as the final tiebreaker.
     */
    fun rank(streams: List<StreamSource>): List<StreamSource> =
        streams
            .map { enrich(it) }
            .distinctBy { it.fingerprint }
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<StreamSource>> { it.value.isDirect }
                    .thenByDescending { it.value.isPlayable && it.value.externalUrl == null }
                    .thenByDescending { qualityRank(it.value.quality) }
                    .thenByDescending { it.value.seeders ?: -1 }
                    .thenBy { it.index },
            )
            .map { it.value }

    /** Best stream for one-click Play: highest ranked direct URL, or null if none can be played. */
    fun bestPlayable(streams: List<StreamSource>): StreamSource? =
        rank(streams).firstOrNull { it.isPlayable }
}
