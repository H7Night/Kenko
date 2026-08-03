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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme

@Composable
fun Settings(
    viewModel: SettingsViewModel,
    onBackPress: () -> Unit,
    onTagManagementClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Settings(
        state = state,
        onSelectLanguage = viewModel::updateLanguage,
        onSelectTheme = viewModel::updateTheme,
        onSelectCapitalize = viewModel::updateCapitalizeExerciseName,
        onBackPress = onBackPress,
        onTagManagementClick = onTagManagementClick,
        onBackupClick = onBackupClick,
        onAboutClick = onAboutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Settings(
    state: SettingsUiData,
    onSelectLanguage: (Language) -> Unit,
    onSelectTheme: (Theme) -> Unit,
    onSelectCapitalize: (Boolean) -> Unit,
    onBackPress: () -> Unit,
    onTagManagementClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            selected = state.language,
            onSelect = { language ->
                onSelectLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            selected = state.selectedTheme,
            onSelect = { theme ->
                onSelectTheme(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
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
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSelectionRow(
                icon = { Icon(imageVector = Icons.Default.Language, contentDescription = null) },
                title = stringResource(R.string.label_language),
                value = stringResource(state.language.labelRes),
                onClick = { showLanguageDialog = true },
            )

            SettingsSelectionRow(
                icon = { Icon(painter = KenkoIcons.Lightbulb, contentDescription = null) },
                title = stringResource(R.string.label_theme),
                value = stringResource(state.selectedTheme.nameRes),
                onClick = { showThemeDialog = true },
            )

            SettingsSwitchRow(
                icon = { Icon(imageVector = Icons.Default.FormatSize, contentDescription = null) },
                title = stringResource(R.string.label_capitalize_exercise_name),
                checked = state.capitalizeExerciseName,
                onCheckedChange = onSelectCapitalize,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = KenkoBorderWidth,
            )

            SettingsNavRow(
                icon = { Icon(imageVector = Icons.Default.Label, contentDescription = null) },
                title = stringResource(R.string.label_tag_management),
                onClick = onTagManagementClick,
            )

            SettingsNavRow(
                icon = { Icon(painter = KenkoIcons.Save, contentDescription = null) },
                title = stringResource(R.string.label_backup),
                subtitle = state.backupUri?.let { extractFolderName(it) }
                    ?: stringResource(R.string.label_backup_location_not_set),
                onClick = onBackupClick,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = KenkoBorderWidth,
            )

            SettingsNavRow(
                icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                title = stringResource(R.string.label_about),
                onClick = onAboutClick,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * A settings row that opens a selection dialog.
 * Layout: [icon] [title]                    [value] [⇅]
 */
@Composable
private fun SettingsSelectionRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.UnfoldMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * A settings row that navigates to another screen.
 * Layout: [icon] [title / subtitle]          [>]
 */
@Composable
private fun SettingsNavRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: @Composable () -> Unit,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageSelectionDialog(
    selected: Language,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_language)) },
        text = {
            Column {
                Language.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (language == selected) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}

@Composable
private fun ThemeSelectionDialog(
    selected: Theme,
    onSelect: (Theme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_theme)) },
        text = {
            Column {
                Theme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(theme.nameRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (theme == selected) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
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
                earliestSessionDate = null,
            ),
            onSelectLanguage = {},
            onSelectTheme = {},
            onSelectCapitalize = {},
            onBackPress = {},
            onTagManagementClick = {},
            onBackupClick = {},
            onAboutClick = {},
        )
    }
}
