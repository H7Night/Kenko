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
import com.looker.kenko.data.repository.StatisticsRepository
import com.looker.kenko.domain.statistics.StatisticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: StatisticsRepository,
) : ViewModel() {

    /** 应用作用域缓存；`null` 表示首次结果尚未就绪（UI 显示骨架）。 */
    val state: StateFlow<StatisticsUiState?> = repository.state
}
