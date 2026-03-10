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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.looker.kenko.data.model.localDate
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
) {
    val today = localDate
    val gridItems = remember(today, sessionDates) {
        val firstDayOfMonth = LocalDate(today.year, today.month, 1)
        
        // Find days in current month
        val nextMonth = if (today.monthNumber == 12) {
            LocalDate(today.year + 1, 1, 1)
        } else {
            LocalDate(today.year, today.monthNumber + 1, 1)
        }
        val daysInMonth = nextMonth.minus(1, DateTimeUnit.DAY).day
        
        // ISO Day Number: 1 (Mon) to 7 (Sun)
        val padding = firstDayOfMonth.dayOfWeek.isoDayNumber - 1
        
        val days = (1..daysInMonth).map { LocalDate(today.year, today.month, it) }
        
        // Combine padding (null) and actual days
        List<LocalDate?>(padding) { null } + days
    }

    val monthLabel = remember(today) {
        val monthStr = today.monthNumber.toString().padStart(2, '0')
        "${today.year}-$monthStr"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )

        Column(
            modifier = Modifier.widthIn(max = 280.dp), // Limit width to make it smaller
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            gridItems.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        if (day != null) {
                            val isTrained = day in sessionDates
                            val isToday = day == today
                            val color = when {
                                isTrained -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
}
