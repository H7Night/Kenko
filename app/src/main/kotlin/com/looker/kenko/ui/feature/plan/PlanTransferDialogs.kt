package com.looker.kenko.ui.feature.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.looker.kenko.domain.model.Plan

@Composable
fun ExportPlanDialog(
    plans: List<Plan>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val planIds = plans.mapNotNull { it.id }.toSet()
    val allSelected = planIds.isNotEmpty() && selectedIds.containsAll(planIds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_export_plan)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIds = if (allSelected) emptySet() else planIds
                        },
                ) {
                    Checkbox(checked = allSelected, onCheckedChange = null)
                    Text(stringResource(R.string.label_select_all))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(plans, key = { it.id ?: it.name }) { plan ->
                        val planId = plan.id
                        val checked = planId != null && planId in selectedIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (planId != null) {
                                        selectedIds = if (checked) selectedIds - planId else selectedIds + planId
                                    }
                                },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column {
                                Text(plan.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = stringResource(
                                        R.string.label_plan_days_summary,
                                        plan.stat.workDays,
                                        (plan.dayCount - plan.stat.workDays).coerceAtLeast(0),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text(stringResource(R.string.label_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}

@Composable
fun ImportPlanConfirmDialog(
    planCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_import_plan_file)) },
        text = { Text(stringResource(R.string.label_import_confirm, planCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.label_import_plan_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}
