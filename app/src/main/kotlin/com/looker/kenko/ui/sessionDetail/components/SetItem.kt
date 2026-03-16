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

package com.looker.kenko.ui.sessionDetail.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.data.local.model.SetType
import com.looker.kenko.data.model.Exercise
import com.looker.kenko.data.model.ExercisesPreviewParameter
import com.looker.kenko.data.model.RepsInReserve
import com.looker.kenko.data.model.Set
import com.looker.kenko.data.model.repDurationStringRes
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.numbers

@Composable
fun SetItem(
    set: Set,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onRepsUpdate: (Int) -> Unit = {},
    onWeightUpdate: (Float) -> Unit = {},
    title: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(64.dp)
            .widthIn(240.dp, 420.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.outline,
            LocalTextStyle provides MaterialTheme.typography.displayMedium.numbers(),
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                title()
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .weight(1F)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PerformedItem(
                title = stringResource(set.exercise.repDurationStringRes),
                performance = "${set.repsOrDuration}",
                isEditMode = isEditMode,
                keyboardType = KeyboardType.Number,
                onValueUpdate = { onRepsUpdate(it.toIntOrNull() ?: set.repsOrDuration) },
            )
            PerformedItem(
                title = stringResource(R.string.label_weight),
                performance = "${set.weight} KG",
                isEditMode = isEditMode,
                keyboardType = KeyboardType.Decimal,
                onValueUpdate = { onWeightUpdate(it.toFloatOrNull() ?: set.weight) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PerformedItem(
    title: String,
    performance: String,
    isEditMode: Boolean,
    keyboardType: KeyboardType,
    onValueUpdate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    val initialValue = performance.replace(" KG", "")
    var textValue by remember(performance) {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .combinedClickable(
                enabled = isEditMode,
                onDoubleClick = { isEditing = true },
                onClick = {},
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        if (isEditing) {
            BasicTextField(
                modifier = Modifier
                    .width(48.dp)
                    .focusRequester(focusRequester),
                value = textValue,
                onValueChange = { textValue = it },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onValueUpdate(textValue.text)
                        isEditing = false
                    },
                ),
                singleLine = true,
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Text(
                text = performance,
                style = MaterialTheme.typography.titleMedium,
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
            Set(12, 40F, SetType.Drop, exercises.first(), RepsInReserve(2)),
        ) {
            Text(text = "01")
        }
    }
}
