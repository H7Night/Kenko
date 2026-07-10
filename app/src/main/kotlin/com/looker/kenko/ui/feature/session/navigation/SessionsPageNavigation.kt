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

package com.looker.kenko.ui.feature.session.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.looker.kenko.ui.feature.session.Sessions
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
object SessionRoute

fun NavController.navigateToSessions(navOptions: NavOptions? = null) {
    navigate(SessionRoute, navOptions = navOptions)
}

fun NavGraphBuilder.sessions(
    onSessionClick: (LocalDate?) -> Unit,
    onBackPress: () -> Unit,
) {
    composable<SessionRoute>(
        deepLinks = listOf(navDeepLink { uriPattern = "kenko://sessions" }),
    ) {
        Sessions(
            onSessionClick = onSessionClick,
            onBackPress = onBackPress,
            viewModel = hiltViewModel(),
        )
    }
}
