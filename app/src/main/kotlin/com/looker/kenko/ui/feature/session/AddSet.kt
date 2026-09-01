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

package com.looker.kenko.ui.feature.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.looker.kenko.R
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.repDurationStringRes
import com.looker.kenko.ui.feature.session.AddSetViewModel.FloatTransformation
import com.looker.kenko.ui.feature.session.AddSetViewModel.IntTransformation
import com.looker.kenko.ui.feature.session.components.DraggableTextField
import com.looker.kenko.ui.feature.session.components.rememberDraggableTextFieldState
import com.looker.kenko.ui.theme.KenkoIcons
import kotlinx.datetime.LocalDate

private val incrementButtonModifier = Modifier
    .width(48.dp)
    .height(48.dp)
    .zIndex(0f)

private val zIndexModifier = Modifier.zIndex(1F)

/**
 * 长按连发容器：按住 400ms 后每 80ms 重复触发 [onRepeat]，松开取消。
 * 单次点击仍由内部按钮的 onClick 处理，拖拽手势不受影响。
 */
@Composable
private fun HoldRepeatWrapper(
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    initialDelay: Long = 400L,
    repeatDelay: Long = 80L,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        delay(initialDelay)
        while (pressed) {
            onRepeat()
            delay(repeatDelay)
        }
    }
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pressed = true
                try {
                    waitForUpOrCancellation()
                } finally {
                    pressed = false
                }
            }
        },
    ) {
        content()
    }
}

/**
 * 紧凑的微调步进按钮：不应用 Material 的最小交互尺寸（48dp 宽），
 * 用于重量行的 ±0.5 微调，使整行按钮并排时不折行。
 */
