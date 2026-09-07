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

package com.looker.kenko.ui.feature.statistics

import androidx.lifecycle.ViewModel
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    sessionRepo: SessionRepo,
) : ViewModel() {

    val state = sessionRepo.streamSummaries
        .map { summaries ->
            val dates = summaries.map { it.date }.toSet()
            val countByDate = summaries.groupBy { it.date }.mapValues { it.value.size }
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            StatisticsUiState(
                sessionDates = dates,
                countByDate = countByDate,
                today = today,
            )
        }
        .asStateFlow(StatisticsUiState())
}

data class StatisticsUiState(
    val sessionDates: Set<kotlinx.datetime.LocalDate> = emptySet(),
    val countByDate: Map<kotlinx.datetime.LocalDate, Int> = emptyMap(),
    val today: kotlinx.datetime.LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
)
