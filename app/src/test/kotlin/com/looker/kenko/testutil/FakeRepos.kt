/*
 * Shared test doubles for repository-level unit tests.
 */

package com.looker.kenko.testutil

import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.local.model.CardioMinutesByDate
import com.looker.kenko.data.local.model.SetEntity
import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.data.repository.SessionRepo
import com.looker.kenko.data.repository.TagRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Set
import com.looker.kenko.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

internal class FakeSessionRepo(
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
    override suspend fun addSet(sessionId: Int, set: Set) = Unit
    override suspend fun addSet(sessionId: Int, exerciseId: Int, weight: Float, reps: Int) = Unit
    override suspend fun updateSet(setId: Int, reps: Int, weight: Float) = Unit
    override suspend fun removeSet(setId: Int) = Unit
    override suspend fun clearSets(date: LocalDate) = Unit
    override suspend fun updateDayIndex(date: LocalDate, dayIndex: Int) = Unit
    override suspend fun updateSessionDuration(sessionId: Int, durationSeconds: Long) = Unit
    override suspend fun getSessionIdOrCreate(date: LocalDate): Int = 1
    override suspend fun snapshotPlanDayTitles(planId: Int) = Unit
    override fun streamByDate(date: LocalDate): Flow<Session?> = MutableStateFlow(null)
    override fun previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?> =
        MutableStateFlow(null)
    override suspend fun getSets(sessionId: Int): List<Set> = emptyList()
    override suspend fun deleteSession(session: Session) = Unit
    override suspend fun deleteSessionById(id: Int) = Unit
}

internal class FakeExerciseRepo(exercises: List<Exercise> = emptyList()) : ExerciseRepo {
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

internal class FakePlanRepo(plan: Plan? = null) : PlanRepo {
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
    override suspend fun createPlan(
        name: String,
        description: String?,
        difficulty: com.looker.kenko.domain.model.Labels.Difficulty?,
        focus: com.looker.kenko.domain.model.Labels.Focus?,
        equipment: com.looker.kenko.domain.model.Labels.Equipment?,
        time: com.looker.kenko.domain.model.Labels.Time?,
    ): Int = 1
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

internal class FakeTagRepo(private val tags: List<Tag> = emptyList()) : TagRepo {
    private val _stream = MutableStateFlow(tags)
    override val stream: Flow<List<Tag>> = _stream
    override val streamParents: Flow<List<Tag>> = MutableStateFlow(tags.filter { it.parentId == null })
    override fun streamChildren(parentId: Int): Flow<List<Tag>> = MutableStateFlow(tags.filter { it.parentId == parentId })
    override suspend fun get(id: Int): Tag? = tags.find { it.id == id }
    override suspend fun upsert(tag: Tag) = Unit
    override suspend fun delete(tag: Tag) = Unit
    override suspend fun deleteById(id: Int) = Unit
    override suspend fun exerciseCount(tagId: Int): Int = 0
    override suspend fun getTagsForExercise(exerciseId: Int): List<Tag> = emptyList()
    override fun streamTagsForExercise(exerciseId: Int): Flow<List<Tag>> = MutableStateFlow(emptyList())
}

internal class FakeSetsDao(
    private val cardio: List<CardioMinutesByDate> = emptyList(),
) : SetsDao {
    var cardioQueryCount = 0
        private set

    override fun setsBySessionId(sessionId: Int): Flow<List<SetEntity>> = MutableStateFlow(emptyList())
    override suspend fun getSetsBySessionId(sessionId: Int): List<SetEntity> = emptyList()
    override suspend fun getSetsCountBySessionId(sessionId: Int): Int? = 0
    override fun setsByExerciseIdPerPlan(exerciseId: Int?, planId: Int?): Flow<List<SetEntity>> = MutableStateFlow(emptyList())
    override suspend fun getSetsByExerciseIdPerPlan(exerciseId: Int?, planId: Int?): List<SetEntity> = emptyList()
    override fun totalSetCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun insert(set: SetEntity) = Unit
    override suspend fun update(set: SetEntity) = Unit
    override suspend fun update(setId: Int, reps: Int, weight: Float) = Unit
    override suspend fun delete(setId: Int) = Unit
    override suspend fun deleteBySessionId(sessionId: Int) = Unit
    override suspend fun hasSetsForExercise(exerciseId: Int): Boolean = false
    override fun streamCardioMinutesByDate(exerciseIds: List<Int>): Flow<List<CardioMinutesByDate>> {
        cardioQueryCount++
        return MutableStateFlow(cardio)
    }
}
