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

import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.TagDao
import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.mapper.toEntity
import com.looker.kenko.data.mapper.toExternal
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalExerciseRepo @Inject constructor(
    private val dao: ExerciseDao,
    private val tagDao: TagDao,
    private val setsDao: com.looker.kenko.data.local.dao.SetsDao,
) : ExerciseRepo {

    override val stream: Flow<List<Exercise>> =
        dao.stream().map { entities ->
            entities.map { entity ->
                val tags = tagDao.getTagsForExercise(entity.id).map { it.toExternal() }
                entity.toExternal(tags)
            }
        }

    override val numberOfExercise: Flow<Int> = dao.number()

    override suspend fun get(id: Int): Exercise? {
        val entity = dao.get(id) ?: return null
        val tags = tagDao.getTagsForExercise(entity.id).map { it.toExternal() }
        return entity.toExternal(tags)
    }

    override suspend fun upsert(exercise: Exercise) {
        val entityId = dao.upsert(exercise.toEntity())
        val id = exercise.id ?: entityId
        if (id != 0) {
            tagDao.replaceExerciseTags(id, exercise.tags.map { it.id })
        }
    }

    override suspend fun remove(id: Int) {
        dao.delete(id)
    }

    override suspend fun isExerciseAvailable(name: String): Boolean =
        dao.exists(name)

    override suspend fun hasHistory(id: Int): Boolean =
        setsDao.hasSetsForExercise(id)
}
