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

package com.looker.kenko.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.looker.kenko.R

val FontFamily.Companion.Numbers
    get() = FontFamily(Font(R.font.maplemono_bold))

val displayFont = FontFamily(
    Font(R.font.darkergrotesque_bold, weight = FontWeight.Bold),
    Font(R.font.darkergrotesque_semibold, weight = FontWeight.SemiBold),
)

val bodyFont = FontFamily(
    Font(R.font.maplemono_bold, weight = FontWeight.Bold),
    Font(R.font.maplemono_regular, weight = FontWeight.Normal),
)

fun Typography.header() = displayLarge.copy(
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.02).sp,
)

fun TextStyle.numbers() = copy(fontFamily = FontFamily.Numbers)

val baseline = Typography()

val Typography = Typography().copy(
    displayLarge = baseline.displayLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).sp,
    ),
    displayMedium = baseline.displayMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.015).sp,
    ),
    displaySmall = baseline.displaySmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp,
    ),
    headlineLarge = baseline.headlineLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).sp,
    ),
    headlineMedium = baseline.headlineMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    headlineSmall = baseline.headlineSmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
    ),
    titleLarge = baseline.titleLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.005).sp,
    ),
    titleMedium = baseline.titleMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    titleSmall = baseline.titleSmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp,
    ),
    bodyLarge = baseline.bodyLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = baseline.bodyMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = baseline.bodySmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = baseline.labelLarge.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.sp,
    ),
    labelMedium = baseline.labelMedium.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.sp,
    ),
    labelSmall = baseline.labelSmall.copy(
        fontFamily = displayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp,
    ),
)
