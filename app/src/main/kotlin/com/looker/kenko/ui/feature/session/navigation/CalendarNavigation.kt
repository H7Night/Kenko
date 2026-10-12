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

package com.looker.kenko.ui.feature.session.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.looker.kenko.ui.feature.session.calendar.CalendarScreen
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
object CalendarRoute

fun NavController.navigateToCalendar(navOptions: NavOptions? = null) {
    navigate(CalendarRoute, navOptions = navOptions)
}

fun NavGraphBuilder.calendar(
    onBackPress: () -> Unit,
    onSessionClick: (LocalDate) -> Unit,
) {
    composable<CalendarRoute> {
        CalendarScreen(
            onBackPress = onBackPress,
            onSessionClick = onSessionClick,
            viewModel = hiltViewModel(),
        )
    }
}
