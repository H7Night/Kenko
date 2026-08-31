package com.looker.kenko.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlanTransferFile(
    val version: Int = 1,
    val plans: List<PlanTransfer> = emptyList(),
)

@Serializable
data class PlanTransfer(
    val name: String,
    val description: String? = null,
    val difficulty: String? = null,
    val focus: String? = null,
    val equipment: String? = null,
    val time: String? = null,
    val dayCount: Int = 7,
    val dayTitles: Map<Int, String> = emptyMap(),
    val days: List<PlanDayTransfer> = emptyList(),
)

@Serializable
data class PlanDayTransfer(
    val dayIndex: Int,
    val exercises: List<PlanExerciseTransfer> = emptyList(),
)

@Serializable
data class PlanExerciseTransfer(
    val name: String,
    val target: String? = null,
    val tags: List<String> = emptyList(),
    val countType: String = "REPS",
    val isBodyweight: Boolean = false,
)

fun PlanExerciseTransfer.toExercise(): Exercise = Exercise(
    name = name,
    countType = runCatching { CountType.valueOf(countType) }.getOrDefault(CountType.REPS),
    isBodyweight = isBodyweight,
    tags = tags.map { tagName ->
        Tag(name = tagName, parentName = target)
    },
)
