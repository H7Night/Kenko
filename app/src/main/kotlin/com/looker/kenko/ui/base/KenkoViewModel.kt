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

package com.looker.kenko.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel exposing the shared one-shot snackbar channel and a
 * [launchCatching] helper that reports failures through it, removing the
 * repeated try/catch + emit boilerplate from feature ViewModels.
 */
abstract class KenkoViewModel : ViewModel() {

    private val _snackbar = MutableSharedFlow<String>()

    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    protected suspend fun emitSnackbar(message: String) {
        _snackbar.emit(message)
    }

    protected fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: DEFAULT_ERROR)
            }
        }
    }

    private companion object {
        const val DEFAULT_ERROR = "An error occurred"
    }
}
