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

package com.looker.kenko.ui.feature.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R
import com.looker.kenko.domain.statistics.buildHeatmapData
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Composable
fun HeatmapCard(
    sessionDates: Set<LocalDate>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    countByDate: Map<LocalDate, Int> = emptyMap(),
) {
    val monthNames = stringArrayResource(R.array.month_short).toList()
    val data = remember(sessionDates, countByDate, today, monthNames) {
        buildHeatmapDisplayData(sessionDates, countByDate, today, monthNames)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Simple header for 90d window — no year navigation
            Text(
                text = stringResource(R.string.label_heatmap_activity),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Last 120 days, fitted to the card width (no horizontal scroll)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                HeatmapGrid(
                    data = data,
                )
            }
        }
    }
}

@Composable
fun HeatmapCard(
    sessionDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    countByDate: Map<LocalDate, Int> = emptyMap(),
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    HeatmapCard(
        sessionDates = sessionDates,
        today = today,
        modifier = modifier,
        countByDate = countByDate
    )
}

/**
 * Deprecated alias kept for backward compatibility — delegates to [HeatmapCard] with 90d window.
 */
@Deprecated("Use HeatmapCard instead", ReplaceWith("HeatmapCard(sessionDates, today, modifier, countByDate)"))
@Composable
fun YearHeatmap(
    sessionDates: Set<LocalDate>,
    countByDate: Map<LocalDate, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    initialYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.year,
    onYearChange: (Int) -> Unit = {},
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    HeatmapCard(
        sessionDates = sessionDates,
        today = today,
        modifier = modifier,
        countByDate = countByDate
    )
}

@Composable
private fun HeatmapGrid(
    data: HeatmapDisplayData,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val futureColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val levels = listOf(
        surfaceColor,
        primary.copy(alpha = 0.25f),
        primary.copy(alpha = 0.5f),
        primary.copy(alpha = 0.75f),
        primary
    )
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val cellSize = 13.dp
    val cellGap = 3.dp
    val labelWidth = 22.dp
    val weekdays = stringArrayResource(R.array.day_of_week_short)

    Column(modifier = modifier) {
        // Month labels on top
        Box(modifier = Modifier.padding(start = labelWidth)) {
            data.monthLabels.forEach { (weekIndex, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = (cellSize + cellGap) * weekIndex)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            // Weekday labels on left (Mon/Wed/Fri = indices 0/2/4)
            Column(
                modifier = Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                listOf(0, 2, 4).forEach { idx ->
                    Box(modifier = Modifier.size(cellSize)) {
                        Text(
                            text = weekdays[idx],
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            // Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                data.weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        week.days.forEach { day ->
                            val level = when {
                                day.isFuture -> 5
                                day.count == 0 -> 0
                                data.maxCount == 0 -> 0
                                else -> {
                                    val ratio = day.count.toFloat() / data.maxCount
                                    when {
                                        ratio <= 0.25f -> 1
                                        ratio <= 0.5f -> 2
                                        ratio <= 0.75f -> 3
                                        else -> 4
                                    }
                                }
                            }
                            val cellColor = when (level) {
                                5 -> futureColor
                                0 -> levels[0]
                                else -> levels[level]
                            }
                            val alpha = when (level) {
                                0, 5 -> 1f
                                else -> animatedProgress.value
                            }
                            val border = if (level == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .then(if (border != null) Modifier.border(border, MaterialTheme.shapes.extraSmall) else Modifier)
                                    .background(cellColor.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HeatmapDay(
    val date: LocalDate,
    val count: Int,
    val isFuture: Boolean,
)

private data class HeatmapWeekDisplay(
    val days: List<HeatmapDay>,
)

private data class HeatmapDisplayData(
    val weeks: List<HeatmapWeekDisplay>,
    val monthLabels: List<Pair<Int, String>>,
    val maxCount: Int,
)

private fun buildHeatmapDisplayData(
    sessionDates: Set<LocalDate>,
    countByDate: Map<LocalDate, Int>,
    today: LocalDate,
    monthNames: List<String>,
): HeatmapDisplayData {
    // 默认显示最近 120 天（不横向滚动，尽量在同一屏内完整展示）。
    val from = today.minus(119, DateTimeUnit.DAY)
    val raw = buildHeatmapData(from, today)
    val monthLabels = mutableListOf<Pair<Int, String>>()
    var maxCount = 0

    val weeks = raw.weeks.mapIndexed { wIndex, week ->
        // 月份标注：含某月 1 号的周标月份；跨年时用年份标注。
        for (d in week.days) {
            val date = d ?: continue
            if (date.day == 1) {
                val label = if (date.monthNumber == 1) date.year.toString() else monthNames[date.month.ordinal]
                monthLabels.add(wIndex to label)
                break
            }
        }

        val days = week.days.map { date ->
            val d = requireNotNull(date)
            val count = countByDate[d] ?: if (d in sessionDates) 1 else 0
            if (count > maxCount) maxCount = count
            HeatmapDay(d, count, isFuture = d > today)
        }
        HeatmapWeekDisplay(days)
    }

    // 若窗口内没有任何 1 号，则至少给首周一个月份标签。
    if (monthLabels.isEmpty() && weeks.isNotEmpty()) {
        val firstDate = weeks.first().days.first().date
        monthLabels.add(0 to monthNames[firstDate.month.ordinal])
    }

    return HeatmapDisplayData(
        weeks = weeks,
        monthLabels = monthLabels,
        maxCount = maxCount,
    )
}
