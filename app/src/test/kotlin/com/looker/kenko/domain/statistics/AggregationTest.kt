package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AggregationTest {
    private val tagDict = mapOf(
        "Bench Press" to ("胸" to CountType.REPS),
        "Squat" to ("腿" to CountType.REPS),
        "Treadmill" to ("有氧" to CountType.MINUTES),
    )

    @Test
    fun `dedup per session per parent`() {
        val summaries = listOf(
            SessionSummary(date = LocalDate(2026, 9, 1), planId = 1, exerciseNames = listOf("Bench Press", "Pec Dec"), setCount = 2),
            SessionSummary(date = LocalDate(2026, 9, 2), planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        val result = aggregateByBodyPart(summaries, { true }, tagDict)
        assertEquals(1, result["胸"])
        assertEquals(1, result["腿"])
        assertEquals(null, result["有氧"])
    }

    @Test
    fun `null tag buckets to 有氧 excluded from strength map`() {
        val summaries = listOf(SessionSummary(date = LocalDate(2026, 9, 1), planId = 1, exerciseNames = listOf("Unknown"), setCount = 1))
        val result = aggregateByBodyPart(summaries, { true }, emptyMap())
        assertEquals(0, result.size) // 有氧 excluded here; cardio handled separately
    }

    @Test
    fun `predicate filters by date`() {
        val summaries = listOf(
            SessionSummary(date = LocalDate(2026, 9, 1), planId = 1, exerciseNames = listOf("Bench Press"), setCount = 1),
            SessionSummary(date = LocalDate(2026, 9, 10), planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        val result = aggregateByBodyPart(summaries, { it == LocalDate(2026, 9, 1) }, tagDict)
        assertEquals(1, result["胸"])
        assertEquals(null, result["腿"])
    }

    @Test
    fun `multiple sessions same parent increments per session`() {
        val summaries = listOf(
            SessionSummary(date = LocalDate(2026, 9, 1), planId = 1, exerciseNames = listOf("Bench Press"), setCount = 1),
            SessionSummary(date = LocalDate(2026, 9, 2), planId = 1, exerciseNames = listOf("Bench Press"), setCount = 1),
        )
        val result = aggregateByBodyPart(summaries, { true }, tagDict)
        assertEquals(2, result["胸"])
    }

    @Test
    fun `cardio minutes sums correctly`() {
        val treadmill = Exercise(name = "Treadmill", tags = listOf(Tag(name = "跑步", parentName = "有氧")), countType = CountType.MINUTES)
        val bench = Exercise(name = "Bench Press", tags = listOf(Tag(name = "中胸", parentName = "胸")), countType = CountType.REPS)
        val sessions = listOf(
            Session(date = LocalDate(2026, 9, 1), planId = 1, sets = listOf(Set(repsOrDuration = 30, weight = 0f, exercise = treadmill))),
            Session(date = LocalDate(2026, 9, 2), planId = 1, sets = listOf(Set(repsOrDuration = 20, weight = 0f, exercise = treadmill), Set(repsOrDuration = 10, weight = 50f, exercise = bench))),
        )
        val result = aggregateCardioMinutes(sessions) { true }
        assertEquals(50, result)
    }

    @Test
    fun `cardio minutes predicate filters`() {
        val treadmill = Exercise(name = "Treadmill", tags = listOf(Tag(name = "跑步", parentName = "有氧")), countType = CountType.MINUTES)
        val sessions = listOf(
            Session(date = LocalDate(2026, 9, 1), planId = 1, sets = listOf(Set(repsOrDuration = 30, weight = 0f, exercise = treadmill))),
            Session(date = LocalDate(2026, 9, 10), planId = 1, sets = listOf(Set(repsOrDuration = 20, weight = 0f, exercise = treadmill))),
        )
        val result = aggregateCardioMinutes(sessions) { it == LocalDate(2026, 9, 1) }
        assertEquals(30, result)
    }

    @Test
    fun `cardio minutes empty returns zero`() {
        assertEquals(0, aggregateCardioMinutes(emptyList()) { true })
    }

    @Test
    fun `heatmap covers 91 days Monday-aligned`() {
        val today = LocalDate(2026, 9, 7) // Monday
        val data = buildHeatmapData90d(setOf(today), today)
        assertEquals(13, data.weeks.size)
        assertEquals(91, data.weeks.sumOf { it.days.count { d -> d != null } })
    }

    @Test
    fun `heatmap start is Monday end is Sunday`() {
        val today = LocalDate(2026, 9, 9) // Wednesday
        val data = buildHeatmapData90d(emptySet(), today)
        val allDates = data.weeks.flatMap { it.days }.filterNotNull()
        assertEquals(91, allDates.size)
        // first date should be Monday
        assertEquals(1, allDates.first().dayOfWeek.isoDayNumber)
        // last date should be Sunday
        assertEquals(7, allDates.last().dayOfWeek.isoDayNumber)
    }

    @Test
    fun `heatmap contains today`() {
        val today = LocalDate(2026, 9, 7)
        val data = buildHeatmapData90d(setOf(today), today)
        val allDates = data.weeks.flatMap { it.days }.filterNotNull()
        assertEquals(true, allDates.contains(today))
    }
}
