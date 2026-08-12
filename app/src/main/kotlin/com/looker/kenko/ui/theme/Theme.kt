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
 * Catppuccin Latte — light color scheme.
 * Palette: https://catppuccin.com/palette (Latte)
 * Latte has no suitable in-palette container colors, so container roles follow the
 * community-standard mix(base, accent, 20%) approach (equivalent to M3 tone 90).
 */
private val CatppuccinLatteColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1E66F5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5D5F5),
    onPrimaryContainer = Color(0xFF1E66F5),
    secondary = Color(0xFF179299),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4DEE3),
    onSecondaryContainer = Color(0xFF179299),
    tertiary = Color(0xFF8839EF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDACCF4),
    onTertiaryContainer = Color(0xFF8839EF),
    error = Color(0xFFD20F39),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE9C4CF),
    onErrorContainer = Color(0xFFD20F39),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF5C5F77), // Subtext1 — secondary text ≥4.5:1
    outline = Color(0xFF7C7F93),
    outlineVariant = Color(0xFFACB0BE),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313244),
    inverseOnSurface = Color(0xFFEFF1F5),
    inversePrimary = Color(0xFF89B4FA),
    surfaceDim = Color(0xFFACB0BE),
    surfaceBright = Color(0xFFEFF1F5),
    surfaceContainerLowest = Color(0xFFEFF1F5),
    surfaceContainerLow = Color(0xFFE6E9EF),
    surfaceContainer = Color(0xFFDCE0E8),
    surfaceContainerHigh = Color(0xFFCCD0DA),
    surfaceContainerHighest = Color(0xFFBCC0CC),
)

/**
 * Catppuccin Mocha — dark color scheme.
 * Palette: https://catppuccin.com/palette (Mocha)
 * Container roles follow the community-standard mix(base, accent, 25%)
 * approach (equivalent to M3 tone 30, desaturated for dark mode);
 * on-container colors are lightened accents for readable on-dark text.
 */
private val CatppuccinMochaColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF89B4FA),
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF394461),
    onPrimaryContainer = Color(0xFFB8D4FB),
    secondary = Color(0xFF94E2D5),
    onSecondary = Color(0xFF1E1E2E),
    secondaryContainer = Color(0xFF3C4F58),
    onSecondaryContainer = Color(0xFFC4EFE8),
    tertiary = Color(0xFFCBA6F7),
    onTertiary = Color(0xFF1E1E2E),
    tertiaryContainer = Color(0xFF494060),
    onTertiaryContainer = Color(0xFFE2CEFB),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF1E1E2E),
    errorContainer = Color(0xFF53394C),
    onErrorContainer = Color(0xFFF8BFCF),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFBAC2DE), // Subtext1 — secondary text ≥7:1
    outline = Color(0xFF9399B2),
    outlineVariant = Color(0xFF45475A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFCCD0DA),
    inverseOnSurface = Color(0xFF4C4F69),
    inversePrimary = Color(0xFF1E66F5),
    surfaceDim = Color(0xFF1E1E2E),
    surfaceBright = Color(0xFF313244),
    surfaceContainerLowest = Color(0xFF11111B),
    surfaceContainerLow = Color(0xFF181825),
    surfaceContainer = Color(0xFF1E1E2E),
    surfaceContainerHigh = Color(0xFF313244),
    surfaceContainerHighest = Color(0xFF45475A),
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
