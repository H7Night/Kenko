# Kenko 统计页性能优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把统计页从「进入等待 1~2 秒」改为「有缓存瞬时显示、无缓存骨架首帧 <100ms，聚合成全程后台、不再有 N+1」。

**Architecture:** 新增进程内单例 `StatisticsRepository`，用 Room 轻量来源（`streamSummaries` + 有氧专用聚合查询 + 小表 flows）在 `Dispatchers.Default` 上做纯内存聚合，经 `stateIn(appScope, WhileSubscribed(5_000), null)` 提供缓存；`StatisticsViewModel` 只消费该 StateFlow，UI 在 `null` 时显示骨架。同时把共享层 `LocalSessionRepo` 的逐组查询改为批量（保持接口不变）。

**Tech Stack:** Kotlin、Jetpack Compose（Material3 1.5.0-alpha08）、Hilt、Room 2.8.3、kotlinx-coroutines、JUnit5。

## Global Constraints

- 平台：Windows 10 / pwsh7；用 `.\gradlew.bat` 运行 Gradle（不要用系统 gradle）。
- 分支：`feat/kenko-faster`（基线 `feat/v1.9.0`）。禁止在 `main` 上改动。未经用户明确指示不要 push/merge。
- 一个功能一个提交；提交信息用 Conventional Commits（中文描述，如 `perf(statistics): …`）。
- 所有用户可见文案走 `res/values/strings.xml` + `res/values-zh/strings.xml`，禁止在 Composable 里硬编码文案。
- 单测：JUnit5（`org.junit.jupiter.api.*`）+ `kotlinx-coroutines-test`，位于 `app/src/test/kotlin/…`；instrumented 在 `app/src/androidTest/…`。
- 不改数据库 schema、不做数据迁移（本计划不新增表/列/索引）。
- 保持 `SessionRepo`/`ExerciseRepo`/`TagRepo`/`PlanRepo` 接口签名不变（向后兼容）。
- 统计口径与展示内容必须与优化前一致。

## 文件结构（新增/修改）

- `data/local/dao/ExerciseDao.kt`（改）：新增 `getByIds(...)`。
- `data/mapper/SetMapper.kt`（改）：新增可测的 `mapSetEntities(...)`（批量映射，注入 loader）。
- `data/repository/local/LocalSessionRepo.kt`（改）：用 `getByIds` + `mapSetEntities` 替换逐组查询。
- `data/local/model/SetEntity.kt`（改）：新增投影 `CardioMinutesByDate`。
- `data/local/dao/SetsDao.kt`（改）：新增 `streamCardioMinutesByDate(...)`。
- `domain/statistics/Aggregation.kt`（改）：新增 `cardioExerciseIds(...)`。
- `domain/statistics/StatisticsUiState.kt`（新建）：从 ViewModel 移出 `StatisticsUiState`。
- `domain/statistics/StatisticsAggregation.kt`（新建）：纯函数 `aggregateStatistics(...)`（含 `buildBodyParts`/`buildWeeklyTrend`）。
- `data/repository/StatisticsRepository.kt`（新建）：单例、缓存、后台聚合。
- `ui/feature/statistics/StatisticsViewModel.kt`（改）：只消费 repository。
- `ui/feature/statistics/components/StatisticsSkeleton.kt`（新建）：骨架卡片。
- `ui/feature/statistics/Statistics.kt`（改）：`null` 时显示骨架。
- `app/src/test/kotlin/com/looker/kenko/testutil/FakeRepos.kt`（新建）：共享测试替身。

---

