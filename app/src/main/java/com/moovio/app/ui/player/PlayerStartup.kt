package com.moovio.app.ui.player

import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.model.SubtitleTrack
import com.moovio.app.data.model.Video

/**
 * Player launch guards (P0 Play-crash hotfix).
 *
 * Pure-JVM helpers with no Android dependencies, so the validation logic is unit-tested
 * (`PlayerStartupTest`). `PlayerActivity` calls these before touching ExoPlayer: a bad intent
 * extra now finishes the player with a log line instead of crashing it.
 *
 * Every helper is total — it never throws, even for the Gson null-field hole (Gson
 * instantiates Kotlin data classes through `Unsafe`, so a missing JSON field lands as a
 * runtime `null` in a non-null slot; see the `LibraryCodec`/`ExtensionCodec` precedent).
 */
fun isValidPlayerItem(item: MediaItem?): Boolean = runCatching {
    item != null && item.id.isNotBlank() && item.title.isNotBlank() && item.type != null
}.getOrDefault(false)

/** True when [source] carries a directly playable HTTP(S) URL. */
fun isPlayableStreamSource(source: StreamSource?): Boolean = runCatching {
    source != null && !source.playableUrl.isNullOrBlank()
}.getOrDefault(false)

/** Drop subtitle entries with no URL (a poisoned entry would NPE `Uri.parse`/`mimeFor`). */
fun sanitizeSubtitleTracks(subs: List<SubtitleTrack>): List<SubtitleTrack> =
    subs.filter { track -> runCatching { track.url.isNotBlank() }.getOrDefault(false) }

/** Drop episode entries with no id (a poisoned id would NPE stream resolution). */
fun sanitizeEpisodes(videos: List<Video>): List<Video> =
    videos.filter { v -> runCatching { v.id.isNotBlank() }.getOrDefault(false) }

/**
 * Headers safe to hand to `OkHttpDataSource.setDefaultRequestProperties`: the map itself may
 * be a Gson null, and individual entries may carry null keys/values — either would NPE the
 * player before the first frame.
 */
fun safeStreamHeaders(source: StreamSource): Map<String, String> =
    runCatching { source.headers }
        .getOrNull()
        ?.filter { entry ->
            runCatching { entry.key.isNotBlank() && entry.value.isNotBlank() }.getOrDefault(false)
        }
        .orEmpty()

/** Backup mirrors with the same Gson-hole treatment (a null list would NPE failover). */
fun safeStreamAlternates(source: StreamSource): List<String> =
    runCatching { source.alternates }
        .getOrNull()
        ?.filter { url -> runCatching { url.isNotBlank() }.getOrDefault(false) }
        .orEmpty()
