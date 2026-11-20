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

package com.h7night.tore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h7night.tore.domain.model.settings.ColorPalette
import com.h7night.tore.domain.model.settings.Language
import com.h7night.tore.domain.model.settings.Theme
import com.h7night.tore.domain.model.today
import com.h7night.tore.data.repository.SessionRepo
import com.h7night.tore.data.repository.SettingsRepo
import com.h7night.tore.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class MainViewModel @Inject constructor(
    repo: SettingsRepo,
    sessionRepo: SessionRepo,
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

    val palette: StateFlow<ColorPalette> = repo.get { colorPalette }
        .asStateFlow(ColorPalette.Material)

    val isExerciseVisible: StateFlow<Boolean> = sessionRepo.streamByDate(today())
        .map { it != null && it.sets.isNotEmpty() }
        .asStateFlow(false)
}
