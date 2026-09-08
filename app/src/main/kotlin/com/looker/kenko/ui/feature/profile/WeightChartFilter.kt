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
    /** 显示区间标签；null 表示当前无任何可展示的数据（无体重记录 / 所选计划无激活区间）。 */
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
 * @param planDateRanges 计划 id -> (激活起始日, 激活结束日)；激活中的计划结束日已解析为今天
 * @param selectedPlanId 选中计划（null = 全部）
 * @param selectedMonth 当前选中月份 (year, month)；仅未选计划时生效（null = 取范围内最近月份）
 *
 * 规则：
 * - 未选计划：月份范围 = 全部体重记录的首末月份，按月翻页筛选
 * - 选了计划：展示该计划整个激活区间内的全部体重记录（如激活 08-01、现 09-17 → 08-01~09-17），
 *   不按月翻页（canGoPrev/canGoNext 恒 false）
 * - 所选计划无激活区间时返回空视图（空态）
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

    if (selectedPlanId != null) {
        val (start, end) = planRange!!
        val visible = weights
            .filter { it.date >= start && it.date <= end }
            .sortedBy { it.date }
        return WeightChartView(
            visibleWeights = visible,
            monthLabel = periodLabel(start, end),
            currentMonth = end.year to end.monthNumber,
            canGoPrev = false,
            canGoNext = false,
        )
    }

    // 未选计划：全部体重记录首末月之间翻页
    val (firstY, firstM) = run {
        val first = weights.minOf { it.date }
        first.year to first.monthNumber
    }
    val (lastY, lastM) = run {
        val last = weights.maxOf { it.date }
        last.year to last.monthNumber
    }

    // 当前月份（默认范围内最近月份；越界时回退）
    val current = selectedMonth
        ?.takeIf { (y, m) -> compareMonth(y, m, firstY, firstM) >= 0 && compareMonth(y, m, lastY, lastM) <= 0 }
        ?: (lastY to lastM)

    // 显示区间 = 所选月份整月
    val startDate = LocalDate(current.first, current.second, 1)
    val endDate = endOfMonth(current.first, current.second)

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

/** 区间标签：同年 "2026-08 ~ 2026-09"，跨年含年份；单月则仅显示该月。 */
private fun periodLabel(start: LocalDate, end: LocalDate): String {
    fun fmt(d: LocalDate) = "%04d-%02d".format(d.year, d.monthNumber)
    return if (start.year == end.year && start.monthNumber == end.monthNumber) {
        fmt(start)
    } else {
        "${fmt(start)} ~ ${fmt(end)}"
    }
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
