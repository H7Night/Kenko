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

package com.looker.kenko.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.domain.model.Set

/**
 * A [SetItem] capsule optionally combined with a trailing delete button.
 *
 * When [onDelete] is non-null the delete button is shown beside the set
 * capsule, sharing the row space (capsule takes remaining width). Reused by
 * Home inline training and Session Detail set lists.
 */
@Composable
fun DeletableSetItem(
    set: Set,
    modifier: Modifier = Modifier,
    isToday: Boolean = true,
    isEditMode: Boolean = false,
    onRepsUpdate: (Int) -> Unit = {},
    onWeightUpdate: (Float) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    val content = @Composable {
        SetItem(
            set = set,
            isToday = isToday,
            isEditMode = isEditMode,
            onRepsUpdate = onRepsUpdate,
            onWeightUpdate = onWeightUpdate,
            title = title,
        )
    }
    if (onDelete != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}
