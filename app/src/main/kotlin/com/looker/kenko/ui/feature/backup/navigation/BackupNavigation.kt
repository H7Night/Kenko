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

package com.looker.kenko.ui.feature.backup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.looker.kenko.ui.feature.backup.BackupScreen
import kotlinx.serialization.Serializable

@Serializable
object BackupRoute

fun NavController.navigateToBackup(navOptions: NavOptions? = null) {
    navigate(BackupRoute, navOptions)
}

fun NavGraphBuilder.backup(
    onBackPress: () -> Unit,
) {
    composable<BackupRoute> {
        BackupScreen(
            onBackPress = onBackPress,
        )
    }
}
