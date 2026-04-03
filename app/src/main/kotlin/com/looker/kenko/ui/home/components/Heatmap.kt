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

package com.looker.kenko.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.looker.kenko.data.model.localDate
import com.looker.kenko.ui.theme.KenkoIcons
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
fun TrainingHeatmap(
    sessionDates: Set<LocalDate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialDate: LocalDate = localDate,
) {
    val pagerState = rememberPagerState(
        initialPage = Int.MAX_VALUE / 2,
        pageCount = { Int.MAX_VALUE }
    )
    val coroutineScope = rememberCoroutineScope()

    val offset = pagerState.currentPage - (Int.MAX_VALUE / 2)
    val displayedDate = remember(initialDate, offset) {
        initialDate.plus(offset, DateTimeUnit.MONTH)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeatmapHeader(
            displayedDate = displayedDate,
            onPreviousMonth = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            },
            onNextMonth = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            },
            onPreviousYear = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 12)
                }
            },
            onNextYear = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 12)
                }
            }
        )

        HorizontalPager(
            state = pagerState,
            key = { it }
        ) { page ->
            val pageOffset = page - (Int.MAX_VALUE / 2)
            val dateForPage = remember(initialDate, pageOffset) {
                initialDate.plus(pageOffset, DateTimeUnit.MONTH)
            }
            HeatmapGrid(
                displayedDate = dateForPage,
                sessionDates = sessionDates
            )
        }
    }
}

@Composable
private fun HeatmapHeader(
    displayedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthLabel = remember(displayedDate) {
        val monthStr = (displayedDate.month.ordinal + 1).toString().padStart(2, '0')
        "${displayedDate.year}-$monthStr"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(onClick = onPreviousYear) {
                Icon(
                    painter = KenkoIcons.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    painter = KenkoIcons.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        Text(
            text = monthLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )

        Row {
            IconButton(onClick = onNextMonth) {
                Icon(
                    painter = KenkoIcons.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onNextYear) {
                Icon(
                    painter = KenkoIcons.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    displayedDate: LocalDate,
    sessionDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val today = localDate
    val gridItems = remember(displayedDate) {
        val firstDayOfMonth = LocalDate(displayedDate.year, displayedDate.month, 1)

        // Find days in current month
        val nextMonth = if ((displayedDate.month.ordinal + 1) == 12) {
            LocalDate(displayedDate.year + 1, 1, 1)
        } else {
            LocalDate(displayedDate.year, (displayedDate.month.ordinal + 1) + 1, 1)
        }
        val daysInMonth = nextMonth.minus(1, DateTimeUnit.DAY).day

        // ISO Day Number: 1 (Mon) to 7 (Sun)
        val padding = firstDayOfMonth.dayOfWeek.isoDayNumber - 1

        val days = (1..daysInMonth).map { LocalDate(displayedDate.year, displayedDate.month, it) }

        // Combine padding (null) and actual days
        List<LocalDate?>(padding) { null } + days
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        gridItems.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { day ->
                    if (day != null) {
                        val isTrained = day in sessionDates
                        val isToday = day == today
                        val color = when {
                            isTrained -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(color)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
