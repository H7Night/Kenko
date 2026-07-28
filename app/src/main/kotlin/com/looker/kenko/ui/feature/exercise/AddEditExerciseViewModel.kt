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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.ui.feature.exercise.navigation.AddEditExerciseRoute
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddEditExerciseViewModel @Inject constructor(
    private val repo: ExerciseRepo,
    private val tagRepo: TagRepo,
    private val stringHandler: StringHandler,
    private val settingsRepo: SettingsRepo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routeData: AddEditExerciseRoute = savedStateHandle.toRoute()

    private val exerciseId: Int? = routeData.id

    private val exerciseName = MutableStateFlow("")

    val isBodyweightFlow = MutableStateFlow(false)

    val countTypeFlow = MutableStateFlow(CountType.REPS)

    val selectedTags = MutableStateFlow<List<Tag>>(emptyList())

    private var originalName: String = ""

    val showRenameConfirmation = MutableStateFlow(false)

    val snackbarState = SnackbarHostState()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    val tagState = combine(
        tagRepo.streamParents,
        tagRepo.stream,
    ) { parents, all ->
        val children = all.filter { it.parentId != null }
        TagSelectorState(parents = parents, children = children)
    }.asStateFlow(TagSelectorState())

    private val exerciseAlreadyExistError = exerciseName
        .debounce(200.milliseconds)
        .mapLatest { repo.isExerciseAvailable(it) && it != originalName }

    val state = combine(
        exerciseAlreadyExistError,
        isBodyweightFlow,
        countTypeFlow,
        selectedTags,
        exerciseName,
        showRenameConfirmation,
    ) { array: Array<*> ->
        AddEditExerciseUiState(
            isError = array[0] as Boolean,
            isBodyweight = array[1] as Boolean,
            countType = array[2] as CountType,
            selectedTags = @Suppress("UNCHECKED_CAST") array[3] as List<Tag>,
            exerciseName = array[4] as String,
            showRenameConfirmation = array[5] as Boolean,
        )
    }.asStateFlow(
        AddEditExerciseUiState(
            isError = false,
            isBodyweight = false,
            countType = CountType.REPS,
            selectedTags = emptyList(),
            exerciseName = "",
            showRenameConfirmation = false,
        ),
    )

    fun setName(value: String) {
        exerciseName.value = value
    }

    fun addTag(tag: Tag) {
        val current = selectedTags.value.toMutableList()
        if (current.none { it.id == tag.id }) {
            current.add(tag)
            selectedTags.value = current
        }
    }

    fun removeTag(tag: Tag) {
        selectedTags.value = selectedTags.value.filter { it.id != tag.id }
    }

    fun setCountType(type: CountType) {
        countTypeFlow.value = type
    }

    fun dismissRenameConfirmation() {
        showRenameConfirmation.value = false
    }

    fun saveExercise(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val name = exerciseName.value
                if (name.isBlank()) {
                    snackbarState.showSnackbar(stringHandler.getString(R.string.error_exercise_name_empty))
                    return@launch
                }
                if (selectedTags.value.isEmpty()) {
                    snackbarState.showSnackbar(stringHandler.getString(R.string.label_at_least_one_tag))
                    return@launch
                }
                if (exerciseId != null && name != originalName && repo.hasHistory(exerciseId)) {
                    showRenameConfirmation.value = true
                    return@launch
                }
                commitSave(onDone)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun confirmRename(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                showRenameConfirmation.value = false
                commitSave(onDone)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    private suspend fun commitSave(onDone: () -> Unit) {
        val name = if (settingsRepo.stream.first().capitalizeExerciseName) {
            exerciseName.value.titleCase()
        } else {
            exerciseName.value
        }
        repo.upsert(
            Exercise(
                name = name,
                tags = selectedTags.value,
                countType = countTypeFlow.value,
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
            try {
                if (exerciseId != null) {
                    val exercise = repo.get(exerciseId)
                    exercise?.let {
                        originalName = it.name
                        exerciseName.value = it.name
                        isBodyweightFlow.value = it.isBodyweight
                        countTypeFlow.value = it.countType
                        selectedTags.value = it.tags
                    }
                } else {
                    if (routeData.name != null) exerciseName.value = routeData.name
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
data class AddEditExerciseUiState(
    val isError: Boolean,
    val isBodyweight: Boolean,
    val countType: CountType = CountType.REPS,
    val selectedTags: List<Tag> = emptyList(),
    val exerciseName: String = "",
    val showRenameConfirmation: Boolean = false,
)

data class TagSelectorState(
    val parents: List<Tag> = emptyList(),
    val children: List<Tag> = emptyList(),
)
