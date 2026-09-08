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

package com.looker.kenko.ui.feature.statistics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.theme.KenkoTheme

@Composable
fun TrendCard(
    weeklyTrend: List<Int>,
    modifier: Modifier = Modifier,
) {
    val max = maxOf(1, weeklyTrend.maxOrNull() ?: 1)
    val total = weeklyTrend.sum()
    val isEmpty = weeklyTrend.isEmpty() || weeklyTrend.all { it == 0 }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "近12周趋势",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "每周训练次数",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isEmpty && weeklyTrend.isNotEmpty()) {
                // All zeros: still draw flat grid with placeholder label
                TrendSparkline(
                    weeklyTrend = weeklyTrend,
                    max = max,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
                Text(
                    text = "暂无记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else if (weeklyTrend.isEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    val outlineVariant = androidx.compose.ui.graphics.Color(0xFFE5E7EB)
                    // minimal grey grid placeholder
                    val stroke = 1.dp.toPx()
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = size.height * i / gridLines
                        drawLine(
                            color = outlineVariant,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke,
                        )
                    }
                }
                Text(
                    text = "暂无记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                TrendSparkline(
                    weeklyTrend = weeklyTrend,
                    max = max,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "总计 $total 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "最高 $max 次/周",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendSparkline(
    weeklyTrend: List<Int>,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        // outlineVariant grid — horizontal lines
        val gridStroke = 1.dp.toPx()
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = size.height * i / gridLines
            drawLine(
                color = outlineVariant,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = gridStroke,
            )
        }
        // vertical grid (lighter)
        val verticalCount = weeklyTrend.size.coerceAtLeast(2)
        for (i in 0 until verticalCount) {
            val x = size.width * i / (verticalCount - 1)
            drawLine(
                color = outlineVariant.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = gridStroke,
            )
        }

        if (weeklyTrend.size < 2) {
            if (weeklyTrend.size == 1) {
                val y = size.height - (weeklyTrend[0].toFloat() / max) * size.height
                drawCircle(
                    color = primary,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2, y),
                )
            }
            return@Canvas
        }

        val path = Path().apply {
            weeklyTrend.forEachIndexed { idx, value ->
                val x = size.width * idx / (weeklyTrend.size - 1)
                val y = size.height - (value.toFloat() / max) * size.height
                if (idx == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = primary,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        // dots at each point
        weeklyTrend.forEachIndexed { idx, value ->
            val x = size.width * idx / (weeklyTrend.size - 1)
            val y = size.height - (value.toFloat() / max) * size.height
            drawCircle(
                color = primary,
                radius = 3.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White,
                radius = 1.2.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardPreviewVaried() {
    KenkoTheme {
        TrendCard(
            weeklyTrend = listOf(2, 3, 1, 4, 2, 5, 3, 3, 4, 2, 6, 4),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardPreviewFlat() {
    KenkoTheme {
        TrendCard(
            weeklyTrend = List(12) { 0 },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardPreviewEmpty() {
    KenkoTheme {
        TrendCard(
            weeklyTrend = emptyList(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardPreviewSinglePeak() {
    KenkoTheme {
        TrendCard(
            weeklyTrend = listOf(0, 0, 0, 1, 0, 0, 3, 0, 0, 0, 0, 0),
        )
    }
}