### Task 1: 批量获取 exercise（修 N+1）

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/ExerciseDao.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/mapper/SetMapper.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalSessionRepo.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/data/mapper/SetMapperN1Test.kt`

**Interfaces:**
- Produces:
  - `ExerciseDao.getByIds(ids: List<Int>): List<ExerciseEntity>`
  - `suspend fun mapSetEntities(entities: List<SetEntity>, loadExercises: suspend (ids: List<Int>) -> List<ExerciseEntity>): List<Set>`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.looker.kenko.data.mapper

import com.looker.kenko.data.local.model.ExerciseEntity
import com.looker.kenko.data.local.model.SetEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetMapperN1Test {

    @Test
    fun `loads exercises once for many sets`() = runTest {
        var loadCalls = 0
        val exercises = listOf(
            ExerciseEntity(name = "Bench", id = 1),
            ExerciseEntity(name = "Squat", id = 2),
        )
        val sets = listOf(
            SetEntity(repsOrDuration = 10, weight = 40f, order = 0, sessionId = 1, exerciseId = 1, id = 11),
            SetEntity(repsOrDuration = 8, weight = 60f, order = 1, sessionId = 1, exerciseId = 1, id = 12),
            SetEntity(repsOrDuration = 5, weight = 80f, order = 2, sessionId = 1, exerciseId = 2, id = 13),
        )

        val result = mapSetEntities(sets) { ids ->
            loadCalls++
            exercises.filter { it.id in ids }
        }

        assertEquals(1, loadCalls)               // 一次批量加载，而非每 set 一次
        assertEquals(3, result.size)
        assertEquals("Bench", result[0].exercise.name)
        assertEquals("Squat", result[2].exercise.name)
    }

    @Test
    fun `empty input performs no load`() = runTest {
        var loadCalls = 0
        val result = mapSetEntities(emptyList()) { loadCalls++; emptyList() }
        assertEquals(0, loadCalls)
        assertEquals(0, result.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.data.mapper.SetMapperN1Test"`
Expected: FAIL（`mapSetEntities` unresolved / 编译失败）

- [ ] **Step 3: Add `getByIds` to `ExerciseDao`**

在 `ExerciseDao.kt` 的 `getByName` 之后加入：

```kotlin
    @Query(
        """
        SELECT *
        FROM exercises
        WHERE id IN (:ids)
        """,
    )
    suspend fun getByIds(ids: List<Int>): List<ExerciseEntity>
```

- [ ] **Step 4: Add `mapSetEntities` to `SetMapper.kt`**

在 `SetMapper.kt` 文件末尾追加（补 import）：

```kotlin
import com.looker.kenko.data.local.model.ExerciseEntity

/**
 * 批量把 SetEntity 映射为领域 Set：一次性加载所有 exercise，避免逐组查询（N+1）。
 * loader 注入以便单测。
 */
suspend fun mapSetEntities(
    entities: List<SetEntity>,
    loadExercises: suspend (ids: List<Int>) -> List<ExerciseEntity>,
): List<Set> {
    if (entities.isEmpty()) return emptyList()
    val exerciseById = loadExercises(entities.map { it.exerciseId }.distinct())
        .associate { it.id to it.toExternal() }
    return entities.mapNotNull { entity ->
        exerciseById[entity.exerciseId]?.let { entity.toExternal(it) }
    }
}
```

- [ ] **Step 5: Use it in `LocalSessionRepo`**

把 `LocalSessionRepo.kt` 末尾的私有函数替换为：

```kotlin
    private suspend fun List<SetEntity>.toExternal(): List<Set> =
        mapSetEntities(this) { ids -> exerciseDao.getByIds(ids) }
```

（`stream`、`streamByDate`、`getSets` 的调用点保持不变。）

- [ ] **Step 6: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.data.mapper.SetMapperN1Test"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/local/dao/ExerciseDao.kt app/src/main/kotlin/com/looker/kenko/data/mapper/SetMapper.kt app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalSessionRepo.kt app/src/test/kotlin/com/looker/kenko/data/mapper/SetMapperN1Test.kt
git commit -m "perf(data): batch-load exercises to remove set N+1"
```

---

### Task 2: 有氧专用聚合查询 + 纯函数

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/model/SetEntity.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/SetsDao.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/statistics/Aggregation.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/domain/statistics/CardioExerciseIdsTest.kt`

