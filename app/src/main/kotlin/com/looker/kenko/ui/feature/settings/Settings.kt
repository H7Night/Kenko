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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.SelectionDialog
import com.looker.kenko.ui.component.SettingsGroup
import com.looker.kenko.ui.component.SettingsRow
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
        onBackPress = onBackPress,
        onTagManagementClick = onTagManagementClick,
        onBackupClick = onBackupClick,
        onAboutClick = onAboutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Settings(
    state: SettingsUiData,
    onSelectLanguage: (Language) -> Unit,
    onSelectTheme: (Theme) -> Unit,
    onBackPress: () -> Unit,
    onTagManagementClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = stringResource(R.string.label_settings)) },
                navigationIcon = { BackButton(onClick = onBackPress) },
                scrollBehavior = scrollBehavior,
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                        subtitle = state.backupUri?.let { extractFolderName(it) }
                            ?: stringResource(R.string.label_backup_location_not_set),
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
                earliestSessionDate = null,
            ),
            onSelectLanguage = {},
            onSelectTheme = {},
            onBackPress = {},
            onTagManagementClick = {},
            onBackupClick = {},
            onAboutClick = {},
        )
    }
}
