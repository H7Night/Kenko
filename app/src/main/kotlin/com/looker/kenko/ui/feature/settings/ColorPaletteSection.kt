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

package com.looker.kenko.ui.feature.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.domain.model.settings.ColorPalettes
import com.looker.kenko.domain.model.settings.Theme
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.dynamicColorSchemes

@Composable
fun ColorPaletteSelection(
    selectedColorPalette: ColorPalettes,
    selectedTheme: Theme,
    onClickPalette: (ColorPalettes) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        ColorPalettes.entries.forEach { colorPalette ->
            ColorPaletteItem(
                isSelected = selectedColorPalette == colorPalette,
                theme = selectedTheme,
                colorPalette = colorPalette,
                modifier = Modifier.clickable { onClickPalette(colorPalette) },
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
internal fun ColorPaletteItem(
    isSelected: Boolean,
    theme: Theme,
    colorPalette: ColorPalettes,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorSchemes = remember(colorPalette) {
        colorPalette.scheme ?: dynamicColorSchemes(context)
    }
    if (colorSchemes == null) return
    val transition = updateTransition(targetState = isSelected, label = null)
    val corner by transition.animateDp(label = "") {
        if (it) 32.dp else 16.dp
    }
    val background by transition.animateColor(label = "") {
        if (it) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    }
    val contentColor by transition.animateColor(label = "") {
        if (it) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }
    Column(
        modifier = Modifier
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(corner, corner, 16.dp, 16.dp)
            }
            .drawBehind { drawRect(background) }
            .then(modifier),
        horizontalAlignment = CenterHorizontally,
    ) {
        KenkoTheme(
            theme = theme,
            colorSchemes = colorSchemes,
        ) {
            Box(Modifier.size(80.dp)) {
                ColorPaletteSample()
                Crossfade(targetState = isSelected, label = "") {
                    if (it) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = KenkoIcons.Done,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
        Text(
            modifier = Modifier.padding(vertical = 2.dp),
            text = stringResource(colorSchemes.nameRes),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
internal fun ColorPaletteSample(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .then(modifier)
            .padding(8.dp)
            .clip(CircleShape),
    ) {
        Spacer(
            modifier = Modifier
                .size(31.dp)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
        Spacer(
            modifier = Modifier
                .size(31.dp)
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
        )
        Spacer(
            modifier = Modifier
                .size(64.dp, 31.dp)
                .align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)),
        )
    }
}
