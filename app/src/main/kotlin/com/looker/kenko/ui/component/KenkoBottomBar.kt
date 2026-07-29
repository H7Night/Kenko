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

package com.looker.kenko.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.kenko.R
import com.looker.kenko.ui.feature.home.navigation.HomeRoute
import com.looker.kenko.ui.feature.profile.navigation.ProfileRoute
import com.looker.kenko.ui.feature.session.navigation.SessionRoute
import com.looker.kenko.ui.theme.KenkoIcons

@Composable
fun KenkoBottomBar(
    currentRouteName: String?,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 5.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            NavigationBarItem(
                selected = currentRouteName == HomeRoute::class.qualifiedName,
                onClick = onHomeClick,
                icon = {
                    Icon(
                        painter = KenkoIcons.Home,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.label_home),
                        fontSize = 10.sp,
                    )
                },
                colors = itemColors,
                alwaysShowLabel = true,
            )
            NavigationBarItem(
                selected = currentRouteName == SessionRoute::class.qualifiedName,
                onClick = onHistoryClick,
                icon = {
                    Icon(
                        painter = KenkoIcons.History,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.label_records),
                        fontSize = 10.sp,
                    )
                },
                colors = itemColors,
                alwaysShowLabel = true,
            )
            NavigationBarItem(
                selected = currentRouteName == ProfileRoute::class.qualifiedName,
                onClick = onProfileClick,
                icon = {
                    Icon(
                        painter = KenkoIcons.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.label_profile),
                        fontSize = 10.sp,
                    )
                },
                colors = itemColors,
                alwaysShowLabel = true,
            )
        }
    }
}
