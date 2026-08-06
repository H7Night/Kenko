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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.ui.component.disableScrollConnection
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

    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

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

        // Create exercise button
        FilledTonalIconButton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(56.dp),
            shape = MaterialTheme.shapes.large,
            onClick = {
                focusManager.clearFocus()
                onRequestNewExercise(null)
            },
        ) {
            Icon(painter = KenkoIcons.Add, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Filter selectors — tap to open modal picker
        var showParentSheet by remember { mutableStateOf(false) }
        var showChildSheet by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Body part selector
            val parentName = selectedParentId?.let { id ->
                parentTags.find { it.id == id }?.name
            } ?: stringResource(R.string.label_select_body_part)
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = parentName,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_select_body_part)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showParentSheet = true },
                )
            }

            // Muscle selector (only visible when body part is selected)
            if (selectedParentId != null) {
                val childName = selectedChildId?.let { id ->
                    children.find { it.id == id }?.name
                } ?: stringResource(R.string.label_select_muscle)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = childName,
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_select_muscle)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showChildSheet = true },
                    )
                }
            }
        }

        // --- Parent (body part) picker sheet ---
        if (showParentSheet) {
            val parentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                sheetState = parentSheetState,
                onDismissRequest = { showParentSheet = false },
            ) {
                Text(
                    text = stringResource(R.string.label_select_body_part),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                HorizontalDivider()
                // "All muscle groups" option
                Surface(
                    onClick = {
                        viewModel.setParentFilter(null)
                        showParentSheet = false
                    },
                    color = if (selectedParentId == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_all_muscle_groups),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedParentId == null)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
                parentTags.forEach { parent ->
                    val isSelected = parent.id == selectedParentId
                    Surface(
                        onClick = {
                            viewModel.setParentFilter(parent.id)
                            showParentSheet = false
                        },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = parent.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // --- Child (muscle) picker sheet ---
        if (showChildSheet && children.isNotEmpty()) {
            val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                sheetState = childSheetState,
                onDismissRequest = { showChildSheet = false },
            ) {
                Text(
                    text = stringResource(R.string.label_select_muscle),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                HorizontalDivider()
                // "All muscles" option — clear child filter
                val allSelected = selectedChildId == null
                Surface(
                    onClick = {
                        viewModel.setChildFilter(null)
                        showChildSheet = false
                    },
                    color = if (allSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_all_muscle_groups),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (allSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
                children.forEach { child ->
                    val isSelected = child.id == selectedChildId
                    Surface(
                        onClick = {
                            viewModel.setChildFilter(child.id)
                            showChildSheet = false
                        },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        val parentLabel = child.parentName?.let { "$it  " } ?: ""
                        Text(
                            text = "$parentLabel${child.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
