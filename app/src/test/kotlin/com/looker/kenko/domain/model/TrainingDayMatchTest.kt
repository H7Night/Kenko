package com.looker.kenko.domain.model

import kotlin.collections.Set
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TrainingDayMatchTest {

    private val planDays = mapOf(
        1 to setOf("Bench Press", "Rows", "Press"),
        2 to setOf("Squat", "Deadlift"),
        3 to setOf("Curls", "Plank"),
    )

    @Test
    fun exactMatchReturnsThatDay() {
        assertEquals(1, TrainingDayMatch.matchDayIndex(setOf("Bench Press", "Rows", "Press"), planDays))
    }

    @Test
    fun subsetMatchReturnsSmallestCoveringDay() {
        // 只练了 Squat(第 2 天动作的子集)
        assertEquals(2, TrainingDayMatch.matchDayIndex(setOf("Squat"), planDays))
    }

    @Test
    fun subsetWithMultipleCoveringDaysPicksFewestExercises() {
        val days = mapOf(
            1 to setOf("A", "B", "C"),
            2 to setOf("A", "B"),
            3 to setOf("A"),
        )
        // session 只练了 "A":三个天都覆盖,取动作数最少的第 3 天
        assertEquals(3, TrainingDayMatch.matchDayIndex(setOf("A"), days))
    }

    @Test
    fun identicalDaysPickSmallestDayIndex() {
        val days = mapOf(
            2 to setOf("A", "B"),
            5 to setOf("A", "B"),
        )
        assertEquals(2, TrainingDayMatch.matchDayIndex(setOf("A", "B"), days))
    }

    @Test
    fun noMatchReturnsNull() {
        assertNull(TrainingDayMatch.matchDayIndex(setOf("Unknown Exercise"), planDays))
        assertNull(TrainingDayMatch.matchDayIndex(emptySet(), planDays))
    }
}
