/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.ui.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.looker.kenko.R
import com.looker.kenko.data.export.ExportOptions
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.start
import com.looker.kenko.utils.toFormat
import kotlin.time.Instant

@Composable
internal fun BackupSection(
    backupUri: String?,
    backupInterval: BackupInterval,
    lastBackupTime: Instant?,
    isBackingUp: Boolean,
    isRestoring: Boolean,
    isExporting: Boolean,
    onSelectLocation: (Uri) -> Unit,
    onSelectInterval: (BackupInterval) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (Uri) -> Unit,
    onExport: (ExportOptions, Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportOptions by remember { mutableStateOf<ExportOptions?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onSelectLocation(it)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreDialog = true
        }
    }

    val jsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null && pendingExportOptions != null) {
            onExport(pendingExportOptions!!, uri!!)
            pendingExportOptions = null
        }
    }

    if (showRestoreDialog) {
        RestoreConfirmationDialog(
            onConfirm = {
                pendingRestoreUri?.let { onRestore(it) }
                showRestoreDialog = false
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreDialog = false
                pendingRestoreUri = null
            },
        )
    }

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { options ->
                pendingExportOptions = options
                jsonFileLauncher.launch("kenko_export.json")
                showExportDialog = false
            },
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackupSettingRow(
            title = stringResource(R.string.label_backup_location),
            value = backupUri?.let { extractFolderName(it) }
                ?: stringResource(R.string.label_backup_location_not_set),
            onClick = { folderPickerLauncher.launch(null) },
        )

        Text(
            text = stringResource(R.string.label_backup_interval),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        BackupIntervalSelector(
            selectedInterval = backupInterval,
            onSelectInterval = onSelectInterval,
            enabled = backupUri != null,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (lastBackupTime != null) {
            Text(
                text = stringResource(R.string.label_last_backup, lastBackupTime.toFormat()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            OutlinedButton(
                onClick = onBackupNow,
                enabled = backupUri != null && !isBackingUp && !isRestoring && !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isBackingUp) {
                        stringResource(R.string.label_backup_in_progress)
                    } else {
                        stringResource(R.string.label_backup_now)
                    },
                )
            }

            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/zip")) },
                enabled = !isBackingUp && !isRestoring && !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isRestoring) {
                        stringResource(R.string.label_restore_in_progress)
                    } else {
                        stringResource(R.string.label_restore)
                    },
                )
            }
        }

        OutlinedButton(
            onClick = { showExportDialog = true },
            enabled = !isBackingUp && !isRestoring && !isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.label_export_data),
            )
        }
    }
}

@Composable
internal fun BackupSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Icon(
            painter = KenkoIcons.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupIntervalSelector(
    selectedInterval: BackupInterval,
    onSelectInterval: (BackupInterval) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        BackupInterval.entries.forEachIndexed { index, interval ->
            SegmentedButton(
                selected = selectedInterval == interval,
                onClick = { onSelectInterval(interval) },
                enabled = enabled,
                shape = when (index) {
                    0 -> CircleShape.end(8.dp)
                    BackupInterval.entries.lastIndex -> CircleShape.start(8.dp)
                    else -> RoundedCornerShape(8.dp)
                },
                colors = backupButtonColors,
                modifier = Modifier.padding(2.dp),
            ) {
                Text(
                    text = stringResource(interval.nameRes),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
internal fun RestoreConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_restore)) },
        text = { Text(stringResource(R.string.label_restore_warning)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.label_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_no))
            }
        },
    )
}

private fun extractFolderName(uri: String): String {
    return try {
        uri.toUri().lastPathSegment?.substringAfterLast('/') ?: uri
    } catch (_: Exception) {
        uri
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val backupButtonColors: SegmentedButtonColors
    @Composable
    get() = SegmentedButtonDefaults.colors(
        activeBorderColor = Color.Transparent,
        inactiveBorderColor = Color.Transparent,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )

@Preview
@Composable
private fun BackupSectionPreview() {
    KenkoTheme {
        BackupSection(
            backupUri = "content://com.android.providers.downloads/tree/downloads",
            backupInterval = BackupInterval.Daily,
            lastBackupTime = null,
            isBackingUp = false,
            isRestoring = false,
            isExporting = false,
            onSelectLocation = {},
            onSelectInterval = {},
            onBackupNow = {},
            onRestore = {},
            onExport = { _, _ -> },
        )
    }
}
