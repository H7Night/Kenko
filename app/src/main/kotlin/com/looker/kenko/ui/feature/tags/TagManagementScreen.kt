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

package com.looker.kenko.ui.feature.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.theme.KenkoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    onBackPress: () -> Unit,
    viewModel: TagManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAddParentDialog by remember { mutableStateOf(false) }
    var showAddChildDialog by remember { mutableStateOf<Int?>(null) }
    var showEditDialog by remember { mutableStateOf<Tag?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Tag?>(null) }
    var deleteWarning by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialog
    showDeleteConfirm?.let { tag ->
        val exerciseCount = remember(tag.id) {
            var count = 0
            viewModel.exerciseCount(tag.id) { count = it }
            count
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.label_delete_tag)) },
            text = {
                Text(
                    if (exerciseCount > 0) {
                        stringResource(R.string.label_delete_tag_warning, exerciseCount)
                    } else {
                        stringResource(R.string.label_confirm_delete_tag)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tag)
                    showDeleteConfirm = null
                }) {
                    Text(stringResource(R.string.label_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(R.string.label_cancel))
                }
            },
        )
    }

    // Add parent dialog
    if (showAddParentDialog) {
        TagNameDialog(
            title = stringResource(R.string.label_add_tag),
            onDismiss = { showAddParentDialog = false },
            onConfirm = { name ->
                viewModel.addParent(name)
                showAddParentDialog = false
            },
        )
    }

    // Add child dialog
    showAddChildDialog?.let { parentId ->
        val parentName = state.parents.find { it.id == parentId }?.name ?: ""
        TagNameDialog(
            title = "${stringResource(R.string.label_add_tag)} ($parentName)",
            onDismiss = { showAddChildDialog = null },
            onConfirm = { name ->
                viewModel.addChild(name, parentId)
                showAddChildDialog = null
            },
        )
    }

    // Edit dialog
    showEditDialog?.let { tag ->
        TagNameDialog(
            title = stringResource(R.string.label_edit_tag),
            initialName = tag.name,
            onDismiss = { showEditDialog = null },
            onConfirm = { name ->
                viewModel.updateTag(tag.copy(name = name))
                showEditDialog = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_tag_management)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_parent_tag),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = { showAddParentDialog = true }) {
                        Icon(Icons.Default.Label, contentDescription = stringResource(R.string.label_add_tag))
                    }
                }
                HorizontalDivider()
            }

            items(state.parents, key = { it.id }) { parent ->
                ParentTagCard(
                    parent = parent,
                    children = state.children.filter { it.parentId == parent.id },
                    onAddChild = { showAddChildDialog = parent.id },
                    onEdit = { showEditDialog = it },
                    onDelete = { showDeleteConfirm = it },
                )
            }
        }
    }
}

@Composable
private fun ParentTagCard(
    parent: Tag,
    children: List<Tag>,
    onAddChild: () -> Unit,
    onEdit: (Tag) -> Unit,
    onDelete: (Tag) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = parent.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Row {
                IconButton(onClick = onAddChild) {
                    Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onEdit(parent) }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = { onDelete(parent) }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        children.forEach { child ->
            ChildTagRow(
                tag = child,
                onEdit = { onEdit(child) },
                onDelete = { onDelete(child) },
            )
        }
    }
}

@Composable
private fun ChildTagRow(
    tag: Tag,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "• ${tag.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.height(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.height(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TagNameDialog(
    title: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_tag_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.label_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}

// Preview placeholder
@Composable
private fun TagManagementPreview() {
    KenkoTheme {
        TagManagementScreen(onBackPress = {})
    }
}
