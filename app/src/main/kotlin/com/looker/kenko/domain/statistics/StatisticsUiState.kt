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

package com.looker.kenko.domain.statistics

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class StatisticsUiState(
    val sessionDates: Set<LocalDate> = emptySet(),
    val countByDate: Map<LocalDate, Int> = emptyMap(),
    val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val heatmapData: HeatmapData = buildHeatmapData90d(
        emptySet(),
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    ),
    val bodyParts: List<String> = emptyList(),
    val weeklyCounts: Map<String, Int> = emptyMap(),
    val monthlyCounts: Map<String, Int> = emptyMap(),
    val planCounts: Map<String, Int> = emptyMap(),
    val cardioWeekly: Int = 0,
    val cardioMonthly: Int = 0,
    val cardioPlan: Int = 0,
    val cardioMinutesWeekly: Int = 0,
    val cardioMinutesMonthly: Int = 0,
    val cardioMinutesPlan: Int = 0,
    val weeklyTrend: List<Int> = emptyList(),
    val actualDays: Int = 0,
    val plannedDays: Int = 0,
)
