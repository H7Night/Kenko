package com.looker.kenko.data.plan

import android.content.Context
import android.net.Uri
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanDayTransfer
import com.looker.kenko.domain.model.PlanExerciseTransfer
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanTransfer
import com.looker.kenko.domain.model.PlanTransferCodec
import com.looker.kenko.domain.model.titlesMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import javax.inject.Inject

class PlanTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planRepo: PlanRepo,
    private val applier: PlanImportApplier,
) {

    suspend fun exportPlans(planIds: List<Int>, destinationUri: Uri) {
        val transfers = planIds.mapNotNull { id ->
            val plan = planRepo.plan(id) ?: return@mapNotNull null
            plan.toTransfer(planRepo.getPlanItems(id))
        }
        val jsonString = PlanTransferCodec.encode(transfers)
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(jsonString)
            }
        } ?: throw IllegalStateException("Cannot open output stream for $destinationUri")
    }

    suspend fun readPlans(uri: Uri): List<PlanTransfer> {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Cannot open input stream for $uri")
        return PlanTransferCodec.decode(jsonString)
    }

    suspend fun importPlans(uri: Uri): ImportSummary = applier.apply(readPlans(uri))
}

private fun Plan.toTransfer(items: List<PlanItem>): PlanTransfer = PlanTransfer(
    name = name,
    description = description,
    difficulty = difficulty?.name,
    focus = focus?.name,
    equipment = equipment?.name,
    time = time?.name,
    dayCount = dayCount,
    dayTitles = titlesMap,
    days = items.groupBy { it.dayIndex }.map { (day, dayItems) ->
        PlanDayTransfer(
            dayIndex = day,
            exercises = dayItems.map { it.exercise.toTransfer() },
        )
    }.sortedBy { it.dayIndex },
)

private fun Exercise.toTransfer(): PlanExerciseTransfer = PlanExerciseTransfer(
    name = name,
    target = tags.firstOrNull()?.parentName,
    tags = tags.map { it.name },
    countType = countType.name,
    isBodyweight = isBodyweight,
)
