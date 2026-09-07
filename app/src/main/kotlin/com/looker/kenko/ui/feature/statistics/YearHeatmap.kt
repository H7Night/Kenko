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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R
import com.looker.kenko.ui.theme.KenkoIcons
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun YearHeatmap(
    sessionDates: Set<LocalDate>,
    countByDate: Map<LocalDate, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    initialYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.year,
    onYearChange: (Int) -> Unit = {},
) {
    var displayYear by remember(initialYear) { mutableStateOf(initialYear) }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val yearData = remember(sessionDates, displayYear) {
        buildYearData(displayYear, sessionDates, countByDate)
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
            YearHeatmapHeader(
                year = displayYear,
                onPreviousYear = { displayYear -= 1; onYearChange(displayYear) },
                onNextYear = { displayYear += 1; onYearChange(displayYear) },
            )

            // Horizontal scrollable grid
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp),
            ) {
                YearHeatmapGrid(
                    data = yearData,
                    today = today,
                    displayYear = displayYear,
                )
            }

            // Legend row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_less),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                val primary = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
                listOf(
                    surfaceColor,
                    primary.copy(alpha = 0.25f),
                    primary.copy(alpha = 0.5f),
                    primary.copy(alpha = 0.75f),
                    primary
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(11.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color)
                            .then(
                                if (color == surfaceColor) Modifier.border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    MaterialTheme.shapes.extraSmall
                                ) else Modifier
                            )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_more),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary row
            val activeDays = yearData.activeDays
            val totalDays = yearData.totalDays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.label_active_days, activeDays, totalDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (yearData.maxCount > 0) {
                    Text(
                        text = stringResource(R.string.label_max_per_day, yearData.maxCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun YearHeatmapHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousYear) {
            Icon(
                painter = KenkoIcons.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onNextYear) {
            Icon(
                painter = KenkoIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun YearHeatmapGrid(
    data: YearHeatmapData,
    today: LocalDate,
    displayYear: Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val futureColor = MaterialTheme.colorScheme.surfaceContainerHigh
    // levels for intensity (binary for now, but support graded if countByDate provides >1)
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

    Column(modifier = modifier) {
        // Month labels on top
        Box(modifier = Modifier.padding(start = labelWidth)) {
            data.monthLabels.forEach { (weekIndex, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = (cellSize + cellGap) * weekIndex)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            // Weekday labels on left
            Column(
                modifier = Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                listOf("Mon", "", "Wed", "", "Fri", "", "").forEach { label ->
                    Box(modifier = Modifier.size(cellSize)) {
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
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
                                day == null -> 5 // empty padding
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
                            if (day == null) {
                                Box(modifier = Modifier.size(cellSize))
                            } else {
                                val cellColor = when (level) {
                                    5 -> if (day.isFuture) futureColor else surfaceColor
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
}

private data class YearDay(
    val date: LocalDate,
    val count: Int,
    val isFuture: Boolean,
)

private data class YearWeek(
    val days: List<YearDay?>,
)

private data class YearHeatmapData(
    val weeks: List<YearWeek>,
    val monthLabels: List<Pair<Int, String>>,
    val maxCount: Int,
    val activeDays: Int,
    val totalDays: Int,
)

private fun buildYearData(
    year: Int,
    sessionDates: Set<LocalDate>,
    countByDate: Map<LocalDate, Int>,
): YearHeatmapData {
    val jan1 = LocalDate(year, 1, 1)
    val dec31 = LocalDate(year, 12, 31)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Monday of week containing Jan 1
    val jan1Dow = jan1.dayOfWeek.isoDayNumber // 1 Mon .. 7 Sun
    val startMonday = jan1.minus(jan1Dow - 1, DateTimeUnit.DAY)
    // Sunday of week containing Dec 31
    val dec31Dow = dec31.dayOfWeek.isoDayNumber
    val endSunday = dec31.plus(7 - dec31Dow, DateTimeUnit.DAY)

    val totalDaysBetween = daysBetween(startMonday, endSunday) + 1
    val weekCount = totalDaysBetween / 7

    val weeks = mutableListOf<YearWeek>()
    val monthLabels = mutableListOf<Pair<Int, String>>()
    var lastMonth = -1
    var activeDays = 0
    var totalDays = 0
    var maxCount = 0

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    for (w in 0 until weekCount) {
        val weekStart = startMonday.plus(w * 7, DateTimeUnit.DAY)
        // month label if this week contains the 1st of a month
        for (d in 0 until 7) {
            val date = weekStart.plus(d, DateTimeUnit.DAY)
            if (date.day == 1 && date.year == year) {
                monthLabels.add(w to monthNames[date.month.ordinal])
                break
            }
        }
        // fallback: if first week, always add Jan
        if (w == 0 && monthLabels.isEmpty()) {
            monthLabels.add(0 to monthNames[0])
        }

        val days = (0 until 7).map { d ->
            val date = weekStart.plus(d, DateTimeUnit.DAY)
            if (date.year != year) {
                null
            } else {
                val count = countByDate[date] ?: if (date in sessionDates) 1 else 0
                if (count > maxCount) maxCount = count
                val isFuture = date > today
                if (!isFuture) {
                    totalDays++
                    if (count > 0) activeDays++
                }
                YearDay(date, count, isFuture)
            }
        }
        weeks.add(YearWeek(days))
    }

    // Ensure Jan label exists if not added
    if (weeks.isNotEmpty() && monthLabels.none { it.second == "Jan" }) {
        // find first week with Jan 1
    }

    return YearHeatmapData(
        weeks = weeks,
        monthLabels = monthLabels,
        maxCount = maxCount,
        activeDays = activeDays,
        totalDays = totalDays,
    )
}

private fun daysBetween(start: LocalDate, end: LocalDate): Int {
    var count = 0
    var cur = start
    while (cur < end) {
        cur = cur.plus(1, DateTimeUnit.DAY)
        count++
    }
    return count
}
