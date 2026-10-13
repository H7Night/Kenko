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

package com.looker.kenko.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.looker.kenko.utils.EpochDays

@Entity(
    "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sessionId", "exerciseId"),
    ],
)
data class SetEntity(
    @ColumnInfo("reps")
    val repsOrDuration: Int,
    val weight: Float,
    val order: Int,
    val sessionId: Int,
    val exerciseId: Int,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

/** 轻量投影：按日期聚合的有氧分钟（供统计页下推查询）。 */
data class CardioMinutesByDate(
    val date: EpochDays,
    val minutes: Int,
)

/** 轻量投影：某动作的每组数据 + 所属训练日（供"单动作进步"查询）。 */
data class ExerciseSetRow(
    val date: EpochDays,
    val weight: Float,
    val reps: Int,
)

/** 轻量投影：某动作"上一次训练"的各组（供训练页"上次对照"）。 */
data class PreviousSetRow(
    val reps: Int,
    val weight: Float,
)
