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

package com.looker.kenko.ui.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.model.Plan
import com.looker.kenko.data.model.PlanStat
import com.looker.kenko.data.model.Weight
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.WeightRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    planRepo: PlanRepo,
    private val weightRepo: WeightRepo,
    exerciseRepo: ExerciseRepo,
) : ViewModel() {

    private val currentPlan: Flow<Plan?> = planRepo.current

    val state: StateFlow<ProfileUiState> = combine(
        currentPlan,
        weightRepo.weights,
        exerciseRepo.numberOfExercise,
    ) { plan, weights, number ->
        ProfileUiState(
            numberOfExercises = number,
            weights = weights,
            isPlanAvailable = plan != null,
            planName = plan?.name ?: "",
            planStat = plan?.stat,
        )
    }.asStateFlow(ProfileUiState())

    fun addWeight(value: Float) {
        viewModelScope.launch {
            weightRepo.addWeight(Weight(localDate, value))
        }
    }

    fun updateWeight(weight: Weight) {
        viewModelScope.launch {
            weightRepo.updateWeight(weight)
        }
    }

    fun deleteWeight(id: Int) {
        viewModelScope.launch {
            weightRepo.deleteWeight(id)
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
)
