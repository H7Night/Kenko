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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.PlanDao
import com.looker.kenko.data.local.dao.PlanHistoryDao
import com.looker.kenko.data.local.model.PlanEntity
import com.looker.kenko.data.local.model.PlanHistoryEntity
import com.looker.kenko.data.mapper.toEntity
import com.looker.kenko.data.mapper.toExternal
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Labels
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanCycle
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanStat
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.utils.toLocalEpochDays
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalPlanRepo @Inject constructor(
    private val dao: PlanDao,
    private val exerciseDao: ExerciseDao,
    private val historyDao: PlanHistoryDao,
    private val sessionRepo: SessionRepo,
) : PlanRepo {

    private val mutex = Mutex()

    override val plans: Flow<List<Plan>> =
        combine(dao.plansFlow(), historyDao.currentIdFlow()) { plans, current ->
            plans.map {
                it.toExternal(isActive = it.id == current, stat = stats(it.id))
            }
        }

    override val current: Flow<Plan?> =
        historyDao.currentIdFlow().map { current ->
            if (current != null) {
                dao.getPlanById(current)?.toExternal(true, stats(current))
            } else {
                null
            }
        }

    override val planItems: Flow<List<PlanItem>> =
        dao.currentPlanItemsFlow().map { planDays ->
            planDays.map { planDay ->
                planDay.toExternal { exerciseId ->
                    exerciseDao.get(exerciseId)?.toExternal()
                }
            }
        }

    override fun planItems(day: Int): Flow<List<PlanItem>> =
        dao.currentPlanItemsByDayFlow(day).map { planDays ->
            planDays.map { planDay ->
                planDay.toExternal { exerciseId ->
                    exerciseDao.get(exerciseId)?.toExternal()
                }
            }
        }

    override suspend fun plan(id: Int): Plan? {
        val isCurrent = current.first()?.id
        return dao.getPlanById(id)?.toExternal(isCurrent == id, stats(id))
    }

    override suspend fun planNameExists(name: String): Boolean =
        dao.exists(name)

    override fun planItemsByPlan(id: Int): Flow<List<PlanItem>> =
        dao.planItemsByPlanIdFlow(id).map {
            it.map { planDay ->
                planDay.toExternal { exerciseId ->
                    exerciseDao.get(exerciseId)?.toExternal()
                }
            }
        }

    override fun planItems(id: Int, day: Int): Flow<List<PlanItem>> =
        dao.planItemsByPlanIdAndDayFlow(id, day).map {
            it.map { planDay ->
                planDay.toExternal { exerciseId ->
                    exerciseDao.get(exerciseId)?.toExternal()
                }
            }
        }

    override fun activeExercises(day: Int): Flow<List<Exercise>> =
        dao.currentPlanItemsByDayFlow(day).map {
            it.mapNotNull { planDay ->
                exerciseDao.get(planDay.exerciseId)?.toExternal()
            }
        }

    override suspend fun getPlanItems(id: Int): List<PlanItem> =
        dao.getPlanItemsByPlanId(id).map {
            it.toExternal { exerciseId ->
                exerciseDao.get(exerciseId)?.toExternal()
            }
        }

    override suspend fun getPlanItems(id: Int, day: Int): List<PlanItem> =
        dao.getPlanItemsByPlanIdAndDay(id, day).map {
            it.toExternal { exerciseId ->
                exerciseDao.get(exerciseId)?.toExternal()
            }
        }

    private suspend fun stats(id: Int): PlanStat =
        PlanStat(dao.getExerciseCountByPlanId(id), dao.getWorkDaysByPlanId(id))

    override suspend fun createPlan(
        name: String,
        description: String?,
        difficulty: Labels.Difficulty?,
        focus: Labels.Focus?,
        equipment: Labels.Equipment?,
        time: Labels.Time?,
    ): Int = dao.upsertPlan(
        PlanEntity(
            name = name,
            description = description,
            difficulty = difficulty,
            focus = focus,
            equipment = equipment,
            time = time,
        ),
    ).toInt()

    override suspend fun updatePlan(plan: Plan) {
        // 修改计划前先把当前(修改前)训练日名称回填为历史 session 快照
        plan.id?.let { sessionRepo.snapshotPlanDayTitles(it) }
        dao.upsertPlan(plan.toEntity())
    }

    override suspend fun setCurrent(id: Int) = mutex.withLock {
        val current = historyDao.getCurrent()
        if (current != null) {
            historyDao.upsert(current.copy(end = today().toLocalEpochDays()))
        }
        historyDao.upsert(PlanHistoryEntity(planId = id, start = today().toLocalEpochDays()))
    }

    override suspend fun deletePlan(id: Int) {
        dao.deletePlan(id)
    }

    override suspend fun addItem(planItem: PlanItem) {
        sessionRepo.snapshotPlanDayTitles(planItem.planId)
        val items = dao.getPlanItemsByPlanIdAndDay(planItem.planId, planItem.dayIndex)
        val nextOrder = (items.maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertPlanItem(planItem.toEntity().copy(sortOrder = nextOrder))
    }

    override suspend fun removeItem(id: Long) {
        dao.getPlanIdByItemId(id)?.let { sessionRepo.snapshotPlanDayTitles(it) }
        dao.deleteItem(id)
    }

    override suspend fun updateOrder(planId: Int, day: Int, exercises: List<Exercise>) {
        sessionRepo.snapshotPlanDayTitles(planId)
        val items = getPlanItems(planId, day)
        if (items.size != exercises.size) return

        // Update sortOrder in-place: existing row IDs stay the same,
        // so Room Flow won't trigger a full sync in PlanEdit UI
        val exerciseOrder = exercises.withIndex().associate { it.value.id to it.index }
        items.forEach { item ->
            val newOrder = exerciseOrder[item.exercise.id] ?: return
            dao.updateItemSortOrder(requireNotNull(item.id), newOrder)
        }
    }

    override suspend fun updateDayIndex(planId: Int, dayIndex: Int) {
        val plan = dao.getPlanById(planId) ?: return
        dao.updateCurrentDayIndex(planId, dayIndex.coerceIn(1, plan.dayCount))
    }

    override suspend fun advanceDay(planId: Int, actualDayIndex: Int) {
        val plan = dao.getPlanById(planId) ?: return
        dao.updateCurrentDayIndex(planId, PlanCycle.nextDayIndex(actualDayIndex, plan.dayCount))
    }

    override suspend fun addDay(planId: Int) {
        dao.incrementDayCount(planId)
    }

    override suspend fun deleteDay(planId: Int, dayIndex: Int) {
        val plan = dao.getPlanById(planId) ?: return
        if (dayIndex !in 1..plan.dayCount) return
        // 搬移训练日序号前回填快照,避免历史记录训练日名称随序号搬移而变化
        sessionRepo.snapshotPlanDayTitles(planId)
        // dayTitles 的 key 同步搬移:删除目标天,后续天前移,范围外保留
        val shifted = plan.toExternal(isActive = false, stat = PlanStat(0, 0)).titlesMap.mapNotNull { (day, title) ->
            val newDay = when {
                day == dayIndex -> return@mapNotNull null
                day > dayIndex -> day - 1
                else -> day
            }
            newDay to title
        }.toMap()
        val newDayTitles = if (shifted.isEmpty()) null else Json.encodeToString(shifted)
        dao.deleteDayWithTitles(planId, dayIndex, newDayTitles)
    }

    override suspend fun moveDay(planId: Int, from: Int, to: Int) {
        val plan = dao.getPlanById(planId) ?: return
        // 搬移训练日序号前回填快照,避免历史记录训练日名称随序号搬移而变化
        sessionRepo.snapshotPlanDayTitles(planId)
        // dayTitles 的 key 同步搬移:范围外条目保留,只移动 from→to 与区间内条目
        val shifted = plan.toExternal(isActive = false, stat = PlanStat(0, 0)).titlesMap.map { (day, title) ->
            val newDay = when {
                day == from -> to
                from < to && day in (from + 1)..to -> day - 1
                from > to && day in to until from -> day + 1
                else -> day
            }
            newDay to title
        }.toMap()
        val newDayTitles = if (shifted.isEmpty()) null else Json.encodeToString(shifted)
        dao.moveDayWithTitles(planId, from, to, newDayTitles)
    }
}
