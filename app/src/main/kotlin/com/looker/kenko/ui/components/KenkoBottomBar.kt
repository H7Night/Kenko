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

package com.looker.kenko.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R
import com.looker.kenko.ui.home.navigation.HomeRoute
import com.looker.kenko.ui.profile.navigation.ProfileRoute
import com.looker.kenko.ui.sessionDetail.navigation.SessionDetailRoute
import com.looker.kenko.ui.sessions.navigation.SessionRoute
import com.looker.kenko.ui.theme.KenkoIcons

@Composable
fun KenkoBottomBar(
    currentRouteName: String?,
    isExerciseVisible: Boolean,
    onHomeClick: () -> Unit,
    onExerciseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.height(56.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
        
        NavigationBarItem(
            selected = currentRouteName == HomeRoute::class.qualifiedName,
            onClick = onHomeClick,
            icon = { 
                Icon(
                    painter = KenkoIcons.Home, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                ) 
            },
            colors = itemColors
        )
        if (isExerciseVisible) {
            NavigationBarItem(
                selected = currentRouteName == SessionDetailRoute::class.qualifiedName,
                onClick = onExerciseClick,
                icon = { 
                    Icon(
                        painter = KenkoIcons.Plan, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                colors = itemColors
            )
        }
        NavigationBarItem(
            selected = currentRouteName == SessionRoute::class.qualifiedName,
            onClick = onHistoryClick,
            icon = {
                Icon(
                    painter = KenkoIcons.History,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRouteName == ProfileRoute::class.qualifiedName,
            onClick = onProfileClick,
            icon = { 
                Icon(
                    painter = KenkoIcons.Person, 
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                ) 
            },
            colors = itemColors
        )
    }
}
