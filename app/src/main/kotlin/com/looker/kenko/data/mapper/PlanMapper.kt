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

import com.looker.kenko.data.local.model.PlanDayEntity
import com.looker.kenko.data.local.model.PlanEntity
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.MuscleGroups
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanStat
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

fun PlanEntity.toExternal(isActive: Boolean, stat: PlanStat) = Plan(
    id = id,
    name = name,
    description = description,
    difficulty = difficulty,
    focus = focus,
    equipment = equipment,
    time = time,
    stat = stat,
    isActive = isActive,
    dayTitles = dayTitles,
)

fun Plan.toEntity(): PlanEntity = PlanEntity(
    id = id ?: 0,
    name = name,
    description = description,
    difficulty = difficulty,
    focus = focus,
    equipment = equipment,
    time = time,
    dayTitles = dayTitles,
)

fun PlanItem.toEntity() = PlanDayEntity(
    id = id ?: 0,
    planId = planId,
    exerciseId = requireNotNull(exercise.id) { "Exercise id cannot be null" },
    dayOfWeek = dayOfWeek.isoDayNumber,
)

inline fun PlanDayEntity.toExternal(block: (exerciseId: Int) -> Exercise?) = PlanItem(
    planId = planId,
    dayOfWeek = DayOfWeek(dayOfWeek),
    exercise = block(exerciseId) ?: DefaultExercise,
    id = id,
)

val DefaultExercise = Exercise(
    name = "Exercise Deleted",
    target = MuscleGroups.Core,
)
