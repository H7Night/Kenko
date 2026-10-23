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

package com.looker.kenko.ui.feature.session.calendar

import androidx.compose.runtime.Stable
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.today
import com.looker.kenko.ui.base.KenkoViewModel
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repo: SessionRepo,
) : KenkoViewModel() {

    private val initialMonth = today().let { it.year to it.monthNumber }

    private val _month = MutableStateFlow(initialMonth)

    val state: StateFlow<CalendarUiData> = combine(
        repo.streamSummaries,
        _month,
    ) { sessions, monthPair ->
        val (year, month) = monthPair
        val sessionDates = sessions.filter { it.hasRecordedWorkout }.map { it.date }.toSet()
        val weeks = buildMonthGrid(year, month)
        CalendarUiData(
            year = year,
            month = month,
            weeks = weeks,
            sessionDates = sessionDates,
            trainingDaysThisMonth = weeks.flatten()
                .count { it != null && it.monthNumber == month && it in sessionDates },
        )
    }.asStateFlow(
        CalendarUiData(
            year = initialMonth.first,
            month = initialMonth.second,
            weeks = buildMonthGrid(initialMonth.first, initialMonth.second),
        ),
        started = SharingStarted.Eagerly,
    )

    fun goToPreviousMonth() {
        _month.value = previousMonth(_month.value.first, _month.value.second)
    }

    fun goToNextMonth() {
        _month.value = nextMonth(_month.value.first, _month.value.second)
    }
}

@Stable
data class CalendarUiData(
    val year: Int = 0,
    val month: Int = 0,
    val weeks: List<List<LocalDate?>> = emptyList(),
    val sessionDates: Set<LocalDate> = emptySet(),
    val trainingDaysThisMonth: Int = 0,
)
