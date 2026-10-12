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

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate

/** 某动作在一次训练中的一组数据（重量 × 次数）。 */
@Immutable
data class ExerciseSetRecord(
    val date: LocalDate,
    val weight: Float,
    val reps: Int,
)

/** 单动作进步页可切换的度量指标。 */
enum class ExerciseMetric {
    /** 每次训练的最重重量（kg）。 */
    HEAVIEST_WEIGHT,

    /** 每次训练的最高估算 1RM（kg，Epley）。 */
    ESTIMATED_1RM,

    /** 每次训练的最佳组重量（按估算 1RM 选出最佳组，取其重量）。 */
    BEST_SET,

    /** 每次训练的总容量（Σ 重量 × 次数，kg）。 */
    VOLUME,
}

/** 曲线上的一个点：某训练日的指标取值。 */
@Immutable
data class ExerciseProgressPoint(
    val date: LocalDate,
    val value: Float,
)

/** 某训练日的汇总（历史列表用）。 */
@Immutable
data class ExerciseSessionEntry(
    val date: LocalDate,
    val heaviestWeight: Float,
    val bestSetWeight: Float,
    val bestSetReps: Int,
    val estimatedOneRepMax: Float,
    val volume: Float,
    val setCount: Int,
)

/**
 * 估算 1RM（Epley 公式）：`weight × (1 + reps / 30)`。
 *
 * 直观：一次能举起的最大重量。次数越多，等效 1RM 越高。
 * - 仅对力量动作（重量 > 0、次数 > 0）有意义；有氧（无重量）与自重（weight = 0）返回 0。
 * - `reps == 1` 时直接取实际重量（公式本身会算出 ×1.033 略偏高）。
 * - 次数 > 12 时误差变大，常规训练组（1–12 次）足够。
 */
fun estimatedOneRepMax(weight: Float, reps: Int): Float = when {
    weight <= 0f || reps <= 0 -> 0f
    reps == 1 -> weight
    else -> weight * (1f + reps / 30f)
}

/** 身体体重记录（用于自重动作的负荷换算）。 */
@Immutable
data class BodyWeightRecord(val date: LocalDate, val weight: Float)

/**
 * 取某日期"当时"的体重：日期 ≤ [date] 的最近一条；若没有不晚于该日的记录，则回退到最早一条；
 * 完全没有记录时返回 null。
 */
fun bodyWeightOnOrBefore(records: List<BodyWeightRecord>, date: LocalDate): Float? {
    if (records.isEmpty()) return null
    val sorted = records.sortedBy { it.date }
    return (sorted.lastOrNull { it.date <= date } ?: sorted.first()).weight
}

/**
 * 单组的有效负荷：
 * - 自重动作：当时体重 + 附加重量（自重模式记录为 0，负重模式记录为附加重量）。
 * - 非自重动作：直接取记录重量。
 */
fun effectiveLoad(
    set: ExerciseSetRecord,
    isBodyweight: Boolean,
    bodyWeights: List<BodyWeightRecord>,
): Float = if (isBodyweight) {
    (bodyWeightOnOrBefore(bodyWeights, set.date) ?: 0f) + set.weight
} else {
    set.weight
}

/**
 * 按训练日聚合记录为历史条目（按日期升序），只保留有效负荷 > 0 的组。
 *
 * 自重动作的负荷 = 当时体重 + 附加重量；非自重动作 = 记录重量。这样纯自重训练日
 * 不再显示为 0kg，而负重训练日显示"体重 + 附加重量"。有氧（无重量）恒为 0，被排除。
 */
fun buildExerciseEntries(
    records: List<ExerciseSetRecord>,
    isBodyweight: Boolean = false,
    bodyWeights: List<BodyWeightRecord> = emptyList(),
): List<ExerciseSessionEntry> =
    records
        .map { record -> record to effectiveLoad(record, isBodyweight, bodyWeights) }
        .filter { (_, load) -> load > 0f }
        .groupBy { (record, _) -> record.date }
        .toSortedMap()
        .map { (date, entries) ->
            val best = entries.maxWithOrNull(
                compareBy(
                    { estimatedOneRepMax(it.second, it.first.reps) },
                    { it.second },
                    { it.first.reps },
                ),
            )
            ExerciseSessionEntry(
                date = date,
                heaviestWeight = entries.maxOf { it.second },
                bestSetWeight = best?.second ?: 0f,
                bestSetReps = best?.first?.reps ?: 0,
                estimatedOneRepMax = best?.let { estimatedOneRepMax(it.second, it.first.reps) } ?: 0f,
                volume = entries.sumOf { (it.second * it.first.reps).toDouble() }.toFloat(),
                setCount = entries.size,
            )
        }

/** 把历史条目映射为指定指标的曲线点。 */
fun buildExerciseProgress(
    entries: List<ExerciseSessionEntry>,
    metric: ExerciseMetric,
): List<ExerciseProgressPoint> = entries.map { entry ->
    ExerciseProgressPoint(
        date = entry.date,
        value = when (metric) {
            ExerciseMetric.HEAVIEST_WEIGHT -> entry.heaviestWeight
            ExerciseMetric.ESTIMATED_1RM -> entry.estimatedOneRepMax
            ExerciseMetric.BEST_SET -> entry.bestSetWeight
            ExerciseMetric.VOLUME -> entry.volume
        },
    )
}
