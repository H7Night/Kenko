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

package com.looker.kenko.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.Session
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.EmptyPage
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.planEdit.components.dayName
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import com.looker.kenko.utils.isToday
import kotlinx.datetime.LocalDate

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.looker.kenko.ui.components.SwipeToDeleteBox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.looker.kenko.ui.components.KenkoBorderWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDate
import java.io.OutputStreamWriter

@Composable
fun Sessions(
    viewModel: SessionsViewModel,
    onSessionClick: (LocalDate?) -> Unit,
    onBackPress: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportData by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null && exportData != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(exportData)
                    }
                }
                exportData = null
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportMarkdown = { start, end ->
                exportData = viewModel.generateMarkdown(start, end)
                createDocumentLauncher.launch("kenko_export_${start}_to_${end}.md")
                showExportDialog = false
            }
        )
    }

    Sessions(
        state = state,
        onSessionClick = onSessionClick,
        onRemoveSession = viewModel::removeSession,
        onBackPress = onBackPress,
        onExportClick = { showExportDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Sessions(
    state: SessionsUiData,
    onSessionClick: (LocalDate?) -> Unit,
    onRemoveSession: (Session) -> Unit,
    onBackPress: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(text = stringResource(R.string.label_delete_session_title)) },
            text = { Text(text = stringResource(R.string.label_delete_session_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSession(sessionToDelete!!)
                        sessionToDelete = null
                    },
                ) {
                    Text(text = stringResource(R.string.label_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(text = stringResource(R.string.label_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onClick = onBackPress)
                },
                title = {
                    Text(text = stringResource(id = R.string.label_sessions_title))
                },
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(
                            imageVector = Icons.Rounded.IosShare,
                            contentDescription = stringResource(R.string.label_export)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        if (state.sessions.isEmpty()) {
            EmptyPage(stringResource(id = R.string.label_no_sessions))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding + PaddingValues(bottom = 96.dp, start = 14.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.sessions,
                    key = { it.id!! },
                ) { session ->
                    SwipeToDeleteBox(
                        modifier = Modifier.animateItem(),
                        onDismiss = { sessionToDelete = session },
                    ) {
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExportMarkdown: (LocalDate, LocalDate) -> Unit,
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(com.looker.kenko.data.model.localDate) }
    var endDate by remember { mutableStateOf(com.looker.kenko.data.model.localDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.label_select_date_range)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DateSelectionRow(
                    label = stringResource(R.string.label_start_date),
                    date = startDate,
                    onDateSelected = { startDate = it }
                )
                DateSelectionRow(
                    label = stringResource(R.string.label_end_date),
                    date = endDate,
                    onDateSelected = { endDate = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onExportMarkdown(startDate, endDate) }) {
                Text(text = stringResource(R.string.label_export_markdown))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.label_cancel))
            }
        }
    )
}

@Composable
private fun DateSelectionRow(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        TextButton(
            onClick = {
                val datePicker = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateSelected(LocalDate(year, month + 1, dayOfMonth))
                    },
                    date.year,
                    date.monthNumber - 1,
                    date.dayOfMonth
                )
                datePicker.show()
            }
        ) {
            Text(
                text = formatDate(date, DateFormat.YearMonthDay),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SessionCard(
    session: Session,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val containerColor = if (session.date.isToday) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val containerShape = if (session.date.isToday) {
        CircleShape
    } else {
        MaterialTheme.shapes.extraLarge
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = containerShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
        ) {
            val titleStyle = MaterialTheme.typography.titleLarge
            val secondaryEmphasis = MaterialTheme.colorScheme.outline
            val dayName = dayName(session.date.dayOfWeek)
            val string = remember(session.date, dayName) {
                buildAnnotatedString {
                    withStyle(titleStyle.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                        append(formatDate(session.date, dateTimeFormat = DateFormat.YearMonthDay))
                    }
                    append(" ${Typography.bullet} ")
                    withStyle(titleStyle.toSpanStyle().copy(color = secondaryEmphasis)) {
                        append(dayName)
                    }
                }
            }
            Text(text = string)

            val exerciseNames = remember(session.performExercises) {
                session.performExercises.joinToString { it.name }
            }
            Text(
                text = exerciseNames,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 3,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SessionCardPreview() {
    KenkoTheme {
        SessionCard(
            session = Session(
                planId = 1,
                date = LocalDate(2024, 4, 15),
                sets = emptyList(),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun SessionsPreview() {
    KenkoTheme {
        Sessions(
            state = SessionsUiData(listOf(Session(1, emptyList())), false),
            onBackPress = {},
            onSessionClick = {},
            onRemoveSession = {},
            onExportClick = {},
        )
    }
}
