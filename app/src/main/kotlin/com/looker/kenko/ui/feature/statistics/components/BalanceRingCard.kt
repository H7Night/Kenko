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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.theme.KenkoTheme
import kotlin.math.cos
import kotlin.math.sin

private val BalanceOrder = listOf("胸", "背", "腿", "手臂", "肩", "核心", "有氧")

private val BalanceColors = mapOf(
    "胸" to Color(0xFF5E6AD2),
    "背" to Color(0xFF10B981),
    "腿" to Color(0xFFF59E0B),
    "手臂" to Color(0xFFEF4444),
    "肩" to Color(0xFF8B5CF6),
    "核心" to Color(0xFF06B6D4),
    "有氧" to Color(0xFF9CA3AF),
)

private const val WeakThreshold = 0.08f

@Composable
fun BalanceRingCard(
    monthlyCounts: Map<String, Int>,
    cardioMonthly: Int,
    modifier: Modifier = Modifier,
) {
    val values = BalanceOrder.associateWith { part ->
        if (part == "有氧") cardioMonthly else monthlyCounts[part] ?: 0
    }
    val total = values.values.sum()
    val weakParts = if (total == 0) emptySet()
    else values.filter { (_, v) -> v.toFloat() / total < WeakThreshold }.keys

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
                text = "本月平衡",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "占比 <8% 为薄弱",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (total == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Grey placeholder ring
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val stroke = 16.dp.toPx()
                            drawArc(
                                color = Color(0xFFE5E7EB),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke),
                                size = Size(size.width - stroke, size.height - stroke),
                                topLeft = Offset(stroke / 2, stroke / 2),
                            )
                        }
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Donut canvas
                    val errorColor = MaterialTheme.colorScheme.error
                    Canvas(modifier = Modifier.size(128.dp)) {
                        val stroke = 18.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                        val arcSize = Size(diameter, diameter)
                        var startAngle = -90f
                        // Draw slices
                        for (part in BalanceOrder) {
                            val v = values[part] ?: 0
                            if (v == 0) continue
                            val sweep = 360f * v.toFloat() / total
                            val color = BalanceColors[part] ?: Color.Gray
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = stroke),
                                topLeft = topLeft,
                                size = arcSize,
                            )
                            startAngle += sweep
                        }
                        // Draw red dots for weak parts
                        var dotStart = -90f
                        for (part in BalanceOrder) {
                            val v = values[part] ?: 0
                            if (v == 0) {
                                if (total != 0) {
                                    val sweepSkip = 360f * v.toFloat() / total
                                    dotStart += sweepSkip
                                }
                                continue
                            }
                            val sweep = 360f * v.toFloat() / total
                            val isWeak = v.toFloat() / total < WeakThreshold
                            if (isWeak) {
                                val midAngle = dotStart + sweep / 2
                                val rad = Math.toRadians(midAngle.toDouble())
                                val dotR = 5.dp.toPx()
                                val borderR = 7.dp.toPx()
                                val dCx = center.x + cos(rad).toFloat() * (diameter / 2)
                                val dCy = center.y + sin(rad).toFloat() * (diameter / 2)
                                // white border for contrast
                                drawCircle(
                                    color = Color.White,
                                    radius = borderR,
                                    center = Offset(dCx, dCy),
                                )
                                drawCircle(
                                    color = errorColor,
                                    radius = dotR,
                                    center = Offset(dCx, dCy),
                                )
                            }
                            dotStart += sweep
                        }
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BalanceOrder.forEach { part ->
                            val v = values[part] ?: 0
                            val pct = if (total == 0) 0f else v.toFloat() / total
                            val isWeak = part in weakParts
                            val color = BalanceColors[part] ?: Color.Gray
                            val label = if (part == "有氧") "$v 分钟" else "$v 次"
                            val pctText = "${(pct * 100).toInt()}%"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, RoundedCornerShape(2.dp)),
                                )
                                Text(
                                    text = part,
                                    modifier = Modifier.width(36.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    text = pctText,
                                    modifier = Modifier.width(36.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (isWeak) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(4.dp),
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50)),
                                        )
                                        Text(
                                            text = "薄弱",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceRingCardPreviewBalanced() {
    KenkoTheme {
        BalanceRingCard(
            monthlyCounts = mapOf("胸" to 4, "背" to 4, "腿" to 3, "手臂" to 2, "肩" to 2, "核心" to 3),
            cardioMonthly = 30,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceRingCardPreviewImbalanced() {
    KenkoTheme {
        BalanceRingCard(
            monthlyCounts = mapOf("胸" to 10, "背" to 1, "腿" to 0, "手臂" to 1, "肩" to 0, "核心" to 0),
            cardioMonthly = 0,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceRingCardPreviewEmpty() {
    KenkoTheme {
        BalanceRingCard(
            monthlyCounts = emptyMap(),
            cardioMonthly = 0,
        )
    }
}
