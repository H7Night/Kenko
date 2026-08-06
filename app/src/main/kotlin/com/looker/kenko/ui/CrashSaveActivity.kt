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

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.looker.kenko.R
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CrashSaveActivity : ComponentActivity() {

    @Inject
    lateinit var crashHandler: CrashHandler

    private var logFilePath: String? = null
    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            saveLogToUri(uri)
        }
        finishAndTerminate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logFilePath = intent.getStringExtra(EXTRA_LOG_FILE_PATH)

        setContent {
            MaterialTheme {
                CrashSaveDialog(
                    onSave = { launchFilePicker() },
                    onDiscard = {
                        crashHandler.deleteLogFile()
                        finishAndTerminate()
                    },
                )
            }
        }
    }

    override fun onBackPressed() {
        // Ignore back press — user must make a choice
    }

    private fun launchFilePicker() {
        val fileName = "kenko-crash-${defaultFileName()}.log"
        // Default to Downloads via initial URI
        val initialUri = "content://com.android.externalstorage.documents/document/primary:Download".toUri()
        createDocLauncher.launch(fileName)
    }

    private fun saveLogToUri(uri: Uri) {
        try {
            val logFile = logFilePath?.let { File(it) }
            if (logFile != null && logFile.exists()) {
                contentResolver.openOutputStream(uri)?.use { output ->
                    logFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, R.string.label_crash_log_saved, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.label_crash_log_save_failed, Toast.LENGTH_SHORT).show()
        } finally {
            crashHandler.deleteLogFile()
        }
    }

    private fun finishAndTerminate() {
        finish()
        // Give the system a moment, then re-throw to trigger the default handler
        Thread {
            Thread.sleep(300)
            android.os.Process.killProcess(android.os.Process.myPid())
        }.start()
    }

    private fun defaultFileName(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    companion object {
        const val EXTRA_LOG_FILE_PATH = "log_file_path"
    }
}

@Composable
private fun CrashSaveDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDiscard,
        title = {
            Text(text = stringResource(R.string.label_crash_dialog_title))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.label_crash_dialog_message))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_crash_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.label_crash_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.label_crash_discard))
            }
        },
    )
}
