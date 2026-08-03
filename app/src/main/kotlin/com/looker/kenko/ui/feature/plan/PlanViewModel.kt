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

package com.looker.kenko.ui.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repo: PlanRepo,
    private val settingsRepo: SettingsRepo,
) : ViewModel() {

    val plans = repo.plans.asStateFlow(emptyList())

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    fun removePlan(id: Int) {
        viewModelScope.launch {
            try {
                repo.deletePlan(id)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun switchPlan(plan: Plan) {
        viewModelScope.launch {
            try {
                if (!plan.isActive) {
                    plan.id?.let { repo.setCurrent(it) }
                } else {
                    repo.updatePlan(plan.copy(isActive = false))
                }
                if (repo.current.first() != null) {
                    settingsRepo.setOnboardingDone()
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}
