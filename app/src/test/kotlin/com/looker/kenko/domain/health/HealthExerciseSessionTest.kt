package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthExerciseSessionTest {

    private fun session(
        startedAt: Long?,
        endedAt: Long?,
        id: Int? = 7,
        dayTitle: String? = null,
    ) = Session(
        date = LocalDate(2026, 10, 13),
        sets = emptyList(),
        planId = null,
        dayTitleOverride = dayTitle,
        startedAt = startedAt,
        endedAt = endedAt,
        id = id,
    )

    @Test
    fun `valid session maps with stable id and interval`() {
        val result = session(1_000L, 5_000L).toHealthExerciseSession(listOf("Bench Press"))
        assertEquals("kenko:session:7", result?.clientRecordId)
        assertEquals(1_000L, result?.startMillis)
        assertEquals(5_000L, result?.endMillis)
        assertEquals("Bench Press", result?.title)
    }

    @Test
    fun `day title overrides exercise names`() {
        val result = session(1L, 2L, dayTitle = "Push Day").toHealthExerciseSession(listOf("Bench"))
        assertEquals("Push Day", result?.title)
    }

    @Test
    fun `missing timestamps are skipped`() {
        assertNull(session(null, 5L).toHealthExerciseSession(emptyList()))
        assertNull(session(1L, null).toHealthExerciseSession(emptyList()))
    }

    @Test
    fun `non positive interval is skipped`() {
        assertNull(session(5L, 5L).toHealthExerciseSession(emptyList()))
        assertNull(session(5L, 4L).toHealthExerciseSession(emptyList()))
    }

    @Test
    fun `missing id is skipped`() {
        assertNull(session(1L, 2L, id = null).toHealthExerciseSession(emptyList()))
    }
}
