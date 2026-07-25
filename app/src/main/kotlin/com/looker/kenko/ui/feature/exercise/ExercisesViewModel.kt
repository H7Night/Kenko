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
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel @Inject constructor(
    private val repo: ExerciseRepo,
    private val tagRepo: TagRepo,
    private val uriHandler: UriHandler,
    private val stringHandler: StringHandler,
) : ViewModel() {

    val snackbarState = SnackbarHostState()

    val selectedParentFilter = MutableStateFlow<Int?>(null)

    val parentTags: StateFlow<List<Tag>> = tagRepo.streamParents
        .asStateFlow(emptyList())

    val exercises: StateFlow<List<Exercise>> = combine(
        repo.stream,
        selectedParentFilter,
    ) { allExercises, parentId ->
        if (parentId == null) {
            allExercises
        } else {
            allExercises.filter { exercise ->
                exercise.tags.any { it.parentId == parentId }
            }
        }
    }.asStateFlow(emptyList())

    fun setParentFilter(parentId: Int?) {
        selectedParentFilter.value = parentId
    }

    fun removeExercise(id: Int?) {
        viewModelScope.launch {
            if (id == null) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.error_unknown))
                return@launch
            }
            repo.remove(id)
        }
    }

    fun onReferenceClick(reference: String) {
        viewModelScope.launch {
            try {
                uriHandler.openUri(reference)
            } catch (e: IllegalStateException) {
                snackbarState.showSnackbar(
                    e.message ?: stringHandler.getString(R.string.error_invalid_url)
                )
            }
        }
    }
}
