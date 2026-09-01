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
import androidx.compose.ui.geometry.Rect
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
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        color = labelColor,
        fontSize = 9.sp,
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        if (weights.size < 2) return@Canvas

        val leftPad = 34f
        val rightPad = 8f
        val topPad = 24f
        val bottomPad = 28f
        val axisLabelRects = mutableListOf<Rect>()
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
            val layout = textMeasurer.measure("%.2f".format(value), textStyle)
            val yLabelLeft = leftPad - layout.size.width - 4f
            val yLabelTop = y - layout.size.height / 2f
            axisLabelRects.add(
                Rect(yLabelLeft, yLabelTop, yLabelLeft + layout.size.width, yLabelTop + layout.size.height)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(yLabelLeft, yLabelTop),
            )
        }

        // 数据点坐标
        val stepX = if (weights.size > 1) chartWidth / (weights.size - 1) else 0f
        val points = weights.mapIndexed { index, weight ->
            Offset(leftPad + index * stepX, yFor(weight.value))
        }
        val bottomY = topPad + chartHeight

        // 细线面积 — Linear: 极淡填充 0.06, hairline 1.25dp
        val areaPath = Path().apply {
            moveTo(points.first().x, bottomY)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, bottomY)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.08f), color.copy(alpha = 0f)),
                startY = topPad,
                endY = bottomY,
            ),
        )

        // 折线 — hairline
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round),
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
                text = "%02d-%02d".format(weights[index].date.monthNumber, weights[index].date.day),
                style = textStyle,
            )
            val xLeft = points[index].x - layout.size.width / 2f
            val xTop = bottomY + 4f
            axisLabelRects.add(
                Rect(xLeft, xTop, xLeft + layout.size.width, xTop + layout.size.height)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x = xLeft, y = xTop),
            )
        }

        // 数据点 — Linear 缩小 2.5/2dp 细点 + 轴标签避让
        val labelAll = weights.size <= 10
        var lastLabelBottom = Float.NEGATIVE_INFINITY
        val labelPadding = 2.dp.toPx()
        points.forEachIndexed { index, point ->
            val isEdge = index == 0 || index == points.lastIndex
            val radius = if (isEdge) 2.5.dp.toPx() else 2.dp.toPx()
            if (isEdge) {
                drawCircle(
                    color = surfaceColor,
                    radius = radius + 1.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawCircle(color = color, radius = radius, center = point)

            if (labelAll || isEdge) {
                val layout = textMeasurer.measure("%.2f".format(weights[index].value), textStyle)
                // 水平居中但限制在图表区内
                val rawLabelX = point.x - layout.size.width / 2f
                val labelX = rawLabelX.coerceIn(leftPad + 2f, size.width - rightPad - layout.size.width - 2f)
                val aboveY = point.y - layout.size.height - 6f
                val belowY = point.y + 6f
                // 顶部/底部边界检查
                val clampedAbove = aboveY.coerceAtLeast(topPad - layout.size.height - 2f)
                val clampedBelow = belowY.coerceAtMost(bottomY - layout.size.height - 4f)
                val overlapsPrev = clampedAbove < lastLabelBottom + 2f
                var labelY = if (overlapsPrev) clampedBelow else clampedAbove
                // 与坐标轴标签碰撞检查：若上方位置与轴标签相交，尝试下方；若均相交则跳过该标签
                fun intersectsAxis(rect: Rect): Boolean {
                    return axisLabelRects.any { axis ->
                        val expanded = Rect(axis.left - 2f, axis.top - 2f, axis.right + 2f, axis.bottom + 2f)
                        rect.overlaps(expanded)
                    }
                }
                val aboveRect = Rect(labelX - labelPadding, labelY, labelX + layout.size.width + labelPadding, labelY + layout.size.height)
                val belowRect = Rect(labelX - labelPadding, clampedBelow, labelX + layout.size.width + labelPadding, clampedBelow + layout.size.height)
                if (intersectsAxis(aboveRect)) {
                    if (!intersectsAxis(belowRect) && clampedBelow > lastLabelBottom + 2f) {
                        labelY = clampedBelow
                    } else {
                        // 两侧均与轴标签重叠，跳过该数据标签以保证坐标可读
                        return@forEachIndexed
                    }
                }
                // 若下方仍与前一标签重叠，也跳过
                if (labelY < lastLabelBottom + 2f && labelY == clampedBelow) {
                    // 尝试跳过而非重叠
                    return@forEachIndexed
                }
                // 绘制标签背景，避免折线/网格穿透
                val bgRect = Rect(labelX - labelPadding, labelY - 1f, labelX + layout.size.width + labelPadding, labelY + layout.size.height + 1f)
                drawRect(
                    color = surfaceColor,
                    topLeft = Offset(bgRect.left, bgRect.top),
                    size = androidx.compose.ui.geometry.Size(bgRect.width, bgRect.height),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelX, labelY),
                )
                lastLabelBottom = labelY + layout.size.height
            }
        }
    }
}
