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

package com.looker.kenko.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.ExercisesPreviewParameter
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.repDurationStringRes
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.numbers

@Composable
fun SetItem(
    set: Set,
    modifier: Modifier = Modifier,
    isToday: Boolean = true,
    isEditMode: Boolean = false,
    ghost: Boolean = false,
    referenceReps: Int? = null,
    referenceWeight: Float? = null,
    onRepsUpdate: (Int) -> Unit = {},
    onWeightUpdate: (Float) -> Unit = {},
    title: @Composable () -> Unit,
) {
    val isCardio = set.exercise.countType == CountType.MINUTES
    // 幽灵行: 上一次训练的对照片,半透明且不可编辑
    val editable = isEditMode && !ghost
    val containerColor = MaterialTheme.colorScheme.surface
    val containerShape = MaterialTheme.shapes.small
    Surface(
        modifier = Modifier
            .widthIn(200.dp, 420.dp)
            .then(if (ghost) Modifier.alpha(0.4f) else Modifier)
            .then(modifier),
        shape = containerShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .heightIn(44.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                LocalTextStyle provides MaterialTheme.typography.titleMedium.numbers(),
            ) {
                Box(modifier = Modifier.padding(end = 12.dp)) {
                    title()
                }
            }
            Row(
                modifier = Modifier.weight(1F),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                PerformedItem(
                    title = stringResource(set.exercise.repDurationStringRes),
                    performance = if (isCardio) "${set.repsOrDuration} ${stringResource(R.string.label_min)}" else "${set.repsOrDuration}",
                    rawValue = "${set.repsOrDuration}",
                    reference = referenceReps?.toString(),
                    isEditMode = editable,
                    keyboardType = KeyboardType.Number,
                    onValueUpdate = { onRepsUpdate(it.toIntOrNull() ?: set.repsOrDuration) },
                )
                if (!isCardio) {
                    // 自重动作: 重量为 0 表示纯自重(显示"自重"); 非 0 表示附加重量,显示数值。
                    if (set.exercise.isBodyweight && set.weight <= 0F) {
                        Column(
                            modifier = Modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.label_weight).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.label_bodyweight_display),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        PerformedItem(
                            title = stringResource(R.string.label_weight),
                            performance = "${set.weight} KG",
                            rawValue = "${set.weight}",
                            reference = referenceWeight?.toString(),
                            isEditMode = editable,
                            keyboardType = KeyboardType.Decimal,
                            onValueUpdate = { onWeightUpdate(it.toFloatOrNull() ?: set.weight) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PerformedItem(
    title: String,
    performance: String,
    rawValue: String,
    reference: String?,
    isEditMode: Boolean,
    keyboardType: KeyboardType,
    onValueUpdate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    // 编辑时从空白开始（不预填当前数字）；清空时以浅色显示"上一个数字"作为占位。
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    val isZero = rawValue == "0" || rawValue == "0.0"
    val showReference = isZero && !reference.isNullOrBlank()
    val referenceColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    fun commit() {
        val text = textValue.text.trim()
        if (text.isNotEmpty()) onValueUpdate(text)
        isEditing = false
        focused = false
        textValue = TextFieldValue("")
    }

    Column(
        modifier = modifier
            .combinedClickable(
                enabled = isEditMode,
                onDoubleClick = { isEditing = true },
                onClick = { isEditing = true },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isEditing) {
            BasicTextField(
                modifier = Modifier
                    .width(48.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            focused = true
                        } else if (focused) {
                            commit()
                        }
                    },
                value = textValue,
                onValueChange = { textValue = it },
                textStyle = MaterialTheme.typography.titleMedium.numbers().copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { commit() },
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (textValue.text.isEmpty() && !reference.isNullOrBlank()) {
                            Text(
                                text = reference,
                                style = MaterialTheme.typography.titleMedium.numbers(),
                                color = referenceColor,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Text(
                text = if (showReference) reference!! else performance,
                style = MaterialTheme.typography.titleMedium.numbers(),
                color = if (showReference) referenceColor else LocalContentColor.current,
            )
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SetItemPreview(
    @PreviewParameter(ExercisesPreviewParameter::class, limit = 2) exercises: List<Exercise>,
) {
    KenkoTheme {
        SetItem(
            Set(12, 40F, exercises.first()),
        ) {
            Text(text = "01")
        }
    }
}
