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

package com.looker.kenko.ui.feature.settings

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.export.ExportOptions
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.component.PreferenceSwitchRow
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.start
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun Settings(
    viewModel: SettingsViewModel,
    onBackPress: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Settings(
        state = state,
        onSelectLanguage = viewModel::updateLanguage,
        onSelectTheme = viewModel::updateTheme,
        onSelectCapitalize = viewModel::updateCapitalizeExerciseName,
        onSelectBackupLocation = viewModel::setBackupLocation,
        onSelectBackupInterval = viewModel::setBackupInterval,
        onBackupNow = viewModel::backupNow,
        onRestore = viewModel::restore,
        onExport = viewModel::exportData,
        onClearMessage = viewModel::clearBackupMessage,
        onBackPress = onBackPress,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Settings(
    state: SettingsUiData,
    onSelectLanguage: (Language) -> Unit,
    onSelectTheme: (Theme) -> Unit,
    onSelectCapitalize: (Boolean) -> Unit,
    onSelectBackupLocation: (Uri) -> Unit,
    onSelectBackupInterval: (BackupInterval) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (Uri) -> Unit,
    onExport: (ExportOptions, Uri) -> Unit,
    onClearMessage: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle backup messages
    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let { message ->
            val text = when (message) {
                BackupMessage.BackupSuccess -> context.getString(R.string.label_backup_success)
                BackupMessage.BackupFailed -> context.getString(R.string.error_backup_failed)
                BackupMessage.RestoreSuccess -> context.getString(R.string.label_restore_success)
                BackupMessage.RestoreFailed -> context.getString(R.string.error_restore_failed)
                BackupMessage.ExportSuccess -> context.getString(R.string.label_export_success)
                BackupMessage.ExportFailed -> context.getString(R.string.error_export_failed)
            }
            snackbarHostState.showSnackbar(text)
            onClearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_settings)) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState()),
        ) {
            HorizontalDivider(thickness = KenkoBorderWidth)
            Spacer(modifier = Modifier.height(16.dp))
            CategoryHeader(title = stringResource(R.string.label_language))
            Spacer(modifier = Modifier.height(8.dp))
            LanguageSelector(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedLanguage = state.language,
                onSelectLanguage = onSelectLanguage,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CategoryHeader(title = stringResource(R.string.label_theme))
            Spacer(modifier = Modifier.height(4.dp))
            ThemeButton(
                modifier = Modifier.align(CenterHorizontally),
                selectedTheme = state.selectedTheme,
                onClick = onSelectTheme,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceSwitchRow(
                title = stringResource(R.string.label_capitalize_exercise_name),
                checked = state.capitalizeExerciseName,
                onCheckedChange = onSelectCapitalize,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CategoryHeader(title = stringResource(R.string.label_backup))
            Spacer(modifier = Modifier.height(8.dp))
            BackupSection(
                backupUri = state.backupUri,
                backupInterval = state.backupInterval,
                lastBackupTime = state.lastBackupTime,
                isBackingUp = state.isBackingUp,
                isRestoring = state.isRestoring,
                isExporting = state.isExporting,
                onSelectLocation = onSelectBackupLocation,
                onSelectInterval = onSelectBackupInterval,
                onBackupNow = onBackupNow,
                onRestore = onRestore,
                onExport = onExport,
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguage: Language,
    onSelectLanguage: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        Language.entries.forEachIndexed { index, language ->
            SegmentedButton(
                selected = selectedLanguage == language,
                onClick = { onSelectLanguage(language) },
                shape = when (index) {
                    0 -> CircleShape.end(8.dp)
                    Language.entries.lastIndex -> CircleShape.start(8.dp)
                    else -> RoundedCornerShape(8.dp)
                },
                colors = themeButtonColors,
                modifier = Modifier.padding(2.dp),
            ) {
                Text(
                    text = stringResource(language.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeButton(
    selectedTheme: Theme,
    onClick: (Theme) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        val isSystem = remember(selectedTheme) { selectedTheme == Theme.System }
        val isDark = remember(selectedTheme) { selectedTheme == Theme.Dark }
        val isLight = remember(selectedTheme) { selectedTheme == Theme.Light }
        SystemButton(isSelected = isSystem, onClick = onClick)
        LightButton(isSelected = isLight, onClick = onClick)
        DarkButton(isSelected = isDark, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.SystemButton(
    isSelected: Boolean,
    onClick: (Theme) -> Unit,
) {
    val theme = Theme.System
    SegmentedButton(
        selected = isSelected,
        onClick = { onClick(theme) },
        shape = CircleShape.end(8.dp),
        colors = themeButtonColors,
        modifier = Modifier.padding(2.dp),
    ) {
        Text(text = stringResource(theme.nameRes))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.LightButton(
    isSelected: Boolean,
    onClick: (Theme) -> Unit,
) {
    val theme = Theme.Light
    SegmentedButton(
        selected = isSelected,
        onClick = { onClick(theme) },
        shape = RoundedCornerShape(8.dp),
        colors = themeButtonColors,
        modifier = Modifier.padding(2.dp),
    ) {
        Text(text = stringResource(theme.nameRes))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmentedButtonRowScope.DarkButton(
    isSelected: Boolean,
    onClick: (Theme) -> Unit,
) {
    val theme = Theme.Dark
    SegmentedButton(
        selected = isSelected,
        onClick = { onClick(theme) },
        shape = CircleShape.start(8.dp),
        colors = themeButtonColors,
        modifier = Modifier.padding(2.dp),
    ) {
        Text(text = stringResource(theme.nameRes))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val themeButtonColors: SegmentedButtonColors
    @Composable
    get() = SegmentedButtonDefaults.colors(
        activeBorderColor = Color.Transparent,
        inactiveBorderColor = Color.Transparent,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )

private fun formatBackupTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.date} ${localDateTime.hour}:${
        localDateTime.minute.toString().padStart(2, '0')
    }"
}

@Preview
@Composable
private fun ThemePreview() {
    KenkoTheme {
        ThemeButton(selectedTheme = Theme.System, onClick = {})
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    KenkoTheme {
        Settings(
            state = SettingsUiData(
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
            ),
            onSelectLanguage = {},
            onSelectTheme = {},
            onSelectCapitalize = {},
            onSelectBackupLocation = {},
            onSelectBackupInterval = {},
            onBackupNow = {},
            onRestore = {},
            onExport = { _, _ -> },
            onClearMessage = {},
            onBackPress = {},
        )
    }
}
