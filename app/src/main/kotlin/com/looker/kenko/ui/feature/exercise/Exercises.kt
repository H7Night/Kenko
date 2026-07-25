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

package com.looker.kenko.ui.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.ExercisesPreviewParameter
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.ErrorSnackbar
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.component.SecondaryKenkoButton
import com.looker.kenko.ui.component.SwipeToDeleteBox
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme

@Composable
fun Exercises(
    viewModel: ExercisesViewModel,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.exercises.collectAsStateWithLifecycle()
    val parentTags by viewModel.parentTags.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedParentFilter.collectAsStateWithLifecycle()

    Exercises(
        state = state,
        parentTags = parentTags,
        selectedFilter = selectedFilter,
        onSelectFilter = viewModel::setParentFilter,
        snackbarState = viewModel.snackbarState,
        onBackPress = onBackPress,
        onExerciseClick = onExerciseClick,
        onCreateClick = onCreateClick,
        onReferenceClick = viewModel::onReferenceClick,
        onRemove = viewModel::removeExercise,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Exercises(
    state: List<Exercise>,
    parentTags: List<Tag>,
    selectedFilter: Int?,
    onSelectFilter: (Int?) -> Unit,
    snackbarState: SnackbarHostState,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onRemove: (Int?) -> Unit,
    onBackPress: () -> Unit,
    onReferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxWidth(),
        floatingActionButton = {
            SecondaryKenkoButton(
                onClick = onCreateClick,
                label = {
                    Icon(
                        painter = KenkoIcons.Add,
                        contentDescription = null,
                    )
                },
                icon = {
                    Text(stringResource(R.string.label_create_exercise))
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) {
                ErrorSnackbar(data = it)
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.label_browse_exercises))
                    },
                    navigationIcon = {
                        BackButton(onClick = onBackPress)
                    }
                )
                HorizontalDivider(thickness = KenkoBorderWidth)
                // Parent tag filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { onSelectFilter(null) },
                            label = { Text(stringResource(R.string.label_all_muscle_groups)) },
                        )
                    }
                    items(parentTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = selectedFilter == tag.id,
                            onClick = { onSelectFilter(tag.id) },
                            label = { Text(tag.name) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.isEmpty()) {
            EmptyExercises(
                modifier = Modifier.padding(innerPadding + PaddingValues(bottom = 80.dp)),
            )
        } else {
            ExercisesList(
                exercises = state,
                contentPadding = innerPadding + PaddingValues(bottom = 80.dp),
                onExerciseClick = onExerciseClick,
                onReferenceClick = onReferenceClick,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun EmptyExercises(modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        Text(
            text = stringResource(R.string.label_no_exercise_today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisesList(
    exercises: List<Exercise>,
    contentPadding: PaddingValues,
    onExerciseClick: (id: Int?) -> Unit,
    onRemove: (Int?) -> Unit,
    onReferenceClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(exercises, key = { it.id!! }) { exercise ->
            val exerciseId by rememberUpdatedState(exercise.id)
            SwipeToDeleteBox(
                modifier = Modifier.animateItem(),
                onDismiss = { onRemove(exerciseId) }
            ) {
                Surface(
                    onClick = { onExerciseClick(exerciseId) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (exercise.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 6.dp),
                            ) {
                                exercise.tags.forEach { tag ->
                                    val label = tag.parentName?.let { "${it}→${tag.name}" } ?: tag.name
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ExercisesPreview(
    @PreviewParameter(ExercisesPreviewParameter::class, limit = 2) exercises: List<Exercise>,
) {
    KenkoTheme {
        Exercises(
            state = exercises,
            parentTags = emptyList(),
            selectedFilter = null,
            onSelectFilter = {},
            snackbarState = SnackbarHostState(),
            onExerciseClick = {},
            onCreateClick = {},
            onBackPress = {},
            onReferenceClick = {},
            onRemove = {}
        )
    }
}
