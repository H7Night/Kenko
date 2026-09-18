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

package com.looker.kenko.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

private const val DETAIL_TRANSITION_DURATION = 300
private const val TAB_TRANSITION_DURATION = 150

/**
 * Slide + fade used when pushing a detail screen onto the back stack.
 * The predictive back gesture animates these in reverse.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.detailEnter(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(DETAIL_TRANSITION_DURATION),
    ) + fadeIn(animationSpec = tween(DETAIL_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.detailExit(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(DETAIL_TRANSITION_DURATION),
    ) + fadeOut(animationSpec = tween(DETAIL_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopEnter(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(DETAIL_TRANSITION_DURATION),
    ) + fadeIn(animationSpec = tween(DETAIL_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopExit(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(DETAIL_TRANSITION_DURATION),
    ) + fadeOut(animationSpec = tween(DETAIL_TRANSITION_DURATION))

/**
 * Fast fade used when switching between bottom-bar tabs.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition =
    fadeIn(animationSpec = tween(TAB_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition = ExitTransition.None

fun AnimatedContentTransitionScope<NavBackStackEntry>.tabPopEnter(): EnterTransition =
    fadeIn(animationSpec = tween(TAB_TRANSITION_DURATION))

fun AnimatedContentTransitionScope<NavBackStackEntry>.tabPopExit(): ExitTransition = ExitTransition.None
