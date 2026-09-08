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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeightChartFilterTest {

    private fun w(date: LocalDate, value: Float) = Weight(date, value)

    @Test
    fun `empty weights yields empty view`() {
        val view = computeWeightChartView(emptyList(), emptyMap(), null, null)
        assertTrue(view.visibleWeights.isEmpty())
        assertEquals(null, view.monthLabel)
        assertFalse(view.canGoPrev)
        assertFalse(view.canGoNext)
    }

    @Test
    fun `no plan - defaults to latest record month and filters by month`() {
        val weights = listOf(
            w(LocalDate(2026, 1, 5), 70f),
            w(LocalDate(2026, 1, 20), 69.5f),
            w(LocalDate(2026, 2, 3), 69f),
        )
        val view = computeWeightChartView(weights, emptyMap(), null, null)
        assertEquals("2026-02", view.monthLabel)
        assertEquals(listOf(69f), view.visibleWeights.map { it.value })
        assertTrue(view.canGoPrev)
        assertFalse(view.canGoNext)
    }

    @Test
    fun `no plan - explicit month filters within that month`() {
        val weights = listOf(
            w(LocalDate(2026, 1, 5), 70f),
            w(LocalDate(2026, 2, 3), 69f),
        )
        val view = computeWeightChartView(weights, emptyMap(), null, (2026 to 1))
        assertEquals("2026-01", view.monthLabel)
        assertEquals(listOf(70f), view.visibleWeights.map { it.value })
        assertFalse(view.canGoPrev)
        assertTrue(view.canGoNext)
    }

    @Test
    fun `plan - shows all weights within activation window, no month paging`() {
        val weights = listOf(
            w(LocalDate(2026, 7, 20), 71f),   // 激活前 → 排除
            w(LocalDate(2026, 8, 1), 70f),
            w(LocalDate(2026, 8, 20), 69.5f),
            w(LocalDate(2026, 9, 10), 69f),   // 今天 09-17 之内
            w(LocalDate(2026, 9, 20), 68f),   // 今天之后 → 排除
        )
        val ranges = mapOf(1 to (LocalDate(2026, 8, 1) to LocalDate(2026, 9, 17)))
        val view = computeWeightChartView(weights, ranges, 1, null)
        assertEquals("2026-08 ~ 2026-09", view.monthLabel)
        assertEquals(listOf(70f, 69.5f, 69f), view.visibleWeights.map { it.value })
        assertFalse(view.canGoPrev)
        assertFalse(view.canGoNext)
    }

    @Test
    fun `plan - activation window within single month labels that month`() {
        val weights = listOf(
            w(LocalDate(2026, 8, 2), 70f),
            w(LocalDate(2026, 8, 15), 69f),
            w(LocalDate(2026, 9, 1), 68f), // 停用后 → 排除
        )
        val ranges = mapOf(1 to (LocalDate(2026, 8, 1) to LocalDate(2026, 8, 31)))
        val view = computeWeightChartView(weights, ranges, 1, null)
        assertEquals("2026-08", view.monthLabel)
        assertEquals(listOf(70f, 69f), view.visibleWeights.map { it.value })
    }

    @Test
    fun `plan - selectedMonth is ignored when a plan is selected`() {
        val weights = listOf(
            w(LocalDate(2026, 8, 2), 70f),
            w(LocalDate(2026, 9, 5), 69f),
        )
        val ranges = mapOf(1 to (LocalDate(2026, 8, 1) to LocalDate(2026, 9, 17)))
        val view = computeWeightChartView(weights, ranges, 1, (2026 to 8))
        // 不再按月过滤，仍展示整个激活期
        assertEquals(listOf(70f, 69f), view.visibleWeights.map { it.value })
    }

    @Test
    fun `plan without activation range yields empty view`() {
        val weights = listOf(w(LocalDate(2026, 1, 5), 70f))
        val view = computeWeightChartView(weights, emptyMap(), 1, null)
        assertTrue(view.visibleWeights.isEmpty())
        assertEquals(null, view.monthLabel)
        assertFalse(view.canGoPrev)
        assertFalse(view.canGoNext)
    }

    @Test
    fun `out of range month falls back to range last month`() {
        val weights = listOf(w(LocalDate(2026, 2, 10), 70f))
        val view = computeWeightChartView(weights, emptyMap(), null, (2026 to 5))
        assertEquals("2026-02", view.monthLabel)
    }
}