**Interfaces:**
- Produces:
  - `data class CardioMinutesByDate(val date: EpochDays, val minutes: Int)`
  - `SetsDao.streamCardioMinutesByDate(exerciseIds: List<Int>): Flow<List<CardioMinutesByDate>>`
  - `fun cardioExerciseIds(exercises: List<Exercise>, allTags: List<Tag>): List<Int>`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardioExerciseIdsTest {

    @Test
    fun `resolves cardio by parent tag name`() {
        val allTags = listOf(
            Tag(id = 7, name = CARDIO_PART),
            Tag(id = 28, name = "跑步", parentId = 7),
        )
        val exercises = listOf(
            Exercise(name = "跑步机", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES, id = 101),
            Exercise(name = "卧推", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS, id = 102),
        )
        assertEquals(listOf(101), cardioExerciseIds(exercises, allTags))
    }

    @Test
    fun `unknown part is not cardio`() {
        val exercises = listOf(Exercise(name = "未知", tags = emptyList(), id = 5))
        assertEquals(emptyList<Int>(), cardioExerciseIds(exercises, emptyList()))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.statistics.CardioExerciseIdsTest"`
Expected: FAIL（`cardioExerciseIds` unresolved）

- [ ] **Step 3: Add `cardioExerciseIds` to `Aggregation.kt`**

在 `Aggregation.kt` 末尾追加：

```kotlin
/**
 * 解析出「解析部位 == 有氧」的动作 id 列表（与 [buildTagDict] 同一解析规则）。
 * 供有氧分钟下推到 SQL 使用。
 */
fun cardioExerciseIds(
    exercises: List<Exercise>,
    allTags: List<Tag>,
): List<Int> {
    val tagNameById = allTags.associate { it.id to it.name }
    return exercises.mapNotNull { ex ->
        val part = ex.tags.firstOrNull()?.let { tag ->
            tag.parentId?.let { tagNameById[it] } ?: tag.name
        }
        if (part == CARDIO_PART) ex.id else null
    }
}
```

- [ ] **Step 4: Add projection + query**

在 `SetEntity.kt` 末尾追加（`EpochDays` 使用 `com.looker.kenko.utils.EpochDays`）：

```kotlin
/** 轻量投影：按日期聚合的有氧分钟（供统计页下推查询）。 */
data class CardioMinutesByDate(
    val date: EpochDays,
    val minutes: Int,
)
```

在 `SetsDao.kt` 的 `totalSetCount()` 之后追加：

```kotlin
    @Query(
        """
        SELECT s.date AS date, SUM(st.reps) AS minutes
        FROM sets st
        JOIN sessions s ON s.id = st.sessionId
        WHERE st.exerciseId IN (:exerciseIds)
        GROUP BY s.date
        """,
    )
    fun streamCardioMinutesByDate(exerciseIds: List<Int>): Flow<List<CardioMinutesByDate>>
```

（补 import：`com.looker.kenko.data.local.model.CardioMinutesByDate`。）

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.statistics.CardioExerciseIdsTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/local/model/SetEntity.kt app/src/main/kotlin/com/looker/kenko/data/local/dao/SetsDao.kt app/src/main/kotlin/com/looker/kenko/domain/statistics/Aggregation.kt app/src/test/kotlin/com/looker/kenko/domain/statistics/CardioExerciseIdsTest.kt
git commit -m "perf(data): add cardio minutes aggregation query"
```

---

### Task 3: 纯聚合函数 `aggregateStatistics` + 迁出 `StatisticsUiState`

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/statistics/StatisticsUiState.kt`
- Create: `app/src/main/kotlin/com/looker/kenko/domain/statistics/StatisticsAggregation.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt`（移除已迁移内容）
- Test: `app/src/test/kotlin/com/looker/kenko/domain/statistics/StatisticsAggregationTest.kt`

**Interfaces:**
- Produces: `fun aggregateStatistics(summaries: List<SessionSummary>, cardioMinutesByDate: Map<LocalDate, Int>, exercises: List<Exercise>, allTags: List<Tag>, plan: Plan?, today: LocalDate): StatisticsUiState`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatisticsAggregationTest {

    private val allTags = listOf(
        Tag(id = 1, name = "胸"),
        Tag(id = 3, name = "腿"),
        Tag(id = 7, name = CARDIO_PART),
        Tag(id = 9, name = "中胸", parentId = 1),
        Tag(id = 15, name = "股四头肌", parentId = 3),
        Tag(id = 28, name = "跑步", parentId = 7),
    )
    private val exercises = listOf(
        Exercise(name = "Bench", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS),
        Exercise(name = "Squat", tags = listOf(Tag(id = 15, name = "股四头肌", parentId = 3)), countType = CountType.REPS),
        Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES),
    )

    @Test
    fun `counts strength parts and cardio minutes`() {
        val today = LocalDate(2026, 9, 18) // Friday
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench", "Treadmill"), setCount = 2),
            SessionSummary(date = LocalDate(2026, 9, 17), planId = 1, exerciseNames = listOf("Squat"), setCount = 1),
        )
        val cardio = mapOf(today to 30, LocalDate(2026, 9, 17) to 10)
        val plan = Plan(name = "PPL", description = null, difficulty = null, focus = null, equipment = null, time = null, isActive = true, dayCount = 4, id = 1)

        val state = aggregateStatistics(summaries, cardio, exercises, allTags, plan, today)

        assertEquals(1, state.weeklyCounts["胸"])
        assertEquals(1, state.weeklyCounts["腿"])
        assertEquals(40, state.cardioWeekly)
        assertEquals(2, state.actualDays)
        assertEquals(4, state.plannedDays)
        assertEquals(true, state.heatmapData.weeks.isNotEmpty())
    }

    @Test
    fun `no plan yields zero planned days`() {
        val today = LocalDate(2026, 9, 18)
        val state = aggregateStatistics(emptyList(), emptyMap(), exercises, allTags, null, today)
        assertEquals(0, state.plannedDays)
        assertEquals(12, state.weeklyTrend.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.statistics.StatisticsAggregationTest"`
Expected: FAIL（`aggregateStatistics` unresolved）

- [ ] **Step 3: Create `StatisticsUiState.kt`**

把 `StatisticsViewModel.kt` 中的 `data class StatisticsUiState(...)`（含默认值）**原样移动**到：

```kotlin
package com.looker.kenko.domain.statistics

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class StatisticsUiState(
    val sessionDates: Set<LocalDate> = emptySet(),
    val countByDate: Map<LocalDate, Int> = emptyMap(),
    val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val heatmapData: HeatmapData = buildHeatmapData90d(
        emptySet(),
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    ),
    val bodyParts: List<String> = emptyList(),
    val weeklyCounts: Map<String, Int> = emptyMap(),
    val monthlyCounts: Map<String, Int> = emptyMap(),
    val planCounts: Map<String, Int> = emptyMap(),
    val cardioWeekly: Int = 0,
    val cardioMonthly: Int = 0,
    val cardioPlan: Int = 0,
    val cardioMinutesWeekly: Int = 0,
    val cardioMinutesMonthly: Int = 0,
    val cardioMinutesPlan: Int = 0,
    val weeklyTrend: List<Int> = emptyList(),
    val actualDays: Int = 0,
    val plannedDays: Int = 0,
)
```

- [ ] **Step 4: Create `StatisticsAggregation.kt`**

```kotlin
package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 纯函数：把统计输入聚合成 UI 状态（无副作用，可在后台线程调用）。 */
fun aggregateStatistics(
    summaries: List<SessionSummary>,
    cardioMinutesByDate: Map<LocalDate, Int>,
    exercises: List<Exercise>,
    allTags: List<Tag>,
    plan: Plan?,
    today: LocalDate,
): StatisticsUiState {
    val tagDict = buildTagDict(exercises, allTags)
    val bodyParts = buildBodyParts(allTags)
    val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val dates = summaries.map { it.date }.toSet()
    val countByDate = summaries.groupBy { it.date }.mapValues { it.value.size }

    val weeklyCounts = aggregateByBodyPart(summaries, { it >= monday }, tagDict)
    val monthlyCounts = aggregateByBodyPart(
        summaries,
        { it.year == today.year && it.month == today.month },
        tagDict,
    )
    val planCounts = if (plan?.id == null) {
        emptyMap()
    } else {
        aggregateByBodyPart(summaries.filter { it.planId == plan.id }, { true }, tagDict)
    }

    fun cardioSum(predicate: (LocalDate) -> Boolean): Int =
        cardioMinutesByDate.entries.filter { predicate(it.key) }.sumOf { it.value }

    val cardioWeekly = cardioSum { it >= monday }
    val cardioMonthly = cardioSum { it.year == today.year && it.month == today.month }
    val planDates = if (plan?.id == null) emptySet() else
        summaries.filter { it.planId == plan.id }.map { it.date }.toSet()
    val cardioPlan = cardioSum { it in planDates }

    return StatisticsUiState(
        sessionDates = dates,
        countByDate = countByDate,
        today = today,
        heatmapData = buildHeatmapData90d(dates, today),
        bodyParts = bodyParts,
        weeklyCounts = weeklyCounts,
        monthlyCounts = monthlyCounts,
        planCounts = planCounts,
        cardioWeekly = cardioWeekly,
        cardioMonthly = cardioMonthly,
        cardioPlan = cardioPlan,
        cardioMinutesWeekly = cardioWeekly,
        cardioMinutesMonthly = cardioMonthly,
        cardioMinutesPlan = cardioPlan,
        weeklyTrend = buildWeeklyTrend(summaries, today),
        actualDays = summaries.filter { it.date.year == today.year && it.date.month == today.month }
            .map { it.date }.toSet().size,
        plannedDays = plan?.dayCount ?: 0,
    )
}

private fun buildWeeklyTrend(summaries: List<SessionSummary>, today: LocalDate): List<Int> {
    val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return (0 until 12).map { idx ->
        val weekStart = monday.minus((11 - idx) * 7, DateTimeUnit.DAY)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        summaries.count { it.date >= weekStart && it.date <= weekEnd }
    }
}

/** 一级部位（parentId==null）按 sortOrder 排序；有氧恒置末位。 */
private fun buildBodyParts(allTags: List<Tag>): List<String> =
    allTags.filter { it.parentId == null }
        .sortedBy { it.sortOrder }
        .map { it.name }
        .let { parts ->
            val cardio = parts.firstOrNull { it == CARDIO_PART }
            if (cardio == null) parts else (parts - cardio) + cardio
        }
```

- [ ] **Step 5: Slim `StatisticsViewModel.kt`（临时保留旧 combine 以便本 step 编译通过）**

本 step 只删除**已迁移**的 `StatisticsUiState`、`buildWeeklyTrend`、`buildBodyParts` 定义，并补 `import com.looker.kenko.domain.statistics.StatisticsUiState`。**暂不重写 `combine`**（Task 5 才替换为 repository）。确认不再有重复定义。

- [ ] **Step 6: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.statistics.StatisticsAggregationTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/statistics/StatisticsUiState.kt app/src/main/kotlin/com/looker/kenko/domain/statistics/StatisticsAggregation.kt app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt app/src/test/kotlin/com/looker/kenko/domain/statistics/StatisticsAggregationTest.kt
git commit -m "refactor(statistics): extract pure aggregateStatistics"
```

---

### Task 4: `StatisticsRepository`（单例 / SWR 缓存 / 后台聚合）+ 共享测试替身

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/data/repository/StatisticsRepository.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/testutil/FakeRepos.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/data/repository/StatisticsRepositoryTest.kt`

**Interfaces:**
- Consumes: `aggregateStatistics(...)`（Task 3）、`cardioExerciseIds(...)`（Task 2）、`SetsDao.streamCardioMinutesByDate(...)`（Task 2）
- Produces: `StatisticsRepository.state: StateFlow<StatisticsUiState?>`

- [ ] **Step 1: Create shared fakes**

新建 `app/src/test/kotlin/com/looker/kenko/testutil/FakeRepos.kt`：把 `StatisticsViewModelTest.kt` 里现有的 `FakeSessionRepo/FakeExerciseRepo/FakePlanRepo/FakeTagRepo` 复制为顶层 `internal class`（同签名），并新增完整 `FakeSetsDao`：

```kotlin
package com.looker.kenko.testutil

import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.data.local.model.CardioMinutesByDate
import com.looker.kenko.data.local.model.SetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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
```

（`FakeSessionRepo` 保留 `streamSummaries` 的 `MutableStateFlow`；`FakeExerciseRepo`/`FakeTagRepo`/`FakePlanRepo` 与现有实现一致。）

- [ ] **Step 2: Write the failing test**

```kotlin
package com.looker.kenko.data.repository

import com.looker.kenko.data.local.model.CardioMinutesByDate
import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.SessionSummary
import com.looker.kenko.domain.model.Tag
import com.looker.kenko.testutil.FakeExerciseRepo
import com.looker.kenko.testutil.FakePlanRepo
import com.looker.kenko.testutil.FakeSessionRepo
import com.looker.kenko.testutil.FakeSetsDao
import com.looker.kenko.testutil.FakeTagRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
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
        val today = LocalDate(2026, 9, 18)
        val summaries = listOf(
            SessionSummary(date = today, planId = 1, exerciseNames = listOf("Bench"), setCount = 1),
        )
        val cardioRows = listOf(CardioMinutesByDate(date = com.looker.kenko.utils.EpochDays(today.toEpochDays().toInt()), minutes = 30))
        val setsDao = FakeSetsDao(cardioRows)
        val repo = build(
            summaries = summaries,
            exercises = listOf(
                Exercise(name = "Bench", tags = listOf(Tag(id = 9, name = "中胸", parentId = 1)), countType = CountType.REPS),
                Exercise(name = "Treadmill", tags = listOf(Tag(id = 28, name = "跑步", parentId = 7)), countType = CountType.MINUTES, id = 7),
            ),
            tags = listOf(Tag(id = 1, name = "胸"), Tag(id = 7, name = "有氧"), Tag(id = 9, name = "中胸", parentId = 1), Tag(id = 28, name = "跑步", parentId = 7)),
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
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.data.repository.StatisticsRepositoryTest"`
Expected: FAIL（`StatisticsRepository` unresolved）

- [ ] **Step 4: Implement `StatisticsRepository`**

```kotlin
package com.looker.kenko.data.repository

import com.looker.kenko.data.local.dao.SetsDao
import com.looker.kenko.di.ApplicationScope
import com.looker.kenko.di.DefaultDispatcher
import com.looker.kenko.domain.model.today
import com.looker.kenko.domain.statistics.StatisticsUiState
import com.looker.kenko.domain.statistics.aggregateStatistics
import com.looker.kenko.domain.statistics.cardioExerciseIds
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

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
                    aggregateStatistics(
                        summaries = input.summaries,
                        cardioMinutesByDate = cardio,
                        exercises = input.exercises,
                        allTags = input.tags,
                        plan = input.plan,
                        today = today(),
                    )
                }
            }
            .flowOn(defaultDispatcher)
            .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private data class Inputs(
        val summaries: List<com.looker.kenko.domain.model.SessionSummary>,
        val exercises: List<com.looker.kenko.domain.model.Exercise>,
        val tags: List<com.looker.kenko.domain.model.Tag>,
        val plan: com.looker.kenko.domain.model.Plan?,
    )
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.data.repository.StatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/repository/StatisticsRepository.kt app/src/test/kotlin/com/looker/kenko/testutil/FakeRepos.kt app/src/test/kotlin/com/looker/kenko/data/repository/StatisticsRepositoryTest.kt
git commit -m "perf(statistics): add app-scoped cached statistics pipeline"
```

---

### Task 5: `StatisticsViewModel` 只消费 repository

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt`
- Modify: `app/src/test/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModelTest.kt`

**Interfaces:**
- Consumes: `StatisticsRepository.state`
- Produces: `StatisticsViewModel.state: StateFlow<StatisticsUiState?>`

- [ ] **Step 1: Update the test to the new shape**

把 `StatisticsViewModelTest.kt` 改为：使用 `testutil` 的共享 fakes 构造 `StatisticsRepository`（`appScope = backgroundScope`、`defaultDispatcher = StandardTestDispatcher(testScheduler)`），再构造 `StatisticsViewModel(repository)`；断言：

```kotlin
val vm = StatisticsViewModel(repository)
val state = vm.state.first { it != null && it.weeklyCounts.isNotEmpty() }!!
assertEquals(1, state.weeklyCounts["胸"])
assertEquals(1, state.weeklyCounts["腿"])
```

删除该文件内本地的 Fake* 定义（改用 `testutil.FakeRepos`）。

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.ui.feature.statistics.StatisticsViewModelTest"`
Expected: FAIL（`StatisticsViewModel(repository)` 构造不匹配）

- [ ] **Step 3: Rewrite `StatisticsViewModel`**

```kotlin
package com.looker.kenko.ui.feature.statistics

import androidx.lifecycle.ViewModel
import com.looker.kenko.data.repository.StatisticsRepository
import com.looker.kenko.domain.statistics.StatisticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: StatisticsRepository,
) : ViewModel() {
    val state: StateFlow<StatisticsUiState?> = repository.state
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.ui.feature.statistics.StatisticsViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt app/src/test/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModelTest.kt
git commit -m "refactor(statistics): viewmodel consumes cached repository"
```

---

### Task 6: 骨架屏 + UI 接线

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/StatisticsSkeleton.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/Statistics.kt`

**Interfaces:**
- Consumes: `StatisticsViewModel.state: StateFlow<StatisticsUiState?>`

- [ ] **Step 1: Create skeleton composable**

```kotlin
package com.looker.kenko.ui.feature.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** 统计页加载骨架：与真实卡片尺寸近似的占位，保证首帧 <100ms 立即有内容。 */
@Composable
fun StatisticsSkeleton(modifier: Modifier = Modifier) {
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHighest
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        item { SkeletonBlock(height = 180.dp, color = placeholder) }
        item { SkeletonBlock(height = 200.dp, color = placeholder) }
        item { SkeletonBlock(height = 160.dp, color = placeholder) }
        item { SkeletonBlock(height = 160.dp, color = placeholder) }
        item { SkeletonBlock(height = 140.dp, color = placeholder) }
    }
}

@Composable
private fun SkeletonBlock(height: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(color),
    ) {}
}
```

- [ ] **Step 2: Wire skeleton in `Statistics.kt`**

把 `Statistics(...)` 内改为：先取 `state`，若为 `null` 渲染骨架，否则渲染现有内容。

```kotlin
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.label_statistics)) }) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val content = state
        if (content == null) {
            StatisticsSkeleton(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { HeatmapCard(sessionDates = content.sessionDates, today = content.today, countByDate = content.countByDate, modifier = Modifier.fillMaxWidth()) }
                item { BalanceRingCard(monthlyCounts = content.monthlyCounts, cardioMonthly = content.cardioMonthly, bodyParts = content.bodyParts) }
                item { BodyPartBarCard(title = stringResource(R.string.label_stat_week), counts = content.weeklyCounts, cardioMinutes = content.cardioWeekly, bodyParts = content.bodyParts) }
                item { BodyPartBarCard(title = stringResource(R.string.label_stat_month), counts = content.monthlyCounts, cardioMinutes = content.cardioMonthly, bodyParts = content.bodyParts) }
                item { BodyPartBarCard(title = stringResource(R.string.label_stat_plan), counts = content.planCounts, cardioMinutes = content.cardioPlan, bodyParts = content.bodyParts) }
                item { TrendCard(weeklyTrend = content.weeklyTrend) }
                item { AdherenceCard(actualDays = content.actualDays, plannedDays = content.plannedDays) }
            }
        }
    }
