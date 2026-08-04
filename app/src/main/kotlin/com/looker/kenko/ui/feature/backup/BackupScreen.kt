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

package com.looker.kenko.ui.feature.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.feature.settings.BackupMessage
import com.looker.kenko.ui.feature.settings.BackupSection
import com.looker.kenko.ui.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBackPress: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
            viewModel.clearBackupMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_backup)) },
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
            BackupSection(
                backupUri = state.backupUri,
                backupInterval = state.backupInterval,
                lastBackupTime = state.lastBackupTime,
                earliestSessionDate = state.earliestSessionDate,
                isBackingUp = state.isBackingUp,
                isRestoring = state.isRestoring,
                isExporting = state.isExporting,
                onSelectLocation = viewModel::setBackupLocation,
                onSelectInterval = viewModel::setBackupInterval,
                onBackupNow = viewModel::backupNow,
                onRestore = viewModel::restore,
                onExport = viewModel::exportData,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}
