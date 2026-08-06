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

package com.looker.kenko.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.looker.kenko.ui.feature.about.navigation.about
import com.looker.kenko.ui.feature.about.navigation.navigateToAbout
import com.looker.kenko.ui.feature.backup.navigation.backup
import com.looker.kenko.ui.feature.backup.navigation.navigateToBackup
import com.looker.kenko.ui.feature.exercise.navigation.addEditExercise
import com.looker.kenko.ui.feature.exercise.navigation.navigateToAddEditExercise
import com.looker.kenko.ui.feature.exercise.navigation.exercises
import com.looker.kenko.ui.feature.exercise.navigation.navigateToExercises
import com.looker.kenko.ui.feature.home.navigation.HomeRoute
import com.looker.kenko.ui.feature.home.navigation.home
import com.looker.kenko.ui.feature.performance.navigation.performance
import com.looker.kenko.ui.feature.plan.navigation.navigateToPlanEdit
import com.looker.kenko.ui.feature.plan.navigation.planEdit
import com.looker.kenko.ui.feature.plan.navigation.navigateToPlans
import com.looker.kenko.ui.feature.plan.navigation.plans
import com.looker.kenko.ui.feature.profile.navigation.navigateToProfile
import com.looker.kenko.ui.feature.profile.navigation.profile
import com.looker.kenko.ui.feature.session.navigation.navigateToSessionDetail
import com.looker.kenko.ui.feature.session.navigation.sessionDetail
import com.looker.kenko.ui.feature.session.navigation.navigateToSessions
import com.looker.kenko.ui.feature.session.navigation.sessions
import com.looker.kenko.ui.feature.settings.navigation.navigateToSettings
import com.looker.kenko.ui.feature.settings.navigation.settings
import com.looker.kenko.ui.feature.tags.navigation.navigateToTagManagement
import com.looker.kenko.ui.feature.tags.navigation.tagManagement

private val singleTopNavOptions = navOptions {
    launchSingleTop = true
}

@Composable
fun KenkoNavHost(
    navController: NavController,
    modifier: Modifier = Modifier,
    startDestination: Any = HomeRoute,
) {
    NavHost(
        modifier = modifier,
        navController = navController as NavHostController,
        startDestination = startDestination,
        // Disable the default crossfade transitions (fadeIn/fadeOut ~700ms).
        // During a pop the outgoing page stays composed on top and still
        // receives clicks, so a fast tap right after back can hit the
        // previous screen's elements (e.g. opening the exercise editor
        // instead of the plan). Instant swaps avoid that click-through.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        home(
            onProfileClick = {
                navController.navigateToProfile(navOptions = singleTopNavOptions)
            },
            onSelectPlanClick = {
                navController.navigateToPlans(navOptions = singleTopNavOptions)
            },
            onExploreSessionsClick = {
                navController.navigateToSessions(navOptions = singleTopNavOptions)
            },
            onStartSessionClick = {
                navController.navigateToSessionDetail(
                    date = null,
                    showBackButton = false,
                    navOptions = singleTopNavOptions
                )
            },
            onCurrentPlanClick = {
                navController.navigateToPlanEdit(id = it, navOptions = singleTopNavOptions)
            },
        )

        sessions(
            onSessionClick = { date ->
                navController.navigateToSessionDetail(
                    date = date,
                    navOptions = singleTopNavOptions
                )
            },
            onBackPress = navController::popBackStackOnResume
        )

        plans(
            onPlanClick = {
                navController.navigateToPlanEdit(
                    id = it,
                    navOptions = singleTopNavOptions
                )
            },
            onBackPress = navController::popBackStackOnResume
        )

        settings(
            navController::popBackStackOnResume,
            onTagManagementClick = {
                navController.navigateToTagManagement(navOptions = singleTopNavOptions)
            },
            onBackupClick = {
                navController.navigateToBackup(navOptions = singleTopNavOptions)
            },
            onAboutClick = {
                navController.navigateToAbout(navOptions = singleTopNavOptions)
            },
        )

        profile(
            onBackPress = navController::popBackStackOnResume,
            onAddExerciseClick = {
                navController.navigateToAddEditExercise(navOptions = singleTopNavOptions)
            },
            onExercisesClick = {
                navController.navigateToExercises(navOptions = singleTopNavOptions)
            },
            onPlanClick = {
                navController.navigateToPlans(navOptions = singleTopNavOptions)
            },
            onSettingsClick = {
                navController.navigateToSettings(navOptions = singleTopNavOptions)
            },
        )

        exercises(
            onExerciseClick = { id ->
                navController.navigateToAddEditExercise(
                    id = id,
                    navOptions = singleTopNavOptions
                )
            },
            onCreateClick = {
                navController.navigateToAddEditExercise(
                    navOptions = singleTopNavOptions
                )
            },
            onBackPress = navController::popBackStackOnResume
        )

        planEdit(navController::popBackStackOnResume) { name ->
            navController.navigateToAddEditExercise(
                name = name,
                navOptions = singleTopNavOptions,
            )
        }

        sessionDetail(
            onBackPress = navController::popBackStackOnResume,
            onHistoryClick = navController::navigateToSessionDetail,
            onAddExerciseClick = { name ->
                navController.navigateToAddEditExercise(
                    name = name,
                    navOptions = singleTopNavOptions,
                )
            },
        )

        addEditExercise(navController::popBackStackOnResume)

        tagManagement(navController::popBackStackOnResume)

        backup(navController::popBackStackOnResume)

        about(navController::popBackStackOnResume)

        performance()
    }
}

private fun NavHostController.popBackStackOnResume() {
    if (lifecycleState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private val NavHostController.lifecycleState: Lifecycle.State?
    get() = currentBackStackEntry?.lifecycle?.currentState
