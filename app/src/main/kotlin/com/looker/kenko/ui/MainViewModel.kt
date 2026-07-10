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

package com.looker.kenko.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.domain.model.localDate
import com.looker.kenko.data.repository.PerformanceRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    repo: SettingsRepo,
    sessionRepo: SessionRepo,
    performanceRepo: PerformanceRepo,
) : ViewModel() {

    val isReady: StateFlow<Boolean> = repo.stream
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val theme: StateFlow<Theme> = repo.get { theme }
        .asStateFlow(Theme.System)

    val language: StateFlow<Language> = repo.get { language }
        .asStateFlow(Language.System)

    val isExerciseVisible: StateFlow<Boolean> = sessionRepo.streamByDate(localDate)
        .map { it != null && it.sets.isNotEmpty() }
        .asStateFlow(false)

    init {
        viewModelScope.launch {
            performanceRepo.updateModifiers()
        }
    }
}
