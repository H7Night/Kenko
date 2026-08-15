/*
 * Copyright (C) 2025 LooKeR & Contributors
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.looker.kenko.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TrainingExercise(
    val exercise: Exercise,
    val sets: List<Set>,
    val sequence: Int? = null,
)

/**
 * 训练动作排序: 未开始动作保持 [planned] 顺序置顶, 已开始动作按首次添加 set 的顺序
 * (取该动作所有 set 中最小的 order) 编号 1..N 并置底。
 */
fun orderTrainingExercises(
    planned: List<Exercise>,
    sets: List<Set>,
): List<TrainingExercise> {
    val setsByExercise: Map<Exercise, List<Set>> =
        sets.sortedBy { it.order }.groupBy { it.exercise }

    val started = setsByExercise.entries
        .sortedBy { (_, list) -> list.minOf { it.order } }
        .mapIndexed { index, (exercise, list) ->
            TrainingExercise(exercise, list, index + 1)
        }

    val notStarted = planned
        .filter { it !in setsByExercise }
        .map { TrainingExercise(it, emptyList(), null) }

    return notStarted + started
}
