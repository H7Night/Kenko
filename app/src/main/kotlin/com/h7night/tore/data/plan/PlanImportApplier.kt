package com.h7night.tore.data.plan

import com.h7night.tore.data.repository.ExerciseRepo
import com.h7night.tore.data.repository.PlanRepo
import com.h7night.tore.domain.model.Labels
import com.h7night.tore.domain.model.Plan
import com.h7night.tore.domain.model.PlanItem
import com.h7night.tore.domain.model.PlanTransfer
import com.h7night.tore.domain.model.toExercise
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ImportSummary(val imported: Int, val failed: Int)

class PlanImportApplier @Inject constructor(
    private val planRepo: PlanRepo,
    private val exerciseRepo: ExerciseRepo,
) {

    suspend fun apply(plans: List<PlanTransfer>): ImportSummary {
        var imported = 0
        var failed = 0
        plans.forEach { plan ->
            try {
                applyOne(plan)
                imported++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed++
            }
        }
        return ImportSummary(imported, failed)
    }

    private suspend fun applyOne(plan: PlanTransfer) {
        val difficulty = plan.difficulty?.let { runCatching { Labels.Difficulty.valueOf(it) }.getOrNull() }
        val focus = plan.focus?.let { runCatching { Labels.Focus.valueOf(it) }.getOrNull() }
        val equipment = plan.equipment?.let { runCatching { Labels.Equipment.valueOf(it) }.getOrNull() }
        val time = plan.time?.let { runCatching { Labels.Time.valueOf(it) }.getOrNull() }

        val planId = planRepo.createPlan(
            name = plan.name,
            description = plan.description,
            difficulty = difficulty,
            focus = focus,
            equipment = equipment,
            time = time,
        )
        // 导入一律非激活、从第 1 天开始；允许重名（不做 planNameExists 校验）
        planRepo.updatePlan(
            Plan(
                name = plan.name,
                description = plan.description,
                difficulty = difficulty,
                focus = focus,
                equipment = equipment,
                time = time,
                isActive = false,
                dayTitles = plan.dayTitles.takeIf { it.isNotEmpty() }
                    ?.let { Json.encodeToString(it) },
                dayCount = plan.dayCount,
                currentDayIndex = 1,
                id = planId,
            ),
        )
        plan.days.forEach { day ->
            day.exercises.forEach { exerciseTransfer ->
                val exercise = exerciseRepo.getOrCreate(exerciseTransfer.toExercise())
                planRepo.addItem(
                    PlanItem(
                        dayIndex = day.dayIndex,
                        exercise = exercise,
                        planId = planId,
                    ),
                )
            }
        }
    }
}
