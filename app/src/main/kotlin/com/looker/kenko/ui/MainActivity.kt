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

package com.looker.kenko.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import com.looker.kenko.ui.components.KenkoBottomBar
import com.looker.kenko.ui.home.navigation.HomeRoute
import com.looker.kenko.ui.home.navigation.navigateToHome
import com.looker.kenko.ui.profile.navigation.ProfileRoute
import com.looker.kenko.ui.profile.navigation.navigateToProfile
import com.looker.kenko.ui.sessionDetail.navigation.SessionDetailRoute
import com.looker.kenko.ui.sessionDetail.navigation.navigateToSessionDetail
import com.looker.kenko.ui.navigation.KenkoNavHost
import com.looker.kenko.ui.theme.KenkoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val colorScheme by viewModel.colorScheme.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val isExerciseVisible by viewModel.isExerciseVisible.collectAsStateWithLifecycle()

            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = if (language.code != null) {
                    LocaleListCompat.forLanguageTags(language.code)
                } else {
                    LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(appLocale)
            }

            KenkoTheme(
                theme = theme,
                colorSchemes = colorScheme,
            ) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                val isTopLevelRoute = currentRoute == HomeRoute::class.qualifiedName ||
                        currentRoute == ProfileRoute::class.qualifiedName ||
                        (currentRoute == SessionDetailRoute::class.qualifiedName && 
                         backStackEntry?.arguments?.getBoolean("showBackButton") == false)

                Kenko(
                    bottomBar = {
                        if (isTopLevelRoute) {
                            KenkoBottomBar(
                                currentRoute = currentRoute,
                                isExerciseVisible = isExerciseVisible,
                                onHomeClick = {
                                    navController.navigateToHome(
                                        navOptions = navOptions {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    )
                                },
                                onExerciseClick = {
                                    navController.navigateToSessionDetail(
                                        date = null,
                                        showBackButton = false,
                                        navOptions = navOptions {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    )
                                },
                                onProfileClick = {
                                    navController.navigateToProfile(
                                        showBackButton = false,
                                        navOptions = navOptions {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    KenkoNavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Kenko(
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (innerPadding: PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        bottomBar = bottomBar,
        content = content,
    )
}
