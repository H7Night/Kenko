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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repo: SessionRepo,
    private val planRepo: PlanRepo,
) : ViewModel() {
    private val sessionsStream = repo.stream
    private val isCurrentSessionActive = repo.streamByDate(today()).map { it != null }

    private val availablePlanItems = planRepo.planItems
        .map { items ->
            items.groupBy { it.dayOfWeek }
                .mapValues { entry -> entry.value.map { it.exercise } }
        }

    val state: StateFlow<SessionsUiData> = combine(
        sessionsStream,
        isCurrentSessionActive,
        availablePlanItems,
        planRepo.plans,
    ) { sessions, isCurrentSessionActive, available, plans ->
        val planTitlesMap = plans.associate { it.id to it.titlesMap }
        SessionsUiData(
            sessions = sessions.filter { it.sets.isNotEmpty() },
            isCurrentSessionActive = isCurrentSessionActive,
            availablePlanDays = available,
            dayTitles = planTitlesMap,
            plans = plans.filter { it.isActive || plans.indexOf(it) < 5 },
        )
    }.asStateFlow(SessionsUiData(emptyList(), false))

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    fun addSession(date: LocalDate, day: DayOfWeek, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.updatePlanDay(date, day)
                onComplete()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun removeSession(session: Session) {
        viewModelScope.launch {
            try {
                repo.deleteSession(session)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
data class SessionsUiData(
    val sessions: List<Session>,
    val isCurrentSessionActive: Boolean,
    val availablePlanDays: Map<DayOfWeek, List<Exercise>> = emptyMap(),
    val dayTitles: Map<Int?, Map<DayOfWeek, String>> = emptyMap(),
    val plans: List<Plan> = emptyList(),
) {
    val sessionDates: Set<LocalDate> get() = sessions.map { it.date }.toSet()
}
