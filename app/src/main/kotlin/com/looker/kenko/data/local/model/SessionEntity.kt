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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.looker.kenko.utils.EpochDays

data class SessionEntity(
    @Embedded
    val data: SessionDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sets: List<SetEntity>,
)

@Entity(
    "sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class SessionDataEntity(
    val date: EpochDays,
    @ColumnInfo(index = true)
    val planId: Int?,
    val dayIndexOverride: Int? = null,
    val dayTitleOverride: String? = null,
    val durationSeconds: Long? = null,
    /** 开始训练时刻(epoch millis)；仅在该 session 首次"开始训练"时写入,随 session 生命周期存亡。 */
    val startedAt: Long? = null,
    /** 最近一次"结束训练"时刻(epoch millis)；继续训练不改开始时间,只在再次结束时更新此值。 */
    val endedAt: Long? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

/**
 * 本次训练"显式添加"的动作（计划里没有的、或暂时没有组也要显示的）。
 * 随 session 级联删除;动作删除时级联删除。
 */
@Entity(
    "session_exercises",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    val sessionId: Int,
    val exerciseId: Int,
    val order: Int = 0,
)

/** 轻量投影：仅 date + planId（供计划训练日期区间计算，不加载 sets）。 */
data class SessionDateEntity(
    val date: EpochDays,
    val planId: Int?,
)

/** 轻量投影：会话概要 + 去重动作名（GROUP_CONCAT），供 Records 列表页一次查询取回。 */
data class SessionSummaryEntity(
    val date: EpochDays,
    val planId: Int?,
    val dayIndexOverride: Int? = null,
    val dayTitleOverride: String? = null,
    val durationSeconds: Long? = null,
    val exerciseNames: String? = null,
    val setCount: Int = 0,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

/** 轻量投影：某计划的 session 概要（回填训练日名称快照用）。 */
data class SessionSnapshotEntity(
    val id: Int,
    val dayIndexOverride: Int?,
    val dayTitleOverride: String? = null,
    val exerciseNames: String? = null,
)
