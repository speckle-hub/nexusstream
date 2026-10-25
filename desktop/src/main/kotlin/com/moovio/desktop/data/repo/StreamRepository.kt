package com.moovio.desktop.data.repo

import com.moovio.desktop.data.remote.StremioRepository
import com.moovio.desktop.domain.model.MediaType
import com.moovio.desktop.domain.model.StreamSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stream resolution: pick the best stream for a title.
 *
 * Ranking: direct HTTP first, then quality (4K > 1080p > 720p > SD), then seeders.
 * Torrent streams require an external torrent client; flagged so the player can offer
 * "Open in external player".
 */
class StreamRepository(private val stremio: StremioRepository) {
    suspend fun bestStream(mediaId: String, type: MediaType, title: String): StreamSource? {
        val detail = stremio.resolveDetail(mediaId, type, title)
        return detail.streams.maxByOrNull { it.rank() }
    }

    suspend fun allStreams(mediaId: String, type: MediaType, title: String): List<StreamSource> =
        stremio.resolveDetail(mediaId, type, title).streams.sortedByDescending { it.rank() }

    private fun StreamSource.rank(): Double =
        when {
            isTorrent -> 0.0 // torrents need external handling, ranked last for in-app playback
            quality == null -> 5.0
            else -> {
                val q = quality.lowercase()
                val base = when {
                    q.contains("4k") || q.contains("2160") -> 100.0
                    q.contains("1080") -> 80.0
                    q.contains("720") -> 60.0
                    q.contains("480") || q.contains("sd") -> 40.0
                    else -> 50.0
                }
                base + (seeders ?: 0).coerceAtMost(1000) * 0.01
            }
        }
}
