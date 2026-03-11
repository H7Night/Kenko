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
import androidx.compose.ui.unit.dp
import com.looker.kenko.data.model.Weight

@Composable
fun WeightLineChart(
    weights: List<Weight>,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (weights.size < 2) return@Canvas

        val maxWeight = weights.maxOf { it.value }
        val minWeight = weights.minOf { it.value }
        val range = (maxWeight - minWeight).coerceAtLeast(1f)
        
        val padding = 20f
        val chartWidth = size.width
        val chartHeight = size.height
        
        val stepX = chartWidth / (weights.size - 1)
        
        val points = weights.mapIndexed { index, weight ->
            val x = index * stepX
            val normalizedY = (weight.value - minWeight) / range
            val y = chartHeight - (normalizedY * chartHeight)
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
            style = Stroke(width = 3.dp.toPx())
        )
        
        points.forEach { point ->
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}
