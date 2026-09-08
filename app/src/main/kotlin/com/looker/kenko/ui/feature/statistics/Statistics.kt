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

package com.looker.kenko.ui.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.kenko.R
import com.looker.kenko.ui.extension.plus
import com.looker.kenko.ui.feature.statistics.components.AdherenceCard
import com.looker.kenko.ui.feature.statistics.components.BalanceRingCard
import com.looker.kenko.ui.feature.statistics.components.BodyPartBarCard
import com.looker.kenko.ui.feature.statistics.components.TrendCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Statistics(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.label_statistics)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeatmapCard(
                    sessionDates = state.sessionDates,
                    today = state.today,
                    countByDate = state.countByDate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                BalanceRingCard(
                    monthlyCounts = state.monthlyCounts,
                    cardioMonthly = state.cardioMonthly,
                )
            }
            item {
                BodyPartBarCard(
                    title = "本周",
                    counts = state.weeklyCounts,
                    cardioMinutes = state.cardioWeekly,
                )
            }
            item {
                BodyPartBarCard(
                    title = "本月",
                    counts = state.monthlyCounts,
                    cardioMinutes = state.cardioMonthly,
                )
            }
            item {
                BodyPartBarCard(
                    title = "本计划",
                    counts = state.planCounts,
                    cardioMinutes = state.cardioPlan,
                )
            }
            item {
                TrendCard(
                    weeklyTrend = state.weeklyTrend,
                )
            }
            item {
                AdherenceCard(
                    actualDays = state.actualDays,
                    plannedDays = state.plannedDays,
                )
            }
        }
    }
}
