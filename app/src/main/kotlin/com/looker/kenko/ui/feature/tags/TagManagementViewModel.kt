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

package com.looker.kenko.ui.feature.tags

import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.ui.base.KenkoViewModel
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TagManagementViewModel @Inject constructor(
    private val tagRepo: TagRepo,
) : KenkoViewModel() {

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

    fun refresh() {
        refreshTrigger.value++
    }

    fun addParent(name: String) {
        launchCatching {
            val maxOrder = state.value.parents.maxOfOrNull { it.sortOrder } ?: 0
            tagRepo.upsert(Tag(name = name, sortOrder = maxOrder + 1))
            refresh()
        }
    }

    fun addChild(name: String, parentId: Int) {
        launchCatching {
            val siblings = state.value.children.filter { it.parentId == parentId }
            val maxOrder = siblings.maxOfOrNull { it.sortOrder } ?: 0
            tagRepo.upsert(Tag(name = name, parentId = parentId, sortOrder = maxOrder + 1))
            refresh()
        }
    }

    fun updateTag(tag: Tag) {
        launchCatching {
            tagRepo.upsert(tag)
            refresh()
        }
    }

    fun deleteTag(tag: Tag) {
        launchCatching {
            tagRepo.delete(tag)
            refresh()
        }
    }

    fun deleteTagById(id: Int) {
        launchCatching {
            tagRepo.deleteById(id)
            refresh()
        }
    }

    fun exerciseCount(tagId: Int, callback: (Int) -> Unit) {
        launchCatching {
            callback(tagRepo.exerciseCount(tagId))
        }
    }

    fun updateSortOrder(tagId: Int, newOrder: Int) {
        launchCatching {
            val tag = tagRepo.get(tagId) ?: return@launchCatching
            tagRepo.upsert(tag.copy(sortOrder = newOrder))
            refresh()
        }
    }
}

data class TagManagementUiState(
    val parents: List<Tag> = emptyList(),
    val children: List<Tag> = emptyList(),
)
