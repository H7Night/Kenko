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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.RepsInReserve
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.domain.model.withDayTitle
import com.looker.kenko.ui.feature.plan.navigation.PlanEditRoute
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanEditViewModel @Inject constructor(
    private val repo: PlanRepo,
    private val stringHandler: StringHandler,
    private val sessionRepo: com.looker.kenko.data.repository.SessionRepo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routeData: PlanEditRoute = savedStateHandle.toRoute()

    private val _planId: Int = routeData.id

    // if null show name edit else plan edit
    private val planIdStream = MutableStateFlow(_planId)

    val planNameState: TextFieldState = TextFieldState("")

    val snackbarState = SnackbarHostState()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    private val _isBackAlreadyPressedOnce = MutableStateFlow(false)
    private val _isSavingDayTitle = MutableStateFlow(false)

    private val _planItemsStream = planIdStream.flatMapLatest { repo.planItems(it) }

    private val _planStream = planIdStream.flatMapLatest { id ->
        repo.plans.map { plans -> plans.find { it.id == id } }
    }

    private val _dayOfWeek: MutableStateFlow<DayOfWeek> = MutableStateFlow(today().dayOfWeek)

    val dayTitleState: TextFieldState = TextFieldState("")

    init {
        viewModelScope.launch {
            try {
                combine(_planStream, _dayOfWeek) { plan, day ->
                    plan?.titlesMap?.get(day) ?: ""
                }.collect { title ->
                    if (!_isSavingDayTitle.value && dayTitleState.text.toString() != title) {
                        dayTitleState.edit {
                            replace(0, length, title)
                        }
                    }
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }

        viewModelScope.launch {
            try {
                _planStream.collect { plan ->
                    if (plan != null && planNameState.text.toString() != plan.name) {
                        planNameState.edit {
                            replace(0, length, plan.name)
                        }
                    }
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }

        viewModelScope.launch {
            try {
                snapshotFlow { dayTitleState.text.toString() }
                    .debounce(200.milliseconds)
                    .collect { title ->
                        _isSavingDayTitle.value = true
                        try {
                            val currentPlan = repo.plan(planIdStream.value) ?: return@collect
                            val day = _dayOfWeek.value
                            if (currentPlan.titlesMap[day] != title) {
                                repo.updatePlan(currentPlan.withDayTitle(day, title))
                            }
                        } finally {
                            _isSavingDayTitle.value = false
                        }
                    }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }

        viewModelScope.launch {
            try {
                snapshotFlow { planNameState.text.toString() }
                    .debounce(500.milliseconds)
                    .collect { name ->
                        val id = planIdStream.value
                        if (id == -1 || name.isBlank() || isNameAlreadyUsed.value) return@collect
                        val currentPlan = repo.plan(id) ?: return@collect
                        if (currentPlan.name != name) {
                            repo.updatePlan(currentPlan.copy(name = name))
                        }
                    }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    private val _isSheetVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _fullDaySelection: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isNameAlreadyUsed = snapshotFlow { planNameState.text.trim().toString() }
        .debounce(200.milliseconds)
        .flatMapLatest { name ->
            _planStream.map { plan ->
                if (name.isBlank() || plan?.name == name) false
                else repo.planNameExists(name)
            }
        }
        .asStateFlow(false)

    val pageState: StateFlow<PlanEditStage> = planIdStream.map { id ->
        if (id == -1) PlanEditStage.NameEdit else PlanEditStage.PlanEdit
    }.asStateFlow(PlanEditStage.NameEdit)

    val state: StateFlow<PlanEditState> = combine(
        _planStream,
        _planItemsStream,
        _dayOfWeek,
        _fullDaySelection,
        _isSheetVisible,
    ) { plan, items, day, daySelection, sheetVisible ->
        PlanEditState(
            currentDay = day,
            selectionMode = daySelection,
            exerciseSheetVisible = sheetVisible,
            planItems = items.filter { it.dayOfWeek == day },
            planTitles = plan?.titlesMap ?: emptyMap(),
        )
    }.asStateFlow(
        PlanEditState(
            currentDay = today().dayOfWeek,
            selectionMode = false,
            exerciseSheetVisible = false,
            planItems = emptyList(),
        ),
    )

    fun saveName() {
        viewModelScope.launch {
            try {
                if (planNameState.text.isBlank()) {
                    snackbarState.showSnackbar(stringHandler.getString(R.string.error_plan_name_empty))
                    return@launch
                }
                if (isNameAlreadyUsed.value) {
                    snackbarState.showSnackbar(stringHandler.getString(R.string.error_plan_name_exists))
                    return@launch
                }
                val createId = repo.createPlan(planNameState.text.toString())
                planIdStream.emit(createId)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun setCurrentDay(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            try {
                _dayOfWeek.emit(dayOfWeek)
                if (_fullDaySelection.value) {
                    _fullDaySelection.emit(false)
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun openFullDaySelection() {
        viewModelScope.launch {
            try {
                _fullDaySelection.emit(true)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun openSheet() {
        viewModelScope.launch {
            try {
                _isSheetVisible.emit(true)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun closeSheet() {
        viewModelScope.launch {
            try {
                _isSheetVisible.emit(false)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                repo.addItem(
                    PlanItem(
                        dayOfWeek = _dayOfWeek.value,
                        exercise = exercise,
                        planId = planIdStream.value,
                    ),
                )
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun removePlanItem(planItemId: Long) {
        viewModelScope.launch {
            try {
                repo.removeItem(planItemId)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateOrder(exercises: List<Exercise>) {
        viewModelScope.launch {
            try {
                repo.updateOrder(planIdStream.value, _dayOfWeek.value, exercises)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun onBackPress(stage: PlanEditStage, onBackPress: () -> Unit) {
        viewModelScope.launch {
            try {
                if (stage == PlanEditStage.NameEdit) {
                    onBackPress()
                    return@launch
                }
                if (_isBackAlreadyPressedOnce.value) {
                    repo.deletePlan(planIdStream.value)
                    onBackPress()
                    return@launch
                }
                if (repo.getPlanItems(planIdStream.value).isEmpty()) {
                    _isBackAlreadyPressedOnce.emit(true)
                    snackbarState.showSnackbar(stringHandler.getString(R.string.error_plan_empty_prompt))
                    return@launch
                }
                onBackPress()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
enum class PlanEditStage {
    NameEdit,
    PlanEdit,
}

@Stable
data class PlanEditState(
    val currentDay: DayOfWeek,
    val selectionMode: Boolean,
    val exerciseSheetVisible: Boolean,
    val planItems: List<PlanItem>,
    val planTitles: Map<DayOfWeek, String> = emptyMap(),
)
