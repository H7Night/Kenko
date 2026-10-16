package com.looker.kenko.testutil

import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.ColorPalette
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Settings
import com.looker.kenko.domain.model.settings.Theme
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeSettingsRepo(
    initial: Settings = Settings(
        isOnboardingDone = true,
        theme = Theme.System,
        lastSetTime = null,
        backupUri = null,
        backupInterval = BackupInterval.Off,
        lastBackupTime = null,
        language = Language.System,
        bodyWeightEnabled = true,
        healthConnectEnabled = false,
        colorPalette = ColorPalette.Material,
    ),
) : SettingsRepo {
    private val state = MutableStateFlow(initial)
    override val stream: Flow<Settings> = state
    override fun <T> get(block: Settings.() -> T): Flow<T> = state.map { it.block() }
    override suspend fun setOnboardingDone() = Unit
    override suspend fun setTheme(theme: Theme) { state.value = state.value.copy(theme = theme) }
    override suspend fun setLastSetTime(instant: Instant?) = Unit
    override suspend fun setBackupUri(uri: String?) = Unit
    override suspend fun setBackupInterval(interval: BackupInterval) = Unit
    override suspend fun setLastBackupTime(instant: Instant?) = Unit
    override suspend fun setLanguage(language: Language) = Unit
    override suspend fun setBodyWeightEnabled(enabled: Boolean) {
        state.value = state.value.copy(bodyWeightEnabled = enabled)
    }

    override suspend fun setHealthConnectEnabled(enabled: Boolean) {
        state.value = state.value.copy(healthConnectEnabled = enabled)
    }

    override suspend fun setColorPalette(palette: ColorPalette) {
        state.value = state.value.copy(colorPalette = palette)
    }
}
