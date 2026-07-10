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

package com.looker.kenko.data.mapper

import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.RepsInReserve
import com.looker.kenko.domain.model.Set

fun SetEntity.toExternal(exercise: Exercise): Set = Set(
    repsOrDuration = repsOrDuration,
    weight = weight,
    type = type,
    exercise = exercise,
    rir = RepsInReserve(rir),
    id = id,
)

fun Set.toEntity(sessionId: Int, order: Int): SetEntity = SetEntity(
    id = id ?: 0,
    repsOrDuration = repsOrDuration,
    weight = weight,
    type = type,
    order = order,
    sessionId = sessionId,
    exerciseId = requireNotNull(exercise.id),
    rir = rir.value,
)
