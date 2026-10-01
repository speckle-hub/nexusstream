package com.novastream.app.data.download

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Pure helpers for turning a remote HLS playlist into an offline one.
 *
 * Only the parsing/rewriting lives here (so it is unit-testable); the worker does the network I/O
 * and file writes. A master playlist is resolved to its highest-bandwidth variant, whose media
 * playlist is then rewritten so every segment URI points at the locally downloaded filename.
 */
object HlsPlaylist {

    /** True when [text] is a master playlist that points at one or more variant streams. */
    fun isMaster(text: String): Boolean = text.contains("#EXT-X-STREAM-INF")

    /** Highest-bandwidth variant URI from a master playlist, absolute against [baseUrl]. */
    fun pickVariant(text: String, baseUrl: String): String? {
        var best: String? = null
        var bestBandwidth = -1L
        var pendingBandwidth: Long? = null
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                pendingBandwidth = BANDWIDTH.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val bandwidth = pendingBandwidth
                if (bandwidth != null && bandwidth > bestBandwidth) {
                    bestBandwidth = bandwidth
                    best = resolve(baseUrl, line)
                }
                pendingBandwidth = null
            }
        }
        return best
    }

    /** Absolute segment URIs from a media playlist, in order. */
    fun segmentUris(text: String, baseUrl: String): List<String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { resolve(baseUrl, it) }

    /** Deterministic local filenames for the segments, preserving order. */
    fun segmentNames(uris: List<String>): List<String> = uris.mapIndexed { index, uri ->
        val ext = uri.substringBefore('?').substringAfterLast('.', "ts").lowercase()
            .takeIf { it.length in 1..4 && it.all { c -> c.isLetterOrDigit() } } ?: "ts"
        "seg_%05d.%s".format(index, ext)
    }

    /**
     * Rewrite a media playlist so every segment line references the matching entry in [names].
     * Tags (including `#EXT-X-KEY`) are preserved verbatim; relative key URIs stay as-is.
     */
    fun localPlaylist(text: String, baseUrl: String, names: List<String>): String {
        val out = StringBuilder()
        var i = 0
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> out.append('\n')
                line.startsWith("#") -> out.append(line).append('\n')
                i < names.size -> {
                    out.append(names[i]).append('\n')
                    i++
                }
                else -> out.append(line).append('\n')
            }
        }
        return out.toString()
    }

    /** Resolve a (possibly relative) playlist URI against its base. */
    fun resolve(baseUrl: String, ref: String): String =
        runCatching { baseUrl.toHttpUrlOrNull()?.resolve(ref)?.toString() }.getOrNull() ?: ref

    private val BANDWIDTH = Regex("BANDWIDTH=(\\d+)")
}
