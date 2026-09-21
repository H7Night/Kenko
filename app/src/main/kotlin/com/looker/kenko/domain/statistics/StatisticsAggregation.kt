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

import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 纯函数：把统计输入聚合成 UI 状态（无副作用，可在后台线程调用）。 */
fun aggregateStatistics(
    summaries: List<SessionSummary>,
    cardioMinutesByDate: Map<LocalDate, Int>,
    exercises: List<Exercise>,
    allTags: List<Tag>,
    plan: Plan?,
    today: LocalDate,
): StatisticsUiState {
    val tagDict = buildTagDict(exercises, allTags)
    val bodyParts = buildBodyParts(allTags)
    val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val dates = summaries.map { it.date }.toSet()
    val countByDate = summaries.groupBy { it.date }.mapValues { it.value.size }

    val weeklyCounts = aggregateByBodyPart(summaries, { it >= monday }, tagDict)
    val monthlyCounts = aggregateByBodyPart(
        summaries,
        { it.year == today.year && it.month == today.month },
        tagDict,
    )
    val planCounts = if (plan?.id == null) {
        emptyMap()
    } else {
        aggregateByBodyPart(summaries.filter { it.planId == plan.id }, { true }, tagDict)
    }

    fun cardioSum(predicate: (LocalDate) -> Boolean): Int =
        cardioMinutesByDate.entries.filter { predicate(it.key) }.sumOf { it.value }

    val cardioWeekly = cardioSum { it >= monday }
    val cardioMonthly = cardioSum { it.year == today.year && it.month == today.month }
    val planDates = if (plan?.id == null) {
        emptySet()
    } else {
        summaries.filter { it.planId == plan.id }.map { it.date }.toSet()
    }
    val cardioPlan = cardioSum { it in planDates }

    return StatisticsUiState(
        sessionDates = dates,
        countByDate = countByDate,
        today = today,
        heatmapData = buildHeatmapData90d(dates, today),
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
        weeklyTrend = buildWeeklyTrend(summaries, today),
    )
}

internal fun buildWeeklyTrend(summaries: List<SessionSummary>, today: LocalDate): List<Int> {
    val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return (0 until 12).map { idx ->
        val weekStart = monday.minus((11 - idx) * 7, DateTimeUnit.DAY)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        summaries.count { it.date >= weekStart && it.date <= weekEnd }
    }
}

/** 一级部位（parentId==null）按 sortOrder 排序；有氧恒置末位。 */
internal fun buildBodyParts(allTags: List<Tag>): List<String> =
    allTags.filter { it.parentId == null }
        .sortedBy { it.sortOrder }
        .map { it.name }
        .let { parts ->
            val cardio = parts.firstOrNull { it == CARDIO_PART }
            if (cardio == null) parts else (parts - cardio) + cardio
        }
