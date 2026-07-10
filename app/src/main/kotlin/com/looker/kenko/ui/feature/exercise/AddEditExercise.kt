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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.MuscleGroups
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.ErrorSnackbar
import com.looker.kenko.ui.component.FlowTargets
import com.looker.kenko.ui.component.KenkoButton
import com.looker.kenko.ui.component.TargetChip
import com.looker.kenko.ui.component.kenkoTextFieldColor
import com.looker.kenko.ui.feature.exercise.string
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme

@Composable
fun AddEditExercise(
    onDone: () -> Unit,
    onBackPress: () -> Unit,
) {
    val viewModel: AddEditExerciseViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (viewModel.showRenameConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameConfirmation,
            title = {
                Text(stringResource(R.string.label_rename_exercise))
            },
            text = {
                Text(stringResource(R.string.label_rename_exercise_description))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRename(onDone) }) {
                    Text(stringResource(R.string.label_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRenameConfirmation) {
                    Text(stringResource(R.string.label_cancel))
                }
            },
        )
    }

    AddEditExercise(
        exerciseName = viewModel.exerciseName,
        state = state,
        snackbarState = viewModel.snackbarState,
        onSelectTarget = viewModel::setTargetMuscle,
        onBodyweightChange = { viewModel.isBodyweightFlow.value = it },
        onNameChange = viewModel::setName,
        onBackPress = onBackPress,
        onDone = { viewModel.saveExercise(onDone) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditExercise(
    exerciseName: String,
    state: AddEditExerciseUiState,
    snackbarState: SnackbarHostState,
    onSelectTarget: (MuscleGroups) -> Unit,
    onBodyweightChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
    onBackPress: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBackPress) },
                title = {
                    Text(
                        text = stringResource(
                            if (exerciseName.isBlank()) {
                                R.string.label_new_exercise
                            } else {
                                R.string.label_edit_exercise
                            }
                        ),
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) {
                ErrorSnackbar(data = it)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 16.dp) + innerPadding),
        ) {
            ExerciseTextField(
                exerciseName = exerciseName,
                onNameChange = onNameChange,
                isError = state.isError,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = stringResource(R.string.label_target),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            FlowTargets {
                TargetChip(
                    selected = state.targetMuscle == it,
                    onClick = {
                        focusManager.clearFocus()
                        onSelectTarget(it)
                    },
                    text = stringResource(it.string),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_use_bodyweight),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                Switch(
                    checked = state.isBodyweight,
                    onCheckedChange = onBodyweightChange,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            KenkoButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding(),
                onClick = {
                    focusManager.clearFocus()
                    onDone()
                },
                label = {
                    Icon(
                        painter = KenkoIcons.Save,
                        contentDescription = null,
                    )
                },
                icon = {
                    Text(stringResource(R.string.label_save))
                }
            )
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ExerciseTextField(
    exerciseName: String,
    isError: Boolean,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        modifier = modifier,
        value = exerciseName,
        onValueChange = onNameChange,
        colors = kenkoTextFieldColor(),
        shape = MaterialTheme.shapes.large,
        label = {
            Text(text = stringResource(R.string.label_name))
        },
        isError = isError,
        leadingIcon = {
            Icon(painter = KenkoIcons.Rename, contentDescription = null)
        },
        supportingText = {
            if (isError) {
                Text(text = stringResource(R.string.label_exercise_exists))
            }
        }
    )
}

@Preview(name = "Exercise Name Field")
@Composable
private fun NameTextFieldPreview() {
    KenkoTheme {
        ExerciseTextField(
            exerciseName = "Bench Press",
            onNameChange = {},
            isError = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Exercise Name Field - Error")
@Composable
private fun ErrorNameTextFieldPreview() {
    KenkoTheme {
        ExerciseTextField(
            exerciseName = "Bench Press",
            onNameChange = {},
            isError = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditPreview() {
    KenkoTheme {
        AddEditExercise(
            exerciseName = "BenchPress",
            state = AddEditExerciseUiState(MuscleGroups.Chest, false, false),
            snackbarState = SnackbarHostState(),
            onSelectTarget = {},
            onBodyweightChange = {},
            onNameChange = {},
            onDone = {},
            onBackPress = {}
        )
    }
}
