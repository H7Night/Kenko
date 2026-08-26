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

package com.looker.kenko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.looker.kenko.data.local.model.SessionDataEntity
import com.looker.kenko.data.local.model.SessionDateEntity
import com.looker.kenko.data.local.model.SessionEntity
import com.looker.kenko.data.local.model.SessionSnapshotEntity
import com.looker.kenko.data.local.model.SessionSummaryEntity
import com.looker.kenko.utils.EpochDays
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: SessionDataEntity): Long

    @Query(
        """
        SELECT EXISTS
        (SELECT *
        FROM sessions
        WHERE date = :date)
        """,
    )
    suspend fun sessionExistsOn(date: EpochDays): Boolean

    @Query(
        """
        SELECT date
        FROM sessions
        WHERE id = :sessionId
        """,
    )
    suspend fun getDatePerformedOn(sessionId: Int): EpochDays

    @Query(
        """
        SELECT MIN(date)
        FROM sessions
        """,
    )
    suspend fun earliestSessionDate(): EpochDays?

    @Query(
        """
        SELECT id
        FROM sessions
        WHERE date = :date
        """,
    )
    suspend fun getSessionId(date: EpochDays): Int?

    @Query(
        """
        SELECT planId
        FROM sessions
        WHERE id = :sessionId
        """,
    )
    suspend fun getSessionPlanId(sessionId: Int): Int?

    @Query(
        """
        UPDATE sessions
        SET dayIndexOverride = :dayIndex
        WHERE date = :date
        """,
    )
    suspend fun updateDayIndexOverride(date: EpochDays, dayIndex: Int)

    @Query(
        """
        UPDATE sessions
        SET dayTitleOverride = :dayTitle
        WHERE id = :sessionId
        """,
    )
    suspend fun updateDayTitleOverride(sessionId: Int, dayTitle: String?)

    /** 某计划的全部 session 概要（id + 训练日 + 动作名），供修改计划前回填训练日名称快照。 */
    @Query(
        """
        SELECT s.id, s.dayIndexOverride, s.dayTitleOverride,
               GROUP_CONCAT(DISTINCT e.name) AS exerciseNames
        FROM sessions s
        LEFT JOIN sets st ON st.sessionId = s.id
        LEFT JOIN exercises e ON e.id = st.exerciseId
        WHERE s.planId = :planId
        GROUP BY s.id
        """,
    )
    suspend fun getSessionsByPlan(planId: Int): List<SessionSnapshotEntity>

    @Query(
        """
        UPDATE sessions
        SET durationSeconds = :duration
        WHERE id = :sessionId
        """,
    )
    suspend fun updateDuration(sessionId: Int, duration: Long)

    @Transaction
    @Query(
        """
        SELECT *
        FROM sessions
        ORDER BY date DESC
        """,
    )
    fun stream(): Flow<List<SessionEntity>>

    /** 轻量查询：仅 date + planId，用于计算计划训练日期区间（避免加载全部 sets）。 */
    @Query(
        """
        SELECT date, planId
        FROM sessions
        ORDER BY date
        """,
    )
    fun streamPlanDates(): Flow<List<SessionDateEntity>>

    /**
     * 轻量查询：会话概要 + 去重动作名（单条 JOIN + GROUP_CONCAT），
     * 供 Records 列表页使用，避免逐 session 加载 sets 与逐 set 加载 exercise 的 N+1。
     */
    @Query(
        """
        SELECT s.id, s.date, s.planId, s.dayIndexOverride, s.dayTitleOverride, s.durationSeconds,
               GROUP_CONCAT(DISTINCT e.name) AS exerciseNames,
               COUNT(st.id) AS setCount
        FROM sessions s
        LEFT JOIN sets st ON st.sessionId = s.id
        LEFT JOIN exercises e ON e.id = st.exerciseId
        GROUP BY s.id
        ORDER BY s.date DESC
        """,
    )
    fun streamSummaries(): Flow<List<SessionSummaryEntity>>

    @Transaction
    @Query(
        """
        SELECT *
        FROM sessions
        WHERE date = :date
        """,
    )
    fun session(date: EpochDays): Flow<SessionEntity?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM sessions
        WHERE date = :date
        """,
    )
    suspend fun getSession(date: EpochDays): SessionEntity?

    @Query(
        """
        SELECT date
        FROM sessions
        WHERE (planId = :planId OR :planId IS NULL)
        AND dayIndexOverride = :dayIndex
        AND date < :date
        ORDER BY date DESC
        LIMIT 1
        """
    )
    fun getPreviousSessionDate(date: Int, planId: Int?, dayIndex: Int): Flow<Int?>

    @Query(
        """
        DELETE FROM sessions
        WHERE id = :sessionId
        """,
    )
    suspend fun delete(sessionId: Int)
}
