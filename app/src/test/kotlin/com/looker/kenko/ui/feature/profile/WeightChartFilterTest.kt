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
    fun `plan - range clamped to first and last session dates`() {
        val weights = listOf(
            w(LocalDate(2026, 1, 1), 70f),
            w(LocalDate(2026, 1, 20), 69.5f),
            w(LocalDate(2026, 2, 15), 69f),
            w(LocalDate(2026, 3, 10), 68f),
        )
        val ranges = mapOf(1 to (LocalDate(2026, 1, 9) to LocalDate(2026, 3, 6)))
        val view = computeWeightChartView(weights, ranges, 1, null)
        assertEquals("2026-03", view.monthLabel)
        // 3 月 ∩ 计划区间 = 3-01~3-06 → 无记录
        assertEquals(emptyList(), view.visibleWeights.map { it.value })
        assertTrue(view.canGoPrev)
        assertFalse(view.canGoNext)
    }

    @Test
    fun `plan - january month only shows records after plan start`() {
        val weights = listOf(
            w(LocalDate(2026, 1, 5), 70f),
            w(LocalDate(2026, 1, 15), 69.5f),
        )
        val ranges = mapOf(1 to (LocalDate(2026, 1, 9) to LocalDate(2026, 3, 6)))
        val view = computeWeightChartView(weights, ranges, 1, (2026 to 1))
        assertEquals(listOf(69.5f), view.visibleWeights.map { it.value })
    }

    @Test
    fun `plan without sessions yields empty view`() {
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

    @Test
    fun `plan - month arrows bounded by plan session months`() {
        val weights = listOf(w(LocalDate(2026, 2, 10), 70f))
        val ranges = mapOf(1 to (LocalDate(2026, 1, 9) to LocalDate(2026, 3, 6)))
        val first = computeWeightChartView(weights, ranges, 1, (2026 to 1))
        assertFalse(first.canGoPrev)
        assertTrue(first.canGoNext)
        val middle = computeWeightChartView(weights, ranges, 1, (2026 to 2))
        assertTrue(middle.canGoPrev)
        assertTrue(middle.canGoNext)
        val last = computeWeightChartView(weights, ranges, 1, (2026 to 3))
        assertTrue(last.canGoPrev)
        assertFalse(last.canGoNext)
    }
}
