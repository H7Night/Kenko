package com.h7night.tore.ui.feature.statistics

import com.h7night.tore.data.repository.StatisticsRepository
import com.h7night.tore.domain.model.CountType
import com.h7night.tore.domain.model.Exercise
import com.h7night.tore.domain.model.SessionSummary
import com.h7night.tore.domain.model.Tag
import com.h7night.tore.domain.model.today
import com.h7night.tore.testutil.FakeExerciseRepo
import com.h7night.tore.testutil.FakePlanRepo
import com.h7night.tore.testutil.FakeSessionRepo
import com.h7night.tore.testutil.FakeSetsDao
import com.h7night.tore.testutil.FakeTagRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    @Test
    fun `viewmodel weeklyCounts aggregates correctly`() = runTest {
        val today = today()
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench Press"), setCount = 1),
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        // 生产形态：TagMapper.toExternal 不填充 parentName（恒 null），仅有 parentId
        val chestTag = Tag(id = 9, name = "中胸", parentId = 1)
        val legTag = Tag(id = 15, name = "股四头肌", parentId = 3)
        val exercises = listOf(
            Exercise(name = "Bench Press", tags = listOf(chestTag), countType = CountType.REPS),
            Exercise(name = "Squat", tags = listOf(legTag), countType = CountType.REPS),
        )
        val allTags = listOf(
            Tag(id = 1, name = "胸"),
            Tag(id = 3, name = "腿"),
            chestTag,
            legTag,
        )
        val repository = StatisticsRepository(
            sessionRepo = FakeSessionRepo(summaries = summaries),
            setsDao = FakeSetsDao(),
            exerciseRepo = FakeExerciseRepo(exercises = exercises),
            tagRepo = FakeTagRepo(tags = allTags),
            planRepo = FakePlanRepo(),
            appScope = backgroundScope,
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )
        val vm = StatisticsViewModel(repository)
        val state = vm.state.first { it != null && it.weeklyCounts.isNotEmpty() }!!
        assertEquals(1, state.weeklyCounts["胸"])
        assertEquals(1, state.weeklyCounts["腿"])
    }
}
