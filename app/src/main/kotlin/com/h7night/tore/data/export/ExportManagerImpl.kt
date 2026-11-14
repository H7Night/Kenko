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

package com.h7night.tore.data.export

import android.content.Context
import android.net.Uri
import com.h7night.tore.data.backup.BackupResult
import com.h7night.tore.data.repository.ExerciseRepo
import com.h7night.tore.data.repository.PlanRepo
import com.h7night.tore.data.repository.SessionRepo
import com.h7night.tore.data.repository.TagRepo
import com.h7night.tore.data.repository.WeightRepo
import com.h7night.tore.domain.model.Exercise
import com.h7night.tore.domain.model.ExportData
import com.h7night.tore.domain.model.ExportExercise
import com.h7night.tore.domain.model.ExportPlan
import com.h7night.tore.domain.model.ExportSession
import com.h7night.tore.domain.model.ExportSet
import com.h7night.tore.domain.model.ExportWeight
import com.h7night.tore.domain.model.Plan
import com.h7night.tore.domain.model.Session
import com.h7night.tore.domain.model.Weight
import com.h7night.tore.domain.statistics.bodyPartByName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import javax.inject.Inject

class ExportManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepo: SessionRepo,
    private val planRepo: PlanRepo,
    private val exerciseRepo: ExerciseRepo,
    private val weightRepo: WeightRepo,
    private val tagRepo: TagRepo,
) : ExportManager {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun export(options: ExportOptions, destinationUri: Uri): BackupResult {
        return try {
            val bodyPartByName = bodyPartByName(
                exerciseRepo.stream.first(),
                tagRepo.stream.first(),
            )
            val data = ExportData(
                sessions = if (options.exportSessions) {
                    val sessions = sessionRepo.stream.first().map { it.toExport(bodyPartByName) }
                    if (options.startDate != null && options.endDate != null) {
                        sessions.filter { it.date >= options.startDate && it.date <= options.endDate }
                    } else {
                        sessions
                    }
                } else null,
                plans = if (options.exportPlans) {
                    planRepo.plans.first().map { it.toExport() }
                } else null,
                exercises = if (options.exportExercises) {
                    exerciseRepo.stream.first().map { it.toExport(bodyPartByName) }
                } else null,
                weights = if (options.exportWeights) {
                    weightRepo.weights.first().map { it.toExport() }
                } else null,
            )
            val jsonString = json.encodeToString(data)
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            } ?: throw IllegalStateException("Cannot open output stream for $destinationUri")
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error("Export failed: ${e.message}", e)
        }
    }
}

private fun Session.toExport(bodyPartByName: Map<String, String?>): ExportSession = ExportSession(
    date = date,
    sets = sets.map { set ->
        ExportSet(
            repsOrDuration = set.repsOrDuration,
            weight = set.weight,
            exerciseName = set.exercise.name,
            exerciseTarget = bodyPartByName[set.exercise.name] ?: "",
        )
    },
    planId = planId,
    durationSeconds = durationSeconds,
)

private fun Plan.toExport(): ExportPlan = ExportPlan(
    name = name,
    description = description,
    isActive = isActive,
    dayTitles = dayTitles,
)

private fun Exercise.toExport(bodyPartByName: Map<String, String?>): ExportExercise = ExportExercise(
    name = name,
    target = bodyPartByName[name] ?: "",
    tags = tags.map { it.name },
    countType = countType.name,
    isBodyweight = isBodyweight,
)

private fun Weight.toExport(): ExportWeight = ExportWeight(
    date = date,
    value = value,
)
