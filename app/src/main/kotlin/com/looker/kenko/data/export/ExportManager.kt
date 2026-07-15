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

package com.looker.kenko.data.export

import android.net.Uri
import com.looker.kenko.data.backup.BackupResult

data class ExportOptions(
    val exportSessions: Boolean = true,
    val exportPlans: Boolean = true,
    val exportExercises: Boolean = true,
    val exportWeights: Boolean = true,
) {
    val hasSelection: Boolean get() = exportSessions || exportPlans || exportExercises || exportWeights
}

interface ExportManager {
    suspend fun export(options: ExportOptions, destinationUri: Uri): BackupResult
}
