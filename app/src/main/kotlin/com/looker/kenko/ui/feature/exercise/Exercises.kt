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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.looker.kenko.domain.model.labelRes
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.EmptyState
import com.looker.kenko.ui.component.ErrorSnackbar
import com.looker.kenko.ui.component.ConfirmDialog
import com.looker.kenko.ui.component.DeleteIconButton
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.feature.plan.components.KenkoAddButton
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.utils.toast

@Composable
fun Exercises(
    viewModel: ExercisesViewModel,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onBackPress: () -> Unit,
    onProgressClick: (Int) -> Unit,
) {
    val state by viewModel.exercises.collectAsStateWithLifecycle()
    val parentTags by viewModel.parentTags.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedParent by viewModel.selectedParentFilter.collectAsStateWithLifecycle()
    val selectedChild by viewModel.selectedChildFilter.collectAsStateWithLifecycle()
    val selectedEquipment by viewModel.selectedEquipment.collectAsStateWithLifecycle()

    Exercises(
        state = state,
        parentTags = parentTags,
        allTags = allTags,
        selectedParent = selectedParent,
        selectedChild = selectedChild,
        selectedEquipment = selectedEquipment,
        onSelectParent = viewModel::setParentFilter,
        onSelectChild = viewModel::setChildFilter,
        onSelectEquipment = { viewModel.selectedEquipment.value = it },
        snackbarState = viewModel.snackbarState,
        onBackPress = onBackPress,
        onExerciseClick = onExerciseClick,
        onCreateClick = onCreateClick,
        onReferenceClick = viewModel::onReferenceClick,
        onRemove = viewModel::removeExercise,
        onProgressClick = onProgressClick,
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
    selectedEquipment: com.looker.kenko.domain.model.ExerciseEquipment? = null,
    onSelectEquipment: (com.looker.kenko.domain.model.ExerciseEquipment?) -> Unit = {},
    snackbarState: SnackbarHostState,
    onExerciseClick: (id: Int?) -> Unit,
    onCreateClick: () -> Unit,
    onRemove: (Int?) -> Unit,
    onBackPress: () -> Unit,
    onReferenceClick: (String) -> Unit,
    onProgressClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exerciseToDelete by remember { mutableStateOf<Int?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var parentExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val childTags = remember(allTags, selectedParent) {
        if (selectedParent == null) emptyList()
        else allTags.filter { it.parentId == selectedParent }
    }
    val query = searchQuery.trim()
    val visibleExercises = remember(state, query) {
        if (query.isEmpty()) state
        else state.filter { it.name.contains(query, ignoreCase = true) }
    }
    val hasActiveFilters = selectedParent != null ||
        selectedChild != null ||
        selectedEquipment != null ||
        query.isNotEmpty()
    val selectedParentName = parentTags.firstOrNull { it.id == selectedParent }?.name
        ?: stringResource(R.string.label_all_parts)
    val selectedChildName = allTags.firstOrNull { it.id == selectedChild }?.name
        ?: stringResource(R.string.label_all_muscles)
    val selectedEquipmentName = selectedEquipment?.let { stringResource(it.labelRes) }
        ?: stringResource(R.string.label_all_equipment)

    Scaffold(
        modifier = modifier.fillMaxWidth(),
        floatingActionButton = {
            KenkoAddButton(
                onClick = onCreateClick,
                label = stringResource(R.string.label_create_exercise),
            )
        },
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.label_search_exercises)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.label_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        FilterChip(
                            selected = selectedParent != null,
                            onClick = { parentExpanded = true },
                            label = { Text(selectedParentName) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = parentExpanded,
                            onDismissRequest = { parentExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_all_parts)) },
                                onClick = {
                                    onSelectParent(null)
                                    parentExpanded = false
                                },
                            )
                            parentTags.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name) },
                                    onClick = {
                                        onSelectParent(parent.id)
                                        parentExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (selectedParent != null) {
                        Box {
                            FilterChip(
                                selected = selectedChild != null,
                                onClick = { childExpanded = true },
                                label = { Text(selectedChildName) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                    )
                                },
                            )
                            DropdownMenu(
                                expanded = childExpanded,
                                onDismissRequest = { childExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_all_muscles)) },
                                    onClick = {
                                        onSelectChild(null)
                                        childExpanded = false
                                    },
                                )
                                childTags.forEach { muscle ->
                                    DropdownMenuItem(
                                        text = { Text(muscle.name) },
                                        onClick = {
                                            onSelectChild(muscle.id)
                                            childExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Box {
                        FilterChip(
                            selected = selectedEquipment != null,
                            onClick = { equipmentExpanded = true },
                            label = { Text(selectedEquipmentName) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = equipmentExpanded,
                            onDismissRequest = { equipmentExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_all_equipment)) },
                                onClick = {
                                    onSelectEquipment(null)
                                    equipmentExpanded = false
                                },
                            )
                            com.looker.kenko.domain.model.ExerciseEquipment.entries.forEach { equipment ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(equipment.labelRes)) },
                                    onClick = {
                                        onSelectEquipment(equipment)
                                        equipmentExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (visibleExercises.isEmpty()) {
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
                    text = stringResource(
                        if (hasActiveFilters) R.string.label_no_matching_exercises
                        else R.string.label_no_exercises,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            onSelectParent(null)
                            onSelectChild(null)
                            onSelectEquipment(null)
                        },
                    ) {
                        Text(stringResource(R.string.label_clear_filters))
                    }
                } else {
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
            }
        } else {
            ExercisesList(
                exercises = visibleExercises,
                contentPadding = innerPadding + PaddingValues(bottom = 80.dp),
                onExerciseClick = onExerciseClick,
                onReferenceClick = onReferenceClick,
                onRequestRemove = { exerciseToDelete = it },
                onProgressClick = onProgressClick,
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
    onProgressClick: (Int) -> Unit,
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
                color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                    exerciseId?.let { id ->
                        IconButton(onClick = { onProgressClick(id) }) {
                            Icon(
                                imageVector = Icons.Rounded.ShowChart,
                                contentDescription = stringResource(R.string.label_exercise_progress),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    DeleteIconButton(onClick = { onRequestRemove(exerciseId) })
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
            onRemove = {},
            onProgressClick = {},
        )
    }
}
