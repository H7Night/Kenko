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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import com.looker.kenko.R
import com.looker.kenko.data.StringHandler
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.data.plan.PlanTransferManager
import com.looker.kenko.utils.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repo: PlanRepo,
    private val settingsRepo: SettingsRepo,
    private val stringHandler: StringHandler,
    private val transferManager: PlanTransferManager,
) : ViewModel() {

    val plans = repo.plans.asStateFlow(emptyList())

    val snackbarState = SnackbarHostState()

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val plans = transferManager.readPlans(uri)
                _importPreview.value = ImportPreview(uri, plans.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.label_plan_file_invalid))
            }
        }
    }

    fun dismissImportPreview() {
        _importPreview.value = null
    }

    fun confirmImport() {
        val preview = _importPreview.value ?: return
        // 立即关闭确认窗口，避免导入执行期间重复点击导致重复导入
        _importPreview.value = null
        viewModelScope.launch {
            try {
                val summary = transferManager.importPlans(preview.uri)
                val message = if (summary.failed == 0) {
                    stringHandler.getString(R.string.label_import_success, summary.imported)
                } else {
                    stringHandler.getString(R.string.label_import_partial, summary.imported, summary.failed)
                }
                snackbarState.showSnackbar(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            }
        }
    }

    fun exportPlans(planIds: List<Int>, destinationUri: Uri) {
        viewModelScope.launch {
            try {
                transferManager.exportPlans(planIds, destinationUri)
                snackbarState.showSnackbar(
                    stringHandler.getString(R.string.label_export_success, planIds.size),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            }
        }
    }

    fun removePlan(id: Int) {
        viewModelScope.launch {
            try {
                repo.deletePlan(id)
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            }
        }
    }

    fun switchPlan(plan: Plan) {
        viewModelScope.launch {
            try {
                if (!plan.isActive) {
                    plan.id?.let { repo.setCurrent(it) }
                } else {
                    repo.updatePlan(plan.copy(isActive = false))
                }
                if (repo.current.first() != null) {
                    settingsRepo.setOnboardingDone()
                }
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            }
        }
    }
}

@Stable
data class ImportPreview(
    val uri: Uri,
    val planCount: Int,
)
