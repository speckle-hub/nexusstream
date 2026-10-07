package com.novastream.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Favorites + continue-watching history.
 *
 * Both lists are read through [LibraryCodec], which sanitizes each element on its own: a single
 * corrupt favourite no longer throws (or worse, silently blanks the whole list) — it is dropped
 * and the rest of the library survives.
 */
class LibraryStore(private val context: Context) {

    private val K_FAV = stringPreferencesKey("favorites")
    private val K_HISTORY = stringPreferencesKey("history")

    val favorites: Flow<List<MediaItem>> = context.dataStore.data.map { LibraryCodec.decodeFavorites(it[K_FAV]) }
    val history: Flow<List<WatchEntry>> = context.dataStore.data.map { LibraryCodec.decodeHistory(it[K_HISTORY]) }

    suspend fun toggleFavorite(item: MediaItem) {
        context.dataStore.edit { prefs ->
            val list = LibraryCodec.decodeFavorites(prefs[K_FAV]).toMutableList()
            val existing = list.indexOfFirst { it.key == item.key }
            if (existing >= 0) list.removeAt(existing) else list.add(0, item)
            prefs[K_FAV] = LibraryCodec.encodeFavorites(list)
        }
    }

    suspend fun isFavorite(key: String): Boolean =
        favorites.first().any { it.key == key }

    /** Remove a single favourite by key. A no-op when it is not in the list (never re-adds). */
    suspend fun removeFavorite(key: String) {
        context.dataStore.edit { prefs ->
            val list = LibraryCodec.decodeFavorites(prefs[K_FAV]).filterNot { it.key == key }
            prefs[K_FAV] = LibraryCodec.encodeFavorites(list)
        }
    }

    suspend fun recordWatch(entry: WatchEntry) {
        // Incognito mode (Phase 12): keep playing but never write to the history log.
        if (incognito) return
        context.dataStore.edit { prefs ->
            val list = LibraryCodec.decodeHistory(prefs[K_HISTORY]).toMutableList()
            list.removeAll { it.item.key == entry.item.key }
            list.add(0, entry)
            prefs[K_HISTORY] = LibraryCodec.encodeHistory(list.take(100))
        }
    }

    /** Drop one continue-watching entry by its title key, clearing the saved playback progress. */
    suspend fun removeWatch(key: String) {
        context.dataStore.edit { prefs ->
            val list = LibraryCodec.decodeHistory(prefs[K_HISTORY]).filterNot { it.item.key == key }
            prefs[K_HISTORY] = LibraryCodec.encodeHistory(list)
        }
    }

    suspend fun clearHistory() = context.dataStore.edit { it.remove(K_HISTORY) }
    suspend fun clearFavorites() = context.dataStore.edit { it.remove(K_FAV) }

    /**
     * Replace favourites/history wholesale — used by backup restore. Passing `null` leaves the
     * corresponding list untouched.
     */
    suspend fun restore(favorites: List<MediaItem>?, history: List<WatchEntry>?) {
        context.dataStore.edit { prefs ->
            favorites?.let { prefs[K_FAV] = LibraryCodec.encodeFavorites(it) }
            history?.let { prefs[K_HISTORY] = LibraryCodec.encodeHistory(it) }
        }
    }

    companion object {
        /**
         * When true, [recordWatch] is a no-op so Incognito Mode can pause history logging without
         * touching playback. Kept in sync from the app scope (see `NovaApp`).
         */
        @Volatile
        var incognito: Boolean = false
    }
}
