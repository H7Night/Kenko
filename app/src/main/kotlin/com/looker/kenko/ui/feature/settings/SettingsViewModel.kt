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

package com.looker.kenko.ui.feature.settings

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.kenko.data.backup.BackupManager
import com.looker.kenko.data.backup.BackupResult
import com.looker.kenko.data.export.ExportManager
import com.looker.kenko.data.export.ExportOptions
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepo,
    private val backupManager: BackupManager,
    private val exportManager: ExportManager,
    sessionRepo: SessionRepo,
) : ViewModel() {

    private val earliestSessionDate: Flow<LocalDate?> = sessionRepo.earliestSessionDate

    private val _backupState = MutableStateFlow(BackupState())

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    val state: StateFlow<SettingsUiData> = combine(
        repo.stream,
        _backupState,
        earliestSessionDate,
    ) { settings, backupState, earliestDate ->
        SettingsUiData(
            selectedTheme = settings.theme,
            backupUri = settings.backupUri,
            backupInterval = settings.backupInterval,
            lastBackupTime = settings.lastBackupTime,
            isBackingUp = backupState.isBackingUp,
            isRestoring = backupState.isRestoring,
            isExporting = backupState.isExporting,
            backupMessage = backupState.message,
            capitalizeExerciseName = settings.capitalizeExerciseName,
            language = settings.language,
            earliestSessionDate = earliestDate,
        )
    }.asStateFlow(
        SettingsUiData(
            selectedTheme = Theme.System,
            backupUri = null,
            backupInterval = BackupInterval.Off,
            lastBackupTime = null,
            isBackingUp = false,
            isRestoring = false,
            isExporting = false,
            backupMessage = null,
            capitalizeExerciseName = true,
            language = Language.System,
            earliestSessionDate = null,
        ),
    )

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            try {
                repo.setTheme(theme)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateCapitalizeExerciseName(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repo.setCapitalizeExerciseName(enabled)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            try {
                repo.setLanguage(language)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun setBackupLocation(uri: Uri) {
        viewModelScope.launch {
            try {
                repo.setBackupUri(uri.toString())
                backupManager.schedulePeriodicBackup(state.value.backupInterval, uri)
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun setBackupInterval(interval: BackupInterval) {
        viewModelScope.launch {
            try {
                repo.setBackupInterval(interval)
                val backupUri = state.value.backupUri?.toUri()
                if (backupUri != null) {
                    backupManager.schedulePeriodicBackup(interval, backupUri)
                }
            } catch (e: Exception) {
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun backupNow() {
        val backupUri = state.value.backupUri?.toUri() ?: return

        viewModelScope.launch {
            try {
                _backupState.update { it.copy(isBackingUp = true, message = null) }

                when (backupManager.createBackup(backupUri)) {
                    is BackupResult.Success -> {
                        repo.setLastBackupTime(Clock.System.now())
                        _backupState.update {
                            it.copy(isBackingUp = false, message = BackupMessage.BackupSuccess)
                        }
                    }

                    is BackupResult.Error -> {
                        _backupState.update {
                            it.copy(isBackingUp = false, message = BackupMessage.BackupFailed)
                        }
                    }
                }
            } catch (e: Exception) {
                _backupState.update { it.copy(isBackingUp = false) }
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            try {
                _backupState.update { it.copy(isRestoring = true, message = null) }

                when (backupManager.restoreBackup(uri)) {
                    is BackupResult.Success -> {
                        repo.setBackupUri(null)
                        repo.setLastBackupTime(null)
                        _backupState.update {
                            it.copy(isRestoring = false, message = BackupMessage.RestoreSuccess)
                        }
                    }

                    is BackupResult.Error -> {
                        _backupState.update {
                            it.copy(isRestoring = false, message = BackupMessage.RestoreFailed)
                        }
                    }
                }
            } catch (e: Exception) {
                _backupState.update { it.copy(isRestoring = false) }
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun exportData(options: ExportOptions, uri: Uri) {
        viewModelScope.launch {
            try {
                _backupState.update { it.copy(isExporting = true, message = null) }

                when (exportManager.export(options, uri)) {
                    is BackupResult.Success -> {
                        _backupState.update {
                            it.copy(isExporting = false, message = BackupMessage.ExportSuccess)
                        }
                    }

                    is BackupResult.Error -> {
                        _backupState.update {
                            it.copy(isExporting = false, message = BackupMessage.ExportFailed)
                        }
                    }
                }
            } catch (e: Exception) {
                _backupState.update { it.copy(isExporting = false) }
                _snackbar.emit(e.message ?: "An error occurred")
            }
        }
    }

    fun clearBackupMessage() {
        _backupState.update { it.copy(message = null) }
    }
}

private data class BackupState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isExporting: Boolean = false,
    val message: BackupMessage? = null,
)

enum class BackupMessage {
    BackupSuccess,
    BackupFailed,
    RestoreSuccess,
    RestoreFailed,
    ExportSuccess,
    ExportFailed,
}

@Stable
data class SettingsUiData(
    val selectedTheme: Theme,
    val backupUri: String?,
    val backupInterval: BackupInterval,
    val lastBackupTime: Instant?,
    val isBackingUp: Boolean,
    val isRestoring: Boolean,
    val isExporting: Boolean,
    val backupMessage: BackupMessage?,
    val capitalizeExerciseName: Boolean,
    val language: Language,
    val earliestSessionDate: LocalDate?,
)
