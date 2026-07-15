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

package com.looker.kenko.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val sessions: List<ExportSession>? = null,
    val plans: List<ExportPlan>? = null,
    val exercises: List<ExportExercise>? = null,
    val weights: List<ExportWeight>? = null,
)

@Serializable
data class ExportSession(
    val date: LocalDate,
    val sets: List<ExportSet>,
    val planId: Int?,
)

@Serializable
data class ExportSet(
    val repsOrDuration: Int,
    val weight: Float,
    val exerciseName: String,
    val exerciseTarget: String,
)

@Serializable
data class ExportPlan(
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val dayTitles: String?,
)

@Serializable
data class ExportExercise(
    val name: String,
    val target: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
)

@Serializable
data class ExportWeight(
    val date: LocalDate,
    val value: Float,
)
