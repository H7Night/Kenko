package com.looker.kenko.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlanCycleTest {

    @Test
    fun advancesToNextDay() {
        assertEquals(2, PlanCycle.nextDayIndex(1, 7))
    }

    @Test
    fun wrapsAroundAtCycleEnd() {
        assertEquals(1, PlanCycle.nextDayIndex(7, 7))
    }

    @Test
    fun supportsCustomCycleLength() {
        assertEquals(2, PlanCycle.nextDayIndex(1, 3))
        assertEquals(1, PlanCycle.nextDayIndex(3, 3))
    }

    @Test
    fun advancesFromRestDaySelection() {
        // 休息日当天选了 Day2 训练 → 推进到 Day3
        assertEquals(3, PlanCycle.nextDayIndex(2, 8))
    }
}
