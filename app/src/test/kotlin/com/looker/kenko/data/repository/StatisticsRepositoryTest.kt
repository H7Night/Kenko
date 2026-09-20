package com.looker.kenko.data.repository

import com.looker.kenko.data.local.model.CardioMinutesByDate
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.domain.model.today
import com.looker.kenko.testutil.FakeExerciseRepo
import com.looker.kenko.testutil.FakePlanRepo
import com.looker.kenko.testutil.FakeSessionRepo
import com.looker.kenko.testutil.FakeSetsDao
import com.looker.kenko.testutil.FakeTagRepo
import com.looker.kenko.utils.EpochDays
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsRepositoryTest {

    @Test
    fun `state is null before first computation`() = runTest {
        val repo = build()
        assertNull(repo.state.value)
    }

    @Test
    fun `emits aggregated state and queries cardio once`() = runTest {
        val today = today()
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench"), setCount = 1),
        )
        val cardioRows = listOf(CardioMinutesByDate(date = EpochDays(today.toEpochDays().toInt()), minutes = 30))
        val setsDao = FakeSetsDao(cardioRows)
        val repo = build(
            summaries = summaries,
            exercises = listOf(
                Exercise(name = "Bench", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS),
                Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES, id = 7),
            ),
            tags = listOf(
                Tag(id = 1, name = "胸"),
                Tag(id = 7, name = "有氧"),
                Tag(id = 9, name = "中胸", parentId = 1),
                Tag(id = 28, name = "跑步", parentId = 7),
            ),
            setsDao = setsDao,
        )
        val state = repo.state.first { it != null }!!
        assertEquals(30, state.cardioWeekly)
        assertEquals(1, setsDao.cardioQueryCount)
    }

    private fun TestScope.build(
        summaries: List<SessionSummary> = emptyList(),
        exercises: List<Exercise> = emptyList(),
        tags: List<Tag> = emptyList(),
        setsDao: FakeSetsDao = FakeSetsDao(),
    ): StatisticsRepository = StatisticsRepository(
        sessionRepo = FakeSessionRepo(summaries = summaries),
        setsDao = setsDao,
        exerciseRepo = FakeExerciseRepo(exercises = exercises),
        tagRepo = FakeTagRepo(tags = tags),
        planRepo = FakePlanRepo(),
        appScope = backgroundScope,
        defaultDispatcher = StandardTestDispatcher(testScheduler),
    )
}
