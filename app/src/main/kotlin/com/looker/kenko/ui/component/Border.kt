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

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val KenkoBorderWidth: Dp = 1.dp
val KenkoHairlineWidth: Dp = 0.8.dp

// 统一边框体系：KenkoBorder(常规) / KenkoBorderStrong(强调)
// 常规 = outlineVariant(可见但克制, 浅色 1.47 / 深色 1.46) , 强调 = outline(更深, 1.89/1.69)
// 全部卡片/分割线统一使用此体系，避免内联 outline/outlineVariant 混用导致的视觉不统一
val KenkoBorder: BorderStroke
    @Composable
    get() = BorderStroke(KenkoBorderWidth, MaterialTheme.colorScheme.outlineVariant)

val KenkoBorderStrong: BorderStroke
    @Composable
    get() = BorderStroke(KenkoBorderWidth, MaterialTheme.colorScheme.outline)

// 兼容旧命名：统一收敛到 KenkoBorder 体系
val LinearBorder: BorderStroke
    @Composable
    get() = KenkoBorder

val LinearBorderStrong: BorderStroke
    @Composable
    get() = KenkoBorderStrong

val PrimaryBorder: BorderStroke
    @Composable
    get() = BorderStroke(KenkoBorderWidth, MaterialTheme.colorScheme.primary)

val SecondaryBorder: BorderStroke
    @Composable
    get() = KenkoBorder

val OutlineBorder: BorderStroke
    @Composable
    get() = KenkoBorder

val OnSurfaceBorder: BorderStroke
    @Composable
    get() = KenkoBorder

val OnSurfaceVariantBorder: BorderStroke
    @Composable
    get() = KenkoBorder
