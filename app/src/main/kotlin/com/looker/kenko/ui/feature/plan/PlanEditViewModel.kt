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
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
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
    private val _isSavingDayTitle = MutableStateFlow<Int?>(null)

    private val _planItemsStream = planIdStream.flatMapLatest { repo.planItemsByPlan(it) }

    private val _planStream = planIdStream.flatMapLatest { id ->
        repo.plans.map { plans -> plans.find { it.id == id } }
    }

    private val _dayIndex: MutableStateFlow<Int> = MutableStateFlow(1)

    val dayTitleState: TextFieldState = TextFieldState("")

    init {
        viewModelScope.launch {
            try {
                combine(_planStream, _dayIndex) { plan, dayIndex ->
                    (plan?.titlesMap?.get(dayIndex) ?: "") to dayIndex
                }.collect { (title, dayIndex) ->
                    // 仅当不是正在保存该天的标题时才回写，避免覆盖用户正在输入的内容
                    if (_isSavingDayTitle.value != dayIndex && dayTitleState.text.toString() != title) {
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
                // 捕获编辑时的 dayIndex，避免切换天后 debounce 将文本保存到错误的天
                snapshotFlow { dayTitleState.text.toString() }
                    .map { it to _dayIndex.value }
                    .debounce(200.milliseconds)
                    .collect { (title, day) ->
                        _isSavingDayTitle.value = day
                        try {
                            val currentPlan = repo.plan(planIdStream.value) ?: return@collect
                            if ((currentPlan.titlesMap[day] ?: "") != title) {
                                repo.updatePlan(currentPlan.withDayTitle(day, title))
                            }
                        } finally {
                            _isSavingDayTitle.value = null
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
        _dayIndex,
        _isSheetVisible,
    ) { plan, items, dayIndex, sheetVisible ->
        PlanEditState(
            currentDay = dayIndex,
            dayCount = plan?.dayCount ?: 7,
            exerciseSheetVisible = sheetVisible,
            planItems = items.filter { it.dayIndex == dayIndex },
            planTitles = plan?.titlesMap ?: emptyMap(),
            allItems = items,
        )
    }.asStateFlow(
        PlanEditState(
            currentDay = 1,
            dayCount = 7,
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

    fun setCurrentDay(dayIndex: Int) {
        viewModelScope.launch {
            try {
                _dayIndex.emit(dayIndex)
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
                        dayIndex = _dayIndex.value,
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
                repo.updateOrder(planIdStream.value, _dayIndex.value, exercises)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun addDay() {
        viewModelScope.launch {
            try {
                repo.addDay(planIdStream.value)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun renameDay(dayIndex: Int) {
        viewModelScope.launch {
            try {
                // 切换到该天,使标题输入框(dayTitleState)聚焦到对应天的标题
                _dayIndex.emit(dayIndex)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun deleteDay(dayIndex: Int) {
        viewModelScope.launch {
            try {
                repo.deleteDay(planIdStream.value, dayIndex)
                _dayIndex.emit(1)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun setDayAsRest(dayIndex: Int) {
        viewModelScope.launch {
            try {
                // 清空该天动作即成为休息日
                repo.getPlanItems(planIdStream.value, dayIndex)
                    .forEach { repo.removeItem(requireNotNull(it.id)) }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun moveDay(from: Int, to: Int) {
        viewModelScope.launch {
            try {
                repo.moveDay(planIdStream.value, from, to)
                _dayIndex.emit(to)
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
    val currentDay: Int,
    val dayCount: Int,
    val exerciseSheetVisible: Boolean,
    val planItems: List<PlanItem>,
    val planTitles: Map<Int, String> = emptyMap(),
    val allItems: List<PlanItem> = emptyList(),
)
