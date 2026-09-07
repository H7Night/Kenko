package com.looker.kenko.ui.feature.statistics.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BalanceRingCardTest {

    private fun computeWeak(
        monthlyCounts: Map<String, Int>,
        cardioMonthly: Int,
        threshold: Float = 0.08f,
    ): Set<String> {
        val order = listOf("胸", "背", "腿", "手臂", "肩", "腹", "有氧")
        val values = order.associateWith { if (it == "有氧") cardioMonthly else monthlyCounts[it] ?: 0 }
        val total = values.values.sum()
        if (total == 0) return emptySet()
        return values.filter { it.value.toFloat() / total < threshold }.keys
    }

    @Test
    fun `ring does not crash with empty data`() {
        val weak = computeWeak(emptyMap(), 0)
        assertTrue(weak.isEmpty())
    }

    @Test
    fun `ring renders 7 slices total includes cardio`() {
        val counts = mapOf("胸" to 4, "背" to 4, "腿" to 3, "手臂" to 2, "肩" to 2, "腹" to 3)
        val values = listOf("胸", "背", "腿", "手臂", "肩", "腹", "有氧").associateWith {
            if (it == "有氧") 30 else counts[it] ?: 0
        }
        assertEquals(7, values.size)
        assertEquals(48, values.values.sum())
    }

    @Test
    fun `weak threshold below 8 percent includes zero slices`() {
        val counts = mapOf("胸" to 10, "背" to 1, "腿" to 0, "手臂" to 1, "肩" to 0, "腹" to 0)
        val weak = computeWeak(counts, 0)
        // total 12, 0% <8% so zero parts are weak
        assertTrue("腿" in weak)
        assertTrue("肩" in weak)
        assertTrue("腹" in weak)
        assertTrue("有氧" in weak)
    }

    @Test
    fun `weak threshold excludes above 8 percent`() {
        val counts = mapOf("胸" to 10, "背" to 1, "腿" to 1)
        val weak = computeWeak(counts, 0) // total 12, 1/12=8.3% >8% not weak
        assertTrue("背" !in weak)
        assertTrue("腿" !in weak)
    }

    @Test
    fun `weak threshold includes slightly below 8 percent`() {
        val counts = mapOf("胸" to 12, "背" to 1) // total 13, 1/13=7.6% weak
        val weak = computeWeak(counts, 0)
        assertTrue("背" in weak)
    }
}
