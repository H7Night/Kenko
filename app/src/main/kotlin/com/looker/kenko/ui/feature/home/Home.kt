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

package com.looker.kenko.ui.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.TrainingExercise
import com.looker.kenko.domain.model.today
import com.looker.kenko.ui.component.ConfirmDialog
import com.looker.kenko.ui.component.DeletableSetItem
import com.looker.kenko.ui.component.StickyHeader
import com.looker.kenko.ui.component.timer.TimerCard
import com.looker.kenko.ui.component.timer.TimerState
import com.looker.kenko.ui.component.timer.TrainingSessionState
import com.looker.kenko.ui.component.timer.rememberNotificationPermissionState
import com.looker.kenko.ui.feature.session.AddSetSheet
import com.looker.kenko.ui.feature.session.ExerciseSearchDialog
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.utils.toast
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit,
    onSelectPlanClick: () -> Unit,
    onExploreSessionsClick: () -> Unit,
    onStartSessionClick: () -> Unit,
    onCurrentPlanClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessionSets by viewModel.sessionSets.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerManager.elapsedSeconds.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val availablePlanDays by viewModel.availablePlanDays.collectAsStateWithLifecycle()
    val planDayTitles by viewModel.planDayTitles.collectAsStateWithLifecycle()
    val notifState = rememberNotificationPermissionState()

    LaunchedEffect(Unit) {
        if (!notifState.granted) {
            notifState.request()
        }
    }

    var showEndConfirm by remember { mutableStateOf(false) }
    var addSetExercise by remember { mutableStateOf<Exercise?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showTrainingDayPicker by remember { mutableStateOf(false) }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text(stringResource(R.string.label_end_workout)) },
            text = { Text(stringResource(R.string.label_end_workout_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.endWorkout()
                        showEndConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.label_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text(stringResource(R.string.label_cancel))
                }
            },
        )
    }

    addSetExercise?.let { exercise ->
        AddSetSheet(
            exercise = exercise,
            date = today(),
            onDismiss = { addSetExercise = null },
        )
    }

    if (showAddExerciseDialog) {
        ExerciseSearchDialog(
            exercises = allExercises,
            onExerciseSelected = { exercise ->
                showAddExerciseDialog = false
                addSetExercise = exercise
            },
            onCreateNew = { _ ->
                showAddExerciseDialog = false
                onStartSessionClick()
            },
            onDismiss = { showAddExerciseDialog = false },
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.label_modify_plan)) },
            text = { Text(stringResource(R.string.label_modify_plan_message)) },
            confirmButton = {
                Button(onClick = {
                    showImportConfirm = false
                    showTrainingDayPicker = true
                }) {
                    Text(stringResource(R.string.label_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text(stringResource(R.string.label_no))
                }
            },
        )
    }

    if (showTrainingDayPicker) {
        TrainingDayPickerDialog(
            availableDays = availablePlanDays.keys.associateWith { planDayTitles[it].orEmpty() },
            onSelect = { day ->
                showTrainingDayPicker = false
                viewModel.selectTrainingDay(day)
            },
            onDismiss = { showTrainingDayPicker = false },
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            TimerCard(
                timerState = state.timerState,
                elapsedSeconds = timerSeconds,
                notificationGranted = notifState.granted,
                hasAccumulatedTime = state.timerState == TimerState.IDLE && timerSeconds > 0,
                showStart = !state.isTodayEmpty,
                onStart = {
                    if (!notifState.granted) {
                        notifState.request()
                    } else {
                        viewModel.startWorkout()
                    }
                },
                onPause = viewModel::pauseWorkout,
                onResume = viewModel::resumeWorkout,
                onEnd = { showEndConfirm = true },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )

            when (state.trainingState) {
                is TrainingSessionState.Idle,
                is TrainingSessionState.Ended -> {
                    PlanInfoCard(
                        isPlanSelected = state.isPlanSelected,
                        planName = state.planName,
                        dayTitle = state.dayTitle,
                        dayIndex = state.dayIndex,
                        dayCount = state.dayCount,
                        isRestDay = state.isRestDay,
                        onSelectPlanClick = onSelectPlanClick,
                        onSwitchTrainingDay = { showTrainingDayPicker = true },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.home_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                is TrainingSessionState.Active -> {
                    TrainingActionBar(
                        onAddExercise = { showAddExerciseDialog = true },
                        onChangePlan = { showImportConfirm = true },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InlineTrainingContent(
                        exerciseSets = sessionSets,
                        onAddSet = { exercise -> addSetExercise = exercise },
                        onRemoveSet = viewModel::removeSet,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingActionBar(
    onAddExercise: () -> Unit,
    onChangePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onAddExercise,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.label_add_exercise), style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(
            onClick = onChangePlan,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.label_change_plan), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InlineTrainingContent(
    exerciseSets: List<TrainingExercise>,
    onAddSet: (Exercise) -> Unit,
    onRemoveSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsedExercises = remember { mutableStateListOf<Int>() }
    var setToDelete by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    setToDelete?.let { id ->
        val deletedMessage = stringResource(R.string.label_deleted)
        ConfirmDialog(
            title = stringResource(R.string.label_delete_set_title),
            message = stringResource(R.string.label_delete_set_message),
            confirmText = stringResource(R.string.label_delete),
            onConfirm = {
                onRemoveSet(id)
                context.toast(deletedMessage)
                setToDelete = null
            },
            onDismiss = { setToDelete = null },
        )
    }

    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        exerciseSets.forEach { row ->
            val exercise = row.exercise
            val sets = row.sets
            val isCollapsed = exercise.id in collapsedExercises
            StickyHeader(
                name = exercise.name,
                sequence = row.sequence?.toString(),
                setCount = sets.size,
                isCollapsed = isCollapsed,
                onCollapseToggle = {
                    if (isCollapsed) {
                        exercise.id?.let { collapsedExercises.remove(it) }
                    } else {
                        exercise.id?.let { collapsedExercises.add(it) }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { onAddSet(exercise) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.label_add), style = MaterialTheme.typography.labelSmall)
                    }
                },
            )
            if (!isCollapsed) {
                sets.forEachIndexed { index, set ->
                    DeletableSetItem(
                        set = set,
                        onDelete = { set.id?.let { setToDelete = it } },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        title = {
                            Text(
                                text = normalizeInt(index + 1),
                                style = MaterialTheme.typography.displayMedium.numbers(),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanInfoCard(
    isPlanSelected: Boolean,
    planName: String?,
    dayTitle: String?,
    dayIndex: Int?,
    dayCount: Int,
    isRestDay: Boolean,
    onSelectPlanClick: () -> Unit,
    onSwitchTrainingDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isPlanSelected) {
        SelectPlanPrompt(onSelectPlanClick = onSelectPlanClick)
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                if (planName != null) {
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = today().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.label_day_progress, dayIndex ?: 1, dayCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                val trainingTitle = dayTitle ?: stringResource(R.string.label_day_n, dayIndex ?: 1)
                Text(
                    text = when {
                        isRestDay -> stringResource(R.string.label_today_rest)
                        else -> "${stringResource(R.string.label_today_plan)}: $trainingTitle"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (isRestDay) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = onSwitchTrainingDay) {
                        Text(stringResource(R.string.label_train_other_day), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (!isRestDay) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_switch_training_day), style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                menuExpanded = false
                                onSwitchTrainingDay()
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingDayPickerDialog(
    availableDays: Map<Int, String>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.label_switch_training_day),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            availableDays.toSortedMap().forEach { (day, title) ->
                OutlinedButton(
                    onClick = { onSelect(day) },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    val displayName = if (title.isNullOrBlank()) {
                        stringResource(R.string.label_day_n, day)
                    } else {
                        title
                    }
                    Text(text = displayName, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SelectPlanPrompt(
    onSelectPlanClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.label_selecting_a_plan),
            style = MaterialTheme.typography.titleLarge.copy(
                lineBreak = LineBreak.Heading,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSelectPlanClick,
            contentPadding = PaddingValues(
                vertical = 12.dp,
                horizontal = 24.dp,
            ),
        ) {
            Text(text = stringResource(R.string.label_select_plan_one), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = KenkoIcons.ArrowOutward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KenkoTopBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = "KENKO") },
        actions = actions,
        modifier = modifier,
    )
}

private fun normalizeInt(value: Int): String = value.toString()
