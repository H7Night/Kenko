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

package com.looker.kenko.data.repository

import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Set
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SessionRepo {

    val stream: Flow<List<Session>>

    /** 会话概要流（Records 列表页用）：轻量 JOIN 一次取回，不含组详情。 */
    val streamSummaries: Flow<List<SessionSummary>>

    /** 每个计划的激活区间（plan_history 首次激活日 ~ 最近停用日；激活中则以今天为终点）。 */
    val planDateRanges: Flow<Map<Int, Pair<LocalDate, LocalDate>>>

    val setsCount: Flow<Int>

    val earliestSessionDate: Flow<LocalDate?>

    suspend fun addSet(sessionId: Int, set: Set)

    suspend fun addSet(
        sessionId: Int,
        exerciseId: Int,
        weight: Float,
        reps: Int,
    )

    suspend fun updateSet(setId: Int, reps: Int, weight: Float)

    suspend fun removeSet(setId: Int)

    suspend fun clearSets(date: LocalDate)

    suspend fun updateDayIndex(date: LocalDate, dayIndex: Int)

    suspend fun updateSessionDuration(sessionId: Int, durationSeconds: Long)

    /**
     * 记录"开始训练"时刻(epoch millis)：仅在该 session 尚无开始时间时写入,
     * 之后再次开始(继续训练)保留最初时刻。session 被删除(重置)时随之消失。
     */
    suspend fun ensureSessionStarted(sessionId: Int, startedAtMillis: Long)

    /** 记录本次"结束训练"时刻(epoch millis);继续训练不改开始时间,只在再次结束时更新此值。 */
    suspend fun updateSessionEnded(sessionId: Int, endedAtMillis: Long)

    /** 某 session 显式添加的动作 id(有序),含暂无组的空动作。 */
    fun streamSessionExerciseIds(sessionId: Int): Flow<List<Int>>

    suspend fun addSessionExercise(sessionId: Int, exerciseId: Int)

    suspend fun removeSessionExercise(sessionId: Int, exerciseId: Int)

    suspend fun getSessionIdOrCreate(date: LocalDate): Int

    /**
     * 把某计划当前(修改前)的训练日名称回填为该计划下所有 session 的快照,
     * 供修改计划动作/训练日名称前调用,使历史记录不再随计划修改而变化。
     */
    suspend fun snapshotPlanDayTitles(planId: Int)

    fun streamByDate(date: LocalDate): Flow<Session?>

    fun previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?>

    suspend fun getSets(sessionId: Int): List<Set>

    suspend fun deleteSession(session: Session)

    suspend fun deleteSessionById(id: Int)
}
