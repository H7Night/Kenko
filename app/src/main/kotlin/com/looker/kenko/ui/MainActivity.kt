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

package com.looker.kenko.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.ui.component.KenkoBottomBar
import com.looker.kenko.ui.component.timer.TimerManager
import com.looker.kenko.ui.component.timer.TimerState
import com.looker.kenko.ui.component.timer.TrainingSessionManager
import com.looker.kenko.ui.component.timer.TrainingSessionState
import com.looker.kenko.ui.feature.home.navigation.HomeRoute
import com.looker.kenko.ui.feature.home.navigation.navigateToHome
import com.looker.kenko.ui.feature.profile.navigation.ProfileRoute
import com.looker.kenko.ui.feature.profile.navigation.navigateToProfile
import com.looker.kenko.ui.feature.session.navigation.SessionDetailRoute
import com.looker.kenko.ui.feature.session.navigation.navigateToSessionDetail
import com.looker.kenko.ui.feature.session.navigation.SessionRoute
import com.looker.kenko.ui.feature.session.navigation.navigateToSessions
import com.looker.kenko.ui.navigation.KenkoNavHost
import com.looker.kenko.ui.theme.KenkoTheme
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface SessionObserverEntryPoint {
        fun timerManager(): TimerManager
        fun trainingSessionManager(): TrainingSessionManager
        fun sessionRepo(): SessionRepo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val entryPoint = EntryPoints.get(this, SessionObserverEntryPoint::class.java)
        val timerManager = entryPoint.timerManager()
        val trainingSessionManager = entryPoint.trainingSessionManager()
        val sessionRepo = entryPoint.sessionRepo()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                val timerState = timerManager.state.value
                if (timerState != TimerState.RUNNING && timerState != TimerState.PAUSED) return
                val sessionState = trainingSessionManager.sessionState.value
                val sessionId = (sessionState as? TrainingSessionState.Active)?.sessionId ?: return
                val elapsed = timerManager.elapsedSeconds.value
                if (elapsed > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        sessionRepo.updateSessionDuration(sessionId, elapsed)
                    }
                }
            }
        })
        installSplashScreen().apply {
            setKeepOnScreenCondition { !viewModel.isReady.value }
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()

            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = if (language.code != null) {
                    LocaleListCompat.forLanguageTags(language.code)
                } else {
                    LocaleListCompat.getEmptyLocaleList()
                }
                if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            KenkoTheme(
                theme = theme,
            ) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val currentRouteName = currentRoute?.substringBefore("?")?.substringBefore("/")

                val homeRouteName = HomeRoute::class.qualifiedName
                val profileRouteName = ProfileRoute::class.qualifiedName
                val sessionDetailRouteName = SessionDetailRoute::class.qualifiedName
                val sessionRouteName = SessionRoute::class.qualifiedName

                val isTopLevelRoute = currentRouteName == homeRouteName ||
                        currentRouteName == profileRouteName ||
                        currentRouteName == sessionRouteName ||
                        (currentRouteName == sessionDetailRouteName && 
                         backStackEntry?.arguments?.getBoolean("showBackButton") == false)

                Kenko(
                    bottomBar = {
                        if (isTopLevelRoute) {
                            KenkoBottomBar(
                                currentRouteName = currentRouteName,
                                onHomeClick = {
                                    navController.navigateToHome(
                                        navOptions = navOptions {
                                            popUpTo(HomeRoute) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    )
                                },
                                onHistoryClick = {
                                    navController.navigateToSessions(
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
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        content(PaddingValues(bottom = 0.dp))

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            bottomBar()
        }
    }
}
