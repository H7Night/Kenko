/*
 * Copyright (C) 2025. LooKeR & Contributors
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

package com.looker.kenko

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.RepsInReserve
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.domain.model.today
import com.looker.kenko.domain.model.withDayTitle
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionRepo: SessionRepo

    @Inject
    lateinit var planRepo: PlanRepo

    @Inject
    lateinit var exerciseRepo: ExerciseRepo

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun checkPlanDeletion() = runTest {
        val planId = planRepo.createPlan("test")
        val exercises = exerciseRepo.stream.first().take(10)
        exercises.forEach {
            planRepo.addItem(
                PlanItem(
                    dayIndex = Random.nextInt(1, 5),
                    exercise = it,
                    planId = planId,
                ),
            )
        }
        val planItems = planRepo.getPlanItems(planId)
        assertEquals(10, planItems.size)
        planRepo.setCurrent(planId)
        val createdSessionId = sessionRepo.getSessionIdOrCreate(today())
        val stream = sessionRepo.streamByDate(today())
        assertNotNull(stream.first())
        val sessionId = stream.first()!!.id!!
        val sets = (1..24).map {
            Set(
                repsOrDuration = 12,
                weight = 12F,
                exercise = exercises.random(),
                rir = RepsInReserve(2)
            )
        }
        sets.forEach { sessionRepo.addSet(createdSessionId, it) }
        assertEquals(24, sessionRepo.getSets(sessionId).size)
        val set = sessionRepo.getSets(sessionId).first()
        sessionRepo.removeSet(set.id!!)
        assertEquals(23, sessionRepo.getSets(sessionId).size)
        val remainingSets = sessionRepo.getSets(sessionId) // 23 个(已删 1 个)
        val randomPerformedExercise = remainingSets.random().exercise
        val setsForRandomExercise = remainingSets.filter { it.exercise.id == randomPerformedExercise.id }
        exerciseRepo.remove(randomPerformedExercise.id!!)
        assertEquals(remainingSets.size - setsForRandomExercise.size, sessionRepo.getSets(sessionId).size)
        planRepo.deletePlan(planId)
        assertEquals(remainingSets.size - setsForRandomExercise.size, sessionRepo.getSets(sessionId).size)
        assertFails {
            planRepo.addItem(
                PlanItem(
                    dayIndex = Random.nextInt(1, 5),
                    exercise = randomPerformedExercise,
                    planId = planId,
                ),
            )
        }
        assertEquals(null, stream.first()!!.planId)
        val planItemsAfter = planRepo.getPlanItems(planId)
        assertEquals(0, planItemsAfter.size)
    }

    @Test
    fun planCycleAdvanceAndOverride() = runTest {
        val planId = planRepo.createPlan("cycle")
        assertEquals(7, planRepo.plan(planId)?.dayCount)
        assertEquals(1, planRepo.plan(planId)?.currentDayIndex)
        planRepo.advanceDay(planId, 7)
        assertEquals(1, planRepo.plan(planId)?.currentDayIndex) // 回绕
        planRepo.updateDayIndex(planId, 3)
        assertEquals(3, planRepo.plan(planId)?.currentDayIndex)
        planRepo.addDay(planId)
        assertEquals(8, planRepo.plan(planId)?.dayCount)
        planRepo.deleteDay(planId, 2)
        assertEquals(7, planRepo.plan(planId)?.dayCount)

        // —— moveDay:dayIndex 分布与 titlesMap 联动(修正后计划 Step 4)——
        // 给 day 1/2/3/6 各放一个动作,并命名 day 1/6
        val exercisePool = exerciseRepo.stream.first().take(4)
        listOf(1, 2, 3, 6).forEachIndexed { i, day ->
            planRepo.addItem(PlanItem(dayIndex = day, exercise = exercisePool[i], planId = planId))
        }
        planRepo.updatePlan(
            planRepo.plan(planId)!!
                .withDayTitle(1, "Push")
                .withDayTitle(6, "Legs"),
        )
        // 向后移动:6 → 2,区间 [2,5] 顺移一格,1 不变
        planRepo.moveDay(planId, from = 6, to = 2)
        assertEquals(listOf(1, 2, 3, 4), planRepo.getPlanItems(planId).map { it.dayIndex }.sorted())
        val titlesAfterMove = planRepo.plan(planId)?.titlesMap
        assertEquals("Push", titlesAfterMove?.get(1)) // 1 在移动区间外,保留
        assertEquals("Legs", titlesAfterMove?.get(2)) // 6 → 2
        assertEquals(2, titlesAfterMove?.size) // 范围外标题不丢失
        // 向前移动:1 → 4,区间 (1,4] 左移一格
        planRepo.moveDay(planId, from = 1, to = 4)
        assertEquals(listOf(1, 2, 3, 4), planRepo.getPlanItems(planId).map { it.dayIndex }.sorted())
        val titlesAfterForward = planRepo.plan(planId)?.titlesMap
        assertEquals("Legs", titlesAfterForward?.get(1)) // 2 → 1
        assertEquals("Push", titlesAfterForward?.get(4)) // 1 → 4
        assertEquals(2, titlesAfterForward?.size)
    }
}