```

补 import：`com.looker.kenko.ui.feature.statistics.components.StatisticsSkeleton`。

- [ ] **Step 3: Build to verify it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/StatisticsSkeleton.kt app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/Statistics.kt
git commit -m "feat(statistics): instant skeleton while cached data loads"
```

---

### Task 7: 验证与回归（tests + 真机）

**Files:** 无（验证任务）

- [ ] **Step 1: 全量单测**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL（含新增测试与既有 `AggregationTest`/`StatisticsViewModelTest`）

- [ ] **Step 2: 构建并安装**

Run: `.\gradlew.bat :app:installDebug`
Expected: `Installed on 1 device.`

- [ ] **Step 3: 真机验证（设备已连接时）**

顺序验证：
1. 冷启动进入统计页：立即出现骨架，不出现 1~2s 白屏；随后内容填充。
2. 离开再进入（5s 内）：**瞬时**显示上次内容。
3. 在 Home/Records 增删一组后回到统计页：数值更新，且切换过程无卡顿。
4. Home/Records 内容与优化前一致（回归）。

- [ ] **Step 4: 若仍有主线程卡顿，抓取证据**

Run: `adb shell top -b -n 1 | Select-String "kenko"` 或在 Android Studio Profiler 观察；确认聚合不在 Main。将结果记录到 spec 的验收清单。

- [ ] **Step 5: 提交（如有文案/日志等收尾改动）**

```bash
git add -A
git commit -m "chore(statistics): verification follow-ups"
```

---

## Self-Review 覆盖检查

- 成功标准（首帧 <100ms / 稳态无顿挫）→ Task 4（后台聚合+缓存）、Task 6（骨架+瞬时渲染）。
- 数据路径（用 summaries + 有氧聚合，去掉重流）→ Task 2、Task 4。
- 共享层 N+1 修复（兼容）→ Task 1。
- 缓存与失效（SWR，自动由 Room 触发）→ Task 4。
- UI 骨架 → Task 6。
- 测试/验收 → Task 4/5/7。
- 非目标（不改 schema/统计口径/图表视觉）→ 全程遵守；Task 7 回归。
