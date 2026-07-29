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

package com.looker.kenko.ui.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface SearchResult {
    data object Loading : SearchResult
    data object NotFound : SearchResult
    data class Success(val exercises: List<Exercise>) : SearchResult
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SelectExerciseViewModel @Inject constructor(
    private val repo: ExerciseRepo,
    private val tagRepo: TagRepo,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val selectedParentId = MutableStateFlow<Int?>(null)
    val selectedChildId = MutableStateFlow<Int?>(null)

    val parentTags: StateFlow<List<Tag>> = tagRepo.streamParents
        .asStateFlow(emptyList())

    val children: StateFlow<List<Tag>> = combine(
        tagRepo.stream,
        selectedParentId,
    ) { all, parentId ->
        if (parentId == null) emptyList()
        else all.filter { it.parentId == parentId }
    }.asStateFlow(emptyList())

    val searchResult: StateFlow<SearchResult> = combine(
        repo.stream,
        searchQuery,
        selectedChildId,
    ) { exercises, query, childId ->
        var filtered = exercises

        // Filter by tag (child/specific muscle)
        if (childId != null) {
            filtered = filtered.filter { exercise ->
                exercise.tags.any { it.id == childId }
            }
        } else if (selectedParentId.value != null) {
            // If only parent selected, show all exercises under that parent
            filtered = filtered.filter { exercise ->
                exercise.tags.any { it.parentId == selectedParentId.value }
            }
        }

        // Filter by name search
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        when {
            filtered.isEmpty() -> SearchResult.NotFound
            else -> SearchResult.Success(filtered)
        }
    }.asStateFlow(SearchResult.Loading)

    fun setSearch(value: String) {
        searchQuery.value = value
    }

    fun setParentFilter(parentId: Int?) {
        selectedParentId.value = parentId
    }

    fun setChildFilter(childId: Int?) {
        selectedChildId.value = childId
    }

    fun reset() {
        searchQuery.value = ""
        selectedParentId.value = null
        selectedChildId.value = null
    }
}
