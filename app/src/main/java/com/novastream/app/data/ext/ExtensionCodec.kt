package com.novastream.app.data.ext

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Element-wise tolerant codec for the persisted extension list (`extensions.json`).
 *
 * This is the same class of bug the Library hit in Phase 13 (see [com.novastream.app.data.local.LibraryCodec]):
 * Gson maps a **missing JSON field onto a Kotlin non-null slot as a silent `null`** — it has no
 * concept of Kotlin nullability and instantiates the data class through `Unsafe` rather than its
 * constructor. So an `extensions.json` entry written by an older build (or truncated mid-write)
 * could decode into an `ExtensionDescriptor` whose `name` was null at runtime even though the Kotlin
 * type says `String`.
 *
 * The Sources dashboard then did two things with that value that both end in a crash on entry:
 * `Text(desc.name, …)` passes null into a non-null parameter (the compiler's
 * `checkNotNullParameter` intrinsic throws an NPE), and `items(installed, key = { it.id })` fed a
 * null/duplicate key into `LazyColumn`. That is the "Settings → Sources dashboard crashes" report —
 * not the early `return@Column` theory Phase 20 chased.
 *
 * The codec therefore sanitizes **per element**: an entry without the fields an extension cannot
 * load without (`id`, `entryClass`, `apkPath`) is dropped, `name`/`version` are defaulted instead of
 * left null, and ids are de-duplicated (a duplicate id would collide as a lazy-list key). One bad
 * entry can no longer take the screen — or the rest of the list — down with it.
 *
 * Pure JVM (no Android types) so it is directly unit-testable.
 */
object ExtensionCodec {

    private val gson = Gson()

    fun encode(list: List<ExtensionDescriptor>): String = gson.toJson(list)

    /** Installed extensions in stored order, minus unusable entries and duplicate ids. */
    fun decode(json: String?): List<ExtensionDescriptor> =
        elements(json).mapNotNull { descriptor(it) }.distinctBy { it.id }

    // ---- elements ----------------------------------------------------------

    /**
     * Top-level array of objects. Also tolerates a single bare object and any parse failure
     * (truncated write, encoding corruption) by yielding an empty list instead of throwing.
     */
    private fun elements(json: String?): List<JsonObject> {
        if (json.isNullOrBlank()) return emptyList()
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        return when {
            root.isJsonArray -> root.asJsonArray.mapNotNull { el ->
                el.takeIf { it.isJsonObject }?.asJsonObject
            }
            root.isJsonObject -> listOf(root.asJsonObject)
            else -> emptyList()
        }
    }

    /**
     * One installed extension, or null when it is unusable and should be dropped.
     *
     * `id`, `entryClass` and `apkPath` are required: without them [ExtensionHost.load] cannot
     * resolve anything, and a null id would also blow up as a lazy-list key. `name` is the field the
     * dashboard renders, so it is defaulted to the id rather than trusted.
     */
    private fun descriptor(o: JsonObject): ExtensionDescriptor? {
        val id = str(o, "id") ?: return null
        val entryClass = str(o, "entryClass") ?: return null
        val apkPath = str(o, "apkPath") ?: return null
        return ExtensionDescriptor(
            id = id,
            name = str(o, "name") ?: id,
            version = str(o, "version"),
            entryClass = entryClass,
            apkPath = apkPath,
        )
    }

    /** Non-blank string from a string/number primitive; null for absent, JsonNull or objects. */
    private fun str(o: JsonObject, key: String): String? {
        val el = o.get(key)?.takeIf { it.isJsonPrimitive } ?: return null
        return runCatching { el.asString }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
