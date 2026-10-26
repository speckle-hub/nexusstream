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

    private const val APP_TITLE = "Moovio"

    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: Pointer, size: Int): Int
    }

    /** Win32 window style / position flags used for borderless fullscreen. */
    private const val GWL_STYLE = -16
    private const val WS_CAPTION = 0x00C00000
    private const val WS_THICKFRAME = 0x00040000
    private const val WS_MINIMIZEBOX = 0x00020000
    private const val WS_MAXIMIZEBOX = 0x00010000
    private const val SWP_NOMOVE = 0x0002
    private const val SWP_NOSIZE = 0x0001
    private const val SWP_FRAMECHANGED = 0x0020
    private const val SWP_SHOWWINDOW = 0x0040

    /** Saved window style so exitFullscreen can restore the exact previous title bar / borders. */
    private var savedStyle: Int? = null

    private val HWND_TOP = HWND(Pointer.createConstant(0))

    /** Locates the native HWND for the app's top-level window by its title. */
    private fun findHwnd(): HWND? = runCatching { User32.INSTANCE.FindWindow(null, APP_TITLE) }.getOrNull()

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

    /**
     * Enter borderless fullscreen: strip the title bar and borders from the native window, then
     * let the OS-sized fullscreen window (set via WindowPlacement.Fullscreen) cover the display
     * without chrome. [SWP_FRAMECHANGED] forces Win32 to re-apply the new style immediately.
     */
    fun enterFullscreen() {
        if (!MpvLibrary.isWindows) return
        val hwnd = findHwnd() ?: return
        savedStyle = runCatching { User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE) }.getOrNull()
        val style = savedStyle ?: 0
        val borderless = (style.toLong()
            and WS_CAPTION.toLong().inv()
            and WS_THICKFRAME.toLong().inv()
            and WS_MINIMIZEBOX.toLong().inv()
            and WS_MAXIMIZEBOX.toLong().inv())
        runCatching { User32.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, borderless.toInt()) }
        runCatching {
            User32.INSTANCE.SetWindowPos(
                hwnd, HWND_TOP, 0, 0, 0, 0,
                SWP_NOMOVE or SWP_NOSIZE or SWP_FRAMECHANGED or SWP_SHOWWINDOW,
            )
        }
    }

    /**
     * Exit fullscreen: restore the saved window style (title bar + borders) so the window returns
     * to its normal floating state. The previous window size/position is restored by
     * [WindowPlacement.Floating] set on the Compose side.
     */
    fun exitFullscreen() {
        if (!MpvLibrary.isWindows) return
        val hwnd = findHwnd() ?: return
        savedStyle?.let { style ->
            runCatching { User32.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, style) }
            runCatching {
                User32.INSTANCE.SetWindowPos(
                    hwnd, HWND_TOP, 0, 0, 0, 0,
                    SWP_NOMOVE or SWP_NOSIZE or SWP_FRAMECHANGED or SWP_SHOWWINDOW,
                )
            }
        }
        savedStyle = null
    }
}
