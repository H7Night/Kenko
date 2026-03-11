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

package com.looker.kenko.ui.sessionDetail

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.kenko.R
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.PlanItem
import com.looker.kenko.data.model.Session
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.model.week
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.ui.sessionDetail.navigation.SessionDetailRoute
import com.looker.kenko.utils.asStateFlow
import com.looker.kenko.utils.isToday
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repo: SessionRepo,
    private val planRepo: PlanRepo,
    private val settingsRepo: SettingsRepo,
    private val savedStateHandle: SavedStateHandle,
    private val uriHandler: UriHandler,
) : ViewModel() {

    private val routeData: SessionDetailRoute = savedStateHandle.toRoute<SessionDetailRoute>()

    private val epochDays: Int? = routeData.epochDays.takeIf { it != -1 }

    private val sessionDate: LocalDate = epochDays?.let {
        LocalDate.fromEpochDays(it)
    } ?: localDate

    private val isTodaySession = epochDays == null

    val previousSessionDate = sessionDate - week

    private val previousSessionExists: Flow<Boolean> = repo.streamByDate(previousSessionDate)
        .map { it != null }

    private val sessionStream: Flow<Session?> = repo.streamByDate(sessionDate)

    private val availablePlanItems: Flow<Map<DayOfWeek, List<Exercise>>> = planRepo.planItems
        .map { items ->
            items.groupBy { it.dayOfWeek }
                .mapValues { entry -> entry.value.map { it.exercise } }
        }

    private val _temporaryExerciseIds: StateFlow<List<Int>> =
        savedStateHandle.getStateFlow("_temporary_exercise_ids", emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val exercisesToday: Flow<List<Exercise>> =
        combine(
            sessionStream,
            _temporaryExerciseIds,
            availablePlanItems
        ) { session, temporaryIds, available ->
            Triple(session, temporaryIds, available)
        }.flatMapLatest { (session, temporaryIds, available) ->
            val temporaryExercises = if (temporaryIds.isNotEmpty()) {
                val allExercises = available.values.flatten().distinctBy { it.id }
                temporaryIds.mapNotNull { id -> allExercises.find { it.id == id } }
            } else {
                emptyList()
            }

            if (temporaryExercises.isNotEmpty()) {
                flowOf(temporaryExercises)
            } else {
                val plannedFlow = when {
                    sessionDate.isToday -> planRepo.activeExercises(sessionDate.dayOfWeek)
                    session?.planId != null -> planRepo.planItems(session.planId, sessionDate.dayOfWeek)
                        .map { it.map(PlanItem::exercise) }

                    else -> flowOf(emptyList())
                }

                plannedFlow.map { planned ->
                    val performed = session?.performExercises ?: emptyList()
                    val result = (planned + performed).toMutableList()

                    // If we have performed exercises but they are not in the current planned list
                    // (e.g. imported plan on a rest day), try to find the best matching day
                    // from the current plan to show remaining exercises from that day.
                    if (sessionDate.isToday && performed.isNotEmpty() && planned.isEmpty()) {
                        val performedIds = performed.map { it.id }.toSet()
                        val matchingDayExercises = available.values.firstOrNull { dayExercises ->
                            dayExercises.any { it.id in performedIds }
                        }
                        if (matchingDayExercises != null) {
                            result.addAll(matchingDayExercises)
                        }
                    }

                    result.distinctBy { it.id }
                }
            }
        }

    private val lastSetTimeStream = settingsRepo.get { lastSetTime }

    private val _isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(isTodaySession)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _currentExercise: MutableStateFlow<Exercise?> = MutableStateFlow(null)
    val current: StateFlow<Exercise?> = _currentExercise

    val state: StateFlow<SessionDetailState> =
        combine(
            sessionStream,
            exercisesToday,
            lastSetTimeStream,
            previousSessionExists,
            availablePlanItems,
            _isEditMode,
        ) { flows ->
            val session = flows[0] as Session?
            val exercises = flows[1] as List<Exercise>
            val lastSetTime = flows[2] as Instant?
            val previousSession = flows[3] as Boolean
            val available = flows[4] as Map<DayOfWeek, List<Exercise>>
            val isEditMode = flows[5] as Boolean

            if (session == null && epochDays != null) {
                return@combine SessionDetailState.Error.InvalidSession
            }

            if (exercises.isEmpty() && sessionDate.isToday) {
                return@combine SessionDetailState.Error.EmptyPlan(available)
            }

            val currentSession = session ?: Session(-1, emptyList())

            val exerciseMap = when {
                sessionDate.isToday || exercises.isNotEmpty() -> exercises.associateWith { exercise ->
                    currentSession.sets.filter { it.exercise.id == exercise.id }
                }

                currentSession.sets.isNotEmpty() -> currentSession.sets.groupBy { it.exercise }
                else -> emptyMap()
            }

            SessionDetailState.Success(
                SessionUiData(
                    date = currentSession.date,
                    sets = exerciseMap,
                    isToday = isTodaySession,
                    isEditMode = isEditMode,
                    lastSetTime = lastSetTime,
                    hasPreviousSession = previousSession,
                ),
            )
        }.onStart { emit(SessionDetailState.Loading) }
            .asStateFlow(SessionDetailState.Loading)

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun importPlanFromDay(exercises: List<Exercise>) {
        savedStateHandle["_temporary_exercise_ids"] = exercises.mapNotNull { it.id }
    }

    fun startRestTimer() {
        viewModelScope.launch {
            settingsRepo.setLastSetTime(Clock.System.now())
        }
    }

    fun resetRestTimer() {
        viewModelScope.launch {
            settingsRepo.setLastSetTime(null)
        }
    }

    fun removeSet(setId: Int?) {
        if (setId == null) return
        viewModelScope.launch {
            repo.removeSet(setId)
        }
    }

    fun showBottomSheet(exercise: Exercise) {
        if (!isEditMode.value) return
        viewModelScope.launch {
            _currentExercise.emit(exercise)
        }
    }

    fun hideSheet() {
        viewModelScope.launch {
            _currentExercise.emit(null)
        }
    }

    fun openReference(reference: String) {
        viewModelScope.launch {
            try {
                uriHandler.openUri(reference)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}

@Stable
data class SessionUiData(
    val date: LocalDate,
    val sets: Map<Exercise, List<Set>>,
    val isToday: Boolean = false,
    val isEditMode: Boolean = false,
    val lastSetTime: Instant? = null,
    val hasPreviousSession: Boolean = false,
)

sealed interface SessionDetailState {

    data object Loading : SessionDetailState

    data class Success(val data: SessionUiData) : SessionDetailState

    sealed class Error(
        @param:StringRes val title: Int,
        @param:StringRes val errorMessage: Int,
    ) : SessionDetailState {
        data object InvalidSession : Error(
            title = R.string.label_missed_day,
            errorMessage = R.string.error_cant_find_session,
        )

        data class EmptyPlan(
            val availablePlanDays: Map<DayOfWeek, List<Exercise>> = emptyMap()
        ) : Error(
            title = R.string.label_nothing_today,
            errorMessage = R.string.label_no_exercise_today,
        )
    }
}
