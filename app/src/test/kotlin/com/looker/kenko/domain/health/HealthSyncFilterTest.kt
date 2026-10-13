package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthSyncFilterTest {

    private fun session(id: Int) = Session(
        date = LocalDate(2026, 10, 13),
        sets = emptyList(),
        planId = null,
        id = id,
    )

    @Test
    fun `null active id keeps every session`() {
        val result = listOf(session(1), session(2)).excludingActiveSession(null)
        assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun `active session is excluded from sync`() {
        val result = listOf(session(1), session(2)).excludingActiveSession(2)
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `active id not present leaves list unchanged`() {
        val result = listOf(session(1), session(2)).excludingActiveSession(9)
        assertEquals(listOf(1, 2), result.map { it.id })
    }
}
