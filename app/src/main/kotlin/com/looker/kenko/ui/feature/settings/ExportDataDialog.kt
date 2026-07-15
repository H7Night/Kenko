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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.export.ExportOptions

@Composable
internal fun ExportDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportOptions) -> Unit,
) {
    var exportSessions by remember { mutableStateOf(true) }
    var exportPlans by remember { mutableStateOf(true) }
    var exportExercises by remember { mutableStateOf(true) }
    var exportWeights by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.label_export_data)) },
        text = {
            Column {
                ExportCheckbox(
                    checked = exportSessions,
                    onCheckedChange = { exportSessions = it },
                    label = stringResource(R.string.label_export_sessions),
                )
                ExportCheckbox(
                    checked = exportPlans,
                    onCheckedChange = { exportPlans = it },
                    label = stringResource(R.string.label_export_plans),
                )
                ExportCheckbox(
                    checked = exportExercises,
                    onCheckedChange = { exportExercises = it },
                    label = stringResource(R.string.label_export_exercises),
                )
                ExportCheckbox(
                    checked = exportWeights,
                    onCheckedChange = { exportWeights = it },
                    label = stringResource(R.string.label_export_weights),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ExportOptions(
                            exportSessions = exportSessions,
                            exportPlans = exportPlans,
                            exportExercises = exportExercises,
                            exportWeights = exportWeights,
                        )
                    )
                },
                enabled = exportSessions || exportPlans || exportExercises || exportWeights,
            ) {
                Text(text = stringResource(R.string.label_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.label_cancel))
            }
        },
    )
}

@Composable
private fun ExportCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
