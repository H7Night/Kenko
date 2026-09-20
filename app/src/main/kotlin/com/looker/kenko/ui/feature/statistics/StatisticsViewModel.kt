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

package com.looker.kenko.ui.feature.statistics

import androidx.lifecycle.ViewModel
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.statistics.StatisticsUiState
import com.looker.kenko.domain.statistics.aggregateByBodyPart
import com.looker.kenko.domain.statistics.aggregateCardioMinutes
import com.looker.kenko.domain.statistics.buildBodyParts
import com.looker.kenko.domain.statistics.buildHeatmapData90d
import com.looker.kenko.domain.statistics.buildTagDict
import com.looker.kenko.domain.statistics.buildWeeklyTrend
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    sessionRepo: SessionRepo,
    exerciseRepo: ExerciseRepo,
    planRepo: PlanRepo,
    tagRepo: TagRepo,
) : ViewModel() {

    val state = combine(
        sessionRepo.streamSummaries,
        sessionRepo.stream,
        exerciseRepo.stream,
        planRepo.current,
        tagRepo.stream,
    ) { summaries, sessions, exercises, plan, allTags ->
        val tagDict = buildTagDict(exercises, allTags)
        val bodyParts = buildBodyParts(allTags)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val dates = summaries.map { it.date }.toSet()
        val countByDate = summaries.groupBy { it.date }.mapValues { it.value.size }

        val weeklyCounts = aggregateByBodyPart(summaries, { it >= monday }, tagDict)
        val monthlyCounts = aggregateByBodyPart(
            summaries,
            { it.year == today.year && it.month == today.month },
            tagDict,
        )
        val planCounts = if (plan?.id == null) emptyMap()
        else aggregateByBodyPart(summaries.filter { it.planId == plan.id }, { true }, tagDict)

        val cardioWeekly = aggregateCardioMinutes(sessions, { it >= monday }, tagDict)
        val cardioMonthly = aggregateCardioMinutes(
            sessions,
            { it.year == today.year && it.month == today.month },
            tagDict,
        )
        val cardioPlan = if (plan?.id == null) 0
        else aggregateCardioMinutes(sessions.filter { it.planId == plan.id }, { true }, tagDict)

        val heatmapData = buildHeatmapData90d(dates, today)
        val weeklyTrend = buildWeeklyTrend(summaries, today)
        val actualDays = summaries.filter { it.date.year == today.year && it.date.month == today.month }.map { it.date }.toSet().size
        val plannedDays = plan?.dayCount ?: 0

        StatisticsUiState(
            sessionDates = dates,
            countByDate = countByDate,
            today = today,
            heatmapData = heatmapData,
            bodyParts = bodyParts,
            weeklyCounts = weeklyCounts,
            monthlyCounts = monthlyCounts,
            planCounts = planCounts,
            cardioWeekly = cardioWeekly,
            cardioMonthly = cardioMonthly,
            cardioPlan = cardioPlan,
            cardioMinutesWeekly = cardioWeekly,
            cardioMinutesMonthly = cardioMonthly,
            cardioMinutesPlan = cardioPlan,
            weeklyTrend = weeklyTrend,
            actualDays = actualDays,
            plannedDays = plannedDays,
        )
    }.asStateFlow(StatisticsUiState())
}
