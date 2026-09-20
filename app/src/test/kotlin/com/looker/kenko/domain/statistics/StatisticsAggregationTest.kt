package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatisticsAggregationTest {

    private val allTags = listOf(
        Tag(id = 1, name = "胸"),
        Tag(id = 3, name = "腿"),
        Tag(id = 7, name = CARDIO_PART),
        Tag(id = 9, name = "中胸", parentId = 1),
        Tag(id = 15, name = "股四头肌", parentId = 3),
        Tag(id = 28, name = "跑步", parentId = 7),
    )
    private val exercises = listOf(
        Exercise(name = "Bench", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS),
        Exercise(name = "Squat", tags = listOf(Tag(id = 15, name = "股四头肌", parentId = 3)), countType = CountType.REPS),
        Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES),
    )

    @Test
    fun `counts strength parts and cardio minutes`() {
        val today = LocalDate(2026, 9, 18) // Friday
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench", "Treadmill"), setCount = 2),
            SessionSummary(date = LocalDate(2026, 9, 17), planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        val cardio = mapOf(today to 30, LocalDate(2026, 9, 17) to 10)
        val plan = Plan(name = "PPL", description = null, difficulty = null, focus = null, equipment = null, time = null, isActive = true, dayCount = 4, id = 1)

        val state = aggregateStatistics(summaries, cardio, exercises, allTags, plan, today)

        assertEquals(1, state.weeklyCounts["胸"])
        assertEquals(1, state.weeklyCounts["腿"])
        assertEquals(40, state.cardioWeekly)
        assertEquals(2, state.actualDays)
        assertEquals(4, state.plannedDays)
        assertEquals(true, state.heatmapData.weeks.isNotEmpty())
    }

    @Test
    fun `no plan yields zero planned days`() {
        val today = LocalDate(2026, 9, 18)
        val state = aggregateStatistics(emptyList(), emptyMap(), exercises, allTags, null, today)
        assertEquals(0, state.plannedDays)
        assertEquals(12, state.weeklyTrend.size)
    }
}
