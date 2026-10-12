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

package com.looker.kenko.ui.feature.session.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 某年月的天数。 */
fun daysInMonth(year: Int, month: Int): Int =
    LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

/**
 * 构建某年月的周历网格（周一为一周起点）。
 * 返回 `weeks × 7` 的日期矩阵；月初/月末的空位为 `null`。
 */
fun buildMonthGrid(year: Int, month: Int): List<List<LocalDate?>> {
    val first = LocalDate(year, month, 1)
    // ISO: Monday = 1 ... Sunday = 7
    val leading = first.dayOfWeek.isoDayNumber - 1
    val cells = ArrayList<LocalDate?>(leading + daysInMonth(year, month) + 6)
    repeat(leading) { cells.add(null) }
    for (day in 1..daysInMonth(year, month)) {
        cells.add(LocalDate(year, month, day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells.chunked(7)
}

/** 上一个月（跨年处理）。 */
fun previousMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) (year - 1) to 12 else year to (month - 1)

/** 下一个月（跨年处理）。 */
fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) (year + 1) to 1 else year to (month + 1)
