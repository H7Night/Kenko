package com.looker.kenko.data.plan

import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Labels
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanTransfer
import com.looker.kenko.domain.model.PlanDayTransfer
import com.looker.kenko.domain.model.PlanExerciseTransfer
import com.looker.kenko.domain.model.titlesMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanImportApplierTest {

    private open class FakePlanRepo : PlanRepo {
        val created = mutableListOf<Plan>()
        val addedItems = mutableListOf<PlanItem>()
        var nextId = 1
        override val plans: Flow<List<Plan>> = emptyFlow()
        override val current: Flow<Plan?> = emptyFlow()
        override val planItems: Flow<List<PlanItem>> = emptyFlow()
        override fun planItems(day: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun planItemsByPlan(id: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun planItems(id: Int, day: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun activeExercises(day: Int): Flow<List<Exercise>> = emptyFlow()
        override suspend fun plan(id: Int): Plan? = created.find { it.id == id }
        override suspend fun planNameExists(name: String): Boolean = false
        override suspend fun getPlanItems(id: Int): List<PlanItem> = emptyList()
        override suspend fun getPlanItems(id: Int, day: Int): List<PlanItem> = emptyList()
        override suspend fun createPlan(
            name: String, description: String?, difficulty: Labels.Difficulty?,
            focus: Labels.Focus?, equipment: Labels.Equipment?, time: Labels.Time?,
        ): Int {
            val plan = Plan(
                name = name, description = description, difficulty = difficulty,
                focus = focus, equipment = equipment, time = time, isActive = false,
                id = nextId++,
            )
            created += plan
            return plan.id!!
        }
        override suspend fun updatePlan(plan: Plan) {
            val idx = created.indexOfFirst { it.id == plan.id }
            if (idx >= 0) created[idx] = plan
        }
        override suspend fun setCurrent(id: Int) = error("must not be called")
        override suspend fun deletePlan(id: Int) = Unit
        override suspend fun addItem(planItem: PlanItem) {
            addedItems += planItem
        }
        override suspend fun removeItem(id: Long) = Unit
        override suspend fun updateOrder(planId: Int, day: Int, exercises: List<Exercise>) = Unit
        override suspend fun updateDayIndex(planId: Int, dayIndex: Int) = Unit
        override suspend fun advanceDay(planId: Int, actualDayIndex: Int) = Unit
        override suspend fun addDay(planId: Int) = Unit
        override suspend fun deleteDay(planId: Int, dayIndex: Int) = Unit
        override suspend fun moveDay(planId: Int, from: Int, to: Int) = Unit
    }

    private class FakeExerciseRepo : ExerciseRepo {
        val known = mutableListOf<Exercise>(Exercise(name = "已有动作", id = 1))
        val createdNames = mutableListOf<String>()
        override val stream: Flow<List<Exercise>> = emptyFlow()
        override val numberOfExercise: Flow<Int> = emptyFlow()
        override suspend fun get(id: Int): Exercise? = known.find { it.id == id }
        override suspend fun upsert(exercise: Exercise) = Unit
        override suspend fun remove(id: Int) = Unit
        override suspend fun isExerciseAvailable(name: String): Boolean = known.any { it.name == name }
        override suspend fun hasHistory(id: Int): Boolean = false
        override suspend fun getOrCreate(exercise: Exercise): Exercise {
            known.find { it.name == exercise.name }?.let { return it }
            val created = exercise.copy(id = known.size + 1)
            known += created
            createdNames += created.name
            return created
        }
    }

    @Test
    fun `imports plans with dayCount dayTitles and order`() = runTest {
        val planRepo = FakePlanRepo()
        val exerciseRepo = FakeExerciseRepo()
        val applier = PlanImportApplier(planRepo, exerciseRepo)
        val summary = applier.apply(
            listOf(
                PlanTransfer(
                    name = "New Plan",
                    dayCount = 5,
                    dayTitles = mapOf(1 to "胸"),
                    days = listOf(
                        PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "新动作A"))),
                        PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "新动作B"))),
                    ),
                ),
            ),
        )
        assertEquals(ImportSummary(1, 0), summary)
        val plan = planRepo.created.single()
        assertEquals(5, plan.dayCount)
        assertEquals("胸", plan.titlesMap[1])
        assertEquals(1, plan.currentDayIndex)
        assertEquals(2, planRepo.addedItems.size)
        assertEquals(listOf("新动作A", "新动作B"), planRepo.addedItems.map { it.exercise.name })
        assertEquals(listOf(1, 1), planRepo.addedItems.map { it.dayIndex })
        assertEquals(listOf("新动作A", "新动作B"), exerciseRepo.createdNames)
    }

    @Test
    fun `reuses existing exercises by name`() = runTest {
        val planRepo = FakePlanRepo()
        val exerciseRepo = FakeExerciseRepo()
        PlanImportApplier(planRepo, exerciseRepo).apply(
            listOf(
                PlanTransfer(
                    name = "P",
                    days = listOf(PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "已有动作")))),
                ),
            ),
        )
        assertTrue(exerciseRepo.createdNames.isEmpty())
        assertEquals("已有动作", planRepo.addedItems.single().exercise.name)
    }

    @Test
    fun `allows duplicate plan names`() = runTest {
        val planRepo = FakePlanRepo()
        val applier = PlanImportApplier(planRepo, FakeExerciseRepo())
        applier.apply(
            listOf(
                PlanTransfer(name = "Same"),
                PlanTransfer(name = "Same"),
            ),
        )
        assertEquals(listOf("Same", "Same"), planRepo.created.map { it.name })
    }

    @Test
    fun `one failing plan does not stop the rest`() = runTest {
        val planRepo = object : FakePlanRepo() {
            override suspend fun createPlan(
                name: String, description: String?, difficulty: Labels.Difficulty?,
                focus: Labels.Focus?, equipment: Labels.Equipment?, time: Labels.Time?,
            ): Int {
                if (name == "Bad") error("boom")
                return super.createPlan(name, description, difficulty, focus, equipment, time)
            }
        }
        val summary = PlanImportApplier(planRepo, FakeExerciseRepo()).apply(
            listOf(PlanTransfer(name = "Bad"), PlanTransfer(name = "Good")),
        )
        assertEquals(ImportSummary(1, 1), summary)
    }
}
