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

package com.looker.kenko.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.component.timer.TimerState
import com.looker.kenko.ui.component.timer.TimerManager
import com.looker.kenko.ui.component.timer.TrainingSessionManager
import com.looker.kenko.ui.component.timer.TrainingSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Persists the current timer elapsed seconds to the database when the app
 * goes to background, preventing data loss on unexpected termination.
 */
class SessionLifecycleObserver @Inject constructor(
    private val timerManager: TimerManager,
    private val trainingSessionManager: TrainingSessionManager,
    private val sessionRepo: SessionRepo,
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val timerState = timerManager.state.value
        if (timerState != TimerState.RUNNING && timerState != TimerState.PAUSED) return

        val sessionState = trainingSessionManager.sessionState.value
        val sessionId = (sessionState as? TrainingSessionState.Active)?.sessionId ?: return

        val elapsed = timerManager.elapsedSeconds.value
        if (elapsed > 0) {
            GlobalScope.launch(Dispatchers.IO) {
                sessionRepo.updateSessionDuration(sessionId, elapsed)
            }
        }
    }
}
