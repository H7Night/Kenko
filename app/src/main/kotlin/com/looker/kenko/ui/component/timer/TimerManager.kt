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

package com.looker.kenko.ui.component.timer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
}

@Singleton
class TimerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @com.looker.kenko.di.ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var tickerJob: Job? = null

    private var service: TimerService? = null

    fun registerService(service: TimerService) {
        this.service = service
    }

    fun unregisterService() {
        this.service = null
    }

    fun start() {
        if (_state.value == TimerState.RUNNING) return
        if (_state.value == TimerState.IDLE) {
            _elapsedSeconds.value = 0L
        }
        _state.value = TimerState.RUNNING
        TimerService.start(context)
        startTicker()
    }

    fun startWithDuration(initialSeconds: Long) {
        if (_state.value == TimerState.RUNNING) return
        _elapsedSeconds.value = initialSeconds
        _state.value = TimerState.RUNNING
        TimerService.start(context)
        startTicker()
    }

    fun pause() {
        if (_state.value != TimerState.RUNNING) return
        _state.value = TimerState.PAUSED
        tickerJob?.cancel()
    }

    fun resume() {
        if (_state.value != TimerState.PAUSED) return
        _state.value = TimerState.RUNNING
        TimerService.start(context)
        startTicker()
    }

    fun stop() {
        _state.value = TimerState.IDLE
        tickerJob?.cancel()
        tickerJob = null
        _elapsedSeconds.value = 0L
        TimerService.stop(context)
    }

    fun forceStop() {
        _state.value = TimerState.IDLE
        tickerJob?.cancel()
        tickerJob = null
        _elapsedSeconds.value = 0L
    }

    fun setElapsedSeconds(seconds: Long) {
        _elapsedSeconds.value = seconds
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_state.value == TimerState.RUNNING) {
                    _elapsedSeconds.value = _elapsedSeconds.value + 1
                }
            }
        }
    }
}
