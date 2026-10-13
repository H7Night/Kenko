/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.model.settings

import androidx.annotation.StringRes
import com.looker.kenko.R

enum class ColorPalette(@StringRes val nameRes: Int) {
    /** The current Kenko accent (Linear-style violet on Zinc neutrals). */
    Line(R.string.label_color_style_line),

    /** Google Material 3 baseline / dynamic color. */
    Material(R.string.label_color_style_material),
}
