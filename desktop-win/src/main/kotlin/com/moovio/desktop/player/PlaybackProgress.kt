package com.moovio.desktop.player

import com.moovio.desktop.data.local.LibraryStore
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.domain.model.PlaybackRequest
import com.moovio.desktop.domain.model.WatchProgress

/**
 * Stores the current playback position for Continue Watching.
 *
 * The library state is updated synchronously and the JSON write is handed to the store's own
 * background scope, so this is safe to call from the UI thread on the periodic tick. Writes are
 * skipped when nothing changed (paused playback ticks), which keeps the 5 s saves from rewriting
 * the file while the position is not moving.
 */
/**
 * Where a new playback should start: an explicit request position, otherwise the saved position
 * for the same title/episode when resume is enabled. Finished titles and mismatched episodes start
 * from the beginning.
 */
internal fun resumePosition(target: PlayTarget, library: LibraryStore, autoResume: Boolean): Long {
    if (target.startPositionMs > 0) return target.startPositionMs
    if (!autoResume) return 0
    val saved = library.progressFor(target.media.key) ?: return 0
    if (saved.finished) return 0
    if (target.season != null && saved.season != null && target.season != saved.season) return 0
    if (target.episode != null && saved.episode != null && target.episode != saved.episode) return 0
    return saved.positionMs
}

internal fun recordPlaybackProgress(
    library: LibraryStore,
    request: PlaybackRequest,
    snapshot: MpvSnapshot,
) {
    if (snapshot.durationMs <= 0 || snapshot.positionMs <= 0) return
    val position = if (snapshot.ended) snapshot.durationMs else snapshot.positionMs
    val existing = library.progressFor(request.media.key)
    if (existing != null &&
        existing.positionMs == position &&
        existing.durationMs == snapshot.durationMs &&
        existing.season == request.season &&
        existing.episode == request.episode
    ) {
        return
    }
    library.recordProgress(
        WatchProgress(
            mediaKey = request.media.key,
            mediaId = request.media.id,
            type = request.media.type,
            name = request.media.name,
            poster = request.media.poster,
            background = request.media.background,
            season = request.season,
            episode = request.episode,
            episodeTitle = request.episodeTitle,
            positionMs = position,
            durationMs = snapshot.durationMs,
            updatedAt = System.currentTimeMillis(),
            adult = request.media.adult,
        ),
    )
}
