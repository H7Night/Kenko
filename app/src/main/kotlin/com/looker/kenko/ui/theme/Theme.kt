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
import android.os.Build
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.looker.kenko.domain.model.settings.ColorPalette
import com.looker.kenko.domain.model.settings.Theme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/** Fallback seed for dynamic/baseline Material schemes on API < 31. */
private val MaterialBaselineSeed = Color(0xFF6750A4)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KenkoTheme(
    theme: Theme = Theme.System,
    palette: ColorPalette = ColorPalette.Material,
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
    val context = LocalContext.current
    val colorScheme = when (palette) {
        ColorPalette.Material ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                rememberDynamicColorScheme(
                    seedColor = MaterialBaselineSeed,
                    isDark = isDarkTheme,
                    style = PaletteStyle.TonalSpot,
                )
            }

        else ->
            rememberDynamicColorScheme(
                seedColor = Color(requireNotNull(palette.seedArgb)),
                isDark = isDarkTheme,
                style = PaletteStyle.TonalSpot,
            )
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
