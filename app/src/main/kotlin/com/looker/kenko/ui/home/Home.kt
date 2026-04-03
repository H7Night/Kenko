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

package com.looker.kenko.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.ui.components.KenkoBorderWidth
import com.looker.kenko.ui.components.TertiaryKenkoButton
import com.looker.kenko.ui.components.TickerText
import com.looker.kenko.ui.home.components.TrainingHeatmap
import com.looker.kenko.ui.theme.KenkoIcons
import com.looker.kenko.ui.theme.KenkoTheme
import com.looker.kenko.ui.theme.header

@Composable
fun Home(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit,
    onSelectPlanClick: () -> Unit,
    onExploreSessionsClick: () -> Unit,
    onStartSessionClick: () -> Unit,
    onCurrentPlanClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Home(
        state = state,
        onProfileClick = onProfileClick,
        onSelectPlanClick = onSelectPlanClick,
        onExploreSessionsClick = onExploreSessionsClick,
        onStartSessionClick = onStartSessionClick,
        onCurrentPlanClick = onCurrentPlanClick,
    )
}

// TODO: Add current plan indicator on this page
@Composable
private fun Home(
    state: HomeUiData,
    onProfileClick: () -> Unit = {},
    onSelectPlanClick: () -> Unit = {},
    onExploreSessionsClick: () -> Unit = {},
    onStartSessionClick: () -> Unit = {},
    onCurrentPlanClick: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            KenkoTopBar {
                FilledTonalIconButton(onClick = onProfileClick) {
                    Icon(painter = KenkoIcons.Person, contentDescription = null)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = state.isPlanSelected,
                label = "plan_status",
            ) { isPlanActive ->
                if (isPlanActive) {
                    TrainingHeatmap(
                        sessionDates = state.sessionDates,
                        onClick = onExploreSessionsClick,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TickerText(
                            text = stringResource(R.string.label_select_a_plan),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            if (state.isPlanSelected) {
                StartSession(
                    onStartSessionClick = onStartSessionClick,
                    content = {
                        val heading = remember(state.isSessionStarted, state.isTodayEmpty) {
                            if (state.isSessionStarted) {
                                R.string.label_continue_session_heading
                            } else if (state.isTodayEmpty) {
                                R.string.label_rest_day_heading
                            } else {
                                if (state.isFirstSession) {
                                    R.string.label_start_first_session
                                } else {
                                    R.string.label_start_session_heading
                                }
                            }
                        }
                        Text(
                            modifier = Modifier
                                .align(CenterHorizontally)
                                .padding(horizontal = 16.dp),
                            text = stringResource(heading),
                            style = MaterialTheme.typography.header()
                                .merge(
                                    lineBreak = LineBreak.Heading,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    },
                    buttonText = {
                        val stringRes = remember(state.isSessionStarted) {
                            if (state.isSessionStarted) {
                                R.string.label_continue_session
                            } else {
                                R.string.label_start_session
                            }
                        }
                        Text(text = stringResource(stringRes))
                    },
                )
            } else {
                SelectPlan(onSelectPlanClick = onSelectPlanClick)
            }
        }
    }
}

@Composable
private fun ColumnScope.StartSession(
    onStartSessionClick: () -> Unit,
    content: @Composable () -> Unit,
    buttonText: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
    TertiaryKenkoButton(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(bottom = 16.dp),
        onClick = onStartSessionClick,
        label = buttonText,
        icon = {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = KenkoIcons.ArrowOutward,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun ColumnScope.SelectPlan(
    onSelectPlanClick: () -> Unit,
) {
    Spacer(modifier = Modifier.weight(1F))
    Text(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(horizontal = 16.dp),
        text = stringResource(R.string.label_selecting_a_plan),
        style = MaterialTheme.typography.header().copy(
            lineBreak = LineBreak.Heading,
        ),
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.weight(1F))
    Button(
        modifier = Modifier.align(CenterHorizontally),
        onClick = onSelectPlanClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
        ),
        contentPadding = PaddingValues(
            vertical = 24.dp,
            horizontal = 40.dp,
        ),
    ) {
        Text(text = stringResource(R.string.label_select_plan_one))
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = KenkoIcons.ArrowOutward,
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KenkoTopBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_icon),
                    contentDescription = null,
                    modifier = Modifier.clip(CircleShape)
                )
                Text(
                    text = "KENKO",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        actions = actions,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HomePreview() {
    KenkoTheme {
        Home(
            state = HomeUiData(
                isPlanSelected = true,
                isSessionStarted = true,
                isTodayEmpty = false,
                isFirstSession = false,
                currentPlanId = null,
                sessionDates = emptySet(),
            ),
        )
    }
}

@Preview
@Composable
private fun StartTodayPreview() {
    KenkoTheme {
        Home(
            state = HomeUiData(
                isPlanSelected = true,
                isSessionStarted = false,
                isTodayEmpty = false,
                isFirstSession = false,
                currentPlanId = null,
                sessionDates = emptySet(),
            ),
        )
    }
}

@Preview
@Composable
private fun TodayEmptyPreview() {
    KenkoTheme {
        Home(
            state = HomeUiData(
                isPlanSelected = true,
                isSessionStarted = false,
                isTodayEmpty = true,
                isFirstSession = false,
                currentPlanId = null,
                sessionDates = emptySet(),
            ),
        )
    }
}

@Preview
@Composable
private fun FirstStartHomePreview() {
    KenkoTheme {
        Home(
            state = HomeUiData(
                isPlanSelected = false,
                isSessionStarted = false,
                isTodayEmpty = false,
                isFirstSession = true,
                currentPlanId = null,
                sessionDates = emptySet(),
            ),
        )
    }
}
