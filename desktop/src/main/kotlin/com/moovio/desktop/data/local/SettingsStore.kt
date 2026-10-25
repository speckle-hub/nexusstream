package com.moovio.desktop.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Simple JSON-backed settings store for the desktop app.
 *
 * Lives at `%APPDATA%\Moovio\settings.json` (Windows) or `~/.config/Moovio/settings.json`.
 */
class SettingsStore {
    private val configDir: File = File(
        System.getenv("APPDATA") ?: System.getProperty("user.home"),
        "Moovio",
    ).apply { mkdirs() }
    private val settingsFile = File(configDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true }

    private var settings: Map<String, Any> = load()

    private fun load(): Map<String, Any> = runCatching {
        json.decodeFromString<Map<String, Any>>(settingsFile.readText())
    }.getOrDefault(emptyMap())

    private fun save() {
        settingsFile.writeText(json.encodeToString(settings))
    }

    // --- NSFW ---------------------------------------------------------------

    var nsfwPin: String?
        get() = settings["nsfwPin"] as? String
        set(value) { settings["nsfwPin"] = value; save() }

    var nsfwUnlockedAt: Long
        get() = (settings["nsfwUnlockedAt"] as? Number)?.toLong() ?: 0
        set(value) { settings["nsfwUnlockedAt"] = value; save() }

    /** PIN lock is considered open while within [UNLOCK_TTL_MS] of the last successful unlock. */
    companion object {
        const val UNLOCK_TTL_MS = 5L * 60 * 1000 // 5 minutes
    }

    fun isNsfwUnlocked(): Boolean =
        nsfwPin == null || System.currentTimeMillis() - nsfwUnlockedAt < UNLOCK_TTL_MS

    // --- UI -----------------------------------------------------------------

    var showNsfwInSearch: Boolean
        get() = (settings["showNsfwInSearch"] as? Boolean ?: false)
        set(value) { settings["showNsfwInSearch"] = value; save() }

    var reducedMotion: Boolean
        get() = (settings["reducedMotion"] as? Boolean ?: false)
        set(value) { settings["reducedMotion"] = value; save() }

    var hardwareDecode: Boolean
        get() = (settings["hardwareDecode"] as? Boolean ?: true)
        set(value) { settings["hardwareDecode"] = value; save() }
}
