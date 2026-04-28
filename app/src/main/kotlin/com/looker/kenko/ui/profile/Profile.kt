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

package com.looker.kenko.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.looker.kenko.data.model.Weight
import com.looker.kenko.ui.components.WeightLineChart
import com.looker.kenko.utils.DateFormat
import com.looker.kenko.utils.formatDate
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBar
import com.looker.kenko.ui.components.SwipeToDeleteBox
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.data.model.PlanStat
import com.looker.kenko.ui.components.BackButton
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.OutlineBorder
import com.looker.kenko.ui.components.SecondaryBorder
import com.looker.kenko.ui.extensions.PHI
import com.looker.kenko.ui.extensions.normalizeInt
import com.looker.kenko.ui.extensions.plus
import com.looker.kenko.ui.extensions.vertical
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.end
import com.looker.kenko.ui.theme.numbers
import com.looker.kenko.ui.theme.start

@Composable
fun Profile(
    viewModel: ProfileViewModel,
    onBackPress: () -> Unit,
    onExercisesClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onPlanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showBackButton: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Profile(
        state = state,
        onBackPress = onBackPress,
        onSettingsClick = onSettingsClick,
        onPlanClick = onPlanClick,
        onAddExerciseClick = onAddExerciseClick,
        onExercisesClick = onExercisesClick,
        onAddWeight = viewModel::addWeight,
        onUpdateWeight = viewModel::updateWeight,
        onDeleteWeight = viewModel::deleteWeight,
        showBackButton = showBackButton,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Profile(
    state: ProfileUiState,
    onBackPress: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlanClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onExercisesClick: () -> Unit,
    onAddWeight: (Float) -> Unit,
    onUpdateWeight: (Weight) -> Unit,
    onDeleteWeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightToEdit by remember { mutableStateOf<Weight?>(null) }
    var showWeightHistory by remember { mutableStateOf(false) }

    if (showWeightDialog || weightToEdit != null) {
        val initialWeight = weightToEdit?.value
            ?: state.weights.lastOrNull()?.value
            ?: 60f
        WeightDialog(
            initialWeight = initialWeight,
            isEdit = weightToEdit != null,
            onDismiss = {
                showWeightDialog = false
                weightToEdit = null
            },
            onConfirm = { value ->
                if (weightToEdit != null) {
                    onUpdateWeight(weightToEdit!!.copy(value = value))
                } else {
                    onAddWeight(value)
                }
                showWeightDialog = false
                weightToEdit = null
            }
        )
    }

    if (showWeightHistory) {
        WeightHistorySheet(
            weights = state.weights,
            onDismiss = { showWeightHistory = false },
            onEdit = { 
                weightToEdit = it
                showWeightHistory = false
            },
            onDelete = onDeleteWeight
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.label_profile)) },
                navigationIcon = { if (showBackButton) BackButton(onBackPress) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(painter = KenkoIcons.Settings, contentDescription = null)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding + PaddingValues(horizontal = 16.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isPlanAvailable) {
                CurrentPlanCard(
                    onPlanClick = onPlanClick,
                    name = state.planName,
                    content = {
                        Text(
                            text = stringResource(
                                R.string.label_plan_description,
                                state.planStat!!.exercises,
                                normalizeInt(state.planStat.workDays),
                                normalizeInt(state.planStat.restDays),
                            ),
                        )
                    },
                )
            } else {
                SelectPlanCard(onPlanClick)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ExerciseCard(
                numberOfExercises = state.numberOfExercises,
                onAddClick = onAddExerciseClick,
                onExercisesClick = onExercisesClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeightCard(
                weights = state.weights,
                onAddClick = { showWeightDialog = true },
                onHistoryClick = { showWeightHistory = true }
            )
        }
    }
}

@Composable
private fun CurrentPlanCard(
    onPlanClick: () -> Unit,
    name: String,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PHI),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        border = SecondaryBorder,
        onClick = onPlanClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 2.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painter = KenkoIcons.Plan, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.label_current_plan),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1F))
                FilledIconButton(onClick = onPlanClick) {
                    Icon(painter = KenkoIcons.Rename, contentDescription = null)
                }
            }
            HorizontalDivider(
                thickness = KenkoBorderWidth,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp, bottom = 16.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyLarge,
                        LocalContentColor provides MaterialTheme.colorScheme.outline,
                    ) {
                        content()
                    }
                }
                Icon(
                    imageVector = KenkoIcons.Stack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.offset(x = 0.dp),
                )
            }
        }
    }
}

