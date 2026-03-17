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

package com.looker.kenko.ui.addSet

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.localDate
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.addSet.components.BoundReached
import com.looker.kenko.ui.addSet.components.Direction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@HiltViewModel(assistedFactory = AddSetViewModel.AddSetViewModelFactory::class)
class AddSetViewModel @AssistedInject constructor(
    private val sessionRepo: SessionRepo,
    @Assisted("id") private val id: Int,
    @Assisted("date") private val date: LocalDate?,
) : ViewModel() {

    val reps: TextFieldState = TextFieldState("12")
    val weights: TextFieldState = TextFieldState("20.0")
    val setsCount: TextFieldState = TextFieldState("2")

    var selectedSetType by mutableStateOf(SetType.Standard)
        private set

    fun setSetType(type: SetType) {
        selectedSetType = type
    }

    fun addRep(value: Int) {
        reps.setTextAndPlaceCursorAtEnd((repInt + value).toString())
    }

    fun addWeight(value: Float) {
        weights.setTextAndPlaceCursorAtEnd((weightFloat + value).toString())
    }

    fun addSetCount(value: Int) {
        setsCount.setTextAndPlaceCursorAtEnd((setsInt + value).coerceAtLeast(1).toString())
    }

    val repsBoundReached = BoundReached { direction ->
        when (direction) {
            Direction.Left -> addRep(-1)
            Direction.Right -> addRep(1)
        }
    }

    val weightsBoundReached = BoundReached { direction ->
        when (direction) {
            Direction.Left -> addWeight(-1F)
            Direction.Right -> addWeight(1F)
        }
    }

    val setsBoundReached = BoundReached { direction ->
        when (direction) {
            Direction.Left -> addSetCount(-1)
            Direction.Right -> addSetCount(1)
        }
    }

    fun addSet() {
        viewModelScope.launch {
            val sessionId = sessionRepo.getSessionIdOrCreate(date ?: localDate)
            repeat(setsInt) {
                sessionRepo.addSet(
                    sessionId = sessionId,
                    exerciseId = id,
                    weight = weightFloat,
                    reps = repInt,
                    setType = selectedSetType,
                    rir = RepsInReserve(2),
                )
            }
        }
    }

    private inline val repInt: Int
        get() = reps.text.toString().toIntOrNull() ?: 0

    private inline val weightFloat: Float
        get() = weights.text.toString().toFloatOrNull() ?: 0F

    private inline val setsInt: Int
        get() = setsCount.text.toString().toIntOrNull() ?: 2

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
