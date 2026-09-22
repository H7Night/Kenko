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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.looker.kenko.R
import com.looker.kenko.utils.toast

/**
 * Confirmation dialog for deleting a set, shared by the Home inline training
 * list and the Session Detail set list. Emits the standard "deleted" toast on
 * confirmation.
 */
@Composable
fun SetDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.label_deleted)
    ConfirmDialog(
        title = stringResource(R.string.label_delete_set_title),
        message = stringResource(R.string.label_delete_set_message),
        confirmText = stringResource(R.string.label_delete),
        onConfirm = {
            onConfirm()
            context.toast(deletedMessage)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
