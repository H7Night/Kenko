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

package com.looker.kenko.ui.feature.exercise.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.looker.kenko.ui.feature.exercise.progress.ExerciseProgressScreen
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseProgressRoute(val exerciseId: Int)

fun NavController.navigateToExerciseProgress(
    exerciseId: Int,
    navOptions: NavOptions? = null,
) {
    navigate(ExerciseProgressRoute(exerciseId), navOptions = navOptions)
}

fun NavGraphBuilder.exerciseProgress(
    onBackPress: () -> Unit,
    onSessionClick: (LocalDate) -> Unit,
) {
    composable<ExerciseProgressRoute> {
        ExerciseProgressScreen(
            onBackPress = onBackPress,
            onSessionClick = onSessionClick,
            viewModel = hiltViewModel(),
        )
    }
}
