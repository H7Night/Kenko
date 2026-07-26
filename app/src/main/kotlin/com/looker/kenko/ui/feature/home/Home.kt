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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.looker.kenko.ui.component.timer.TimerCard
import com.looker.kenko.ui.component.timer.TimerState
import com.looker.kenko.ui.component.timer.TrainingSessionState
import com.looker.kenko.ui.component.timer.rememberNotificationPermissionState
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.header

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
    val timerSeconds by viewModel.timerManager.elapsedSeconds.collectAsStateWithLifecycle()
    val notifState = rememberNotificationPermissionState()

    // Request notification permission on first launch
    LaunchedEffect(Unit) {
        if (!notifState.granted) {
            notifState.request()
        }
    }

    var showEndConfirm by remember { mutableStateOf(false) }

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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            // Timer card - always visible at top
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

            // Main content below timer
            when (state.trainingState) {
                is TrainingSessionState.Idle,
                is TrainingSessionState.Ended -> {
                    // Show current plan with today's exercises
                    PlanInfoCard(
                        isPlanSelected = state.isPlanSelected,
                        planName = state.planName,
                        todayExercises = state.todayExercises,
                        isTodayEmpty = state.isTodayEmpty,
                        onSelectPlanClick = onSelectPlanClick,
                    )
                }
                is TrainingSessionState.Active -> {
                    // Training session card with action buttons
                    TrainingSessionCard(
                        onAddExercise = onStartSessionClick,
                        onChangePlan = onSelectPlanClick,
                        onHistoryClick = onExploreSessionsClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanInfoCard(
    isPlanSelected: Boolean,
    planName: String?,
    todayExercises: List<Exercise>,
    isTodayEmpty: Boolean,
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
            if (planName != null) {
                Text(
                    text = stringResource(R.string.label_current_plan_title, planName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = stringResource(R.string.label_today_plan),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (isTodayEmpty) {
                Text(
                    text = stringResource(R.string.label_no_plan_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_train_anyway_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (todayExercises.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    todayExercises.forEach { exercise ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingSessionCard(
    onAddExercise: () -> Unit,
    onChangePlan: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.label_training_session),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onAddExercise,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = KenkoIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.label_add_exercise))
                }
                FilledTonalButton(
                    onClick = onChangePlan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_change_plan))
                }
                FilledTonalButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_session))
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
