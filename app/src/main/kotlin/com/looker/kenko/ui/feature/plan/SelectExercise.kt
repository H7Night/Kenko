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

package com.looker.kenko.ui.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.ui.component.disableScrollConnection
import com.looker.kenko.ui.component.kenkoTextFieldColor
import com.looker.kenko.ui.feature.plan.components.ExerciseItem
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.start

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SelectExercise(
    onDone: (Exercise) -> Unit,
    onRequestNewExercise: (name: String?) -> Unit,
    title: String? = null,
) {
    val viewModel: SelectExerciseViewModel = hiltViewModel()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .nestedScroll(disableScrollConnection())
            .wrapContentHeight(),
    ) {
        val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
        val parentTags by viewModel.parentTags.collectAsStateWithLifecycle()
        val children by viewModel.children.collectAsStateWithLifecycle()
        val selectedParentId by viewModel.selectedParentId.collectAsStateWithLifecycle()
        val selectedChildId by viewModel.selectedChildId.collectAsStateWithLifecycle()

        AddExerciseHeader(
            title = title,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Search field
        ExerciseSearchField(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = viewModel.searchQuery.value,
            onNameChange = viewModel::setSearch,
            onAddClick = {
                focusManager.clearFocus()
                onRequestNewExercise(viewModel.searchQuery.value.ifBlank { null })
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Two-level filter
        var parentExpanded by remember { mutableStateOf(false) }
        var childExpanded by remember { mutableStateOf(false) }

        // Parent dropdown (body part filter)
        ExposedDropdownMenuBox(
            expanded = parentExpanded,
            onExpandedChange = { parentExpanded = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val parentName = selectedParentId?.let { id ->
                parentTags.find { it.id == id }?.name
                    ?: stringResource(R.string.label_select_body_part)
            } ?: stringResource(R.string.label_select_body_part)
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                value = parentName,
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
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_all_muscle_groups)) },
                    onClick = {
                        viewModel.setParentFilter(null)
                        viewModel.setChildFilter(null)
                        parentExpanded = false
                    },
                )
                parentTags.forEach { parent ->
                    DropdownMenuItem(
                        text = { Text(parent.name) },
                        onClick = {
                            viewModel.setParentFilter(parent.id)
                            viewModel.setChildFilter(null)
                            parentExpanded = false
                            childExpanded = true
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Child dropdown (specific muscle filter)
        if (selectedParentId != null) {
            ExposedDropdownMenuBox(
                expanded = childExpanded && children.isNotEmpty(),
                onExpandedChange = { childExpanded = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                val childName = selectedChildId?.let { id ->
                    children.find { it.id == id }?.name
                        ?: stringResource(R.string.label_select_muscle)
                } ?: stringResource(R.string.label_select_muscle)
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = childName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = children.isNotEmpty(),
                    label = { Text(stringResource(R.string.label_select_muscle)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = childExpanded) },
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = childExpanded && children.isNotEmpty(),
                    onDismissRequest = { childExpanded = false },
                ) {
                    children.forEach { child ->
                        DropdownMenuItem(
                            text = { Text(child.name) },
                            onClick = {
                                viewModel.setChildFilter(child.id)
                                childExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(240.dp),
        ) {
            when (searchResult) {
                SearchResult.Loading -> ContainedLoadingIndicator()
                SearchResult.NotFound -> SearchNotFound(
                    onAddNewExercise = {
                        focusManager.clearFocus()
                        onRequestNewExercise(viewModel.searchQuery.value)
                    }
                )

                is SearchResult.Success -> SearchResult(
                    searchResult = searchResult as SearchResult.Success,
                    onClick = onDone,
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchResult: SearchResult.Success,
    onClick: (Exercise) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(searchResult.exercises) { exercise ->
            ExerciseItem(
                exercise = exercise,
                onClick = { onClick(exercise) },
            )
        }
    }
}

@Composable
private fun SearchNotFound(onAddNewExercise: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_cant_find_exercise),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddNewExercise,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                ),
            ) {
                Icon(painter = KenkoIcons.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.label_create_exercise))
            }
        }
    }
}

@Composable
private fun ExerciseSearchField(
    name: String,
    onNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = name,
            onValueChange = onNameChange,
            colors = kenkoTextFieldColor(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = MaterialTheme.shapes.large.end(8.dp),
            label = {
                Text(text = stringResource(R.string.label_search_exercise))
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalIconButton(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large.start(8.dp),
            onClick = onAddClick,
        ) {
            Icon(painter = KenkoIcons.Add, contentDescription = null)
        }
    }
}

@Composable
private fun AddExerciseHeader(
    title: String?,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = title?.ifBlank { null } ?: stringResource(R.string.label_add_exercise_header),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Preview
@Composable
private fun ErrorPreview() {
    KenkoTheme {
        SearchNotFound(onAddNewExercise = {})
    }
}
