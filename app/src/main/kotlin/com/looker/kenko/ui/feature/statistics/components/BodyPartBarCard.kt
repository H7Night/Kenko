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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.looker.kenko.ui.theme.KenkoTheme

private val BodyPartOrder = listOf("胸", "背", "腿", "手臂", "肩", "核心", "有氧")

@Composable
fun BodyPartBarCard(
    title: String,
    counts: Map<String, Int>,
    cardioMinutes: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = maxOf(1, counts.values.maxOrNull() ?: 1),
    maxMinutes: Int = maxOf(1, cardioMinutes),
) {
    // Separate scales: strength parts use maxCount, 有氧 uses maxMinutes
    // to avoid 30 min dominating 3 times
    val safeMaxCount = maxOf(1, maxCount)
    val safeMaxMinutes = maxOf(1, maxMinutes)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            BodyPartOrder.forEach { part ->
                val isCardio = part == "有氧"
                val value = if (isCardio) cardioMinutes else counts[part] ?: 0
                val max = if (isCardio) safeMaxMinutes else safeMaxCount
                val progress = (value.toFloat() / max).coerceIn(0f, 1f)
                val label = if (isCardio) "$value 分钟" else "$value 次"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = part,
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BodyPartBarCardPreview() {
    KenkoTheme {
        BodyPartBarCard(
            title = "本周",
            counts = mapOf("胸" to 3, "腿" to 0),
            cardioMinutes = 30,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BodyPartBarCardEmptyPreview() {
    KenkoTheme {
        BodyPartBarCard(
            title = "本月",
            counts = emptyMap(),
            cardioMinutes = 0,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BodyPartBarCardFullPreview() {
    KenkoTheme {
        BodyPartBarCard(
            title = "本计划",
            counts = mapOf("胸" to 5, "背" to 3, "腿" to 4, "手臂" to 2, "肩" to 3, "核心" to 1),
            cardioMinutes = 45,
            maxCount = 5,
            maxMinutes = 60,
        )
    }
}
