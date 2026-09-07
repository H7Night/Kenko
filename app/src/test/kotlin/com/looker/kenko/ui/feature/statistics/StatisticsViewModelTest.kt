package com.looker.kenko.ui.feature.statistics

import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class StatisticsViewModelTest {

    private class FakeSessionRepo(
        summaries: List<SessionSummary> = emptyList(),
        sessions: List<Session> = emptyList(),
    ) : SessionRepo {
        private val _summaries = MutableStateFlow(summaries)
        private val _stream = MutableStateFlow(sessions)
        override val stream: Flow<List<Session>> = _stream
        override val streamSummaries: Flow<List<SessionSummary>> = _summaries
        override val planDateRanges: Flow<Map<Int, Pair<LocalDate, LocalDate>>> = MutableStateFlow(emptyMap())
        override val setsCount: Flow<Int> = MutableStateFlow(0)
        override val earliestSessionDate: Flow<LocalDate?> = MutableStateFlow(null)
        override suspend fun addSet(sessionId: Int, set: com.looker.kenko.domain.model.Set) = Unit
        override suspend fun addSet(sessionId: Int, exerciseId: Int, weight: Float, reps: Int) = Unit
        override suspend fun updateSet(setId: Int, reps: Int, weight: Float) = Unit
        override suspend fun removeSet(setId: Int) = Unit
        override suspend fun clearSets(date: LocalDate) = Unit
        override suspend fun updateDayIndex(date: LocalDate, dayIndex: Int) = Unit
        override suspend fun updateSessionDuration(sessionId: Int, durationSeconds: Long) = Unit
        override suspend fun getSessionIdOrCreate(date: LocalDate): Int = 1
        override suspend fun snapshotPlanDayTitles(planId: Int) = Unit
        override fun streamByDate(date: LocalDate): Flow<Session?> = MutableStateFlow(null)
        override fun previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?> = MutableStateFlow(null)
        override suspend fun getSets(sessionId: Int): List<com.looker.kenko.domain.model.Set> = emptyList()
        override suspend fun deleteSession(session: Session) = Unit
        override suspend fun deleteSessionById(id: Int) = Unit
    }

    private class FakeExerciseRepo(exercises: List<Exercise> = emptyList()) : ExerciseRepo {
        private val _stream = MutableStateFlow(exercises)
        override val stream: Flow<List<Exercise>> = _stream
        override val numberOfExercise: Flow<Int> = MutableStateFlow(0)
        override suspend fun get(id: Int): Exercise? = null
        override suspend fun upsert(exercise: Exercise) = Unit
        override suspend fun remove(id: Int) = Unit
        override suspend fun isExerciseAvailable(name: String): Boolean = false
        override suspend fun getOrCreate(exercise: Exercise): Exercise = exercise
        override suspend fun hasHistory(id: Int): Boolean = false
    }

    private class FakePlanRepo(plan: Plan? = null) : PlanRepo {
        private val _current = MutableStateFlow(plan)
        override val plans: Flow<List<Plan>> = MutableStateFlow(emptyList())
        override val current: Flow<Plan?> = _current
        override val planItems: Flow<List<com.looker.kenko.domain.model.PlanItem>> = MutableStateFlow(emptyList())
        override fun planItems(day: Int): Flow<List<com.looker.kenko.domain.model.PlanItem>> = MutableStateFlow(emptyList())
        override fun planItemsByPlan(id: Int): Flow<List<com.looker.kenko.domain.model.PlanItem>> = MutableStateFlow(emptyList())
        override fun planItems(id: Int, day: Int): Flow<List<com.looker.kenko.domain.model.PlanItem>> = MutableStateFlow(emptyList())
        override fun activeExercises(day: Int): Flow<List<Exercise>> = MutableStateFlow(emptyList())
        override suspend fun plan(id: Int): Plan? = null
        override suspend fun planNameExists(name: String): Boolean = false
        override suspend fun getPlanItems(id: Int): List<com.looker.kenko.domain.model.PlanItem> = emptyList()
        override suspend fun getPlanItems(id: Int, day: Int): List<com.looker.kenko.domain.model.PlanItem> = emptyList()
        override suspend fun createPlan(name: String, description: String?, difficulty: com.looker.kenko.domain.model.Labels.Difficulty?, focus: com.looker.kenko.domain.model.Labels.Focus?, equipment: com.looker.kenko.domain.model.Labels.Equipment?, time: com.looker.kenko.domain.model.Labels.Time?): Int = 1
        override suspend fun updatePlan(plan: Plan) = Unit
        override suspend fun setCurrent(id: Int) = Unit
        override suspend fun deletePlan(id: Int) = Unit
        override suspend fun addItem(planItem: com.looker.kenko.domain.model.PlanItem) = Unit
        override suspend fun removeItem(id: Long) = Unit
        override suspend fun updateOrder(planId: Int, day: Int, exercises: List<Exercise>) = Unit
        override suspend fun updateDayIndex(planId: Int, dayIndex: Int) = Unit
        override suspend fun advanceDay(planId: Int, actualDayIndex: Int) = Unit
        override suspend fun addDay(planId: Int) = Unit
        override suspend fun deleteDay(planId: Int, dayIndex: Int) = Unit
        override suspend fun moveDay(planId: Int, from: Int, to: Int) = Unit
    }

    @Test
    fun `viewmodel weeklyCounts aggregates correctly`() = runTest {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench Press"), setCount = 1),
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        val chestTag = Tag(name = "中胸", parentName = "胸")
        val legTag = Tag(name = "股四头肌", parentName = "腿")
        val exercises = listOf(
            Exercise(name = "Bench Press", tags = listOf(chestTag), countType = CountType.REPS),
            Exercise(name = "Squat", tags = listOf(legTag), countType = CountType.REPS),
        )
        val fakeSessionRepo = FakeSessionRepo(summaries = summaries, sessions = emptyList())
        val fakeExerciseRepo = FakeExerciseRepo(exercises = exercises)
        val fakePlanRepo = FakePlanRepo(plan = null)
        val vm = StatisticsViewModel(fakeSessionRepo, fakeExerciseRepo, fakePlanRepo)
        val state = vm.state.first { it.weeklyCounts.isNotEmpty() || it.sessionDates.isNotEmpty() }
        // weekly should contain chest count 1 this week
        assertEquals(1, state.weeklyCounts["胸"])
    }
}
