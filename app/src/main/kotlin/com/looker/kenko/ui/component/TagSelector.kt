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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.domain.model.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    parents: List<Tag>,
    children: List<Tag>,
    selectedTags: List<Tag>,
    onAddTag: (Tag) -> Unit,
    onRemoveTag: (Tag) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var parentExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }

    val filteredChildren = if (selectedParentId != null) {
        children.filter { it.parentId == selectedParentId }
    } else {
        emptyList()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_selected_tags),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Selected tags as chips
        if (selectedTags.isEmpty()) {
            Text(
                text = stringResource(R.string.label_at_least_one_tag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selectedTags.forEach { tag ->
                    val tagLabel = tag.parentName?.let { "$it → ${tag.name}" } ?: tag.name
                    AssistChip(
                        onClick = {},
                        label = { Text(tagLabel, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveTag(tag) },
                                modifier = Modifier.size(18.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Parent dropdown (body part)
        ExposedDropdownMenuBox(
            expanded = parentExpanded,
            onExpandedChange = { parentExpanded = it },
        ) {
            val selectedParentName = selectedParentId?.let { id ->
                parents.find { it.id == id }?.name ?: ""
            } ?: ""
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                value = selectedParentName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_select_body_part)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = parentExpanded,
                onDismissRequest = { parentExpanded = false },
            ) {
                parents.forEach { parent ->
                    DropdownMenuItem(
                        text = { Text(parent.name) },
                        onClick = {
                            selectedParentId = parent.id
                            parentExpanded = false
                            childExpanded = true
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Child dropdown (specific muscle)
        ExposedDropdownMenuBox(
            expanded = childExpanded && filteredChildren.isNotEmpty(),
            onExpandedChange = { childExpanded = it },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                value = "",
                onValueChange = {},
                readOnly = true,
                enabled = selectedParentId != null,
                label = { Text(stringResource(R.string.label_select_muscle)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = childExpanded) },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = childExpanded && filteredChildren.isNotEmpty(),
                onDismissRequest = { childExpanded = false },
            ) {
                filteredChildren.forEach { child ->
                    val alreadySelected = selectedTags.any { it.id == child.id }
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(child.name)
                                if (alreadySelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        onClick = {
                            if (!alreadySelected) {
                                val parentName = parents.find { it.id == child.parentId }?.name
                                onAddTag(child.copy(parentName = parentName))
                            }
                            childExpanded = false
                        },
                    )
                }
            }
        }

        // Add more button
        if (selectedParentId != null && filteredChildren.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_add),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
