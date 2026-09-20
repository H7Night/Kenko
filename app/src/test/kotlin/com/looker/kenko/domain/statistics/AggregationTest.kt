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
import org.junit.jupiter.api.Assertions.assertTrue
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
        val treadmill = Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES)
        val bench = Exercise(name = "Bench Press", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS)
        val allTags = listOf(
            Tag(id = 1, name = "胸"),
            Tag(id = 7, name = CARDIO_PART),
            Tag(id = 9, name = "中胸", parentId = 1),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val tagDict = buildTagDict(listOf(treadmill, bench), allTags)
        val sessions = listOf(
            Session(date = LocalDate(2026, 9, 1), planId = 1, sets = listOf(Set(repsOrDuration = 30, weight = 0f, exercise = treadmill))),
            Session(date = LocalDate(2026, 9, 2), planId = 1, sets = listOf(Set(repsOrDuration = 20, weight = 0f, exercise = treadmill), Set(repsOrDuration = 10, weight = 50f, exercise = bench))),
        )
        val result = aggregateCardioMinutes(sessions, { true }, tagDict)
        assertEquals(50, result)
    }

    @Test
    fun `cardio minutes predicate filters`() {
        val treadmill = Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES)
        val allTags = listOf(
            Tag(id = 7, name = CARDIO_PART),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val tagDict = buildTagDict(listOf(treadmill), allTags)
        val sessions = listOf(
            Session(date = LocalDate(2026, 9, 1), planId = 1, sets = listOf(Set(repsOrDuration = 30, weight = 0f, exercise = treadmill))),
            Session(date = LocalDate(2026, 9, 10), planId = 1, sets = listOf(Set(repsOrDuration = 20, weight = 0f, exercise = treadmill))),
        )
        val result = aggregateCardioMinutes(sessions, { it == LocalDate(2026, 9, 1) }, tagDict)
        assertEquals(30, result)
    }

    @Test
    fun `cardio minutes empty returns zero`() {
        assertEquals(0, aggregateCardioMinutes(emptyList(), { true }, emptyMap()))
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

    @Test
    fun `buildHeatmapData spans full range Monday to Sunday aligned`() {
        val data = buildHeatmapData(LocalDate(2026, 1, 15), LocalDate(2026, 9, 20))
        assertEquals(1, data.weeks.first().days.first()!!.dayOfWeek.isoDayNumber)
        assertEquals(7, data.weeks.last().days.last()!!.dayOfWeek.isoDayNumber)
        assertTrue(data.weeks.last().days.last()!! >= LocalDate(2026, 9, 20))
        assertTrue(data.weeks.first().days.first()!! <= LocalDate(2026, 1, 15))
    }

    // ── 回归：TagMapper.toExternal 从不填充 parentName（TagEntity 无此列），
    // 任何依赖 tag.parentName 的解析都会把全部动作误归「有氧」→ 力量统计恒为空。
    // 以下 fixture 镜像生产形态：parentName=null，仅 parentId 可用。

    @Test
    fun `buildTagDict resolves parent via parentId when parentName is null`() {
        val exercises = listOf(
            Exercise(name = "腿屈伸", tags = listOf(Tag(id = 15, name = "股四头肌", parentId = 3)), countType = CountType.REPS),
            Exercise(name = "坐姿划船", tags = listOf(Tag(id = 13, name = "竖脊肌", parentId = 2)), countType = CountType.REPS),
        )
        val allTags = listOf(
            Tag(id = 2, name = "背"),
            Tag(id = 3, name = "腿"),
            Tag(id = 13, name = "竖脊肌", parentId = 2),
            Tag(id = 15, name = "股四头肌", parentId = 3),
        )
        val dict = buildTagDict(exercises, allTags)
        assertEquals("腿", dict["腿屈伸"]?.first)
        assertEquals("背", dict["坐姿划船"]?.first)
    }

    @Test
    fun `buildTagDict top-level tag uses own name`() {
        val exercises = listOf(
            Exercise(name = "俯卧撑", tags = listOf(Tag(id = 1, name = "胸", parentId = null)), countType = CountType.REPS),
        )
        val dict = buildTagDict(exercises, listOf(Tag(id = 1, name = "胸")))
        assertEquals("胸", dict["俯卧撑"]?.first)
    }

    @Test
    fun `buildTagDict without tags yields null (unknown, not cardio)`() {
        val exercises = listOf(Exercise(name = "未知动作", tags = emptyList(), countType = CountType.REPS))
        val dict = buildTagDict(exercises, emptyList())
        assertEquals(null, dict["未知动作"]?.first)
    }

    @Test
    fun `buildTagDict cardio only when parentName is cardio`() {
        val exercises = listOf(
            Exercise(name = "有氧快走", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES),
            Exercise(name = "无标签力量", tags = emptyList(), countType = CountType.REPS),
        )
        val allTags = listOf(
            Tag(id = 7, name = CARDIO_PART),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val dict = buildTagDict(exercises, allTags)
        assertEquals(CARDIO_PART, dict["有氧快走"]?.first)
        assertEquals(null, dict["无标签力量"]?.first)
    }

    @Test
    fun `regression - production-shaped tags yield non-empty strength counts`() {
        // 旧实现（parentName ?: "有氧"）在此 fixture 下返回 {}，即用户报告的「统计页无记录」
        val exercises = listOf(
            Exercise(name = "腿屈伸", tags = listOf(Tag(id = 15, name = "股四头肌", parentId = 3)), countType = CountType.REPS),
            Exercise(name = "坐姿划船", tags = listOf(Tag(id = 13, name = "竖脊肌", parentId = 2)), countType = CountType.REPS),
            Exercise(name = "有氧快走", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES),
        )
        val allTags = listOf(
            Tag(id = 2, name = "背"),
            Tag(id = 3, name = "腿"),
            Tag(id = 7, name = "有氧"),
            Tag(id = 13, name = "竖脊肌", parentId = 2),
            Tag(id = 15, name = "股四头肌", parentId = 3),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val dict = buildTagDict(exercises, allTags)
        val summaries = listOf(
            SessionSummary(date = LocalDate(2026, 9, 4), planId = 3, exerciseNames = listOf("腿屈伸", "坐姿划船", "有氧快走"), setCount = 10),
            SessionSummary(date = LocalDate(2026, 9, 5), planId = 3, exerciseNames = listOf("坐姿划船"), setCount = 6),
        )
        val result = aggregateByBodyPart(summaries, { true }, dict)
        assertEquals(1, result["腿"])
        assertEquals(2, result["背"])
        assertEquals(null, result["有氧"])
    }
}
