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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightToEdit by remember { mutableStateOf<Weight?>(null) }
    var showWeightHistory by remember { mutableStateOf(false) }

    if (showWeightDialog || weightToEdit != null) {
        val initialWeight = weightToEdit?.value
            ?: state.weights.lastOrNull()?.value
            ?: 60f
        WeightDialog(
            initialWeight = initialWeight,
            isEdit = weightToEdit != null,
            onDismiss = {
                showWeightDialog = false
                weightToEdit = null
            },
            onConfirm = { value ->
                if (weightToEdit != null) {
                    onUpdateWeight(weightToEdit!!.copy(value = value))
                } else {
                    onAddWeight(value)
                }
                showWeightDialog = false
                weightToEdit = null
            }
        )
    }

    if (showWeightHistory) {
        WeightHistorySheet(
            weights = state.weights,
            onDismiss = { showWeightHistory = false },
            onEdit = { 
                weightToEdit = it
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
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding + PaddingValues(horizontal = 16.dp))
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
                                state.planStat!!.exercises,
                                normalizeInt(state.planStat.workDays),
                                normalizeInt(state.planStat.restDays),
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
                onAddClick = { showWeightDialog = true },
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
        val cardShape = MaterialTheme.shapes.extraLarge
        val surfaceShape = remember(cardShape) {
            cardShape.end(16.dp, 16.dp)
        }
        Surface(
            modifier = Modifier.weight(1.5F),
            shape = surfaceShape,
            border = OutlineBorder,
            onClick = onExercisesClick,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.label_exercise),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = numberOfExercises.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }
        val buttonShape = remember(cardShape) {
            cardShape.start(16.dp, 16.dp)
        }
        Box(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
                .clip(buttonShape)
                .clickable(onClick = onAddClick)
                .border(
                    border = SecondaryBorder,
                    shape = buttonShape,
                )
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = KenkoIcons.Add,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = stringResource(R.string.label_add),
            )
        }
    }
}

@Composable
private fun WeightCard(
    weights: List<Weight>,
    onAddClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick),
        shape = MaterialTheme.shapes.extraLarge,
        border = SecondaryBorder,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    onClick = onHistoryClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = KenkoIcons.Rename,
                        contentDescription = stringResource(R.string.label_body_weight_history),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (weights.size >= 2) {
                WeightLineChart(
                    weights = weights,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weights.lastOrNull()?.let { "${it.value} ${stringResource(R.string.label_weight_unit)}" }
                            ?: stringResource(R.string.label_add_body_weight),
                        style = MaterialTheme.typography.displaySmall.numbers()
                    )
                }
            }
        }
    }
}

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
        )
    }
}
