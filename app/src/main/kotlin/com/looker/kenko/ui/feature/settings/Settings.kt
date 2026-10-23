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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.ColorPalette
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.utils.toFormat
import com.looker.kenko.ui.component.SelectionDialog
import com.looker.kenko.ui.component.SettingsGroup
import com.looker.kenko.ui.component.SettingsRow
import com.looker.kenko.ui.component.SwitchSettingsRow
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
        onSelectColorPalette = viewModel::updateColorPalette,
        onToggleBodyWeight = viewModel::updateBodyWeightEnabled,
        onToggleHealthConnect = viewModel::updateHealthConnectEnabled,
        onHealthPermissionsResult = viewModel::onHealthPermissionsResult,
        onSyncHealthConnect = viewModel::syncHealthConnect,
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
    onSelectColorPalette: (ColorPalette) -> Unit,
    onToggleBodyWeight: (Boolean) -> Unit,
    onToggleHealthConnect: (Boolean) -> Unit,
    onHealthPermissionsResult: (Boolean) -> Unit,
    onSyncHealthConnect: () -> Unit,
    onBackPress: () -> Unit,
    onTagManagementClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorStyleDialog by remember { mutableStateOf(false) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        onHealthPermissionsResult(granted.containsAll(state.healthConnectPermissions))
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.label_language),
            items = Language.entries,
            selected = state.language,
            label = { stringResource(it.labelRes) },
            onSelect = { language ->
                onSelectLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = stringResource(R.string.label_theme),
            items = Theme.entries,
            selected = state.selectedTheme,
            label = { stringResource(it.nameRes) },
            onSelect = { theme ->
                onSelectTheme(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showColorStyleDialog) {
        SelectionDialog(
            title = stringResource(R.string.label_color_style),
            items = ColorPalette.entries,
            selected = state.colorPalette,
            label = { stringResource(it.nameRes) },
            onSelect = { palette ->
                onSelectColorPalette(palette)
                showColorStyleDialog = false
            },
            onDismiss = { showColorStyleDialog = false },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.label_settings)) },
                navigationIcon = { BackButton(onClick = onBackPress) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup(title = stringResource(R.string.label_settings_appearance)) {
                item {
                    SettingsRow(
                        icon = { Icon(imageVector = Icons.Default.Language, contentDescription = null) },
                        title = stringResource(R.string.label_language),
                        value = stringResource(state.language.labelRes),
                        onClick = { showLanguageDialog = true },
                    )
                }
                item {
                    SettingsRow(
                        icon = { Icon(painter = KenkoIcons.Lightbulb, contentDescription = null) },
                        title = stringResource(R.string.label_theme),
                        value = stringResource(state.selectedTheme.nameRes),
                        onClick = { showThemeDialog = true },
                    )
                }
                item {
                    SettingsRow(
                        icon = { Icon(imageVector = Icons.Outlined.Palette, contentDescription = null) },
                        title = stringResource(R.string.label_color_style),
                        value = stringResource(state.colorPalette.nameRes),
                        onClick = { showColorStyleDialog = true },
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.label_settings_system)) {
                item {
                    SwitchSettingsRow(
                        icon = { Icon(painter = KenkoIcons.Person, contentDescription = null) },
                        title = stringResource(R.string.label_body_weight_toggle),
                        subtitle = stringResource(R.string.label_body_weight_toggle_subtitle),
                        checked = state.bodyWeightEnabled,
                        onCheckedChange = onToggleBodyWeight,
                    )
                }
                if (state.healthConnectAvailable) {
                    item {
                        SwitchSettingsRow(
                            icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = null) },
                            title = stringResource(R.string.label_health_connect),
                            subtitle = stringResource(R.string.label_health_connect_sync),
                            checked = state.healthConnectEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    healthPermissionLauncher.launch(state.healthConnectPermissions)
                                } else {
                                    onToggleHealthConnect(false)
                                }
                            },
                        )
                    }
                    if (state.healthConnectEnabled) {
                        item {
                            SettingsRow(
                                icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                                title = stringResource(R.string.label_health_connect_sync_now),
                                value = state.healthConnectSyncCount?.let {
                                    stringResource(R.string.label_health_connect_synced, it)
                                },
                                onClick = onSyncHealthConnect,
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.label_settings_data)) {
                item {
                    SettingsRow(
                        icon = { Icon(imageVector = Icons.Default.Label, contentDescription = null) },
                        title = stringResource(R.string.label_tag_management),
                        onClick = onTagManagementClick,
                    )
                }
                item {
                    SettingsRow(
                        icon = { Icon(painter = KenkoIcons.Save, contentDescription = null) },
                        title = stringResource(R.string.label_backup),
                        subtitle = when {
                            state.isBackingUp -> stringResource(R.string.label_backup_in_progress)
                            state.lastBackupTime != null -> stringResource(
                                R.string.label_last_backup,
                                state.lastBackupTime.toFormat(),
                            )

                            state.backupUri != null -> stringResource(
                                R.string.label_backup_folder,
                                extractFolderName(state.backupUri),
                            )

                            else -> stringResource(R.string.label_backup_location_not_set)
                        },
                        onClick = onBackupClick,
                    )
                }
            }

            SettingsGroup {
                item {
                    SettingsRow(
                        icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                        title = stringResource(R.string.label_about),
                        onClick = onAboutClick,
                    )
                }
            }
        }
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
                language = Language.System,
                bodyWeightEnabled = true,
                healthConnectEnabled = false,
                healthConnectAvailable = false,
                healthConnectPermissions = emptySet(),
                healthConnectSyncCount = null,
                colorPalette = ColorPalette.Material,
                earliestSessionDate = null,
            ),
            onSelectLanguage = {},
            onSelectTheme = {},
            onSelectColorPalette = {},
            onToggleBodyWeight = {},
            onToggleHealthConnect = {},
            onHealthPermissionsResult = {},
            onSyncHealthConnect = {},
            onBackPress = {},
            onTagManagementClick = {},
            onBackupClick = {},
            onAboutClick = {},
        )
    }
}
