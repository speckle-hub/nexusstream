package com.novastream.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.reflect.TypeToken
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.data.remote.Http
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Favorites + continue-watching history. */
class LibraryStore(private val context: Context) {

    private val K_FAV = stringPreferencesKey("favorites")
    private val K_HISTORY = stringPreferencesKey("history")

    private inline fun <reified T> decode(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            Http.gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val favorites: Flow<List<MediaItem>> = context.dataStore.data.map { decode<MediaItem>(it[K_FAV]) }
    val history: Flow<List<WatchEntry>> = context.dataStore.data.map { decode<WatchEntry>(it[K_HISTORY]) }

    suspend fun toggleFavorite(item: MediaItem) {
        context.dataStore.edit { prefs ->
            val list = decode<MediaItem>(prefs[K_FAV]).toMutableList()
            val existing = list.indexOfFirst { it.key == item.key }
            if (existing >= 0) list.removeAt(existing) else list.add(0, item)
            prefs[K_FAV] = Http.gson.toJson(list)
        }
    }

    suspend fun isFavorite(key: String): Boolean =
        favorites.first().any { it.key == key }

    /** Remove a single favourite by key. A no-op when it is not in the list (never re-adds). */
    suspend fun removeFavorite(key: String) {
        context.dataStore.edit { prefs ->
            val list = decode<MediaItem>(prefs[K_FAV]).filterNot { it.key == key }
            prefs[K_FAV] = Http.gson.toJson(list)
        }
    }

    suspend fun recordWatch(entry: WatchEntry) {
        context.dataStore.edit { prefs ->
            val list = decode<WatchEntry>(prefs[K_HISTORY]).toMutableList()
            list.removeAll { it.item.key == entry.item.key }
            list.add(0, entry)
            prefs[K_HISTORY] = Http.gson.toJson(list.take(100))
        }
    }

    /** Drop one continue-watching entry by its title key, clearing the saved playback progress. */
    suspend fun removeWatch(key: String) {
        context.dataStore.edit { prefs ->
            val list = decode<WatchEntry>(prefs[K_HISTORY]).filterNot { it.item.key == key }
            prefs[K_HISTORY] = Http.gson.toJson(list)
        }
    }

    suspend fun clearHistory() = context.dataStore.edit { it.remove(K_HISTORY) }
    suspend fun clearFavorites() = context.dataStore.edit { it.remove(K_FAV) }
}
