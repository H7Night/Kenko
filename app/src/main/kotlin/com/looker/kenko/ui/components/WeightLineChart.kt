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

package com.looker.kenko.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.data.model.Weight

@Composable
fun WeightLineChart(
    weights: List<Weight>,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        color = labelColor,
        fontSize = 10.sp
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        if (weights.size < 2) return@Canvas

        val maxWeight = weights.maxOf { it.value }
        val minWeight = weights.minOf { it.value }
        val range = (maxWeight - minWeight).coerceAtLeast(1f)
        
        val horizontalPadding = 40f
        val topPadding = 40f
        val bottomPadding = 60f // Increased bottom padding to prevent overlap
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - topPadding - bottomPadding
        
        val stepX = if (weights.size > 1) chartWidth / (weights.size - 1) else 0f
        
        val points = weights.mapIndexed { index, weight ->
            val x = horizontalPadding + index * stepX
            val normalizedY = (weight.value - minWeight) / range
            val y = size.height - bottomPadding - (normalizedY * chartHeight)
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
        
        points.forEachIndexed { index, point ->
            val weight = weights[index]
            
            // Draw point
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = point
            )

            // Draw Y label (Weight)
            val yLabel = "%.1f".format(weight.value)
            val yTextLayout = textMeasurer.measure(yLabel, textStyle)
            drawText(
                textLayoutResult = yTextLayout,
                topLeft = Offset(
                    x = point.x - yTextLayout.size.width / 2,
                    y = point.y - yTextLayout.size.height - 4.dp.toPx()
                )
            )

            // Draw X label (Date MM-DD)
            val xLabel = "%02d-%02d".format(weight.date.monthNumber, weight.date.dayOfMonth)
            val xTextLayout = textMeasurer.measure(xLabel, textStyle)
            drawText(
                textLayoutResult = xTextLayout,
                topLeft = Offset(
                    x = point.x - xTextLayout.size.width / 2,
                    y = size.height - xTextLayout.size.height - 4.dp.toPx()
                )
            )
        }
    }
}
