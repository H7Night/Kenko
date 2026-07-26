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

package com.looker.kenko.ui.feature.session

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.localDate
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
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
    private val isCurrentSessionActive = repo.streamByDate(localDate).map { it != null }

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
        val currentPlan = plans.find { it.isActive }
        val currentPlanTitles = currentPlan?.titlesMap ?: emptyMap()
        SessionsUiData(
            sessions = sessions.filter { it.sets.isNotEmpty() },
            isCurrentSessionActive = isCurrentSessionActive,
            availablePlanDays = available,
            dayTitles = currentPlanTitles,
            plans = plans.filter { it.isActive || plans.indexOf(it) < 5 },
        )
    }.asStateFlow(SessionsUiData(emptyList(), false))

    fun addSession(date: LocalDate, day: DayOfWeek, onComplete: () -> Unit) {
        viewModelScope.launch {
            repo.updatePlanDay(date, day)
            onComplete()
        }
    }

    fun removeSession(session: Session) {
        viewModelScope.launch {
            repo.deleteSession(session)
        }
    }
}

@Stable
data class SessionsUiData(
    val sessions: List<Session>,
    val isCurrentSessionActive: Boolean,
    val availablePlanDays: Map<DayOfWeek, List<Exercise>> = emptyMap(),
    val dayTitles: Map<DayOfWeek, String> = emptyMap(),
    val plans: List<Plan> = emptyList(),
) {
    val sessionDates: Set<LocalDate> get() = sessions.map { it.date }.toSet()
}
