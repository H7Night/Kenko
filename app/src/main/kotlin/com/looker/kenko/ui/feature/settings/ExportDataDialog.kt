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

package com.looker.kenko.ui.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportDataDialog(
    earliestSessionDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (ExportOptions) -> Unit,
) {
    val today = remember {
        Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    var exportSessions by remember { mutableStateOf(true) }
    var exportPlans by remember { mutableStateOf(true) }
    var exportExercises by remember { mutableStateOf(true) }
    var exportWeights by remember { mutableStateOf(true) }

    var startDate by remember { mutableStateOf(earliestSessionDate ?: today) }
    var endDate by remember { mutableStateOf(today) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.toEpochDayMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = millis.toLocalDate()
                        }
                        showStartDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.label_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.toEpochDayMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDate = millis.toLocalDate()
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.label_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                if (exportSessions) {
                    DateRangeRow(
                        label = stringResource(R.string.label_export_start_date),
                        date = startDate,
                        onClick = { showStartDatePicker = true },
                    )
                    DateRangeRow(
                        label = stringResource(R.string.label_export_end_date),
                        date = endDate,
                        onClick = { showEndDatePicker = true },
                    )
                }
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
                            startDate = if (exportSessions) startDate else null,
                            endDate = if (exportSessions) endDate else null,
                        ),
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

@Composable
private fun DateRangeRow(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClick) {
            Text(
                text = "${date.year}-${date.month.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun LocalDate.toEpochDayMillis(): Long {
    return toEpochDays().toInt().toLong() * 86_400_000L
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC).date
}
