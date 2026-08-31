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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.domain.model.Tag

/**
 * Reusable two-level search filter: body part (parent tag) + muscle (child tag) dropdowns.
 *
 * Stateless — selection state is hoisted to the caller via [selectedParentId] / [selectedChildId]
 * and the callbacks. When a body part is picked the caller should clear the child selection;
 * when "All" is picked the caller should clear both.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyPartMuscleFilter(
    parentTags: List<Tag>,
    allTags: List<Tag>,
    selectedParentId: Int?,
    selectedChildId: Int?,
    onParentSelect: (Int?) -> Unit,
    onChildSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var parentExpanded by remember { mutableStateOf(false) }
    var childExpanded by remember { mutableStateOf(false) }

    val children = remember(allTags, selectedParentId) {
        val parentId = selectedParentId
        if (parentId == null) emptyList()
        else allTags.filter { it.parentId == parentId }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Body part dropdown
        ExposedDropdownMenuBox(
            expanded = parentExpanded,
            onExpandedChange = { parentExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            val parentName = selectedParentId?.let { id ->
                parentTags.find { it.id == id }?.name
                    ?: stringResource(R.string.label_select_body_part)
            } ?: stringResource(R.string.label_all_muscle_groups)
            OutlinedTextField(
                value = parentName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_select_body_part)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = parentExpanded,
                onDismissRequest = { parentExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_all_muscle_groups)) },
                    onClick = {
                        onParentSelect(null)
                        parentExpanded = false
                    },
                )
                parentTags.forEach { parent ->
                    DropdownMenuItem(
                        text = { Text(parent.name) },
                        onClick = {
                            onParentSelect(parent.id)
                            parentExpanded = false
                            childExpanded = true
                        },
                    )
                }
            }
        }
        // Muscle dropdown — only visible when a body part is selected
        if (selectedParentId != null) {
            ExposedDropdownMenuBox(
                expanded = childExpanded && children.isNotEmpty(),
                onExpandedChange = { childExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                val childName = selectedChildId?.let { id ->
                    children.find { it.id == id }?.name
                        ?: stringResource(R.string.label_select_muscle)
                } ?: stringResource(R.string.label_select_muscle)
                OutlinedTextField(
                    value = childName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_select_muscle)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = childExpanded && children.isNotEmpty()) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = childExpanded && children.isNotEmpty(),
                    onDismissRequest = { childExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.label_all_muscle_groups)) },
                        onClick = {
                            onChildSelect(null)
                            childExpanded = false
                        },
                    )
                    children.forEach { child ->
                        val label = parentTags.find { it.id == child.parentId }?.name
                            ?.let { "$it → ${child.name}" } ?: child.name
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onChildSelect(child.id)
                                childExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
