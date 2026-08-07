/*
 * Copyright (C) 2025 LooKeR & Contributors
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import com.looker.kenko.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global uncaught exception handler that collects crash logs and
 * prompts the user to save them before the process terminates.
 */
@Singleton
class CrashHandler @Inject constructor(
    private val app: Application,
) : Thread.UncaughtExceptionHandler {

    private lateinit var defaultHandler: Thread.UncaughtExceptionHandler

    private var crashLogFile: File? = null

    /** Register this handler as the global uncaught exception handler. */
    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler() ?: Thread.UncaughtExceptionHandler { _, _ ->
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        crashLogFile = collectCrashLog(thread, ex)

        try {
            val intent = Intent(app, CrashSaveActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashSaveActivity.EXTRA_LOG_FILE_PATH, crashLogFile?.absolutePath)
            }
            app.startActivity(intent)
        } catch (_: Exception) {
            // If we can't even start the save activity, just pass through
            defaultHandler.uncaughtException(thread, ex)
        }
    }

    /** Directly trigger the original handler to terminate the process. */
    fun terminate(thread: Thread = Thread.currentThread(), ex: Throwable) {
        defaultHandler.uncaughtException(thread, ex)
    }

    /** Clean up temp file if the user chose not to save. */
    fun deleteLogFile() {
        crashLogFile?.delete()
        crashLogFile = null
    }

    private fun collectCrashLog(thread: Thread, ex: Throwable): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val file = File(app.cacheDir, "crash_$timestamp.log")

            PrintWriter(file).use { writer ->
                // Header
                writer.println("=== Kenko Crash Report ===")
                writer.println("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
                writer.println("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                writer.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                writer.println("Thread: ${thread.name}")
                writer.println()

                // Exception stack trace
                writer.println("=== Exception ===")
                ex.printStackTrace(writer)
                writer.println()

                // Logcat (our process only)
                writer.println("=== Logcat ===")
                collectLogcat(writer)
            }

            file
        } catch (_: Exception) {
            null
        }
    }

    private fun collectLogcat(writer: PrintWriter) {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-v", "threadtime", "--pid", Process.myPid().toString(), "*:V")
            )
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { writer.println(it) }
            }
            // Drain error stream
            process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()
        } catch (_: Exception) {
            writer.println("(logcat unavailable)")
        }
    }
}
