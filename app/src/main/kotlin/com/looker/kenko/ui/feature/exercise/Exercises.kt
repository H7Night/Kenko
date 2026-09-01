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

package com.looker.kenko.ui.feature.exercise

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.ExercisesPreviewParameter
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.EmptyState
import com.looker.kenko.ui.component.ErrorSnackbar
import com.looker.kenko.ui.component.KenkoBorder
import com.looker.kenko.ui.component.KenkoBorderStrong
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.component.SecondaryKenkoButton
import com.looker.kenko.ui.component.ConfirmDialog
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.utils.toast

@Composable
fun Exercises(
    viewModel: ExercisesViewModel,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.exercises.collectAsStateWithLifecycle()
    val parentTags by viewModel.parentTags.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedParent by viewModel.selectedParentFilter.collectAsStateWithLifecycle()
    val selectedChild by viewModel.selectedChildFilter.collectAsStateWithLifecycle()

    Exercises(
        state = state,
        parentTags = parentTags,
        allTags = allTags,
        selectedParent = selectedParent,
        selectedChild = selectedChild,
        onSelectParent = viewModel::setParentFilter,
        onSelectChild = viewModel::setChildFilter,
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
    parentTags: List<com.looker.kenko.domain.model.Tag>,
    allTags: List<com.looker.kenko.domain.model.Tag>,
    selectedParent: Int?,
    selectedChild: Int?,
    onSelectParent: (Int?) -> Unit,
    onSelectChild: (Int?) -> Unit,
    snackbarState: SnackbarHostState,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onRemove: (Int?) -> Unit,
    onBackPress: () -> Unit,
    onReferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exerciseToDelete by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

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

                // Task 2: Two-level FilterChip rows — reuse Tag grouping, KenkoBorder/KenkoBorderStrong
                val childTags = remember(allTags, selectedParent) {
                    if (selectedParent == null) emptyList()
                    else allTags.filter { it.parentId == selectedParent }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedParent == null,
                        onClick = { onSelectParent(null) },
                        label = { Text(stringResource(R.string.label_all_muscle_groups)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = if (selectedParent == null) KenkoBorderStrong else KenkoBorder,
                    )
                    parentTags.forEach { parent ->
                        val isSelected = parent.id == selectedParent
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectParent(parent.id) },
                            label = { Text(parent.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = if (isSelected) KenkoBorderStrong else KenkoBorder,
                        )
                    }
                }
                if (selectedParent != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = selectedChild == null,
                            onClick = { onSelectChild(null) },
                            label = { Text(stringResource(R.string.label_all_muscle_groups)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = if (selectedChild == null) KenkoBorderStrong else KenkoBorder,
                        )
                        childTags.forEach { muscle ->
                            val isSelected = muscle.id == selectedChild
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectChild(muscle.id) },
                                label = { Text(muscle.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                                border = if (isSelected) KenkoBorderStrong else KenkoBorder,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding + PaddingValues(bottom = 80.dp))
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    icon = Icons.Rounded.FitnessCenter,
                    text = stringResource(R.string.label_no_exercise_today),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(painter = KenkoIcons.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.label_create_exercise))
                }
            }
        } else {
            ExercisesList(
                exercises = state,
                contentPadding = innerPadding + PaddingValues(bottom = 80.dp),
                onExerciseClick = onExerciseClick,
                onReferenceClick = onReferenceClick,
                onRequestRemove = { exerciseToDelete = it },
            )
        }
    }

    exerciseToDelete?.let { id ->
        val deletedMessage = stringResource(R.string.label_deleted)
        ConfirmDialog(
            title = stringResource(R.string.label_delete_exercise_title),
            message = stringResource(R.string.label_delete_exercise_message),
            confirmText = stringResource(R.string.label_delete),
            onConfirm = {
                onRemove(id)
                exerciseToDelete = null
                context.toast(deletedMessage)
            },
            onDismiss = { exerciseToDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisesList(
    exercises: List<Exercise>,
    contentPadding: PaddingValues,
    onExerciseClick: (id: Int?) -> Unit,
    onRequestRemove: (Int?) -> Unit,
    onReferenceClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(exercises, key = { it.id ?: it.hashCode() }) { exercise ->
            val exerciseId by rememberUpdatedState(exercise.id)
            Surface(
                modifier = Modifier.animateItem(),
                onClick = { onExerciseClick(exerciseId) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (exercise.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                exercise.tags.forEach { tag ->
                                    val label = tag.parentName?.let { "${it}→${tag.name}" } ?: tag.name
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { onRequestRemove(exerciseId) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.label_delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
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
            allTags = emptyList(),
            selectedParent = null,
            selectedChild = null,
            onSelectParent = {},
            onSelectChild = {},
            snackbarState = SnackbarHostState(),
            onExerciseClick = {},
            onCreateClick = {},
            onBackPress = {},
            onReferenceClick = {},
            onRemove = {}
        )
    }
}
