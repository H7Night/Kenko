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

package com.looker.kenko.ui.feature.profile

import com.looker.kenko.domain.model.Weight
import kotlinx.datetime.LocalDate

/** 计算后的体重图视图状态（纯函数，便于单元测试）。 */
data class WeightChartView(
    val visibleWeights: List<Weight>,
    /** yyyy-MM；null 表示当前无任何可展示的数据（无体重记录 / 所选计划无 session）。 */
    val monthLabel: String?,
    /** 当前展示月份 (year, month)；null 表示无数据。 */
    val currentMonth: Pair<Int, Int>?,
    val canGoPrev: Boolean,
    val canGoNext: Boolean,
)

/**
 * 根据筛选条件计算体重图显示区间与月份导航状态。
 *
 * @param weights 全部体重记录（升序或乱序均可，结果会排序）
 * @param planDateRanges 计划 id -> (首 session 日期, 末 session 日期)
 * @param selectedPlanId 选中计划（null = 全部）
 * @param selectedMonth 当前选中月份 (year, month)（null = 取范围内最近月份）
 *
 * 规则：
 * - 未选计划：月份范围 = 全部体重记录的首末月份
 * - 选了计划：月份范围 = 该计划首末 session 所在月份；最终区间 = 计划区间 ∩ 所选月份
 * - 所选月份超出范围时回退到范围末月
 * - 所选计划无 session 时返回空视图（空态）
 */
fun computeWeightChartView(
    weights: List<Weight>,
    planDateRanges: Map<Int, Pair<LocalDate, LocalDate>>,
    selectedPlanId: Int?,
    selectedMonth: Pair<Int, Int>?,
): WeightChartView {
    if (weights.isEmpty()) {
        return WeightChartView(emptyList(), null, null, canGoPrev = false, canGoNext = false)
    }

    val planRange: Pair<LocalDate, LocalDate>? = selectedPlanId?.let { planDateRanges[it] }
    if (selectedPlanId != null && planRange == null) {
        return WeightChartView(emptyList(), null, null, canGoPrev = false, canGoNext = false)
    }

    val (firstY, firstM) = planRange?.let { (start, _) -> start.year to start.monthNumber } ?: run {
        val first = weights.minOf { it.date }
        first.year to first.monthNumber
    }
    val (lastY, lastM) = planRange?.let { (_, end) -> end.year to end.monthNumber } ?: run {
        val last = weights.maxOf { it.date }
        last.year to last.monthNumber
    }

    // 当前月份（默认范围内最近月份；越界时回退）
    val current = selectedMonth
        ?.takeIf { (y, m) -> compareMonth(y, m, firstY, firstM) >= 0 && compareMonth(y, m, lastY, lastM) <= 0 }
        ?: (lastY to lastM)

    // 显示区间 = 计划区间 ∩ 所选月份（未选计划则为整月）
    val startDate = planRange?.let { maxOf(LocalDate(current.first, current.second, 1), it.first) }
        ?: LocalDate(current.first, current.second, 1)
    val endDate = planRange?.let { minOf(endOfMonth(current.first, current.second), it.second) }
        ?: endOfMonth(current.first, current.second)

    val visible = weights
        .filter { it.date in startDate..endDate }
        .sortedBy { it.date }

    return WeightChartView(
        visibleWeights = visible,
        monthLabel = "%04d-%02d".format(current.first, current.second),
        currentMonth = current,
        canGoPrev = compareMonth(current.first, current.second, firstY, firstM) > 0,
        canGoNext = compareMonth(current.first, current.second, lastY, lastM) < 0,
    )
}

/** 月份加减（纯整数运算，避免依赖具体日期库的月份 API）。 */
fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val total = year * 12 + (month - 1) + delta
    return (total / 12) to (total % 12 + 1)
}

private fun compareMonth(y1: Int, m1: Int, y2: Int, m2: Int): Int =
    (y1 * 12 + m1).compareTo(y2 * 12 + m2)

private fun endOfMonth(year: Int, month: Int): LocalDate {
    val days = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    }
    return LocalDate(year, month, days)
}