@Composable
fun SelectPlanCard(
    onSelectPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .clickable(onClick = onSelectPlanClick)
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_select_plan),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.paddingFromBaseline(bottom = 16.dp)
            )

            Icon(
                painter = KenkoIcons.ArrowOutward,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    numberOfExercises: Int,
    onAddClick: () -> Unit,
    onExercisesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cardShape = MaterialTheme.shapes.extraLarge
        val surfaceShape = remember(cardShape) {
            cardShape.end(16.dp, 16.dp)
        }
        Surface(
            modifier = Modifier.weight(1.5F),
            shape = surfaceShape,
            border = OutlineBorder,
            onClick = onExercisesClick,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.label_exercise),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = numberOfExercises.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }
        val buttonShape = remember(cardShape) {
            cardShape.start(16.dp, 16.dp)
        }
        Box(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
                .clip(buttonShape)
                .clickable(onClick = onAddClick)
                .border(
                    border = SecondaryBorder,
                    shape = buttonShape,
                )
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = KenkoIcons.Add,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = stringResource(R.string.label_add),
            )
        }
    }
}

@Composable
private fun WeightCard(
    weights: List<Weight>,
    onAddClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick),
        shape = MaterialTheme.shapes.extraLarge,
        border = SecondaryBorder,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_body_weight),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = KenkoIcons.Rename,
                        contentDescription = stringResource(R.string.label_body_weight_history),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (weights.size >= 2) {
                WeightLineChart(
                    weights = weights,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weights.lastOrNull()?.let { "${it.value} ${stringResource(R.string.label_weight_unit)}" }
                            ?: stringResource(R.string.label_add_body_weight),
                        style = MaterialTheme.typography.displaySmall.numbers()
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightDialog(
    initialWeight: Float,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var selectedWeight by remember { mutableStateOf(initialWeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) stringResource(R.string.label_edit_body_weight)
                else stringResource(R.string.label_add_body_weight)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WeightPicker(
                    value = selectedWeight,
                    onValueChange = { selectedWeight = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_weight_unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedWeight) }
            ) {
                Text(stringResource(R.string.label_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    )
}

@Composable
private fun WeightPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tens = (value / 10).toInt()
    val ones = (value % 10).toInt()
    val decimal = ((value * 10) % 10).toInt()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DigitPicker(
            value = tens,
            onValueChange = { onValueChange((it * 10 + ones + decimal * 0.1f)) },
            range = 0..15 // Support up to 159.9 kg or similar if needed, or just 0..9 for tens
        )
        DigitPicker(
            value = ones,
            onValueChange = { onValueChange((tens * 10 + it + decimal * 0.1f)) }
        )
        Text(
            text = ".",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        DigitPicker(
            value = decimal,
            onValueChange = { onValueChange((tens * 10 + ones + it * 0.1f)) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DigitPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..9
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeight = 48.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }
    
    val digits = remember(range) { range.toList() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = digits.indexOf(value).coerceAtLeast(0))
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in digits.indices) {
                onValueChange(digits[centerIndex])
            }
        }
    }

    Box(
        modifier = modifier
            .width(40.dp)
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                )
                .align(Alignment.Center)
        )
        
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize()
        ) {
            items(digits.size) { index ->
                val digit = digits[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit.toString(),
                        style = if (index == listState.firstVisibleItemIndex) 
                            MaterialTheme.typography.headlineMedium.numbers() 
                            else MaterialTheme.typography.titleMedium.numbers(),
                        color = if (index == listState.firstVisibleItemIndex)
                            MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightHistorySheet(
    weights: List<Weight>,
    onDismiss: () -> Unit,
    onEdit: (Weight) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.label_body_weight_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weights.reversed()) { weight ->
                    SwipeToDeleteBox(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        onDismiss = { onDelete(weight.id) },
                        showIcon = true
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEdit(weight) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDate(weight.date, DateFormat.YearMonthDay),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${weight.value} ${stringResource(R.string.label_weight_unit)}",
                                    style = MaterialTheme.typography.titleMedium.numbers()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanCard() {
    KenkoTheme {
        CurrentPlanCard(
            onPlanClick = {
            },
            name = "Push-Pull-Leg",
            content = {
                Text(
                    text = stringResource(
                        R.string.label_plan_description,
                        12,
                        normalizeInt(5),
                        normalizeInt(2),
                    ),
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPlanCardPreview() {
    KenkoTheme {
        SelectPlanCard({})
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseCardPreview() {
    KenkoTheme {
        ExerciseCard(21, {}, {})
    }
}

@Preview
@Composable
private fun ProfileNoPlanPreview() {
    KenkoTheme {
        Profile(
            state = ProfileUiState(12, false, "Push-Pull-Leg", emptyList(), PlanStat(12, 5)),
            onBackPress = { },
            onSettingsClick = { },
            onPlanClick = { },
            onAddExerciseClick = { },
            onExercisesClick = { },
            onAddWeight = {},
            onUpdateWeight = {},
            onDeleteWeight = {},
        )
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    KenkoTheme {
        Profile(
            state = ProfileUiState(12, true, "Push-Pull-Leg", emptyList(), PlanStat(12, 5)),
            onBackPress = { },
            onSettingsClick = { },
            onPlanClick = { },
            onAddExerciseClick = { },
            onExercisesClick = { },
            onAddWeight = {},
            onUpdateWeight = {},
            onDeleteWeight = {},
        )
    }
}
