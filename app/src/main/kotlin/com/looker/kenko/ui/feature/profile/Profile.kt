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

package com.looker.kenko.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.PlanStat
import com.looker.kenko.domain.model.Weight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.OutlineBorder
import com.looker.kenko.ui.component.SecondaryBorder
import com.looker.kenko.ui.component.WeightLineChart
import com.looker.kenko.ui.extension.normalizeInt
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.ui.theme.start

private sealed interface WeightDialogState {
    data object Hidden : WeightDialogState
    data class Add(val initial: Float) : WeightDialogState
    data class Edit(val weight: Weight) : WeightDialogState
}

@Composable
fun Profile(
    viewModel: ProfileViewModel,
    onBackPress: () -> Unit,
    onExercisesClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onPlanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showBackButton: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Profile(
        state = state,
        onBackPress = onBackPress,
        onSettingsClick = onSettingsClick,
        onPlanClick = onPlanClick,
        onAddExerciseClick = onAddExerciseClick,
        onExercisesClick = onExercisesClick,
        onAddWeight = viewModel::addWeight,
        onUpdateWeight = viewModel::updateWeight,
        onDeleteWeight = viewModel::deleteWeight,
        onPrevMonth = viewModel::prevMonth,
        onNextMonth = viewModel::nextMonth,
        onSetCustomRange = viewModel::setCustomRange,
        onClearCustomRange = viewModel::clearCustomRange,
        showBackButton = showBackButton,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Profile(
    state: ProfileUiState,
    onBackPress: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlanClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onAddWeight: (Float) -> Unit,
    onUpdateWeight: (Weight) -> Unit,
    onDeleteWeight: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetCustomRange: (LocalDate, LocalDate) -> Unit,
    onClearCustomRange: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
) {
    var weightDialog by remember { mutableStateOf<WeightDialogState>(WeightDialogState.Hidden) }
    var showWeightHistory by remember { mutableStateOf(false) }

    when (val dialog = weightDialog) {
        is WeightDialogState.Add -> {
            WeightDialog(
                initialWeight = dialog.initial,
                isEdit = false,
                onDismiss = { weightDialog = WeightDialogState.Hidden },
                onConfirm = { value ->
                    onAddWeight(value)
                    weightDialog = WeightDialogState.Hidden
                },
            )
        }
        is WeightDialogState.Edit -> {
            WeightDialog(
                initialWeight = dialog.weight.value,
                isEdit = true,
                onDismiss = { weightDialog = WeightDialogState.Hidden },
                onConfirm = { value ->
                    onUpdateWeight(dialog.weight.copy(value = value))
                    weightDialog = WeightDialogState.Hidden
                },
            )
        }
        WeightDialogState.Hidden -> Unit
    }

    if (showWeightHistory) {
        WeightHistorySheet(
            weights = state.weights,
            onDismiss = { showWeightHistory = false },
            onEdit = {
                weightDialog = WeightDialogState.Edit(it)
                showWeightHistory = false
            },
            onDelete = onDeleteWeight
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.label_profile)) },
                navigationIcon = { if (showBackButton) BackButton(onBackPress) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(painter = KenkoIcons.Settings, contentDescription = null)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        // 页面位于外层 Scaffold(底部导航栏)内,外层已通过 innerPadding 处理
        // 系统导航条避让;这里禁用内层 Scaffold 的 systemBars insets,
        // 否则底部会多出一段与导航栏高度不符的空白(白条)。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding + PaddingValues(start = 16.dp, end = 16.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isPlanAvailable) {
                CurrentPlanCard(
                    onPlanClick = onPlanClick,
                    name = state.planName,
                    content = {
                        Text(
                            text = stringResource(
                                R.string.label_plan_description,
                                state.planStat?.exercises ?: 0,
                                normalizeInt(state.planStat?.workDays ?: 0),
                                normalizeInt(
                                    (state.planDayCount - (state.planStat?.workDays ?: 0))
                                        .coerceAtLeast(0),
                                ),
                            ),
                        )
                    },
                )
            } else {
                SelectPlanCard(onPlanClick)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ExerciseCard(
                numberOfExercises = state.numberOfExercises,
                onAddClick = onAddExerciseClick,
                onExercisesClick = onExercisesClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeightCard(
                weights = state.weights,
                filteredWeights = state.filteredWeights,
                selectedMonthLabel = state.selectedMonthLabel,
                canGoPrev = state.canGoPrev,
                canGoNext = state.canGoNext,
                customRangeActive = state.customRangeActive,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onSetCustomRange = onSetCustomRange,
                onClearCustomRange = onClearCustomRange,
                onAddClick = { weightDialog = WeightDialogState.Add(state.weights.lastOrNull()?.value ?: 60f) },
                onHistoryClick = { showWeightHistory = true }
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    numberOfExercises: Int,
    onAddClick: () -> Unit,
    onExercisesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cardShape = MaterialTheme.shapes.medium
        val surfaceShape = remember(cardShape) {
            cardShape.end(8.dp, 8.dp)
        }
        Surface(
            modifier = Modifier.weight(1.5F),
            shape = surfaceShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            onClick = onExercisesClick,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.label_exercise),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = numberOfExercises.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        val buttonShape = remember(cardShape) {
            cardShape.start(8.dp, 8.dp)
        }
        Box(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
                .clip(buttonShape)
                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), buttonShape)
                .clickable(onClick = onAddClick)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = KenkoIcons.Add,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = stringResource(R.string.label_add),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightCard(
    weights: List<Weight>,
    filteredWeights: List<Weight>,
    selectedMonthLabel: String?,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    customRangeActive: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSetCustomRange: (LocalDate, LocalDate) -> Unit,
    onClearCustomRange: () -> Unit,
    onAddClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRangeDialog by remember { mutableStateOf(false) }

    if (showRangeDialog) {
        WeightRangeDialog(
            onDismiss = { showRangeDialog = false },
            onConfirm = { start, end ->
                onSetCustomRange(start, end)
                showRangeDialog = false
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_body_weight),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = KenkoIcons.Add,
                        contentDescription = stringResource(R.string.label_add),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Filter row: month switcher + custom range (only when there is any record)
            if (weights.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onPrevMonth,
                        enabled = canGoPrev,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = selectedMonthLabel.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    IconButton(
                        onClick = onNextMonth,
                        enabled = canGoNext,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showRangeDialog = true }) {
                        Text(stringResource(R.string.label_custom_range))
                    }
                    if (customRangeActive) {
                        TextButton(onClick = onClearCustomRange) {
                            Text(stringResource(R.string.label_clear))
                        }
                    }
                }
            }

            when {
                weights.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.label_add_body_weight),
                            style = MaterialTheme.typography.displaySmall.numbers()
                        )
                    }
                }
                filteredWeights.size >= 2 -> {
                    // Tap the chart to open the weight history/edit sheet
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onHistoryClick),
                    ) {
                        WeightLineChart(
                            weights = filteredWeights,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }
                filteredWeights.size == 1 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${filteredWeights[0].value} ${stringResource(R.string.label_weight_unit)}",
                            style = MaterialTheme.typography.displaySmall.numbers()
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.label_no_weight_in_period),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val today = com.looker.kenko.domain.model.today()
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (startDate ?: today).toEpochDayMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { startDate = it.toLocalDate() }
                        showStartPicker = false
                    },
                ) { Text(stringResource(R.string.label_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.label_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (endDate ?: today).toEpochDayMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { endDate = it.toLocalDate() }
                        showEndPicker = false
                    },
                ) { Text(stringResource(R.string.label_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.label_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_custom_range_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeDateRow(
                    label = stringResource(R.string.label_start_date),
                    date = startDate,
                    onClick = { showStartPicker = true },
                )
                RangeDateRow(
                    label = stringResource(R.string.label_end_date),
                    date = endDate,
                    onClick = { showEndPicker = true },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = startDate ?: return@TextButton
                    val e = endDate ?: return@TextButton
                    onConfirm(minOf(s, e), maxOf(s, e))
                },
                enabled = startDate != null && endDate != null,
            ) { Text(stringResource(R.string.label_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.label_cancel)) }
        },
    )
}

