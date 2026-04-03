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

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.ui.home.navigation.HomeRoute
import com.looker.kenko.ui.profile.navigation.ProfileRoute
import com.looker.kenko.ui.sessionDetail.navigation.SessionDetailRoute
import com.looker.kenko.ui.theme.KenkoIcons

@Composable
fun KenkoBottomBar(
    currentRoute: String?,
    isExerciseVisible: Boolean,
    onHomeClick: () -> Unit,
    onExerciseClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.height(64.dp),
    ) {
        NavigationBarItem(
            selected = currentRoute == HomeRoute::class.qualifiedName,
            onClick = onHomeClick,
            icon = { Icon(painter = KenkoIcons.Home, contentDescription = null) },
            label = { Text(text = stringResource(R.string.label_home)) }
        )
        if (isExerciseVisible) {
            NavigationBarItem(
                selected = currentRoute == SessionDetailRoute::class.qualifiedName,
                onClick = onExerciseClick,
                icon = { Icon(painter = KenkoIcons.Plan, contentDescription = null) },
                label = { Text(text = stringResource(R.string.label_exercise)) }
            )
        }
        NavigationBarItem(
            selected = currentRoute == ProfileRoute::class.qualifiedName,
            onClick = onProfileClick,
            icon = { Icon(painter = KenkoIcons.Person, contentDescription = null) },
            label = { Text(text = stringResource(R.string.label_profile)) }
        )
    }
}
