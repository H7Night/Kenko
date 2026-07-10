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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.MuscleGroups
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.ui.feature.exercise.navigation.AddEditExerciseRoute
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddEditExerciseViewModel @Inject constructor(
    private val repo: ExerciseRepo,
    private val stringHandler: StringHandler,
    private val settingsRepo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routeData: AddEditExerciseRoute = savedStateHandle.toRoute()

    private val exerciseId: Int? = routeData.id

    private val defaultTarget: MuscleGroups? = routeData.target?.let { MuscleGroups.valueOf(it) }

    private val targetMuscle = MutableStateFlow(MuscleGroups.Chest)

    val snackbarState = SnackbarHostState()

    var exerciseName: String by mutableStateOf("")
        private set

    val isBodyweightFlow = MutableStateFlow(false)

    private var originalName: String = ""

    var showRenameConfirmation: Boolean by mutableStateOf(false)
        private set

    private val exerciseAlreadyExistError = snapshotFlow { exerciseName }
        .debounce(200.milliseconds)
        .mapLatest { repo.isExerciseAvailable(it) && it != originalName }

    val state = combine(
        targetMuscle,
        exerciseAlreadyExistError,
        isBodyweightFlow,
    ) { target, alreadyExist, bodyweight ->
        AddEditExerciseUiState(
            targetMuscle = target,
            isError = alreadyExist,
            isBodyweight = bodyweight,
        )
    }.asStateFlow(
        AddEditExerciseUiState(
            targetMuscle = MuscleGroups.Chest,
            isError = false,
            isBodyweight = false,
        ),
    )

    fun setName(value: String) {
        exerciseName = value
    }

    fun setTargetMuscle(value: MuscleGroups) {
        viewModelScope.launch {
            targetMuscle.emit(value)
        }
    }

    fun dismissRenameConfirmation() {
        showRenameConfirmation = false
    }

    fun saveExercise(onDone: () -> Unit) {
        viewModelScope.launch {
            if (exerciseName.isBlank()) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_exercise_name_empty))
                return@launch
            }
            if (exerciseId != null && exerciseName != originalName && repo.hasHistory(exerciseId)) {
                showRenameConfirmation = true
                return@launch
            }
            commitRename(onDone)
        }
    }

    fun confirmRename(onDone: () -> Unit) {
        viewModelScope.launch {
            showRenameConfirmation = false
            commitRename(onDone)
        }
    }

    private suspend fun commitRename(onDone: () -> Unit) {
        val name = if (settingsRepo.stream.first().capitalizeExerciseName) {
            exerciseName.titleCase()
        } else {
            exerciseName
        }
        repo.upsert(
            Exercise(
                name = name,
                target = targetMuscle.value,
                isBodyweight = isBodyweightFlow.value,
                id = exerciseId,
            ),
        )
        onDone()
    }

    private fun String.titleCase(): String =
        trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) } }

    init {
        viewModelScope.launch {
            if (exerciseId != null) {
                val exercise = repo.get(exerciseId)
                exercise?.let {
                    originalName = it.name
                    setName(it.name)
                    setTargetMuscle(it.target)
                    isBodyweightFlow.value = it.isBodyweight
                }
            } else {
                if (routeData.name != null) setName(routeData.name)
                if (defaultTarget != null) setTargetMuscle(defaultTarget)
            }
        }
    }
}

@Stable
data class AddEditExerciseUiState(
    val targetMuscle: MuscleGroups,
    val isError: Boolean,
    val isBodyweight: Boolean,
)
