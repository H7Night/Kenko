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

import com.looker.kenko.domain.model.RepsInReserve
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Set
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

interface SessionRepo {

    val stream: Flow<List<Session>>

    /** 会话概要流（Records 列表页用）：轻量 JOIN 一次取回，不含组详情。 */
    val streamSummaries: Flow<List<SessionSummary>>

    /** 每个计划的训练日期区间（首 session 日期 ~ 末 session 日期），仅依赖轻量查询。 */
    val planDateRanges: Flow<Map<Int, Pair<LocalDate, LocalDate>>>

    val setsCount: Flow<Int>

    val earliestSessionDate: Flow<LocalDate?>

    suspend fun addSet(sessionId: Int, set: Set)

    suspend fun addSet(
        sessionId: Int,
        exerciseId: Int,
        weight: Float,
        reps: Int,
        rir: RepsInReserve,
    )

    suspend fun updateSet(setId: Int, reps: Int, weight: Float)

    suspend fun removeSet(setId: Int)

    suspend fun clearSets(date: LocalDate)

    suspend fun updatePlanDay(date: LocalDate, day: DayOfWeek)

    suspend fun updateSessionDuration(sessionId: Int, durationSeconds: Long)

    suspend fun getSessionIdOrCreate(date: LocalDate): Int

    fun streamByDate(date: LocalDate): Flow<Session?>

    fun previousSessionDate(date: LocalDate, planId: Int?, day: DayOfWeek): Flow<LocalDate?>

    suspend fun getSets(sessionId: Int): List<Set>

    suspend fun deleteSession(session: Session)

    suspend fun deleteSessionById(id: Int)
}
