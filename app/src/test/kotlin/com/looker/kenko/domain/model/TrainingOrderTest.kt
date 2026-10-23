package com.looker.kenko.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TrainingOrderTest {

    private fun exercise(name: String, id: Int? = null) = Exercise(name = name, id = id)

    private fun setOf(exercise: Exercise, order: Int) = Set(
        repsOrDuration = 10,
        weight = 20F,
        exercise = exercise,
        order = order,
    )

    @Test
    fun emptySetsKeepPlanOrderWithoutSequence() {
        val planned = listOf(exercise("高位下拉"), exercise("侧平举"), exercise("深蹲"))
        val result = orderTrainingExercises(planned, emptyList())
        assertEquals(listOf("高位下拉", "侧平举", "深蹲"), result.map { it.exercise.name })
        assertEquals(listOf(null, null, null), result.map { it.sequence })
    }

    @Test
    fun firstSetMovesExerciseToBottomWithSequenceOne() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        val result = orderTrainingExercises(planned, listOf(setOf(raise, 0)))
        assertEquals(listOf("高位下拉", "深蹲", "侧平举"), result.map { it.exercise.name })
        assertNull(result[0].sequence)
        assertNull(result[1].sequence)
        assertEquals(1, result[2].sequence)
    }

    @Test
    fun subsequentFirstSetsNumberInStartOrder() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 侧平举先加 set(order 0、1), 高位下拉后加 set(order 2)
        val result = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(raise, 1), setOf(pullDown, 2)),
        )
        assertEquals(listOf("深蹲", "侧平举", "高位下拉"), result.map { it.exercise.name })
        assertEquals(1, result[1].sequence)
        assertEquals(2, result[2].sequence)
    }

    @Test
    fun addingMoreSetsDoesNotReorderOrRenumber() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        val first = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(pullDown, 1)),
        )
        val second = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(pullDown, 1), setOf(raise, 2)),
        )
        assertEquals(first.map { it.exercise.name }, second.map { it.exercise.name })
        assertEquals(first.map { it.sequence }, second.map { it.sequence })
    }

    @Test
    fun extraExerciseNotInPlanIsNumberedAndSorted() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val extra = exercise("额外动作", 9)
        val planned = listOf(pullDown, raise)
        val result = orderTrainingExercises(planned, listOf(setOf(extra, 0)))
        assertEquals(listOf("高位下拉", "侧平举", "额外动作"), result.map { it.exercise.name })
        assertEquals(1, result[2].sequence)
    }

    @Test
    fun extraExerciseWithTagsIsNotDuplicatedByTaglessSetExercise() {
        val pullUp = exercise("引体向上", 9)
        // 计划/额外动作来自 ExerciseRepo.stream(带 tags),而组来自 SessionRepo(不带 tags);
        // 两者 id 相同但对象不相等,不能因此被当成两个动作。
        val planned = listOf(pullUp.copy(tags = listOf(Tag(id = 12, name = "背阔肌", parentId = 2))))
        val set = setOf(pullUp, 0)
        val result = orderTrainingExercises(planned, listOf(set))
        assertEquals(listOf("引体向上"), result.map { it.exercise.name })
        assertEquals(1, result.single().sequence)
    }

    @Test
    fun unorderedSetsStillSortedByOrder() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 输入顺序打乱, 由 order 决定首组顺序
        val result = orderTrainingExercises(
            planned,
            listOf(setOf(pullDown, 2), setOf(raise, 0), setOf(pullDown, 3)),
        )
        assertEquals(listOf("深蹲", "侧平举", "高位下拉"), result.map { it.exercise.name })
        assertEquals(1, result[1].sequence)
        assertEquals(2, result[2].sequence)
    }

    @Test
    fun recordedOnlyKeepsExercisesWithSets() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val ordered = orderTrainingExercises(
            planned = listOf(pullDown, raise),
            sets = listOf(setOf(pullDown, 0)),
        )
        assertEquals(listOf("高位下拉"), ordered.recordedOnly().map { it.exercise.name })
    }

    @Test
    fun deletingAllSetsOfOneExerciseRenumbersRemaining() {        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 侧平举删光全部 set 后: 回未开始组, 剩余高位下拉重编号为 1
        val result = orderTrainingExercises(planned, listOf(setOf(pullDown, 1)))
        assertEquals(listOf("侧平举", "深蹲", "高位下拉"), result.map { it.exercise.name })
        assertNull(result[0].sequence)
        assertNull(result[1].sequence)
        assertEquals(1, result[2].sequence)
    }
}
