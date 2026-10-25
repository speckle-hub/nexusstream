package com.moovio.desktop.player

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.File

/**
 * Minimal JNA surface for libmpv. Only the handful of calls Moovio needs are bound:
 * core lifecycle, string properties/commands and the software render API (which lets us draw
 * video frames with Compose while keeping full custom controls on top).
 */
interface LibMpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(data: Pointer)
    fun mpv_command_string(ctx: Pointer, args: String): Int
    fun mpv_error_string(code: Int): String

    fun mpv_render_context_create(
        result: PointerByReference,
        mpv: Pointer,
        params: Pointer,
    ): Int
    fun mpv_render_context_update(ctx: Pointer): Long
    fun mpv_render_context_render(ctx: Pointer, params: Pointer): Int
    fun mpv_render_context_free(ctx: Pointer)
    fun mpv_terminate_destroy(ctx: Pointer)
}

object MpvLibrary {

    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")

    private val libraryName: String = when {
        isWindows -> "libmpv-2.dll"
        System.getProperty("os.name").lowercase().contains("mac") -> "libmpv.dylib"
        else -> "libmpv.so.2"
    }

    /** Returns the first existing libmpv binary, or null. */
    fun locate(overridePath: String = ""): String? {
        val candidates = buildList {
            if (overridePath.isNotBlank()) add(File(overridePath))
            System.getProperty("moovio.mpv.lib")?.takeIf { it.isNotBlank() }?.let { add(File(it)) }
            val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
            add(File(appData, "Moovio/runtime/$libraryName"))
            add(File(System.getProperty("user.dir"), "native/mpv/$libraryName"))
            add(File(System.getProperty("user.dir"), libraryName))

            // jpackage app dir: <app>/Moovio.exe. appResourcesRootDir lands in app/resources,
            // so check both the launcher directory and the resources directory.
            System.getProperty("jpackage.app-path")?.let { appPath ->
                File(appPath).parentFile?.let { appDir ->
                    add(File(appDir, libraryName))
                    add(File(appDir, "app/resources/$libraryName"))
                    add(File(appDir, "resources/$libraryName"))
                }
            }
            runCatching {
                val location = File(MpvLibrary::class.java.protectionDomain.codeSource.location.toURI())
                val dir = if (location.isDirectory) location else location.parentFile
                if (dir != null) {
                    add(File(dir, libraryName))
                    add(File(dir, "resources/$libraryName"))
                }
            }
        }
        return candidates.firstOrNull { it.isFile }?.absolutePath
    }

    fun load(path: String): LibMpv = Native.load(path, LibMpv::class.java)

    /** Load from an explicit path; falls back to the plain library name on PATH. */
    fun loadBest(overridePath: String = ""): Pair<LibMpv, String>? {
        val located = locate(overridePath)
        if (located != null) {
            runCatching { return load(located) to located }
        }
        return runCatching {
            val lib = Native.load(libraryName.removeSuffix(".dll"), LibMpv::class.java) as LibMpv
            lib to libraryName
        }.getOrNull()
    }
}
