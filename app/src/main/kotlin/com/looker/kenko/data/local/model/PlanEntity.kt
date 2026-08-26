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
import com.looker.kenko.domain.model.Labels.Difficulty
import com.looker.kenko.domain.model.Labels.Equipment
import com.looker.kenko.domain.model.Labels.Focus
import com.looker.kenko.domain.model.Labels.Time

@Entity(tableName = "plans")
data class PlanEntity(
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val description: String?,
    @ColumnInfo(defaultValue = "NULL")
    val difficulty: Difficulty?,
    @ColumnInfo(defaultValue = "NULL")
    val focus: Focus?,
    @ColumnInfo(defaultValue = "NULL")
    val equipment: Equipment?,
    @ColumnInfo(defaultValue = "NULL")
    val time: Time?,
    @ColumnInfo(defaultValue = "NULL")
    val dayTitles: String? = null,
    @ColumnInfo(defaultValue = "7")
    val dayCount: Int = 7,
    @ColumnInfo(defaultValue = "1")
    val currentDayIndex: Int = 1,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

@Entity(
    "plan_day",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("planId", "exerciseId"),
    ],
)
data class PlanDayEntity(
    val planId: Int,
    val exerciseId: Int,
    val dayIndex: Int,
    val sortOrder: Int = 0,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

/** 解析计划的训练日名称 JSON（{"1":"胸A",...}）；解析失败或为空返回空 map。 */
fun PlanEntity.dayTitlesMap(): Map<Int, String> {
    if (dayTitles.isNullOrBlank()) return emptyMap()
    return try {
        kotlinx.serialization.json.Json.decodeFromString<Map<Int, String>>(dayTitles)
    } catch (e: Exception) {
        emptyMap()
    }
}
