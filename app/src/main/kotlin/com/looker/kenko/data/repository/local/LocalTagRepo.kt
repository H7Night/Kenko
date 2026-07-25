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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.TagDao
import com.looker.kenko.data.mapper.toEntity
import com.looker.kenko.data.mapper.toExternal
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalTagRepo @Inject constructor(
    private val dao: TagDao,
) : TagRepo {

    override val stream: Flow<List<Tag>> =
        dao.stream().map { list -> list.map { it.toExternal() } }

    override val streamParents: Flow<List<Tag>> =
        dao.streamParents().map { list -> list.map { it.toExternal() } }

    override fun streamChildren(parentId: Int): Flow<List<Tag>> =
        dao.streamChildren(parentId).map { list -> list.map { it.toExternal() } }

    override suspend fun get(id: Int): Tag? =
        dao.get(id)?.toExternal()

    override suspend fun upsert(tag: Tag) {
        dao.upsert(tag.toEntity())
    }

    override suspend fun delete(tag: Tag) {
        dao.delete(tag.toEntity())
    }

    override suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }

    override suspend fun exerciseCount(tagId: Int): Int =
        dao.exerciseCount(tagId)

    override suspend fun getTagsForExercise(exerciseId: Int): List<Tag> =
        dao.getTagsForExercise(exerciseId).map { it.toExternal() }

    override fun streamTagsForExercise(exerciseId: Int): Flow<List<Tag>> =
        dao.streamTagsForExercise(exerciseId).map { list -> list.map { it.toExternal() } }
}
