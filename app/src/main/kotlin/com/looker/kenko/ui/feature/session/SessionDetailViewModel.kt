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

package com.looker.kenko.ui.feature.session

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.TrainingExercise
import com.looker.kenko.domain.model.orderTrainingExercises
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.domain.model.TrainingDayMatch
import com.looker.kenko.ui.feature.session.navigation.SessionDetailRoute
import com.looker.kenko.utils.asStateFlow
import com.looker.kenko.utils.isToday
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repo: SessionRepo,
    private val planRepo: PlanRepo,
    private val exerciseRepo: ExerciseRepo,
    private val settingsRepo: SettingsRepo,
    private val savedStateHandle: SavedStateHandle,
    private val uriHandler: UriHandler,
) : ViewModel() {

    private val routeData: SessionDetailRoute = savedStateHandle.toRoute<SessionDetailRoute>()

    private val epochDays: Int? = routeData.epochDays.takeIf { it != -1 }

    private val sessionDate: LocalDate = epochDays?.let {
        LocalDate.fromEpochDays(it)
    } ?: today()

    private val isTodaySession = epochDays == null

    private val sessionStream: Flow<Session?> = repo.streamByDate(sessionDate)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousSessionDateStream: Flow<LocalDate?> =
        combine(sessionStream, planRepo.plans) { session, plans ->
            session to plans
        }.flatMapLatest { (session, plans) ->
            val planId = session?.planId ?: plans.find { it.isActive }?.id
            val day = session?.dayIndexOverride ?: plans.find { it.isActive }?.currentDayIndex
            if (day == null) flowOf(null) else repo.previousSessionDate(sessionDate, planId, day)
        }

    private val availablePlanItems: Flow<Map<Int, List<Exercise>>> = planRepo.planItems
        .map { items ->
            items.groupBy { it.dayIndex }
                .mapValues { entry -> entry.value.map { it.exercise } }
        }

    /** 每个计划的训练日 → 动作名集合,用于无 dayIndexOverride 的历史记录反查训练日。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val planDayExerciseNames: StateFlow<Map<Int, Map<Int, kotlin.collections.Set<String>>>> =
        planRepo.plans
            .flatMapLatest { plans ->
                val flows = plans.map { plan ->
                    planRepo.planItems(requireNotNull(plan.id)).map { items ->
                        plan.id to items.groupBy({ it.dayIndex }, { it.exercise.name })
                            .mapValues { it.value.toSet() }
                    }
                }
                if (flows.isEmpty()) flowOf(emptyMap())
                else combine(flows) { array ->
                    @Suppress("UNCHECKED_CAST")
                    array.map { it as Pair<Int, Map<Int, kotlin.collections.Set<String>>> }.toMap()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allExercises: StateFlow<List<Exercise>> = exerciseRepo.stream
        .asStateFlow(initial = emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val exercisesToday: Flow<List<Exercise>> =
        combine(
            sessionStream,
            availablePlanItems,
            planRepo.plans,
        ) { session, available, plans ->
            Triple(session, available, plans)
        }.flatMapLatest { (session, available, plans) ->
            val currentPlan = plans.find { it.isActive }
            val dayOverride = session?.dayIndexOverride
            val plannedFlow = when {
                dayOverride != null -> {
                    val pid = session.planId
                    if (pid != null) {
                        planRepo.planItems(pid, dayOverride)
                            .map { it.map(PlanItem::exercise) }
                    } else {
                        planRepo.activeExercises(dayOverride)
                    }
                }

                sessionDate.isToday -> {
                    val day = currentPlan?.currentDayIndex
                    if (day == null) flowOf(emptyList()) else planRepo.activeExercises(day)
                }

                session?.planId != null -> {
                    val day = currentPlan?.currentDayIndex
                    if (day == null) flowOf(emptyList()) else planRepo.planItems(session.planId, day)
                        .map { it.map(PlanItem::exercise) }
                }

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

    private val _isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(isTodaySession)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    private val _currentExercise: MutableStateFlow<Exercise?> = MutableStateFlow(null)
    val current: StateFlow<Exercise?> = _currentExercise

    val state: StateFlow<SessionDetailState> =
        combine(
            sessionStream,
            exercisesToday,
            previousSessionDateStream,
            availablePlanItems,
            _isEditMode,
            planRepo.plans,
        ) { flows ->
            val session = flows[0] as Session?
            val exercises = flows[1] as List<Exercise>
            val previousSessionDate = flows[2] as LocalDate?
            val available = flows[3] as Map<Int, List<Exercise>>
            val isEditMode = flows[4] as Boolean
            val plans = flows[5] as List<com.looker.kenko.domain.model.Plan>

            if (session == null && epochDays != null) {
                return@combine SessionDetailState.Error.InvalidSession
            }

            val currentPlan = plans.find { it.isActive }
            val currentPlanTitles = currentPlan?.titlesMap ?: emptyMap()

            if (exercises.isEmpty() && sessionDate.isToday) {
                return@combine SessionDetailState.Error.EmptyPlan(available, currentPlanTitles)
            }

            val currentSession = session ?: Session(-1, emptyList())
            // 无 dayIndexOverride:今天回退当前训练日;历史记录按动作名反查所属训练日
            val dayIndex = currentSession.dayIndexOverride ?: if (sessionDate.isToday) {
                currentPlan?.currentDayIndex
            } else {
                currentSession.planId?.let { planId ->
                    TrainingDayMatch.matchDayIndex(
                        currentSession.performExercises.map { it.name }.toSet(),
                        planDayExerciseNames.value[planId] ?: emptyMap(),
                    )
                }
            }
            val dayTitle = dayIndex?.let { day ->
                plans.find { it.id == currentSession.planId }?.titlesMap?.get(day)
                    ?: currentPlanTitles[day]
            }

            val sets = orderTrainingExercises(
                planned = exercises,
                sets = currentSession.sets,
            )

            SessionDetailState.Success(
                SessionUiData(
                    date = currentSession.date,
                    sets = sets,
                    isToday = isTodaySession,
                    isEditMode = isEditMode,
                    dayTitle = dayTitle,
                    dayIndexOverride = dayIndex,
                    previousSessionDate = previousSessionDate,
                    availablePlanDays = available,
                    dayTitles = currentPlanTitles,
                ),
            )
        }.onStart { emit(SessionDetailState.Loading) }
            .asStateFlow(SessionDetailState.Loading)

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun importPlanFromDay(dayIndex: Int) {
        viewModelScope.launch {
            try {
                repo.updateDayIndex(sessionDate, dayIndex)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun clearTodaySets() {
        viewModelScope.launch {
            try {
                repo.clearSets(sessionDate)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun removeSet(setId: Int?) {
        if (setId == null) return
        viewModelScope.launch {
            try {
                repo.removeSet(setId)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateSet(setId: Int?, reps: Int, weight: Float) {
        if (setId == null) return
        viewModelScope.launch {
            try {
                repo.updateSet(setId, reps, weight)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun showBottomSheet(exercise: Exercise) {
        if (!isEditMode.value) return
        viewModelScope.launch {
            try {
                _currentExercise.emit(exercise)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun hideSheet() {
        viewModelScope.launch {
            try {
                _currentExercise.emit(null)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun openReference(reference: String) {
        viewModelScope.launch {
            try {
                uriHandler.openUri(reference)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
data class SessionUiData(
    val date: LocalDate,
    val sets: List<TrainingExercise>,
    val isToday: Boolean = false,
    val isEditMode: Boolean = false,
    val dayTitle: String? = null,
    val dayIndexOverride: Int? = null,
    val previousSessionDate: LocalDate? = null,
    val availablePlanDays: Map<Int, List<Exercise>> = emptyMap(),
    val dayTitles: Map<Int, String> = emptyMap(),
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
            val availablePlanDays: Map<Int, List<Exercise>> = emptyMap(),
            val dayTitles: Map<Int, String> = emptyMap()
        ) : Error(
            title = R.string.label_nothing_today,
            errorMessage = R.string.label_no_exercise_today,
        )
    }
}
