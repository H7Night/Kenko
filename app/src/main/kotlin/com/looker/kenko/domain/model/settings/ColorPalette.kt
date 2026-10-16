/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.model.settings

import androidx.annotation.StringRes
import com.looker.kenko.R

/**
 * Selectable color style for the app theme.
 *
 * [seedArgb] is the Material Color Utilities seed for the palette; the UI layer
 * generates the full light/dark scheme from it (MaterialKolor). `null` marks
 * [Material], which uses Android dynamic color (Monet) instead.
 */
enum class ColorPalette(@StringRes val nameRes: Int, val seedArgb: Long? = null) {
    /** Android dynamic color (Monet on Android 12+); MaterialKolor baseline below. */
    Material(R.string.label_color_style_material),

    Blue(R.string.label_color_style_blue, 0xFF4285F4),
    Green(R.string.label_color_style_green, 0xFF0F9D58),
    Amber(R.string.label_color_style_amber, 0xFFF4B400),
    Pink(R.string.label_color_style_pink, 0xFFE91E63),
    Monochrome(R.string.label_color_style_monochrome, 0xFF5F6368),
}
