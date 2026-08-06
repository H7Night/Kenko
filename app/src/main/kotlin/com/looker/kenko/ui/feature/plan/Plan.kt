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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanPreviewParameters
import com.looker.kenko.ui.component.BackButton
import com.looker.kenko.ui.component.KenkoBorderWidth
import com.looker.kenko.ui.component.ConfirmDialog
import com.looker.kenko.ui.component.endItem
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.feature.plan.components.KenkoAddButton
import com.looker.kenko.ui.feature.plan.components.PlanItem
import androidx.compose.ui.platform.LocalContext
import com.looker.kenko.utils.toast
import com.looker.kenko.ui.theme.KenkoTheme

@Composable
fun Plan(
    viewModel: PlanViewModel,
    onBackPress: () -> Unit,
    onPlanClick: (Int) -> Unit,
) {
    val plans: List<Plan> by viewModel.plans.collectAsStateWithLifecycle()
    var planToDelete by remember { mutableStateOf<Plan?>(null) }
    val context = LocalContext.current

    Plan(
        plans = plans,
        onBackPress = onBackPress,
        onSelectPlan = viewModel::switchPlan,
        onRemove = viewModel::removePlan,
        onPlanClick = onPlanClick,
        onRequestRemove = { planToDelete = it },
    )

    planToDelete?.let { plan ->
        val deletedMessage = stringResource(R.string.label_deleted)
        ConfirmDialog(
            title = stringResource(R.string.label_delete_plan_title),
            message = stringResource(R.string.label_delete_plan_message, plan.name),
            confirmText = stringResource(R.string.label_delete),
            onConfirm = {
                plan.id?.let { viewModel.removePlan(it) }
                context.toast(deletedMessage)
                planToDelete = null
            },
            onDismiss = { planToDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Plan(
    plans: List<Plan>,
    onBackPress: () -> Unit,
    onSelectPlan: (Plan) -> Unit,
    onRemove: (Int) -> Unit,
    onPlanClick: (Int) -> Unit,
    onRequestRemove: (Plan) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onClick = onBackPress) },
                title = { Text(text = stringResource(R.string.label_plans_title)) }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { KenkoAddButton(onClick = { onPlanClick(-1) }) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = it + PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = plans,
                key = { plan -> plan.id ?: plan.hashCode() },
            ) { plan ->
                PlanItem(
                    modifier = Modifier.animateItem(),
                    plan = plan,
                    onClick = { plan.id?.let { onPlanClick(it) } },
                    onActiveChange = { onSelectPlan(plan) },
                    onDelete = { onRequestRemove(plan) },
                )
            }
            endItem()
        }
    }
}

@Preview
@Composable
private fun PlanPreview(
    @PreviewParameter(PlanPreviewParameters::class) plans: List<Plan>,
) {
    KenkoTheme {
        Plan(
            plans = plans,
            onSelectPlan = {},
            onBackPress = {},
            onPlanClick = {},
            onRemove = {},
        )
    }
}
