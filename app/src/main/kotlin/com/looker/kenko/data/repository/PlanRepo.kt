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

import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Labels.Difficulty
import com.looker.kenko.domain.model.Labels.Equipment
import com.looker.kenko.domain.model.Labels.Focus
import com.looker.kenko.domain.model.Labels.Time
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
import kotlinx.coroutines.flow.Flow

interface PlanRepo {

    val plans: Flow<List<Plan>>

    val current: Flow<Plan?>

    val planItems: Flow<List<PlanItem>>

    fun planItems(day: Int): Flow<List<PlanItem>>

    fun planItemsByPlan(id: Int): Flow<List<PlanItem>>

    fun planItems(id: Int, day: Int): Flow<List<PlanItem>>

    fun activeExercises(day: Int): Flow<List<Exercise>>

    suspend fun plan(id: Int): Plan?

    suspend fun planNameExists(name: String): Boolean

    suspend fun getPlanItems(id: Int): List<PlanItem>

    suspend fun getPlanItems(id: Int, day: Int): List<PlanItem>

    suspend fun createPlan(
        name: String,
        description: String? = null,
        difficulty: Difficulty? = null,
        focus: Focus? = null,
        equipment: Equipment? = null,
        time: Time? = null,
    ): Int

    suspend fun updatePlan(plan: Plan)

    suspend fun setCurrent(id: Int)

    suspend fun deletePlan(id: Int)

    suspend fun addItem(planItem: PlanItem)

    suspend fun removeItem(id: Long)

    suspend fun updateOrder(planId: Int, day: Int, exercises: List<Exercise>)

    suspend fun updateDayIndex(planId: Int, dayIndex: Int)

    suspend fun advanceDay(planId: Int, actualDayIndex: Int)

    suspend fun addDay(planId: Int)

    suspend fun deleteDay(planId: Int, dayIndex: Int)

    suspend fun moveDay(planId: Int, from: Int, to: Int)
}
