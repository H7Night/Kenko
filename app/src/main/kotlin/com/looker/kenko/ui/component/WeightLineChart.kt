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

package com.looker.kenko.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.domain.model.Weight

/**
 * 渐变面积折线图：折线 + 半透明渐变填充 + 水平网格 + 坐标刻度。
 * 数据 ≤10 条时每个点标注数值（自动防重叠），>10 条时仅标注首尾点。
 */
@Composable
fun WeightLineChart(
    weights: List<Weight>,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        color = labelColor,
        fontSize = 8.sp,
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        if (weights.size < 2) return@Canvas

        val leftPad = 34f
        val rightPad = 8f
        val topPad = 18f
        val bottomPad = 22f
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        val rawMin = weights.minOf { it.value }
        val rawMax = weights.maxOf { it.value }
        val pad = (rawMax - rawMin).coerceAtLeast(1f) * 0.1f
        val yMin = rawMin - pad
        val yMax = rawMax + pad
        val yRange = (yMax - yMin).coerceAtLeast(1f)

        fun yFor(value: Float): Float =
            topPad + (1f - (value - yMin) / yRange) * chartHeight

        // 水平网格 + Y 轴刻度
        val gridLines = 4
        for (i in 0 until gridLines) {
            val t = i.toFloat() / (gridLines - 1)
            val value = yMin + t * yRange
            val y = yFor(value)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1f,
            )
            val layout = textMeasurer.measure("%.1f".format(value), textStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(leftPad - layout.size.width - 4f, y - layout.size.height / 2f),
            )
        }

        // 数据点坐标
        val stepX = if (weights.size > 1) chartWidth / (weights.size - 1) else 0f
        val points = weights.mapIndexed { index, weight ->
            Offset(leftPad + index * stepX, yFor(weight.value))
        }
        val bottomY = topPad + chartHeight

        // 渐变面积
        val areaPath = Path().apply {
            moveTo(points.first().x, bottomY)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, bottomY)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f)),
                startY = topPad,
                endY = bottomY,
            ),
        )

        // 折线
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // X 轴日期刻度（含首尾，3-5 个避免拥挤）
        val xLabelIndices = when {
            weights.size <= 4 -> (0 until weights.size).toList()
            else -> {
                val n = weights.size
                listOf(0, n / 3, (2 * n) / 3, n - 1).distinct()
            }
        }
        xLabelIndices.forEach { index ->
            val layout = textMeasurer.measure(
                text = "%02d-%02d".format(weights[index].date.month, weights[index].date.day),
                style = textStyle,
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = points[index].x - layout.size.width / 2f,
                    y = bottomY + 4f,
                ),
            )
        }

        // 数据点 + 数值标签（≤10 条全标并防重叠，否则仅首尾）
        val labelAll = weights.size <= 10
        var lastLabelBottom = Float.NEGATIVE_INFINITY
        points.forEachIndexed { index, point ->
            val isEdge = index == 0 || index == points.lastIndex
            val radius = if (isEdge) 4.dp.toPx() else 3.dp.toPx()
            if (isEdge) {
                drawCircle(
                    color = surfaceColor,
                    radius = radius + 1.5.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            drawCircle(color = color, radius = radius, center = point)

            if (labelAll || isEdge) {
                val layout = textMeasurer.measure("%.1f".format(weights[index].value), textStyle)
                val aboveY = point.y - layout.size.height - 4f
                val overlaps = aboveY < lastLabelBottom + 2f
                val labelY = if (overlaps) point.y + 4f else aboveY
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(point.x - layout.size.width / 2f, labelY),
                )
                lastLabelBottom = if (overlaps) labelY + layout.size.height else aboveY
            }
        }
    }
}
