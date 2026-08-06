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

package com.looker.kenko.data.repository

import com.looker.kenko.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepo {

    val stream: Flow<List<Tag>>

    val streamParents: Flow<List<Tag>>

    fun streamChildren(parentId: Int): Flow<List<Tag>>

    suspend fun get(id: Int): Tag?

    suspend fun upsert(tag: Tag)

    suspend fun delete(tag: Tag)

    suspend fun deleteById(id: Int)

    suspend fun exerciseCount(tagId: Int): Int

    suspend fun getTagsForExercise(exerciseId: Int): List<Tag>

    fun streamTagsForExercise(exerciseId: Int): Flow<List<Tag>>
}
