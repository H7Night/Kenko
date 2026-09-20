package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardioExerciseIdsTest {

    @Test
    fun `resolves cardio by parent tag name`() {
        val allTags = listOf(
            Tag(id = 7, name = CARDIO_PART),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val exercises = listOf(
            Exercise(name = "跑步机", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES, id = 101),
            Exercise(name = "卧推", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS, id = 102),
        )
        assertEquals(listOf(101), cardioExerciseIds(exercises, allTags))
    }

    @Test
    fun `unknown part is not cardio`() {
        val exercises = listOf(Exercise(name = "未知", tags = emptyList(), id = 5))
        assertEquals(emptyList<Int>(), cardioExerciseIds(exercises, emptyList()))
    }
}
