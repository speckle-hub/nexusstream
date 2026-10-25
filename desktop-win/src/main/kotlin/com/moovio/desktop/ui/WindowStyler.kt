package com.moovio.desktop.ui

import com.moovio.desktop.player.MpvLibrary
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Frame
import java.awt.Window

/**
 * Gives the native Windows title bar the same near-black finish as the app.
 * Windows 11 also accepts a caption/border colour; older builds simply ignore those calls.
 */
object WindowStyler {

    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: Pointer, size: Int): Int
    }

    fun apply(window: Window) {
        if (!MpvLibrary.isWindows) return
        val frame = window as? Frame ?: return
        val hwnd = runCatching { User32.INSTANCE.FindWindow(null, frame.title) }.getOrNull() ?: return
        val dwm = runCatching { Native.load("dwmapi", DwmApi::class.java) }.getOrNull() ?: return

        fun setInt(attribute: Int, value: Int) {
            val memory = Memory(4)
            memory.setInt(0, value)
            runCatching { dwm.DwmSetWindowAttribute(hwnd, attribute, memory, 4) }
        }

        setInt(20, 1) // DWMWA_USE_IMMERSIVE_DARK_MODE
        setInt(35, 0x00120B0B) // DWMWA_CAPTION_COLOR (COLORREF, BGR)
        setInt(36, 0x00FAF5F5) // DWMWA_TEXT_COLOR
        setInt(34, 0x002A2A3A) // DWMWA_BORDER_COLOR
    }
}
