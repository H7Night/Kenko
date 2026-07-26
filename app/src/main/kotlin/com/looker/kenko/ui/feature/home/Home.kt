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

package com.looker.kenko.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.localDate
import com.looker.kenko.ui.component.StickyHeader
import com.looker.kenko.ui.component.timer.TimerCard
import com.looker.kenko.ui.component.timer.TrainingSessionState
import com.looker.kenko.ui.component.timer.rememberNotificationPermissionState
import com.looker.kenko.ui.feature.plan.components.dayName
import com.looker.kenko.ui.feature.session.ExerciseSearchDialog
import com.looker.kenko.ui.feature.session.AddSetSheet
import com.looker.kenko.ui.feature.session.components.SetItem
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.header
import com.looker.kenko.ui.theme.numbers
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
    var showImportSheet by remember { mutableStateOf(false) }

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
            date = localDate,
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
                    showImportSheet = true
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

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = stringResource(R.string.label_import_plan),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                availablePlanDays.toSortedMap().forEach { (day, _) ->
                    Button(
                        onClick = {
                            viewModel.importPlanFromDay(day)
                            showImportSheet = false
                        },
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(text = dayName(day))
                    }
                }
            }
        }
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when (state.trainingState) {
                is TrainingSessionState.Idle,
                is TrainingSessionState.Ended -> {
                    PlanInfoCard(
                        isPlanSelected = state.isPlanSelected,
                        planName = state.planName,
                        dayTitle = state.dayTitle,
                        onSelectPlanClick = onSelectPlanClick,
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
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Stick to the plan\nNot your mood.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
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
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FilledTonalButton(
            onClick = onAddExercise,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(stringResource(R.string.label_add_exercise), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(4.dp))
        FilledTonalButton(
            onClick = onChangePlan,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(stringResource(R.string.label_change_plan), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InlineTrainingContent(
    exerciseSets: Map<Exercise, List<Set>>,
    onAddSet: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsedExercises = remember { mutableStateListOf<Int>() }
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        exerciseSets.forEach { (exercise, sets) ->
            val isCollapsed = exercise.id in collapsedExercises
            StickyHeader(
                name = exercise.name,
                setCount = sets.size,
                isCollapsed = isCollapsed,
                onCollapseToggle = {
                    if (isCollapsed) {
                        collapsedExercises.remove(exercise.id!!)
                    } else {
                        collapsedExercises.add(exercise.id!!)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onAddSet(exercise) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(stringResource(R.string.label_add), style = MaterialTheme.typography.labelSmall)
                    }
                },
            )
            if (!isCollapsed) {
                sets.forEachIndexed { index, set ->
                    SetItem(
                        set = set,
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
    onSelectPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isPlanSelected) {
        SelectPlanPrompt(onSelectPlanClick = onSelectPlanClick)
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Plan name
            if (planName != null) {
                Text(
                    text = planName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Date and day of week
            val dateStr = localDate.toString()
            val dayStr = dayName(localDate.dayOfWeek)
            Text(
                text = "$dateStr $dayStr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Today's training title
            Text(
                text = "${stringResource(R.string.label_today_plan)}: ${dayTitle ?: "-"}",
                style = MaterialTheme.typography.titleMedium,
            )
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
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.label_selecting_a_plan),
            style = MaterialTheme.typography.header().copy(
                lineBreak = LineBreak.Heading,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSelectPlanClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            contentPadding = PaddingValues(
                vertical = 24.dp,
                horizontal = 40.dp,
            ),
        ) {
            Text(text = stringResource(R.string.label_select_plan_one))
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                painter = KenkoIcons.ArrowOutward,
                contentDescription = null,
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
