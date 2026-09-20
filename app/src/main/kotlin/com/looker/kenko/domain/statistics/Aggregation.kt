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
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

const val CARDIO_PART = "有氧"

/**
 * 动作名 → (一级部位或 null, 计量方式)。
 * 部位解析必须走 parentId → 父标签名：TagEntity 没有 parentName 列，
 * TagMapper.toExternal 从不填充 parentName（恒为 null），
 * 直接读 tag.parentName 会把所有动作误归为「有氧」并被力量统计过滤为空。
 * 一级标签（parentId==null）取自身名；无有效部位的标签解析为 null（unknown，
 * 不进入部位统计），仅 parentName == "有氧" 才算有氧。
 */
fun buildTagDict(
    exercises: List<Exercise>,
    allTags: List<Tag>,
): Map<String, Pair<String?, CountType>> {
    val tagNameById = allTags.associate { it.id to it.name }
    return exercises.associate { ex ->
        val part = ex.tags.firstOrNull()?.let { tag ->
            tag.parentId?.let { tagNameById[it] } ?: tag.name
        }
        ex.name to (part to ex.countType)
    }
}

/**
 * 动作名 → 一级部位名（导出 target 用）。解析逻辑与 [buildTagDict] 一致；
 * 无有效部位时为 null，调用方用空串兜底。
 */
fun bodyPartByName(
    exercises: List<Exercise>,
    allTags: List<Tag>,
): Map<String, String?> = buildTagDict(exercises, allTags).mapValues { it.value.first }

fun aggregateByBodyPart(
    summaries: List<SessionSummary>,
    predicate: (LocalDate) -> Boolean,
    tagDict: Map<String, Pair<String?, CountType>>,
): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (s in summaries) {
        if (!predicate(s.date)) continue
        // null（unknown）自动丢弃；仅统计非有氧的力量部位
        val parents = s.exerciseNames.mapNotNull { tagDict[it]?.first }.toSet()
        for (p in parents) if (p != CARDIO_PART) counts[p] = (counts[p] ?: 0) + 1
    }
    return counts
}

/**
 * 有氧分钟：按动作的解析部位 == [CARDIO_PART] 判定，countType 仅作时长单位
 * （repsOrDuration 即分钟数）。无标签/unknown（部位为 null）不计入有氧。
 */
fun aggregateCardioMinutes(
    sessions: List<Session>,
    predicate: (LocalDate) -> Boolean,
    tagDict: Map<String, Pair<String?, CountType>>,
): Int {
    var sum = 0
    for (sess in sessions) if (predicate(sess.date)) {
        for (set in sess.sets) {
            if (tagDict[set.exercise.name]?.first == CARDIO_PART) {
                sum += set.repsOrDuration
            }
        }
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

/**
 * 解析出「解析部位 == 有氧」的动作 id 列表（与 [buildTagDict] 同一解析规则）。
 * 供有氧分钟下推到 SQL 使用。
 */
fun cardioExerciseIds(
    exercises: List<Exercise>,
    allTags: List<Tag>,
): List<Int> {
    val tagNameById = allTags.associate { it.id to it.name }
    return exercises.mapNotNull { ex ->
        val part = ex.tags.firstOrNull()?.let { tag ->
            tag.parentId?.let { tagNameById[it] } ?: tag.name
        }
        if (part == CARDIO_PART) ex.id else null
    }
}
