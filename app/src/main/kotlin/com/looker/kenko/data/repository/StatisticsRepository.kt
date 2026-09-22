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

package com.looker.kenko.data.repository

import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.di.ApplicationScope
import com.looker.kenko.di.DefaultDispatcher
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.domain.model.today
import com.looker.kenko.domain.statistics.StatisticsUiState
import com.looker.kenko.domain.statistics.aggregateStatistics
import com.looker.kenko.domain.statistics.cardioExerciseIds
import com.looker.kenko.utils.SharingStartedDefault
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/**
 * 统计页数据管道（应用作用域单例）：
 * - 输入用廉价来源（session summaries + 有氧聚合查询 + 小表 flows），不再订阅带 N+1 的 `sessionRepo.stream`；
 * - 聚合在 [defaultDispatcher]（后台）执行；
 * - `stateIn` 在应用作用域缓存最后结果：再次进入统计页瞬时拿到上次值，上游失效后自动刷新。
 * `state == null` 表示尚无结果（UI 显示骨架）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class StatisticsRepository @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val setsDao: SetsDao,
    private val exerciseRepo: ExerciseRepo,
    private val tagRepo: TagRepo,
    private val planRepo: PlanRepo,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    @Volatile
    private var hasEmitted = false

    val state: StateFlow<StatisticsUiState?> =
        combine(
            sessionRepo.streamSummaries,
            exerciseRepo.stream,
            tagRepo.stream,
            planRepo.current,
        ) { summaries, exercises, tags, plan -> Inputs(summaries, exercises, tags, plan) }
            .flatMapLatest { input ->
                val cardioIds = cardioExerciseIds(input.exercises, input.tags)
                val cardioFlow = if (cardioIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    setsDao.streamCardioMinutesByDate(cardioIds).map { rows ->
                        rows.associate { LocalDate.fromEpochDays(it.date.value.toLong()) to it.minutes }
                    }
                }
                cardioFlow.map { cardio ->
                    val startNanos = System.nanoTime()
                    val result = aggregateStatistics(
                        summaries = input.summaries,
                        cardioMinutesByDate = cardio,
                        exercises = input.exercises,
                        allTags = input.tags,
                        plan = input.plan,
                        today = today(),
                    )
                    StatisticsTiming.logAggregation(
                        durationNanos = System.nanoTime() - startNanos,
                        threadName = Thread.currentThread().name,
                        cold = !hasEmitted,
                    )
                    hasEmitted = true
                    result
                }
            }
            .flowOn(defaultDispatcher)
            .stateIn(appScope, SharingStarted.WhileSubscribed(SharingStartedDefault), initialValue = null)

    private data class Inputs(
        val summaries: List<SessionSummary>,
        val exercises: List<Exercise>,
        val tags: List<Tag>,
        val plan: Plan?,
    )
}
