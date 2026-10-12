/*
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

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarMonthTest {

    @Test
    fun `daysInMonth handles leap years`() {
        assertEquals(31, daysInMonth(2026, 10))
        assertEquals(28, daysInMonth(2026, 2))
        assertEquals(29, daysInMonth(2024, 2))
    }

    @Test
    fun `month grid aligns Monday-first with null padding`() {
        // 2026-10-01 is a Thursday -> 3 leading blanks, 31 days -> 5 full weeks
        val grid = buildMonthGrid(2026, 10)
        assertEquals(5, grid.size)
        assertTrue(grid.all { it.size == 7 })
        assertEquals(null, grid[0][0])
        assertEquals(null, grid[0][2])
        assertEquals(LocalDate(2026, 10, 1), grid[0][3])
        assertEquals(31, grid.flatten().count { it != null })
        assertEquals(LocalDate(2026, 10, 31), grid.flatten().filterNotNull().last())
    }

    @Test
    fun `month stepping wraps across the year`() {
        assertEquals(2025 to 12, previousMonth(2026, 1))
        assertEquals(2026 to 9, previousMonth(2026, 10))
        assertEquals(2027 to 1, nextMonth(2026, 12))
        assertEquals(2026 to 11, nextMonth(2026, 10))
    }
}
