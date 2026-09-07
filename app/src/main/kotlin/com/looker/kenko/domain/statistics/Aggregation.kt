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

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun aggregateByBodyPart(
    summaries: List<SessionSummary>,
    predicate: (LocalDate) -> Boolean,
    tagDict: Map<String, Pair<String, CountType>>,
): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (s in summaries) {
        if (!predicate(s.date)) continue
        val parents = s.exerciseNames.mapNotNull { tagDict[it]?.first }.toSet()
        for (p in parents) if (p != "有氧") counts[p] = (counts[p] ?: 0) + 1
    }
    return counts
}

fun aggregateCardioMinutes(
    sessions: List<Session>,
    predicate: (LocalDate) -> Boolean,
): Int {
    var sum = 0
    for (sess in sessions) if (predicate(sess.date)) {
        for (set in sess.sets) if (set.exercise.countType == CountType.MINUTES) sum += set.repsOrDuration
    }
    return sum
}

data class HeatmapData(
    val weeks: List<HeatmapWeek>,
)

data class HeatmapWeek(
    val days: List<LocalDate?>,
)

fun buildHeatmapData90d(
    dates: Set<LocalDate>,
    today: LocalDate,
): HeatmapData {
    val dow = today.dayOfWeek.isoDayNumber
    val endSunday = today.plus(7 - dow, DateTimeUnit.DAY)
    val startMonday = endSunday.minus(13 * 7 - 1, DateTimeUnit.DAY)
    val weeks = mutableListOf<HeatmapWeek>()
    for (w in 0 until 13) {
        val weekStart = startMonday.plus(w * 7, DateTimeUnit.DAY)
        val days = (0 until 7).map { d ->
            weekStart.plus(d, DateTimeUnit.DAY)
        }
        weeks.add(HeatmapWeek(days))
    }
    return HeatmapData(weeks = weeks)
}
