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

package com.h7night.tore.data.mapper

import com.h7night.tore.data.local.model.ExerciseEntity
import com.h7night.tore.domain.model.Exercise

fun ExerciseEntity.toExternal(
    tags: List<com.h7night.tore.domain.model.Tag> = emptyList(),
): Exercise = Exercise(
    id = id,
    name = name,
    tags = tags,
    countType = countType,
    reference = reference,
    isBodyweight = isBodyweight,
    equipment = equipment,
    primaryTagId = primaryTagId,
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id ?: 0,
    name = name,
    countType = countType,
    reference = reference,
    isBodyweight = isBodyweight,
    equipment = equipment,
    primaryTagId = primaryTagId,
)