@Composable
private fun RangeDateRow(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) {
            Text(
                text = date?.toString() ?: stringResource(R.string.label_select_date),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun LocalDate.toEpochDayMillis(): Long =
    toEpochDays().toInt().toLong() * 86_400_000L

private fun Long.toLocalDate(): LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date

@Preview(showBackground = true)
@Composable
private fun ExerciseCardPreview() {
    KenkoTheme {
        ExerciseCard(21, {}, {})
    }
}

@Preview
@Composable
private fun ProfileNoPlanPreview() {
    KenkoTheme {
        Profile(
            state = ProfileUiState(12, false, "Push-Pull-Leg", emptyList(), PlanStat(12, 5)),
            onBackPress = { },
            onSettingsClick = { },
            onPlanClick = { },
            onAddExerciseClick = { },
            onExercisesClick = { },
            onAddWeight = {},
            onUpdateWeight = {},
            onDeleteWeight = {},
            onPrevMonth = {},
            onNextMonth = {},
            onSetCustomRange = { _, _ -> },
            onClearCustomRange = {},
        )
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    KenkoTheme {
        Profile(
            state = ProfileUiState(12, true, "Push-Pull-Leg", emptyList(), PlanStat(12, 5)),
            onBackPress = { },
            onSettingsClick = { },
            onPlanClick = { },
            onAddExerciseClick = { },
            onExercisesClick = { },
            onAddWeight = {},
            onUpdateWeight = {},
            onDeleteWeight = {},
            onPrevMonth = {},
            onNextMonth = {},
            onSetCustomRange = { _, _ -> },
            onClearCustomRange = {},
        )
    }
}
