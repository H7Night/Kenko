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

package com.looker.kenko.data.repository.local

import com.looker.kenko.data.local.dao.ExerciseDao
import com.looker.kenko.data.local.dao.PlanHistoryDao
import com.looker.kenko.data.local.dao.SessionDao
import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.local.model.SessionDataEntity
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.data.mapper.toEntity
import com.looker.kenko.data.mapper.toExternal
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Set
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.toLocalEpochDays
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

class LocalSessionRepo @Inject constructor(
    private val dao: SessionDao,
    private val setsDao: SetsDao,
    private val historyDao: PlanHistoryDao,
    private val exerciseDao: ExerciseDao,
) : SessionRepo {

    private val mutex = Mutex()

    override val stream: Flow<List<Session>> =
        dao.stream().map {
            it.map { session ->
                session.toExternal(session.sets.toExternal())
            }
        }

    override val planDateRanges: Flow<Map<Int, Pair<LocalDate, LocalDate>>> =
        dao.streamPlanDates().map { list ->
            list.mapNotNull { entry ->
                entry.planId?.let { it to LocalDate.fromEpochDays(entry.date.value.toLong()) }
            }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, dates) ->
                    (dates.minOrNull()!!) to (dates.maxOrNull()!!)
                }
        }

    override val streamSummaries: Flow<List<SessionSummary>> =
        dao.streamSummaries().map { list ->
            list.map { entity ->
                SessionSummary(
                    date = LocalDate.fromEpochDays(entity.date.value.toLong()),
                    planId = entity.planId,
                    dayIndexOverride = entity.dayIndexOverride,
                    durationSeconds = entity.durationSeconds,
                    exerciseNames = entity.exerciseNames
                        ?.split(",")
                        ?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    setCount = entity.setCount,
                    id = entity.id,
                )
            }
        }
    override val setsCount: Flow<Int> =
        setsDao.totalSetCount()

    override val earliestSessionDate: Flow<LocalDate?> = flow {
        emit(dao.earliestSessionDate()?.let { LocalDate.fromEpochDays(it.value) })
    }

    override suspend fun addSet(sessionId: Int, set: Set) = mutex.withLock {
        setsDao.insert(
            set.toEntity(
                sessionId,
                setsDao.getSetsCountBySessionId(sessionId) ?: 0,
            ),
        )
    }

    override suspend fun addSet(
        sessionId: Int,
        exerciseId: Int,
        weight: Float,
        reps: Int,
    ) = mutex.withLock {
        setsDao.insert(
            SetEntity(
                repsOrDuration = reps,
                weight = weight,
                exerciseId = exerciseId,
                sessionId = sessionId,
                order = setsDao.getSetsCountBySessionId(sessionId) ?: 0,
            ),
        )
    }

    override suspend fun updateSet(setId: Int, reps: Int, weight: Float) {
        setsDao.update(setId, reps, weight)
    }

    override suspend fun removeSet(setId: Int) {
        setsDao.delete(setId)
    }

    override suspend fun clearSets(date: LocalDate) {
        val sessionId = dao.getSessionId(date.toLocalEpochDays()) ?: return
        setsDao.deleteBySessionId(sessionId)
    }

    override suspend fun updateDayIndex(date: LocalDate, dayIndex: Int) {
        getSessionIdOrCreate(date)
        dao.updateDayIndexOverride(date.toLocalEpochDays(), dayIndex)
    }

    override suspend fun updateSessionDuration(sessionId: Int, durationSeconds: Long) {
        dao.updateDuration(sessionId, durationSeconds)
    }

    override suspend fun getSessionIdOrCreate(date: LocalDate): Int = mutex.withLock {
        val currentPlanId = requireNotNull(historyDao.getCurrentId()) { "No plan active" }
        val existingId = dao.getSessionId(date.toLocalEpochDays())
        if (existingId != null) {
            return@withLock existingId
        }
        return@withLock dao.insert(SessionDataEntity(date.toLocalEpochDays(), currentPlanId)).toInt()
    }

    override fun streamByDate(date: LocalDate): Flow<Session?> {
        return dao
            .session(date.toLocalEpochDays())
            .map { session ->
                if (session == null) return@map null
                session.toExternal(session.sets.toExternal())
            }
    }

    override fun previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?> {
        return dao.getPreviousSessionDate(date.toLocalEpochDays().value, planId, dayIndex)
            .map { it?.let(LocalDate::fromEpochDays) }
    }

    override suspend fun getSets(sessionId: Int): List<Set> =
        setsDao.getSetsBySessionId(sessionId).toExternal()

    override suspend fun deleteSession(session: Session) {
        val sessionId = session.id ?: return
        setsDao.deleteBySessionId(sessionId)
        dao.delete(sessionId)
    }

    override suspend fun deleteSessionById(id: Int) {
        setsDao.deleteBySessionId(id)
        dao.delete(id)
    }

    private suspend fun List<SetEntity>.toExternal(): List<Set> = mapNotNull {
        val exercise = exerciseDao.get(it.exerciseId) ?: return@mapNotNull null
        it.toExternal(exercise.toExternal())
    }
}
