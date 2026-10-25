package com.moovio.desktop.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import com.moovio.desktop.domain.model.MediaItem
import java.io.File

/** A single continue-watching entry. */
@Serializable
@SerialName("ContinueEntry")
data class ContinueEntry(
    val mediaId: String,
    val title: String,
    val type: String,
    val poster: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toLibraryItem(): LibraryItem =
        LibraryItem(mediaId, title, type, poster, updatedAt)
}

/** Library row (favorite or continue-watching). */
@Serializable
@SerialName("LibraryItem")
data class LibraryItem(
    val mediaId: String,
    val title: String,
    val type: String,
    val poster: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
private data class LibraryBundle(
    val favorites: List<LibraryItem> = emptyList(),
    val continueWatching: List<ContinueEntry> = emptyList(),
)

/**
 * Persisted library: favorites + continue watching, stored as one atomic JSON bundle.
 * One failing write never breaks reads.
 */
class LibraryStore(private val settings: SettingsStore) {
    private val libraryFile = File(
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Moovio"),
        "library.json",
    )
    private val json = Json { ignoreUnknownKeys = true }

    private var bundle: LibraryBundle = loadBundle()

    private fun loadBundle(): LibraryBundle = runCatching {
        json.decodeFromString(libraryFile.readText())
    }.getOrDefault(LibraryBundle())

    private fun save() {
        libraryFile.writeText(json.encodeToString(bundle))
    }

    private fun <T> mutate(block: LibraryBundle.() -> T): T {
        val result = bundle.block()
        save()
        return result
    }

    // --- Favorites ----------------------------------------------------------

    val favoriteIds: List<String> get() = bundle.favorites.map { it.mediaId }

    fun isFavorite(mediaId: String): Boolean = bundle.favorites.any { it.mediaId == mediaId }

    fun toggleFavorite(item: LibraryItem): Boolean = mutate {
        val already = bundle.favorites.any { it.mediaId == item.mediaId }
        bundle.favorites = if (already) bundle.favorites.filter { it.mediaId != item.mediaId }
        else bundle.favorites + item
        !already
    }

    fun favoriteItems(): List<LibraryItem> = bundle.favorites.sortedByDescending { it.updatedAt }

    // --- Continue watching --------------------------------------------------

    val continueWatchingList: List<ContinueEntry> get() =
        bundle.continueWatching.sortedByDescending { it.updatedAt }

    fun addOrUpdateContinue(entry: ContinueEntry) = mutate {
        val idx = bundle.continueWatching.indexOfFirst { it.mediaId == entry.mediaId }
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        bundle.continueWatching = if (idx >= 0) {
            bundle.continueWatching.updated(idx, updated)
        } else {
            bundle.continueWatching + updated
        }
    }

    fun removeContinue(mediaId: String) = mutate {
        bundle.continueWatching = bundle.continueWatching.filter { it.mediaId != mediaId }
    }

    fun clearContinue() = mutate { bundle.continueWatching = emptyList() }
}
