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

package com.looker.kenko.ui.component.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TimerCard(
    timerState: TimerState,
    elapsedSeconds: Long,
    notificationGranted: Boolean,
    hasAccumulatedTime: Boolean = false,
    showStart: Boolean = true,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isActive = timerState != TimerState.IDLE
    val isIdle = timerState == TimerState.IDLE
    val isRunning = timerState == TimerState.RUNNING
    val showReset = onReset != null && (isActive || hasAccumulatedTime)
    val showPrimary = !(isIdle && !showStart)
    val showEnd = isActive

    val statusText = when (timerState) {
        TimerState.RUNNING -> stringResource(R.string.timer_status_running)
        TimerState.PAUSED -> stringResource(R.string.timer_status_paused)
        TimerState.IDLE -> stringResource(R.string.timer_status_idle)
    }
    val statusColor by animateColorAsState(
        targetValue = when (timerState) {
            TimerState.RUNNING -> MaterialTheme.colorScheme.primary
            TimerState.PAUSED -> MaterialTheme.colorScheme.tertiary
            TimerState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(600),
        label = "timerStatusColor",
    )

    val buttonCount = (if (showReset) 1 else 0) + (if (showPrimary) 1 else 0) + (if (showEnd) 1 else 0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimerStatusPill(
                text = statusText,
                color = statusColor,
                isRunning = isRunning,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlipClockText(
                text = TimerService.formatTime(elapsedSeconds),
                color = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = ButtonGroupDefaults.ConnectedSpaceBetween,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                var index = 0

                if (showReset) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { onReset?.invoke() },
                        shapes = connectedToggleShapes(index, buttonCount),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            checkedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.label_reset))
                    }
                    index++
                }

                if (showPrimary) {
                    val primaryEnabled = !isIdle || notificationGranted
                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            when (timerState) {
                                TimerState.IDLE -> onStart()
                                TimerState.RUNNING -> onPause()
                                TimerState.PAUSED -> onResume()
                            }
                        },
                        enabled = primaryEnabled,
                        shapes = connectedToggleShapes(index, buttonCount),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = when (timerState) {
                                TimerState.IDLE -> if (hasAccumulatedTime) {
                                    stringResource(R.string.label_continue_session)
                                } else {
                                    stringResource(R.string.label_start_workout)
                                }
                                TimerState.RUNNING -> stringResource(R.string.label_pause)
                                TimerState.PAUSED -> stringResource(R.string.label_resume)
                            },
                        )
                    }
                    index++
                }

                if (showEnd) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { onEnd() },
                        shapes = connectedToggleShapes(index, buttonCount),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.error,
                            checkedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            checkedContentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.label_end_workout))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun connectedToggleShapes(index: Int, count: Int): ToggleButtonShapes = when {
    count <= 1 -> ToggleButtonDefaults.shapes()
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(
        checkedShape = ButtonGroupDefaults.connectedLeadingButtonShape,
    )
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes(
        checkedShape = ButtonGroupDefaults.connectedTrailingButtonShape,
    )
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes(
        shape = ButtonGroupDefaults.connectedButtonCheckedShape,
    )
}

@Composable
private fun TimerStatusPill(
    text: String,
    color: Color,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "timerPulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "timerPulseAlpha",
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isRunning) pulseScale else 1f,
        animationSpec = tween(300),
        label = "timerDotScale",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isRunning) pulseAlpha else 0.6f,
        animationSpec = tween(300),
        label = "timerDotAlpha",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(dotScale)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha)),
        )
        Crossfade(
            targetState = text,
            animationSpec = tween(300),
            label = "timerStatusText",
        ) { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

@Composable
private fun FlipClockText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val charStyle = MaterialTheme.typography.displayMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).sp,
        fontFeatureSettings = "tnum",
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { index, ch ->
            if (ch.isDigit()) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                                (slideOutVertically { -it } + fadeOut())
                    },
                    label = "timerDigit_$index",
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = charStyle,
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = ch.toString(),
                    style = charStyle,
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
