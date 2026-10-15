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

package com.looker.kenko.ui.feature.exercise.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Weight
import com.looker.kenko.domain.statistics.ExerciseMetric
import com.looker.kenko.domain.statistics.ExerciseSessionEntry
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.EmptyState
import com.looker.kenko.ui.component.KenkoBorder
import com.looker.kenko.ui.component.WeightLineChart
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    viewModel: ExerciseProgressViewModel,
    onBackPress: () -> Unit,
    onSessionClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = {
                    Column {
                        Text(
                            text = state.exercise?.name ?: stringResource(R.string.label_exercise_progress),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.label_exercise_progress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when {
            state.isLoading -> Unit
            state.entries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(
                        icon = Icons.Rounded.Insights,
                        text = stringResource(R.string.label_progress_no_data),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        MetricSelector(
                            selected = state.metric,
                            onSelect = viewModel::setMetric,
                        )
                    }
                    item {
                        ProgressChartCard(
                            points = state.points,
                            metric = state.metric,
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.label_progress_history),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                    items(
                        items = state.entries.reversed(),
                        key = { it.date.toString() },
                    ) { entry ->
                        SessionEntryCard(
                            entry = entry,
                            onClick = { onSessionClick(entry.date) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSelector(
    selected: ExerciseMetric,
    onSelect: (ExerciseMetric) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ExerciseMetric.entries.forEach { metric ->
            val isSelected = metric == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(metric) },
                label = { Text(stringResource(metric.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                } else {
                    KenkoBorder
                },
            )
        }
    }
}

@Composable
private fun ProgressChartCard(
    points: List<com.looker.kenko.domain.statistics.ExerciseProgressPoint>,
    metric: ExerciseMetric,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(metric.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (points.size < 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.label_progress_no_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                WeightLineChart(
                    weights = points.map { Weight(date = it.date, value = it.value) },
                    modifier = Modifier.height(180.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionEntryCard(
    entry: ExerciseSessionEntry,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(entry.date, DateFormat.YearMonthDay),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.label_best_set_value,
                        formatWeight(entry.bestSetWeight),
                        entry.bestSetReps,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        R.string.label_estimated_1rm_value,
                        formatWeight(entry.estimatedOneRepMax),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.label_volume_value,
                        formatWeight(entry.volume),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val ExerciseMetric.labelRes: Int
    @StringRes
    get() = when (this) {
        ExerciseMetric.HEAVIEST_WEIGHT -> R.string.label_metric_heaviest_weight
        ExerciseMetric.ESTIMATED_1RM -> R.string.label_metric_estimated_1rm
        ExerciseMetric.BEST_SET -> R.string.label_metric_best_set
        ExerciseMetric.VOLUME -> R.string.label_metric_volume
    }

private fun formatWeight(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
