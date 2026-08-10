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

package com.looker.kenko.ui.feature.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanStat
import com.looker.kenko.domain.model.Weight
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.WeightRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@HiltViewModel
class ProfileViewModel @Inject constructor(
    planRepo: PlanRepo,
    private val weightRepo: WeightRepo,
    exerciseRepo: ExerciseRepo,
    sessionRepo: SessionRepo,
) : ViewModel() {

    private val currentPlan: Flow<Plan?> = planRepo.current

    val plans: StateFlow<List<Plan>> = planRepo.plans
        .asStateFlow(emptyList())

    private val planDateRanges: Flow<Map<Int, Pair<LocalDate, LocalDate>>> = sessionRepo.stream
        .map { sessions ->
            sessions.mapNotNull { session -> session.planId?.let { it to session.date } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, dates) ->
                    (dates.minOrNull()!!) to (dates.maxOrNull()!!)
                }
        }

    private val _selectedPlanId = MutableStateFlow<Int?>(null)
    val selectedPlanId: StateFlow<Int?> = _selectedPlanId.asStateFlow()

    private val _selectedMonth = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedMonth: StateFlow<Pair<Int, Int>?> = _selectedMonth.asStateFlow()

    private data class Bundle(
        val plan: Plan?,
        val weights: List<Weight>,
        val numberOfExercises: Int,
        val plans: List<Plan>,
        val planDateRanges: Map<Int, Pair<LocalDate, LocalDate>>,
    )

    val state: StateFlow<ProfileUiState> = combine(
        combine(
            currentPlan,
            weightRepo.weights,
            exerciseRepo.numberOfExercise,
            plans,
            planDateRanges,
        ) { plan, weights, number, allPlans, ranges ->
            Bundle(plan, weights, number, allPlans, ranges)
        },
        _selectedPlanId,
        _selectedMonth,
    ) { bundle, planId, month ->
        val view = computeWeightChartView(bundle.weights, bundle.planDateRanges, planId, month)
        ProfileUiState(
            numberOfExercises = bundle.numberOfExercises,
            weights = bundle.weights,
            isPlanAvailable = bundle.plan != null,
            planName = bundle.plan?.name ?: "",
            planStat = bundle.plan?.stat,
            plans = bundle.plans,
            filteredWeights = view.visibleWeights,
            selectedMonthLabel = view.monthLabel,
            canGoPrev = view.canGoPrev,
            canGoNext = view.canGoNext,
        )
    }.asStateFlow(ProfileUiState())

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value?.let { (year, month) -> addMonths(year, month, -1) }
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value?.let { (year, month) -> addMonths(year, month, 1) }
    }

    fun selectPlan(planId: Int?) {
        _selectedPlanId.value = planId
        _selectedMonth.value = null
    }

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    fun addWeight(value: Float) {
        viewModelScope.launch {
            try {
                weightRepo.addWeight(Weight(today(), value))
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateWeight(weight: Weight) {
        viewModelScope.launch {
            try {
                weightRepo.updateWeight(weight)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteWeight(id: Int) {
        viewModelScope.launch {
            try {
                weightRepo.deleteWeight(id)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
data class ProfileUiState(
    val numberOfExercises: Int = 0,
    val isPlanAvailable: Boolean = false,
    val planName: String = "",
    val weights: List<Weight> = emptyList(),
    val planStat: PlanStat? = null,
    val plans: List<Plan> = emptyList(),
    val filteredWeights: List<Weight> = emptyList(),
    val selectedMonthLabel: String? = null,
    val canGoPrev: Boolean = false,
    val canGoNext: Boolean = false,
)