@Composable
private fun CompactStepButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun AddSet(exercise: Exercise, date: LocalDate? = null, onDone: () -> Unit) {
    val isCardio = exercise.countType == CountType.MINUTES
    val viewModel: AddSetViewModel =
        hiltViewModel<AddSetViewModel, AddSetViewModel.AddSetViewModelFactory>(key = exercise.name) {
            exercise.id?.let { id -> it.create(id, date) }
                ?: error("Exercise id is null")
        }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .wrapContentHeight(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AddSetHeader(
            modifier = Modifier.fillMaxWidth(),
            exerciseName = exercise.name,
            onClick = {
                viewModel.addSet()
                onDone()
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        SwipeableTextField(
            modifier = Modifier.align(CenterHorizontally),
        ) {
            HoldRepeatWrapper(onRepeat = { viewModel.addRep(-10) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addRep(-10) },
                ) {
                    Text(text = stringResource(R.string.label_minus_int, 10))
                }
            }
            if (!isCardio) {
                HoldRepeatWrapper(onRepeat = { viewModel.addRep(-1) }) {
                    TextButton(
                        modifier = incrementButtonModifier,
                        onClick = { viewModel.addRep(-1) },
                    ) {
                        Text(text = stringResource(R.string.label_minus_int, 1))
                    }
                }
            }
            val reps = rememberDraggableTextFieldState(viewModel.repsBoundReached)
            DraggableTextField(
                dragState = reps,
                textFieldState = viewModel.reps,
                inputTransformation = IntTransformation,
                supportingText = stringResource(exercise.repDurationStringRes),
                modifier = zIndexModifier,
            )
            if (!isCardio) {
                HoldRepeatWrapper(onRepeat = { viewModel.addRep(1) }) {
                    TextButton(
                        modifier = incrementButtonModifier,
                        onClick = { viewModel.addRep(1) },
                    ) {
                        Text(text = stringResource(R.string.label_plus_int, 1))
                    }
                }
            }
            HoldRepeatWrapper(onRepeat = { viewModel.addRep(10) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addRep(10) },
                ) {
                    Text(text = stringResource(R.string.label_plus_int, 10))
                }
            }
            HoldRepeatWrapper(onRepeat = { viewModel.addRep(20) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addRep(20) },
                ) {
                    Text(text = stringResource(R.string.label_plus_int, 20))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SwipeableTextField(
            modifier = Modifier.align(CenterHorizontally),
        ) {
            HoldRepeatWrapper(onRepeat = { viewModel.addSetCount(-1) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addSetCount(-1) },
                ) {
                    Text(text = stringResource(R.string.label_minus_int, 1))
                }
            }
            val sets = rememberDraggableTextFieldState(viewModel.setsBoundReached)
            DraggableTextField(
                dragState = sets,
                textFieldState = viewModel.setsCount,
                supportingText = stringResource(R.string.label_sets),
                inputTransformation = IntTransformation,
                modifier = zIndexModifier,
            )
            HoldRepeatWrapper(onRepeat = { viewModel.addSetCount(1) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addSetCount(1) },
                ) {
                    Text(text = stringResource(R.string.label_plus_int, 1))
                }
            }
            HoldRepeatWrapper(onRepeat = { viewModel.addSetCount(2) }) {
                TextButton(
                    modifier = incrementButtonModifier,
                    onClick = { viewModel.addSetCount(2) },
                ) {
                    Text(text = stringResource(R.string.label_plus_int, 2))
                }
            }
        }
        if (!isCardio) {
            Spacer(modifier = Modifier.height(24.dp))
            SwipeableTextField(
                modifier = Modifier.align(CenterHorizontally),
            ) {
                if (exercise.isBodyweight) {
                    TextButton(
                        modifier = incrementButtonModifier,
                        onClick = { viewModel.setBodyweight() },
                        colors = if (viewModel.isBodyweightMode) {
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.textButtonColors()
                        },
                    ) {
                        Text(text = stringResource(R.string.label_bodyweight))
                    }
                }
                if (viewModel.isBodyweightMode) {
                    Text(
                        text = stringResource(R.string.label_bodyweight_display),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(48.dp).padding(horizontal = 4.dp),
                    )
                } else {
                    if (!viewModel.isWeightZero) {
                        HoldRepeatWrapper(onRepeat = { viewModel.addWeight(-1F) }) {
                            TextButton(
                                modifier = incrementButtonModifier,
                                onClick = { viewModel.addWeight(-1F) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                Text(text = stringResource(R.string.label_minus_int, 1))
                            }
                        }
                    }
                    HoldRepeatWrapper(onRepeat = { viewModel.addWeight(-0.5F) }) {
                        CompactStepButton(
                            text = stringResource(R.string.label_minus_int, 0.5F),
                            onClick = { viewModel.addWeight(-0.5F) },
                            modifier = Modifier.width(48.dp),
                        )
                    }
                    val weights = rememberDraggableTextFieldState(viewModel.weightsBoundReached)
                    DraggableTextField(
                        dragState = weights,
                        textFieldState = viewModel.weights,
                        supportingText = stringResource(R.string.label_weight),
                        inputTransformation = FloatTransformation,
                        modifier = zIndexModifier,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                    HoldRepeatWrapper(onRepeat = { viewModel.addWeight(0.5F) }) {
                        CompactStepButton(
                            text = stringResource(R.string.label_plus_int, 0.5F),
                            onClick = { viewModel.addWeight(0.5F) },
                            modifier = Modifier.width(48.dp),
                        )
                    }
                    HoldRepeatWrapper(onRepeat = { viewModel.addWeight(1F) }) {
                        TextButton(
                            modifier = incrementButtonModifier,
                            onClick = { viewModel.addWeight(1F) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(R.string.label_plus_int, 1))
                        }
                    }
                    HoldRepeatWrapper(onRepeat = { viewModel.addWeight(5F) }) {
                        TextButton(
                            modifier = incrementButtonModifier,
                            onClick = { viewModel.addWeight(5F) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(R.string.label_plus_int, 5))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun AddSetHeader(
    exerciseName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = stringResource(R.string.label_add_set_for).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        FilledTonalIconButton(onClick = onClick) {
            Icon(
                painter = KenkoIcons.Done,
                contentDescription = "",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SwipeableTextField(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().requiredHeight(44.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
