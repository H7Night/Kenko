package com.looker.kenko.data.mapper

import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.local.model.SetEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetMapperN1Test {

    @Test
    fun `loads exercises once for many sets`() = runTest {
        var loadCalls = 0
        val exercises = listOf(
            ExerciseEntity(name = "Bench", id = 1),
            ExerciseEntity(name = "Squat", id = 2),
        )
        val sets = listOf(
            SetEntity(repsOrDuration = 10, weight = 40f, order = 0, sessionId = 1, exerciseId = 1, id = 11),
            SetEntity(repsOrDuration = 8, weight = 60f, order = 1, sessionId = 1, exerciseId = 1, id = 12),
            SetEntity(repsOrDuration = 5, weight = 80f, order = 2, sessionId = 1, exerciseId = 2, id = 13),
        )

        val result = mapSetEntities(sets) { ids ->
            loadCalls++
            exercises.filter { it.id in ids }
        }

        assertEquals(1, loadCalls)               // 一次批量加载，而非每 set 一次
        assertEquals(3, result.size)
        assertEquals("Bench", result[0].exercise.name)
        assertEquals("Squat", result[2].exercise.name)
    }

    @Test
    fun `empty input performs no load`() = runTest {
        var loadCalls = 0
        val result = mapSetEntities(emptyList()) { loadCalls++; emptyList() }
        assertEquals(0, loadCalls)
        assertEquals(0, result.size)
    }
}
