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

package com.looker.kenko.data.mapper

import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Set

fun SetEntity.toExternal(exercise: Exercise): Set = Set(
    repsOrDuration = repsOrDuration,
    weight = weight,
    exercise = exercise,
    order = order,
    id = id,
)

fun Set.toEntity(sessionId: Int, order: Int): SetEntity = SetEntity(
    id = id ?: 0,
    repsOrDuration = repsOrDuration,
    weight = weight,
    order = order,
    sessionId = sessionId,
    exerciseId = requireNotNull(exercise.id),
)

/**
 * 批量把 SetEntity 映射为领域 Set：一次性加载所有 exercise，避免逐组查询（N+1）。
 * loader 注入以便单测。
 */
suspend fun mapSetEntities(
    entities: List<SetEntity>,
    loadExercises: suspend (ids: List<Int>) -> List<ExerciseEntity>,
): List<Set> {
    if (entities.isEmpty()) return emptyList()
    val exerciseById = loadExercises(entities.map { it.exerciseId }.distinct())
        .associate { it.id to it.toExternal() }
    return entities.mapNotNull { entity ->
        exerciseById[entity.exerciseId]?.let { entity.toExternal(it) }
    }
}
