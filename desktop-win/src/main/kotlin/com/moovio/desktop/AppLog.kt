package com.moovio.desktop

import com.moovio.desktop.data.local.AppPaths
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Small rotating-free log in %APPDATA%\Moovio\logs. Startup diagnostics matter for a packaged
 * Windows app: there is no console, so this is where libmpv discovery and crashes are visible.
 */
object AppLog {
    private val file = File(AppPaths.logsDir, "moovio.log")
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun info(message: String) = append("INFO", message)

    fun error(message: String, throwable: Throwable? = null) {
        append("ERROR", message)
        if (throwable != null) {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            append("ERROR", writer.toString())
        }
    }

    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error("Uncaught exception on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun append(level: String, message: String) = synchronized(this) {
        runCatching {
            if (file.length() > 2_000_000) file.delete()
            file.parentFile?.mkdirs()
            file.appendText("${LocalDateTime.now().format(stamp)} [$level] $message\n")
        }
    }
}
