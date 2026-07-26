/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.ui.component.timer

import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.localDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TrainingSessionState {
    data object Idle : TrainingSessionState
    data class Active(val sessionId: Int) : TrainingSessionState
    data object Ended : TrainingSessionState
}

@Singleton
class TrainingSessionManager @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val timerManager: TimerManager,
    @com.looker.kenko.di.ApplicationScope private val scope: CoroutineScope,
) {

    private val _sessionState = MutableStateFlow<TrainingSessionState>(TrainingSessionState.Idle)
    val sessionState: StateFlow<TrainingSessionState> = _sessionState.asStateFlow()

    fun startTraining() {
        if (_sessionState.value is TrainingSessionState.Active) return
        scope.launch {
            val sessionId = sessionRepo.getSessionIdOrCreate(localDate)
            _sessionState.value = TrainingSessionState.Active(sessionId)
            // Continue from existing duration if any
            val existingSession = sessionRepo.streamByDate(localDate)
            existingSession.first { true }.let { session ->
                val existingDuration = session?.durationSeconds ?: 0L
                if (existingDuration > 0) {
                    timerManager.startWithDuration(existingDuration)
                } else {
                    timerManager.start()
                }
            }
        }
    }

    fun endTraining(onEmptySessionDeleted: () -> Unit = {}) {
        scope.launch {
            val elapsed = timerManager.elapsedSeconds.value
            val sessionId = (_sessionState.value as? TrainingSessionState.Active)?.sessionId

            timerManager.stop()

            if (sessionId != null) {
                // Save duration to session
                if (elapsed > 0) {
                    sessionRepo.updateSessionDuration(sessionId, elapsed)
                }

                val sets = sessionRepo.getSets(sessionId)
                if (sets.isEmpty() && elapsed < 60) {
                    sessionRepo.deleteSession(
                        com.looker.kenko.domain.model.Session(
                            date = localDate,
                            sets = emptyList(),
                            planId = null,
                            id = sessionId,
                        )
                    )
                    onEmptySessionDeleted()
                }
            }

            _sessionState.value = TrainingSessionState.Ended
        }
    }

    fun reset() {
        timerManager.forceStop()
        _sessionState.value = TrainingSessionState.Idle
    }
}
