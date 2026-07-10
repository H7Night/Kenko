/*
 * Copyright (C) 2025 LooKeR & Contributors
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

package com.looker.kenko.domain.model.settings

import androidx.annotation.StringRes
import com.looker.kenko.R
import com.looker.kenko.ui.theme.colorSchemes.ColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.defaultColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.tokyoNightColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.catppuccinColorSchemes
import com.looker.kenko.ui.theme.colorSchemes.gruvboxColorSchemes

enum class Theme(@StringRes val nameRes: Int) {
    System(R.string.label_theme_system),
    Light(R.string.label_theme_light),
    Dark(R.string.label_theme_dark),
}

enum class ColorPalettes(val scheme: ColorSchemes?) {
    Dynamic(null),
    Default(defaultColorSchemes),
    TokyoNight(tokyoNightColorSchemes),
    Gruvbox(gruvboxColorSchemes),
    Catppuccin(catppuccinColorSchemes),
}
