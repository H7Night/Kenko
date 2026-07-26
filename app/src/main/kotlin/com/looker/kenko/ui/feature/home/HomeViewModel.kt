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

package com.looker.kenko.ui.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.localDate
import com.looker.kenko.ui.component.timer.TimerManager
import com.looker.kenko.ui.component.timer.TimerState
import com.looker.kenko.ui.component.timer.TrainingSessionManager
import com.looker.kenko.ui.component.timer.TrainingSessionState
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.datetime.LocalDate

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    planRepo: PlanRepo,
    sessionRepo: SessionRepo,
    exerciseRepo: ExerciseRepo,
    val timerManager: TimerManager,
    val trainingSessionManager: TrainingSessionManager,
) : ViewModel() {

    private val planStream = planRepo.current
    val sessionStream = sessionRepo.streamByDate(localDate)
    private val sessionsStream = sessionRepo.stream

    private val planItemStream = combine(
        sessionStream,
        planRepo.planItems
    ) { session, planItems ->
        val day = session?.planDayOverride ?: localDate.dayOfWeek
        planItems.filter { it.dayOfWeek == day }
    }

    val planName: StateFlow<String?> = planStream.map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allExercises: StateFlow<List<com.looker.kenko.domain.model.Exercise>> = exerciseRepo.stream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planExercises: StateFlow<List<com.looker.kenko.domain.model.PlanItem>> = planItemStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionSets: StateFlow<Map<com.looker.kenko.domain.model.Exercise, List<com.looker.kenko.domain.model.Set>>> =
        combine(sessionStream, planItemStream) { session, planItems ->
            val fromPlan = planItems.map { it.exercise }.distinct()
            val fromSession = session?.sets?.groupBy { it.exercise } ?: emptyMap()
            // Ensure all planned exercises are present, even with empty sets
            val result = fromPlan.associateWith { exercise ->
                fromSession[exercise] ?: emptyList()
            }.toMutableMap()
            // Also include exercises that have sets but aren't in today's plan
            fromSession.forEach { (ex, sets) ->
                if (ex !in result) result[ex] = sets
            }
            result.toMap()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val state: StateFlow<HomeUiData> = combine(
        planStream,
        sessionStream,
        sessionsStream,
        planItemStream,
        timerManager.state,
        trainingSessionManager.sessionState,
    ) { array: Array<*> ->
        val currentPlan = array[0] as? com.looker.kenko.domain.model.Plan
        val currentSession = array[1] as? com.looker.kenko.domain.model.Session
        @Suppress("UNCHECKED_CAST")
        val sessions = array[2] as List<com.looker.kenko.domain.model.Session>
        @Suppress("UNCHECKED_CAST")
        val planItems = array[3] as List<com.looker.kenko.domain.model.PlanItem>
        val timerState = array[4] as TimerState
        val trainingState = array[5] as TrainingSessionState

        val isFirstSession = sessions.size <= 1 && sessions.firstOrNull()?.date == localDate
        HomeUiData(
            isPlanSelected = currentPlan != null,
            isSessionStarted = currentSession != null && currentSession.sets.isNotEmpty(),
            isTodayEmpty = planItems.isEmpty(),
            isFirstSession = isFirstSession,
            currentPlanId = currentPlan?.id,
            sessionDates = sessions.map { it.date }.toSet(),
            timerState = timerState,
            trainingState = trainingState,
            planName = currentPlan?.name,
            todayExercises = planItems.mapNotNull { it.exercise },
        )
    }.asStateFlow(
        HomeUiData(
            isPlanSelected = true,
            isSessionStarted = false,
            isTodayEmpty = false,
            isFirstSession = false,
            currentPlanId = null,
            sessionDates = emptySet(),
            timerState = TimerState.IDLE,
            trainingState = TrainingSessionState.Idle,
            planName = null,
            todayExercises = emptyList(),
        ),
    )

    fun startWorkout() {
        trainingSessionManager.startTraining()
    }

    fun pauseWorkout() {
        timerManager.pause()
    }

    fun resumeWorkout() {
        timerManager.resume()
    }

    fun endWorkout() {
        trainingSessionManager.endTraining()
    }

    fun dismissEndedSession() {
        trainingSessionManager.reset()
    }
}

@Immutable
data class HomeUiData(
    val isPlanSelected: Boolean,
    val isSessionStarted: Boolean,
    val isTodayEmpty: Boolean,
    val isFirstSession: Boolean,
    val currentPlanId: Int?,
    val sessionDates: Set<LocalDate>,
    val timerState: TimerState = TimerState.IDLE,
    val trainingState: TrainingSessionState = TrainingSessionState.Idle,
    val planName: String? = null,
    val todayExercises: List<com.looker.kenko.domain.model.Exercise> = emptyList(),
)
