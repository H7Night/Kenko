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

package com.looker.kenko.ui.feature.plan.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R

/**
 * 横向可滚动的训练日标签条:
 * - 每个序列位置一个标签(1..dayCount),文本 = title ?: "第 N 天"
 * - 休息日(restDays 中)灰显并带"休"角标
 * - 末尾 "+" 按钮新增训练日(onAddDay)
 * - 点击标签切换当天(onSelectDay)
 * - 长按标签弹出菜单:重命名 / 移到前 / 移到后 / 设为休息日 / 删除该天
 *
 * 说明:重排采用退化方案(菜单"移到前/后",经 onMoveDay 调用 repo.moveDay),
 * 而非长按拖拽——FilterChip + combinedClickable 与 horizontalScroll 的横向拖拽
 * 手势相互冲突,拖拽在可滚动容器中不可靠。
 */
@Composable
fun TrainingDayBar(
    dayCount: Int,
    selectedDay: Int,
    titles: Map<Int, String>,
    restDays: Set<Int>, // dayIndex 集合(无 plan_day 行的位置由调用方计算)
    onSelectDay: (Int) -> Unit,
    onAddDay: () -> Unit,
    onMoveDay: (Int, Int) -> Unit,
    onRename: () -> Unit,
    onSetAsRest: () -> Unit,
    onDeleteDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..dayCount).forEach { day ->
            val title = titles[day]
            val isRest = day in restDays
            DayTab(
                day = day,
                dayCount = dayCount,
                label = title ?: stringResource(R.string.label_day_n, day),
                isRest = isRest,
                selected = day == selectedDay,
                onClick = { onSelectDay(day) },
                onMoveDay = onMoveDay,
                onRename = onRename,
                onSetAsRest = onSetAsRest,
                onDeleteDay = onDeleteDay,
            )
        }
        FilledTonalIconButton(
            onClick = onAddDay,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.label_add_day))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayTab(
    day: Int,
    dayCount: Int,
    label: String,
    isRest: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onMoveDay: (Int, Int) -> Unit,
    onRename: () -> Unit,
    onSetAsRest: () -> Unit,
    onDeleteDay: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isRest) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (isRest) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.label_rest_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { menuExpanded = true },
        ),
    )
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.label_rename_day)) },
            onClick = {
                menuExpanded = false
                onRename()
            },
        )
        if (day > 1) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.label_move_day_backward)) },
                onClick = {
                    menuExpanded = false
                    onMoveDay(day, day - 1)
                },
            )
        }
        if (day < dayCount) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.label_move_day_forward)) },
                onClick = {
                    menuExpanded = false
                    onMoveDay(day, day + 1)
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.label_set_as_rest_day)) },
            onClick = {
                menuExpanded = false
                onSetAsRest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.label_delete_day)) },
            onClick = {
                menuExpanded = false
                onDeleteDay()
            },
        )
    }
}
