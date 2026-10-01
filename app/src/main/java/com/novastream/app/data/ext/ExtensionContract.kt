package com.novastream.app.data.ext

/** A catalog entry an extension can surface. */
data class ExtensionItem(
    val id: String,
    val title: String,
    val poster: String? = null,
    val url: String? = null,
)

/** A playable stream resolved by an extension. */
data class ExtensionStream(
    val url: String,
    val quality: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * Contract every dynamically-loaded extension must implement.
 *
 * Kept intentionally tiny so external `.apk` / `.dex` / `.extension` packages can target it
 * without pulling in the whole app. Extensions are loaded with [ExtensionHost], which runs them
 * through a [dalvik.system.DexClassLoader] rooted in app-private storage.
 */
interface NexusExtension {
    /** Stable identifier, unique per extension. */
    val id: String

    /** Human-readable name. */
    val name: String

    /** Optional version string surfaced in the UI. */
    val version: String?

    /** Search/browse the extension's catalogue. `query == null` means "top / trending". */
    fun catalog(query: String? = null): List<ExtensionItem>

    /** Resolve playable streams for an item previously returned by [catalog]. */
    fun streams(itemId: String): List<ExtensionStream>
}
