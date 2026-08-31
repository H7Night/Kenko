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

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.looker.kenko.domain.model.settings.Theme

/**
 * Linear-inspired light color scheme.
 * Single accent: #5E6AD2 (Linear violet-blue) + neutral Zinc palette.
 * Hierarchy via border/alpha, not filled containers.
 */
private val CatppuccinLatteColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF5E6AD2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8EAFB),
    onPrimaryContainer = Color(0xFF2F3764),
    secondary = Color(0xFF6B7280),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2F3F5),
    onSecondaryContainer = Color(0xFF363A43),
    tertiary = Color(0xFF6B7280),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF8F9FA),
    onTertiaryContainer = Color(0xFF363A43),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0E0E10),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E0E10),
    surfaceVariant = Color(0xFFF8F9FA),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE8EAED),
    outlineVariant = Color(0xFFF2F3F5),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF0E0E10),
    inverseOnSurface = Color(0xFFF7F8F8),
    inversePrimary = Color(0xFF8B92FF),
    surfaceDim = Color(0xFFE8EAED),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFFE8EAED),
    surfaceContainerHighest = Color(0xFFD9DCE0),
)

/**
 * Linear-inspired dark color scheme.
 * Base #080A0E + Zinc borders + single accent #7C83FD (lighter for dark).
 */
private val CatppuccinMochaColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7C83FD),
    onPrimary = Color(0xFF080A0E),
    primaryContainer = Color(0xFF1A1D3A),
    onPrimaryContainer = Color(0xFFC2C8FD),
    secondary = Color(0xFF8A8F98),
    onSecondary = Color(0xFF080A0E),
    secondaryContainer = Color(0xFF1E2025),
    onSecondaryContainer = Color(0xFFD0D4DA),
    tertiary = Color(0xFF8A8F98),
    onTertiary = Color(0xFF080A0E),
    tertiaryContainer = Color(0xFF1E2025),
    onTertiaryContainer = Color(0xFFD0D4DA),
    error = Color(0xFFF87171),
    onError = Color(0xFF080A0E),
    errorContainer = Color(0xFF3A1F24),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF080A0E),
    onBackground = Color(0xFFF7F8F8),
    surface = Color(0xFF080A0E),
    onSurface = Color(0xFFF7F8F8),
    surfaceVariant = Color(0xFF12151D),
    onSurfaceVariant = Color(0xFF8A8F98),
    outline = Color(0xFF23252B),
    outlineVariant = Color(0xFF1A1E2A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF7F8F8),
    inverseOnSurface = Color(0xFF0E0E10),
    inversePrimary = Color(0xFF5E6AD2),
    surfaceDim = Color(0xFF080A0E),
    surfaceBright = Color(0xFF1A1E2A),
    surfaceContainerLowest = Color(0xFF06080C),
    surfaceContainerLow = Color(0xFF0E1015),
    surfaceContainer = Color(0xFF12151D),
    surfaceContainerHigh = Color(0xFF1A1E2A),
    surfaceContainerHighest = Color(0xFF23252B),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KenkoTheme(
    theme: Theme = Theme.System,
    content: @Composable () -> Unit,
) {
    val systemTheme = isSystemInDarkTheme()
    val isDarkTheme = remember(theme) {
        when (theme) {
            Theme.System -> systemTheme
            Theme.Light -> false
            Theme.Dark -> true
        }
    }
    val colorScheme = if (isDarkTheme) {
        CatppuccinMochaColorScheme
    } else {
        CatppuccinLatteColorScheme
    }

    val localView = LocalView.current
    SideEffect {
        // Keep the window background in sync with the theme so page transitions
        // don't flash a mismatched (e.g. white) background under dark themes.
        (localView.context as Activity).window.setBackgroundDrawable(
            ColorDrawable(colorScheme.surface.toArgb()),
        )
    }
    SideEffect { setupSystemBar(localView, isDarkTheme) }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

fun setupSystemBar(view: View, isDarkTheme: Boolean) {
    if (view.isInEditMode) return
    val window = (view.context as Activity).window
    with(WindowCompat.getInsetsController(window, view)) {
        isAppearanceLightStatusBars = !isDarkTheme
        isAppearanceLightNavigationBars = !isDarkTheme
    }
}
