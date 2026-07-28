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

package com.looker.kenko.ui.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TagManagementViewModel @Inject constructor(
    private val tagRepo: TagRepo,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    private val tags = refreshTrigger.flatMapLatest { tagRepo.stream }

    private val parents = tags.map { list -> list.filter { it.parentId == null } }

    private val children = tags.map { list -> list.filter { it.parentId != null } }

    val state = combine(parents, children) { parentsList, childrenList ->
        TagManagementUiState(
            parents = parentsList,
            children = childrenList,
        )
    }.asStateFlow(TagManagementUiState())

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    fun refresh() {
        refreshTrigger.value++
    }

    fun addParent(name: String) {
        viewModelScope.launch {
            try {
                val maxOrder = state.value.parents.maxOfOrNull { it.sortOrder } ?: 0
                tagRepo.upsert(Tag(name = name, sortOrder = maxOrder + 1))
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun addChild(name: String, parentId: Int) {
        viewModelScope.launch {
            try {
                val siblings = state.value.children.filter { it.parentId == parentId }
                val maxOrder = siblings.maxOfOrNull { it.sortOrder } ?: 0
                tagRepo.upsert(Tag(name = name, parentId = parentId, sortOrder = maxOrder + 1))
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepo.upsert(tag)
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagRepo.delete(tag)
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteTagById(id: Int) {
        viewModelScope.launch {
            try {
                tagRepo.deleteById(id)
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun exerciseCount(tagId: Int, callback: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                callback(tagRepo.exerciseCount(tagId))
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateSortOrder(tagId: Int, newOrder: Int) {
        viewModelScope.launch {
            try {
                val tag = tagRepo.get(tagId) ?: return@launch
                tagRepo.upsert(tag.copy(sortOrder = newOrder))
                refresh()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

data class TagManagementUiState(
    val parents: List<Tag> = emptyList(),
    val children: List<Tag> = emptyList(),
)
