package com.looker.kenko.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutRetentionTest {

    @Test
    fun `session with sets is kept`() {
        assertTrue(shouldKeepSession(setCount = 1, elapsedSeconds = 0))
    }

    @Test
    fun `long set-less session is kept`() {
        assertTrue(shouldKeepSession(setCount = 0, elapsedSeconds = 60))
        assertTrue(shouldKeepSession(setCount = 0, elapsedSeconds = 61))
    }

    @Test
    fun `short set-less session is discarded`() {
        assertFalse(shouldKeepSession(setCount = 0, elapsedSeconds = 0))
        assertFalse(shouldKeepSession(setCount = 0, elapsedSeconds = 59))
    }
}
