package com.looker.kenko.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanTransferCodecTest {

    @Test
    fun `round trip preserves all fields`() {
        val plans = listOf(
            PlanTransfer(
                name = "Push Pull Leg",
                description = "desc",
                difficulty = "ADAPTABLE",
                focus = "STRENGTH",
                equipment = "FULL_GYM",
                time = "NORMAL",
                dayCount = 6,
                dayTitles = mapOf(1 to "推", 2 to "拉"),
                days = listOf(
                    PlanDayTransfer(
                        dayIndex = 1,
                        exercises = listOf(
                            PlanExerciseTransfer(
                                name = "杠铃卧推",
                                target = "胸",
                                tags = listOf("平板"),
                                countType = "REPS",
                                isBodyweight = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val jsonString = PlanTransferCodec.encode(plans)
        assertEquals(plans, PlanTransferCodec.decode(jsonString))
    }

    @Test
    fun `version 1 is accepted`() {
        val jsonString = Json.encodeToString(PlanTransferFile(version = 1, plans = emptyList()))
        assertEquals(emptyList(), PlanTransferCodec.decode(jsonString))
    }

    @Test
    fun `unsupported version is rejected`() {
        val jsonString = Json.encodeToString(PlanTransferFile(version = 2, plans = emptyList()))
        assertFailsWith<IllegalArgumentException> { PlanTransferCodec.decode(jsonString) }
    }

    @Test
    fun `invalid json is rejected`() {
        assertFailsWith<Exception> { PlanTransferCodec.decode("not json") }
    }

    @Test
    fun `exercise transfer maps to exercise with target as parentName`() {
        val exercise = PlanExerciseTransfer(
            name = "卧推", target = "胸", tags = listOf("平板"), countType = "MINUTES", isBodyweight = true,
        ).toExercise()
        assertEquals("卧推", exercise.name)
        assertEquals(CountType.MINUTES, exercise.countType)
        assertEquals(true, exercise.isBodyweight)
        assertEquals("胸", exercise.tags.first().parentName)
        assertEquals("平板", exercise.tags.first().name)
    }
}
