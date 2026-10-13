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

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.today
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.base.KenkoViewModel
import com.looker.kenko.ui.feature.session.components.BoundReached
import com.looker.kenko.ui.feature.session.components.Direction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.LocalDate

/**
 * ViewModel for the AddSet bottom sheet.
 *
 * 现在的语义：**只选组数**——确认后创建 N 条空组（次数/重量留待训练页手动输入，
 * 输入时会显示"上次对照"）。有氧仍只记一条"分钟"。
 *
 * Uses [AssistedInject] because parameters are passed directly by the
 * composable caller, not via Navigation routes. Navigation-based ViewModels
 * use [SavedStateHandle] instead.
 */
@HiltViewModel(assistedFactory = AddSetViewModel.AddSetViewModelFactory::class)
class AddSetViewModel @AssistedInject constructor(
    private val sessionRepo: SessionRepo,
    private val exerciseRepo: ExerciseRepo,
    @Assisted("id") private val id: Int,
    @Assisted("date") private val date: LocalDate?,
) : KenkoViewModel() {

    /** 有氧用的分钟数；力量动作用不到。 */
    val reps: TextFieldState = TextFieldState(DEFAULT_REPS)
    val setsCount: TextFieldState = TextFieldState(DEFAULT_SETS)

    private var isCardio by mutableStateOf(false)

    init {
        launchCatching {
            val exercise = exerciseRepo.get(id)
            isCardio = exercise?.countType == CountType.MINUTES
            if (isCardio) {
                reps.setTextAndPlaceCursorAtEnd(CARDIO_REPS)
            }
        }
    }

    fun addRep(value: Int) {
        reps.setTextAndPlaceCursorAtEnd((repInt + value).coerceAtLeast(0).toString())
    }

    fun addSetCount(value: Int) {
        setsCount.setTextAndPlaceCursorAtEnd((setsInt + value).coerceAtLeast(1).toString())
    }

    val repsBoundReached = BoundReached { direction ->
        when (direction) {
            Direction.Left -> addRep(-5)
            Direction.Right -> addRep(5)
        }
    }

    val setsBoundReached = BoundReached { direction ->
        when (direction) {
            Direction.Left -> addSetCount(-1)
            Direction.Right -> addSetCount(1)
        }
    }

    fun addSet() {
        launchCatching {
            val sessionId = sessionRepo.getSessionIdOrCreate(date ?: today())
            if (isCardio) {
                // 有氧只记录一条时长，不涉及组数概念。
                sessionRepo.addSet(sessionId = sessionId, exerciseId = id, weight = 0F, reps = repInt)
            } else {
                // 只创建空组：次数/重量稍后在训练页手动输入。
                repeat(setsInt) {
                    sessionRepo.addSet(sessionId = sessionId, exerciseId = id, weight = 0F, reps = 0)
                }
            }
        }
    }

    private inline val repInt: Int
        get() = reps.text.toString().toIntOrNull() ?: 0

    private inline val setsInt: Int
        get() = setsCount.text.toString().toIntOrNull() ?: DEFAULT_SETS.toInt()

    private companion object {
        const val DEFAULT_REPS = "10"
        const val DEFAULT_SETS = "2"
        const val CARDIO_REPS = "20"
    }

    @AssistedFactory
    interface AddSetViewModelFactory {
        fun create(
            @Assisted("id") id: Int,
            @Assisted("date") date: LocalDate? = null,
        ): AddSetViewModel
    }

    object IntTransformation : InputTransformation {
        override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        override fun TextFieldBuffer.transformInput() {
            if (!asCharSequence().isDigitsOnly()) {
                revertAllChanges()
            }
        }
    }

    object FloatTransformation : InputTransformation {
        override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        override fun TextFieldBuffer.transformInput() {
            toString().toFloatOrNull() ?: revertAllChanges()
        }
    }
}
