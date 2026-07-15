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

package com.looker.kenko.data.export

import android.content.Context
import android.net.Uri
import com.looker.kenko.data.backup.BackupResult
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.WeightRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.ExportData
import com.looker.kenko.domain.model.ExportExercise
import com.looker.kenko.domain.model.ExportPlan
import com.looker.kenko.domain.model.ExportSession
import com.looker.kenko.domain.model.ExportSet
import com.looker.kenko.domain.model.ExportWeight
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.Weight
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
) : ExportManager {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun export(options: ExportOptions, destinationUri: Uri): BackupResult {
        return try {
            val data = ExportData(
                sessions = if (options.exportSessions) {
                    sessionRepo.stream.first().map { it.toExport() }
                } else null,
                plans = if (options.exportPlans) {
                    planRepo.plans.first().map { it.toExport() }
                } else null,
                exercises = if (options.exportExercises) {
                    exerciseRepo.stream.first().map { it.toExport() }
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

private fun Session.toExport(): ExportSession = ExportSession(
    date = date,
    sets = sets.map { set ->
        ExportSet(
            repsOrDuration = set.repsOrDuration,
            weight = set.weight,
            exerciseName = set.exercise.name,
            exerciseTarget = set.exercise.target.name,
        )
    },
    planId = planId,
)

private fun Plan.toExport(): ExportPlan = ExportPlan(
    name = name,
    description = description,
    isActive = isActive,
    dayTitles = dayTitles,
)

private fun Exercise.toExport(): ExportExercise = ExportExercise(
    name = name,
    target = target.name,
    isBodyweight = isBodyweight,
    isIsometric = isIsometric,
)

private fun Weight.toExport(): ExportWeight = ExportWeight(
    date = date,
    value = value,
)
