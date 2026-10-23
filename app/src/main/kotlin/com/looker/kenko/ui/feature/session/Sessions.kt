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

package com.looker.kenko.ui.feature.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.today
import com.looker.kenko.domain.model.titlesMap
import com.looker.kenko.domain.model.TrainingDayMatch
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.ui.component.DeleteIconButton
import com.looker.kenko.ui.component.KenkoDatePickerDialog
import com.looker.kenko.ui.component.TooltipIconButton
import com.looker.kenko.ui.component.EmptyState
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.component.timer.TimerService
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import com.looker.kenko.utils.isToday
import com.looker.kenko.utils.toast
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Timer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.theme.numbers
import androidx.compose.ui.platform.LocalContext

@Composable
fun Sessions(
    viewModel: SessionsViewModel,
    onSessionClick: (LocalDate?) -> Unit,
    onCalendarClick: () -> Unit,
    initialWeekOnly: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAddHistoryDialog by remember { mutableStateOf(false) }

    if (showAddHistoryDialog) {
        AddHistoryDialog(
            availablePlanDays = state.availablePlanDays,
            dayTitles = state.plans.find { it.isActive }?.titlesMap ?: emptyMap(),
            onDismiss = { showAddHistoryDialog = false },
            onConfirm = { date, day ->
                viewModel.addSession(date, day) {
                    onSessionClick(date)
                }
                showAddHistoryDialog = false
            }
        )
    }

    Sessions(
        state = state,
        onSessionClick = onSessionClick,
        onRemoveSession = viewModel::removeSession,
        onAddClick = { showAddHistoryDialog = true },
        onCalendarClick = onCalendarClick,
        initialWeekOnly = initialWeekOnly,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sessions(
    state: SessionsUiData,
    onSessionClick: (LocalDate?) -> Unit,
    onRemoveSession: (SessionSummary) -> Unit,
    onAddClick: () -> Unit,
    onCalendarClick: () -> Unit,
    initialWeekOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var sessionToDelete by remember { mutableStateOf<SessionSummary?>(null) }
    var planExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var weekOnly by rememberSaveable(initialWeekOnly) { mutableStateOf(initialWeekOnly) }
    val context = LocalContext.current

    val selectedPlanName = selectedPlan?.name ?: stringResource(R.string.label_all_plans)
    val selectedDayName = selectedDay?.let { day -> selectedPlan?.titlesMap?.get(day) ?: stringResource(R.string.label_day_n, day) } ?: stringResource(R.string.label_select_day)
    val availableDays: Map<Int, String> = remember(selectedPlan) {
        selectedPlan?.titlesMap ?: emptyMap()
    }
    val currentDate = today()
    val weekStart = currentDate.minus(currentDate.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    val filteredSessions = remember(state.sessions, selectedPlan, selectedDay, weekOnly, currentDate) {
        val planId = selectedPlan?.id
        state.sessions.filter { session ->
            val planMatch = planId == null || session.planId == planId
            val dayMatch = selectedDay == null || session.dayIndexOverride == selectedDay
            val weekMatch = !weekOnly || (session.date >= weekStart && session.date <= currentDate)
            planMatch && dayMatch && weekMatch
        }
    }

    if (sessionToDelete != null) {
        val deletedMessage = stringResource(R.string.label_deleted)
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(text = stringResource(R.string.label_delete_session_title)) },
            text = { Text(text = stringResource(R.string.label_delete_session_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToDelete?.let { onRemoveSession(it) }
                        context.toast(deletedMessage)
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
                title = {
                    Text(text = stringResource(id = R.string.label_records))
                },
                actions = {
                    TooltipIconButton(
                        tooltip = stringResource(R.string.label_calendar),
                        onClick = onCalendarClick,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = stringResource(R.string.label_calendar),
                        )
                    }
                    TooltipIconButton(
                        tooltip = stringResource(R.string.label_add_history),
                        onClick = onAddClick,
                    ) {
                        Icon(
                            painter = KenkoIcons.Add,
                            contentDescription = stringResource(R.string.label_add_history)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        // 页面位于外层 Scaffold(底部导航栏)内,外层已通过 innerPadding 处理
        // 系统导航条避让;禁用内层 Scaffold 的 systemBars insets,
        // 避免底部重复避让产生额外空白。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (!state.hasAnySessions) {
            EmptyState(
                icon = Icons.Rounded.History,
                text = stringResource(id = R.string.label_no_sessions),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        FilterChip(
                            selected = selectedPlan != null,
                            onClick = { planExpanded = true },
                            label = { Text(selectedPlanName) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = planExpanded,
                            onDismissRequest = { planExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_all_plans)) },
                                onClick = {
                                    selectedPlan = null
                                    selectedDay = null
                                    planExpanded = false
                                },
                            )
                            state.plans.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.name) },
                                    onClick = {
                                        selectedPlan = plan
                                        selectedDay = null
                                        planExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    FilterChip(
                        selected = weekOnly,
                        onClick = { weekOnly = !weekOnly },
                        label = { Text(stringResource(R.string.label_stat_week)) },
                    )
                    if (selectedPlan != null) {
                        Box {
                            FilterChip(
                                selected = selectedDay != null,
                                onClick = { dayExpanded = true },
                                label = { Text(selectedDayName) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                    )
                                },
                            )
                            DropdownMenu(
                                expanded = dayExpanded,
                                onDismissRequest = { dayExpanded = false },
                            ) {
                                availableDays.forEach { (day, title) ->
                                    DropdownMenuItem(
                                        text = { Text(title) },
                                        onClick = {
                                            selectedDay = day
                                            dayExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyState(
                                icon = Icons.Rounded.History,
                                text = stringResource(R.string.label_no_sessions),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            )
                            if (selectedPlan != null || selectedDay != null || weekOnly) {
                                TextButton(
                                    onClick = {
                                        selectedPlan = null
                                        selectedDay = null
                                        weekOnly = false
                                    },
                                ) {
                                    Text(stringResource(R.string.label_clear_filters))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(
                            items = filteredSessions,
                            key = { it.id ?: it.hashCode() },
                        ) { session ->
                            SessionCard(
                                modifier = Modifier.animateItem(),
                                session = session,
                                onClick = { onSessionClick(session.date) },
                                dayTitles = state.dayTitles,
                                planDayExerciseNames = state.planDayExerciseNames,
                                onDelete = { sessionToDelete = session },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHistoryDialog(
    availablePlanDays: Map<Int, List<Exercise>>,
    dayTitles: Map<Int, String>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Int) -> Unit,
) {
    var date by remember { mutableStateOf<LocalDate>(today()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(availablePlanDays) {
        selectedDay = availablePlanDays.keys.firstOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.label_add_to_history)) },
        text = {
            if (availablePlanDays.isEmpty()) {
                Text(
                    text = stringResource(R.string.error_no_active_plan_for_history),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DateSelectionRow(
                        label = stringResource(R.string.label_select_date),
                        date = date,
                        onDateSelected = { date = it }
                    )
                    
                    Text(
                        text = stringResource(R.string.label_select_train_day),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        availablePlanDays.toSortedMap().forEach { (day, _) ->
                            item {
                                val title = dayTitles[day]
                                val isSelected = selectedDay == day
                                val text = if (title.isNullOrBlank()) stringResource(R.string.label_day_n, day) else title
                                val colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { selectedDay = day },
                                    shape = MaterialTheme.shapes.large,
                                    colors = colors,
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDay?.let { onConfirm(date, it) }
                },
                enabled = selectedDay != null && availablePlanDays.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.label_confirm))
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
    var showDatePicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { showDatePicker = true }) {
            Text(
                text = formatDate(date, DateFormat.YearMonthDay),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (showDatePicker) {
        KenkoDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { selected ->
                selected?.let(onDateSelected)
                showDatePicker = false
            },
        )
    }
}

@Composable
fun SessionCard(
    session: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    dayTitles: Map<Int?, Map<Int, String>> = emptyMap(),
    planDayExerciseNames: Map<Int, Map<Int, kotlin.collections.Set<String>>> = emptyMap(),
    onDelete: (() -> Unit)? = null,
) {
    val isToday = session.date.isToday
    Surface(
        modifier = modifier,
        color = if (isToday) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
            val titleStyle = MaterialTheme.typography.titleSmall
            val secondaryEmphasis = MaterialTheme.colorScheme.onSurfaceVariant
            val effectiveDay = session.dayIndexOverride
            // 无 dayIndexOverride 的老记录:按动作名反查所属训练日,还原训练日名称
            val inferredDay = effectiveDay ?: session.planId?.let { planId ->
                TrainingDayMatch.matchDayIndex(
                    session.exerciseNames.map { it.trim() }.toSet(),
                    planDayExerciseNames[planId] ?: emptyMap(),
                )
            }
            val day = effectiveDay ?: inferredDay
            val dayTitle = dayTitles[session.planId]?.get(day)
            // 优先显示训练日名称快照(计划修改后历史记录保持不变)
            val displayName = session.dayTitleOverride
                ?: dayTitle
                ?: day?.let { stringResource(R.string.label_day_n, it) }
                ?: ""
            val string = remember(session.date, displayName) {
                buildAnnotatedString {
                    withStyle(titleStyle.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                        append(formatDate(session.date, dateTimeFormat = DateFormat.YearMonthDay))
                    }
                    // 训练名称缺失(如老记录反查失败)时不残留孤立的 " • " 分隔符
                    if (displayName.isNotBlank()) {
                        append(" ${Typography.bullet} ")
                        withStyle(titleStyle.toSpanStyle().copy(color = secondaryEmphasis)) {
                            append(displayName)
                        }
                    }
                }
            }
            Text(text = string)

            // Duration — mono, subtle
            if (session.durationSeconds != null && session.durationSeconds > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = TimerService.formatTime(session.durationSeconds),
                        style = MaterialTheme.typography.labelMedium.numbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val exerciseNames = remember(session.exerciseNames) {
                session.exerciseNames.joinToString { it }
            }
            Text(
                text = exerciseNames,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
            }
            if (onDelete != null) {
                DeleteIconButton(
                    onClick = onDelete,
                    iconSize = 18.dp,
                    tintAlpha = 0.7f,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SessionCardPreview() {
    KenkoTheme {
        SessionCard(
            session = SessionSummary(
                date = LocalDate(2024, 4, 15),
                planId = 1,
                exerciseNames = listOf("Bench Press", "Curls"),
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
            state = SessionsUiData(
                sessions = listOf(
                    SessionSummary(
                        date = LocalDate(2024, 4, 15),
                        planId = 1,
                        exerciseNames = listOf("Bench Press"),
                    )
                ),
                isCurrentSessionActive = false,
                hasAnySessions = true,
            ),
            onSessionClick = {},
            onRemoveSession = {},
            onAddClick = {},
            onCalendarClick = {},
        )
    }
}
