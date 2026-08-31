# 计划导入导出 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在编辑计划页右上角提供"导入计划/导出计划"入口：多选导出为单 JSON 文件、从 JSON 导入创建新计划（允许重名、缺失动作自动创建、全部非激活）。

**Architecture:** 分层模仿现有 `ExportManager`/`BackupManager` 模式：纯逻辑 DTO + Codec（`domain/model/PlanTransferData.kt`、`PlanTransferCodec.kt`，JVM 可测）→ 无 Android 依赖的导入应用器 `PlanImportApplier`（fake repo 可测）→ Android 业务门面 `PlanTransferManager`（SAF 读写）→ UI 对话框 + `PlanEdit` top bar `actions` 接线。动作按 name 引用，缺失时 `ExerciseRepo.getOrCreate` 自动创建。

**Tech Stack:** Kotlin, Compose Material3, kotlinx.serialization, Room, Hilt, JUnit5（JVM 单测）, AndroidJUnit4（androidTest）

## Global Constraints

- 分支 `feat/v1.8.0`；一个任务一个提交，禁止合并提交，禁止直接改 main。
- 导入的计划**全部非激活**（不调用 `PlanRepo.setCurrent`），`currentDayIndex = 1`。
- 导入**允许计划名重复**，不调用 `planNameExists` 校验。
- JSON 顶层对象 `PlanTransferFile(version: Int = 1, plans: List<PlanTransfer>)`；`version != 1` 视为无效文件。
- 动作按 name 引用；缺失动作用内联字段自动创建，创建时 tags 按名匹配本地 Tag 表（`TagDao.getByName`），匹配不到则忽略该 tag。
- 休息天数显示用新规则 `dayCount - workDays`（`coerceAtLeast(0)`），不得用 `PlanStat.restDays`（已删除）。
- 字符串：现有 `label_import_plan` 已被 SessionDetail 占用，**不可复用**；新入口用 `label_import_plan_file`。
- 测试：JVM 单测放 `app/src/test`（JUnit5，`@Test` + kotlinx.coroutines.test）；androidTest 放 `app/src/androidTest`（Hilt 模式仿 `RepositoryTest.kt`）。androidTest 若本机无设备/模拟器，以 `compileDebugKotlin` 通过为准。

---

### Task 1: DTO + Codec（纯逻辑，JVM 可测）

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/model/PlanTransferData.kt`
- Create: `app/src/main/kotlin/com/looker/kenko/domain/model/PlanTransferCodec.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/domain/model/PlanTransferCodecTest.kt`

**Interfaces:**
- Consumes: 无（纯标准库 + kotlinx.serialization）
- Produces:
  - `data class PlanTransferFile(version: Int = 1, plans: List<PlanTransfer>)`
  - `data class PlanTransfer(name, description: String?, difficulty: String?, focus: String?, equipment: String?, time: String?, dayCount: Int = 7, dayTitles: Map<Int, String> = emptyMap(), days: List<PlanDayTransfer>)`
  - `data class PlanDayTransfer(dayIndex: Int, exercises: List<PlanExerciseTransfer>)`
  - `data class PlanExerciseTransfer(name, target: String?, tags: List<String>, countType: String = "REPS", isBodyweight: Boolean = false)`
  - `object PlanTransferCodec { fun encode(plans: List<PlanTransfer>): String; fun decode(jsonString: String): List<PlanTransfer> }`
  - `fun PlanExerciseTransfer.toExercise(): Exercise`（tags 用 `parentName = target` 构造占位 Tag）

- [ ] **Step 1: 写失败的测试** `PlanTransferCodecTest.kt`

```kotlin
package com.looker.kenko.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanTransferCodecTest {

    @Test
    fun `round trip preserves all fields`() {
        val plans = listOf(
            PlanTransfer(
                name = "Push Pull Leg",
                description = "desc",
                difficulty = "ADAPTABLE",
                focus = "STRENGTH",
                equipment = "FULL_GYM",
                time = "NORMAL",
                dayCount = 6,
                dayTitles = mapOf(1 to "推", 2 to "拉"),
                days = listOf(
                    PlanDayTransfer(
                        dayIndex = 1,
                        exercises = listOf(
                            PlanExerciseTransfer(
                                name = "杠铃卧推",
                                target = "胸",
                                tags = listOf("平板"),
                                countType = "REPS",
                                isBodyweight = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val jsonString = PlanTransferCodec.encode(plans)
        assertEquals(plans, PlanTransferCodec.decode(jsonString))
    }

    @Test
    fun `version 1 is accepted`() {
        val jsonString = Json.encodeToString(PlanTransferFile(version = 1, plans = emptyList()))
        assertEquals(emptyList(), PlanTransferCodec.decode(jsonString))
    }

    @Test
    fun `unsupported version is rejected`() {
        val jsonString = Json.encodeToString(PlanTransferFile(version = 2, plans = emptyList()))
        assertFailsWith<IllegalArgumentException> { PlanTransferCodec.decode(jsonString) }
    }

    @Test
    fun `invalid json is rejected`() {
        assertFailsWith<Exception> { PlanTransferCodec.decode("not json") }
    }

    @Test
    fun `exercise transfer maps to exercise with target as parentName`() {
        val exercise = PlanExerciseTransfer(
            name = "卧推", target = "胸", tags = listOf("平板"), countType = "MINUTES", isBodyweight = true,
        ).toExercise()
        assertEquals("卧推", exercise.name)
        assertEquals(CountType.MINUTES, exercise.countType)
        assertEquals(true, exercise.isBodyweight)
        assertEquals("胸", exercise.tags.first().parentName)
        assertEquals("平板", exercise.tags.first().name)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.looker.kenko.domain.model.PlanTransferCodecTest"`
Expected: FAIL（`PlanTransferCodec` / `PlanTransfer` 未定义）

- [ ] **Step 3: 实现 DTO** `PlanTransferData.kt`

```kotlin
package com.looker.kenko.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlanTransferFile(
    val version: Int = 1,
    val plans: List<PlanTransfer> = emptyList(),
)

@Serializable
data class PlanTransfer(
    val name: String,
    val description: String? = null,
    val difficulty: String? = null,
    val focus: String? = null,
    val equipment: String? = null,
    val time: String? = null,
    val dayCount: Int = 7,
    val dayTitles: Map<Int, String> = emptyMap(),
    val days: List<PlanDayTransfer> = emptyList(),
)

@Serializable
data class PlanDayTransfer(
    val dayIndex: Int,
    val exercises: List<PlanExerciseTransfer> = emptyList(),
)

@Serializable
data class PlanExerciseTransfer(
    val name: String,
    val target: String? = null,
    val tags: List<String> = emptyList(),
    val countType: String = "REPS",
    val isBodyweight: Boolean = false,
)

fun PlanExerciseTransfer.toExercise(): Exercise = Exercise(
    name = name,
    countType = runCatching { CountType.valueOf(countType) }.getOrDefault(CountType.REPS),
    isBodyweight = isBodyweight,
    tags = tags.map { tagName ->
        Tag(name = tagName, parentName = target)
    },
)
```

- [ ] **Step 4: 实现 Codec** `PlanTransferCodec.kt`

```kotlin
package com.looker.kenko.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PlanTransferCodec {

    private const val CURRENT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(plans: List<PlanTransfer>): String =
        json.encodeToString(PlanTransferFile(version = CURRENT_VERSION, plans = plans))

    fun decode(jsonString: String): List<PlanTransfer> {
        val file = json.decodeFromString<PlanTransferFile>(jsonString)
        check(file.version == CURRENT_VERSION) { "Unsupported plan file version: ${file.version}" }
        return file.plans
    }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.looker.kenko.domain.model.PlanTransferCodecTest"`
Expected: PASS（5 项）

- [ ] **Step 6: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/model/PlanTransferData.kt app/src/main/kotlin/com/looker/kenko/domain/model/PlanTransferCodec.kt app/src/test/kotlin/com/looker/kenko/domain/model/PlanTransferCodecTest.kt
git commit -m "feat: plan transfer DTO and JSON codec"
```

---

### Task 2: ExerciseDao.getByName + TagDao.getByName + ExerciseRepo.getOrCreate

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/ExerciseDao.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/TagDao.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/ExerciseRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalExerciseRepo.kt`
- Test: `app/src/androidTest/kotlin/com/looker/kenko/RepositoryTest.kt`（追加用例）

**Interfaces:**
- Consumes: `ExerciseEntity.toExternal(tags)`（ExerciseMapper）、`Exercise.toEntity()`、`TagEntity.toExternal()`（TagMapper）、`tagDao.replaceExerciseTags(id, tagIds)`（已存在）
- Produces:
  - `ExerciseDao.getByName(name: String): ExerciseEntity?`
  - `TagDao.getByName(name: String): TagEntity?`
  - `ExerciseRepo.getOrCreate(exercise: Exercise): Exercise`（存在返回带 id 的现有动作，不存在则插入并返回；tags 按名匹配本地 Tag 关联，匹配不到忽略）

- [ ] **Step 1: DAO 查询 + 接口 + 实现**

`ExerciseDao.kt` 在 `get(id)` 查询后追加：

```kotlin
    @Query(
        """
        SELECT *
        FROM exercises
        WHERE name = :name
        LIMIT 1
        """,
    )
    suspend fun getByName(name: String): ExerciseEntity?
```

`TagDao.kt` 在 `get(id)` 后追加：

```kotlin
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?
```

`ExerciseRepo.kt` 在 `isExerciseAvailable` 后追加：

```kotlin
    /** 按 name 查动作；不存在则创建（tags 按名匹配本地 Tag 关联）并返回带 id 的结果。 */
    suspend fun getOrCreate(exercise: Exercise): Exercise
```

`LocalExerciseRepo.kt` 在 `isExerciseAvailable` 后追加实现：

```kotlin
    override suspend fun getOrCreate(exercise: Exercise): Exercise {
        dao.getByName(exercise.name)?.let { entity ->
            val tags = tagDao.getTagsForExercise(entity.id).map { it.toExternal() }
            return entity.toExternal(tags)
        }
        val newId = dao.upsert(exercise.toEntity()).toInt()
        val tagIds = exercise.tags.mapNotNull { tag -> tagDao.getByName(tag.name)?.id }
        if (tagIds.isNotEmpty()) {
            tagDao.replaceExerciseTags(newId, tagIds)
        }
        return get(newId) ?: exercise.copy(id = newId)
    }
```

- [ ] **Step 2: 追加 androidTest 用例**（`RepositoryTest.kt` 末尾 `}` 前）

```kotlin
    @Test
    fun getOrCreateCreatesMissingExerciseAndReusesExisting() = runTest {
        val created = exerciseRepo.getOrCreate(
            com.looker.kenko.domain.model.Exercise(name = "Unique-Import-Test-动作"),
        )
        assertNotNull(created.id)
        val reused = exerciseRepo.getOrCreate(
            com.looker.kenko.domain.model.Exercise(name = "Unique-Import-Test-动作"),
        )
        assertEquals(created.id, reused.id)
    }
```

（顶部已有 `assertNotNull` import；若缺 `com.looker.kenko.domain.model.Exercise` 引用则补 import。）

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

（可选，需设备/模拟器）Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.looker.kenko.RepositoryTest#getOrCreateCreatesMissingExerciseAndReusesExisting"`

- [ ] **Step 4: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/local/dao/ExerciseDao.kt app/src/main/kotlin/com/looker/kenko/data/local/dao/TagDao.kt app/src/main/kotlin/com/looker/kenko/data/repository/ExerciseRepo.kt app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalExerciseRepo.kt app/src/androidTest/kotlin/com/looker/kenko/RepositoryTest.kt
git commit -m "feat: add ExerciseRepo.getOrCreate with tag matching"
```

---

### Task 3: PlanImportApplier（无 Android 依赖的导入应用器，JVM 可测）

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/data/plan/PlanImportApplier.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/data/plan/PlanImportApplierTest.kt`

**Interfaces:**
- Consumes: `PlanRepo`（createPlan/updatePlan/addItem）、`ExerciseRepo`（getOrCreate）、`PlanTransfer`/`PlanExerciseTransfer.toExercise()`（Task 1）、`Plan.titlesMap`（Plan.kt:55）
- Produces:
  - `data class ImportSummary(val imported: Int, val failed: Int)`
  - `class PlanImportApplier(planRepo: PlanRepo, exerciseRepo: ExerciseRepo) { suspend fun apply(plans: List<PlanTransfer>): ImportSummary }`

- [ ] **Step 1: 写失败的测试** `PlanImportApplierTest.kt`（fake repos 记录调用）

```kotlin
package com.looker.kenko.data.plan

import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Labels
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanTransfer
import com.looker.kenko.domain.model.PlanDayTransfer
import com.looker.kenko.domain.model.PlanExerciseTransfer
import com.looker.kenko.domain.model.titlesMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanImportApplierTest {

    private class FakePlanRepo : PlanRepo {
        val created = mutableListOf<Plan>()
        val addedItems = mutableListOf<PlanItem>()
        var nextId = 1
        override val plans: Flow<List<Plan>> = emptyFlow()
        override val current: Flow<Plan?> = emptyFlow()
        override val planItems: Flow<List<PlanItem>> = emptyFlow()
        override fun planItems(day: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun planItemsByPlan(id: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun planItems(id: Int, day: Int): Flow<List<PlanItem>> = emptyFlow()
        override fun activeExercises(day: Int): Flow<List<Exercise>> = emptyFlow()
        override suspend fun plan(id: Int): Plan? = created.find { it.id == id }
        override suspend fun planNameExists(name: String): Boolean = false
        override suspend fun getPlanItems(id: Int): List<PlanItem> = emptyList()
        override suspend fun getPlanItems(id: Int, day: Int): List<PlanItem> = emptyList()
        override suspend fun createPlan(
            name: String, description: String?, difficulty: Labels.Difficulty?,
            focus: Labels.Focus?, equipment: Labels.Equipment?, time: Labels.Time?,
        ): Int {
            val plan = Plan(
                name = name, description = description, difficulty = difficulty,
                focus = focus, equipment = equipment, time = time, isActive = false,
                id = nextId++,
            )
            created += plan
            return plan.id!!
        }
        override suspend fun updatePlan(plan: Plan) {
            val idx = created.indexOfFirst { it.id == plan.id }
            if (idx >= 0) created[idx] = plan
        }
        override suspend fun setCurrent(id: Int) = error("must not be called")
        override suspend fun deletePlan(id: Int) = Unit
        override suspend fun addItem(planItem: PlanItem) {
            addedItems += planItem
        }
        override suspend fun removeItem(id: Long) = Unit
        override suspend fun updateOrder(planId: Int, day: Int, exercises: List<Exercise>) = Unit
        override suspend fun updateDayIndex(planId: Int, dayIndex: Int) = Unit
        override suspend fun advanceDay(planId: Int, actualDayIndex: Int) = Unit
        override suspend fun addDay(planId: Int) = Unit
        override suspend fun deleteDay(planId: Int, dayIndex: Int) = Unit
        override suspend fun moveDay(planId: Int, from: Int, to: Int) = Unit
    }

    private class FakeExerciseRepo : ExerciseRepo {
        val known = mutableListOf<Exercise>(Exercise(name = "已有动作", id = 1))
        val createdNames = mutableListOf<String>()
        override val stream: Flow<List<Exercise>> = emptyFlow()
        override val numberOfExercise: Flow<Int> = emptyFlow()
        override suspend fun get(id: Int): Exercise? = known.find { it.id == id }
        override suspend fun upsert(exercise: Exercise) = Unit
        override suspend fun remove(id: Int) = Unit
        override suspend fun isExerciseAvailable(name: String): Boolean = known.any { it.name == name }
        override suspend fun hasHistory(id: Int): Boolean = false
        override suspend fun getOrCreate(exercise: Exercise): Exercise {
            known.find { it.name == exercise.name }?.let { return it }
            val created = exercise.copy(id = known.size + 1)
            known += created
            createdNames += created.name
            return created
        }
    }

    @Test
    fun `imports plans with dayCount dayTitles and order`() = runTest {
        val planRepo = FakePlanRepo()
        val exerciseRepo = FakeExerciseRepo()
        val applier = PlanImportApplier(planRepo, exerciseRepo)
        val summary = applier.apply(
            listOf(
                PlanTransfer(
                    name = "New Plan",
                    dayCount = 5,
                    dayTitles = mapOf(1 to "胸"),
                    days = listOf(
                        PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "新动作A"))),
                        PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "新动作B"))),
                    ),
                ),
            ),
        )
        assertEquals(ImportSummary(1, 0), summary)
        val plan = planRepo.created.single()
        assertEquals(5, plan.dayCount)
        assertEquals("胸", plan.titlesMap[1])
        assertEquals(1, plan.currentDayIndex)
        assertEquals(2, planRepo.addedItems.size)
        assertEquals(listOf("新动作A", "新动作B"), planRepo.addedItems.map { it.exercise.name })
        assertEquals(listOf(1, 1), planRepo.addedItems.map { it.dayIndex })
        assertEquals(listOf("新动作A", "新动作B"), exerciseRepo.createdNames)
    }

    @Test
    fun `reuses existing exercises by name`() = runTest {
        val planRepo = FakePlanRepo()
        val exerciseRepo = FakeExerciseRepo()
        PlanImportApplier(planRepo, exerciseRepo).apply(
            listOf(
                PlanTransfer(
                    name = "P",
                    days = listOf(PlanDayTransfer(1, listOf(PlanExerciseTransfer(name = "已有动作")))),
                ),
            ),
        )
        assertTrue(exerciseRepo.createdNames.isEmpty())
        assertEquals("已有动作", planRepo.addedItems.single().exercise.name)
    }

    @Test
    fun `allows duplicate plan names`() = runTest {
        val planRepo = FakePlanRepo()
        val applier = PlanImportApplier(planRepo, FakeExerciseRepo())
        applier.apply(
            listOf(
                PlanTransfer(name = "Same"),
                PlanTransfer(name = "Same"),
            ),
        )
        assertEquals(listOf("Same", "Same"), planRepo.created.map { it.name })
    }

    @Test
    fun `one failing plan does not stop the rest`() = runTest {
        val planRepo = object : FakePlanRepo() {
            override suspend fun createPlan(
                name: String, description: String?, difficulty: Labels.Difficulty?,
                focus: Labels.Focus?, equipment: Labels.Equipment?, time: Labels.Time?,
            ): Int {
                if (name == "Bad") error("boom")
                return super.createPlan(name, description, difficulty, focus, equipment, time)
            }
        }
        val summary = PlanImportApplier(planRepo, FakeExerciseRepo()).apply(
            listOf(PlanTransfer(name = "Bad"), PlanTransfer(name = "Good")),
        )
        assertEquals(ImportSummary(1, 1), summary)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.looker.kenko.data.plan.PlanImportApplierTest"`
Expected: FAIL（`PlanImportApplier` 未定义）

- [ ] **Step 3: 实现** `PlanImportApplier.kt`

```kotlin
package com.looker.kenko.data.plan

import com.looker.kenko.data.repository.ExerciseRepo
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Labels
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanTransfer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ImportSummary(val imported: Int, val failed: Int)

class PlanImportApplier @Inject constructor(
    private val planRepo: PlanRepo,
    private val exerciseRepo: ExerciseRepo,
) {

    suspend fun apply(plans: List<PlanTransfer>): ImportSummary {
        var imported = 0
        var failed = 0
        plans.forEach { plan ->
            try {
                applyOne(plan)
                imported++
            } catch (e: Exception) {
                failed++
            }
        }
        return ImportSummary(imported, failed)
    }

    private suspend fun applyOne(plan: PlanTransfer) {
        val difficulty = plan.difficulty?.let { runCatching { Labels.Difficulty.valueOf(it) }.getOrNull() }
        val focus = plan.focus?.let { runCatching { Labels.Focus.valueOf(it) }.getOrNull() }
        val equipment = plan.equipment?.let { runCatching { Labels.Equipment.valueOf(it) }.getOrNull() }
        val time = plan.time?.let { runCatching { Labels.Time.valueOf(it) }.getOrNull() }

        val planId = planRepo.createPlan(
            name = plan.name,
            description = plan.description,
            difficulty = difficulty,
            focus = focus,
            equipment = equipment,
            time = time,
        )
        // 导入一律非激活、从第 1 天开始；允许重名（不做 planNameExists 校验）
        planRepo.updatePlan(
            Plan(
                name = plan.name,
                description = plan.description,
                difficulty = difficulty,
                focus = focus,
                equipment = equipment,
                time = time,
                isActive = false,
                dayTitles = plan.dayTitles.takeIf { it.isNotEmpty() }
                    ?.let { Json.encodeToString(it) },
                dayCount = plan.dayCount,
                currentDayIndex = 1,
                id = planId,
            ),
        )
        plan.days.forEach { day ->
            day.exercises.forEach { exerciseTransfer ->
                val exercise = exerciseRepo.getOrCreate(exerciseTransfer.toExercise())
                planRepo.addItem(
                    PlanItem(
                        dayIndex = day.dayIndex,
                        exercise = exercise,
                        planId = planId,
                    ),
                )
            }
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.looker.kenko.data.plan.PlanImportApplierTest"`
Expected: PASS（4 项）

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/plan/PlanImportApplier.kt app/src/test/kotlin/com/looker/kenko/data/plan/PlanImportApplierTest.kt
git commit -m "feat: plan import applier with duplicate-name and partial-failure handling"
```

---

### Task 4: PlanTransferManager（Android 业务门面：导出组装 + SAF 读写）

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/data/plan/PlanTransferManager.kt`

**Interfaces:**
- Consumes: `PlanRepo`（plan(id)/getPlanItems）、`PlanImportApplier`（Task 3）、`PlanTransferCodec`（Task 1）、`Plan.titlesMap`、`PlanItem`、`Exercise`
- Produces:
  - `class PlanTransferManager @Inject constructor(@ApplicationContext context: Context, planRepo: PlanRepo, applier: PlanImportApplier)`
  - `suspend fun exportPlans(planIds: List<Int>, destinationUri: Uri)`（无效 id 跳过；写入失败抛异常）
  - `suspend fun readPlans(uri: Uri): List<PlanTransfer>`（解析失败/version 不符抛异常）
  - `suspend fun importPlans(uri: Uri): ImportSummary`（= `applier.apply(readPlans(uri))`）

- [ ] **Step 1: 实现** `PlanTransferManager.kt`

```kotlin
package com.looker.kenko.data.plan

import android.content.Context
import android.net.Uri
import com.looker.kenko.data.repository.PlanRepo
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Plan
import com.looker.kenko.domain.model.PlanDayTransfer
import com.looker.kenko.domain.model.PlanExerciseTransfer
import com.looker.kenko.domain.model.PlanItem
import com.looker.kenko.domain.model.PlanTransfer
import com.looker.kenko.domain.model.PlanTransferCodec
import com.looker.kenko.domain.model.titlesMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStreamWriter
import javax.inject.Inject

class PlanTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val planRepo: PlanRepo,
    private val applier: PlanImportApplier,
) {

    suspend fun exportPlans(planIds: List<Int>, destinationUri: Uri) {
        val transfers = planIds.mapNotNull { id ->
            val plan = planRepo.plan(id) ?: return@mapNotNull null
            plan.toTransfer(planRepo.getPlanItems(id))
        }
        val jsonString = PlanTransferCodec.encode(transfers)
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(jsonString)
            }
        } ?: throw IllegalStateException("Cannot open output stream for $destinationUri")
    }

    suspend fun readPlans(uri: Uri): List<PlanTransfer> {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Cannot open input stream for $uri")
        return PlanTransferCodec.decode(jsonString)
    }

    suspend fun importPlans(uri: Uri): ImportSummary = applier.apply(readPlans(uri))
}

private fun Plan.toTransfer(items: List<PlanItem>): PlanTransfer = PlanTransfer(
    name = name,
    description = description,
    difficulty = difficulty?.name,
    focus = focus?.name,
    equipment = equipment?.name,
    time = time?.name,
    dayCount = dayCount,
    dayTitles = titlesMap,
    days = items.groupBy { it.dayIndex }.map { (day, dayItems) ->
        PlanDayTransfer(
            dayIndex = day,
            exercises = dayItems.map { it.exercise.toTransfer() },
        )
    }.sortedBy { it.dayIndex },
)

private fun Exercise.toTransfer(): PlanExerciseTransfer = PlanExerciseTransfer(
    name = name,
    target = tags.firstOrNull()?.parentName,
    tags = tags.map { it.name },
    countType = countType.name,
    isBodyweight = isBodyweight,
)
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/plan/PlanTransferManager.kt
git commit -m "feat: plan transfer manager with SAF read/write"
```

---

### Task 5: 字符串资源（中英）

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

**Interfaces:**
- Produces: 以下 `@string` 资源（中英各一份）：

| name | en | zh |
|---|---|---|
| `label_export_plan` | Export plans | 导出计划 |
| `label_import_plan_file` | Import plan | 导入计划 |
| `label_select_all` | Select all | 全选 |
| `label_plan_days_summary` | %1$s training days · %2$s rest days | %1$s 天训练 %2$s 天休息 |
| `label_import_confirm` | Import %1$s plan(s)? | 将导入 %1$s 个计划？ |
| `label_import_success` | Imported %1$s plan(s) | 已导入 %1$s 个计划 |
| `label_import_partial` | Imported %1$s, failed %2$s | 成功导入 %1$s 个，失败 %2$s 个 |
| `label_export_success` | Exported %1$s plan(s) | 已导出 %1$s 个计划 |
| `label_plan_file_invalid` | Invalid plan file | 无效的计划文件 |

- [ ] **Step 1: values/strings.xml** 追加（放在 `label_import_plan` 相关区域附近即可）

```xml
    <string name="label_export_plan">Export plans</string>
    <string name="label_import_plan_file">Import plan</string>
    <string name="label_select_all">Select all</string>
    <string name="label_plan_days_summary">%1$s training days · %2$s rest days</string>
    <string name="label_import_confirm">Import %1$s plan(s)?</string>
    <string name="label_import_success">Imported %1$s plan(s)</string>
    <string name="label_import_partial">Imported %1$s, failed %2$s</string>
    <string name="label_export_success">Exported %1$s plan(s)</string>
    <string name="label_plan_file_invalid">Invalid plan file</string>
```

- [ ] **Step 2: values-zh/strings.xml** 追加

```xml
    <string name="label_export_plan">导出计划</string>
    <string name="label_import_plan_file">导入计划</string>
    <string name="label_select_all">全选</string>
    <string name="label_plan_days_summary">%1$s 天训练 %2$s 天休息</string>
    <string name="label_import_confirm">将导入 %1$s 个计划？</string>
    <string name="label_import_success">已导入 %1$s 个计划</string>
    <string name="label_import_partial">成功导入 %1$s 个，失败 %2$s 个</string>
    <string name="label_export_success">已导出 %1$s 个计划</string>
    <string name="label_plan_file_invalid">无效的计划文件</string>
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat: plan import/export strings"
```

---

### Task 6: 导出选择器 + 导入确认对话框（PlanTransferDialogs.kt）

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanTransferDialogs.kt`

**Interfaces:**
- Consumes: `Plan`（name/id/stat/dayCount）、`@string/label_export_plan`、`label_select_all`、`label_plan_days_summary`、`label_import_confirm`、`label_export`（已存在）、`label_cancel`（已存在）
- Produces:
  - `@Composable fun ExportPlanDialog(plans: List<Plan>, onConfirm: (List<Int>) -> Unit, onDismiss: () -> Unit)`（全选 checkbox + LazyColumn 列表，每项"名字 + %1$s 天训练 %2$s 天休息"；未选中任何计划时导出按钮禁用）
  - `@Composable fun ImportPlanConfirmDialog(planCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit)`

- [ ] **Step 1: 实现** `PlanTransferDialogs.kt`

```kotlin
package com.looker.kenko.ui.feature.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.domain.model.Plan

@Composable
fun ExportPlanDialog(
    plans: List<Plan>,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var selectAll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_export_plan)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectAll = !selectAll
                            selectedIds = if (selectAll) plans.mapNotNull { it.id }.toSet() else emptySet()
                        },
                ) {
                    Checkbox(checked = selectAll, onCheckedChange = null)
                    Text(stringResource(R.string.label_select_all))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(plans, key = { it.id ?: it.name }) { plan ->
                        val planId = plan.id
                        val checked = planId != null && planId in selectedIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (planId != null) {
                                        selectedIds = if (checked) selectedIds - planId else selectedIds + planId
                                    }
                                },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column {
                                Text(plan.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = stringResource(
                                        R.string.label_plan_days_summary,
                                        plan.stat.workDays,
                                        (plan.dayCount - plan.stat.workDays).coerceAtLeast(0),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text(stringResource(R.string.label_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}

@Composable
fun ImportPlanConfirmDialog(
    planCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_import_plan_file)) },
        text = { Text(stringResource(R.string.label_import_confirm, planCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.label_import_plan_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        },
    )
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanTransferDialogs.kt
git commit -m "feat: plan export selector and import confirm dialogs"
```

---

### Task 7: StringHandler 格式化重载 + PlanEditViewModel 扩展（导出/导入状态与操作）

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/StringHandler.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEditViewModel.kt`

**Interfaces:**
- Consumes: `PlanTransferManager`（Task 4：exportPlans/readPlans/importPlans）、`PlanRepo.plans`、`Plan`
- Produces:
  - `StringHandler.getString(id: Int, vararg formatArgs: Any): String`（新增重载，不破坏现有无参调用）
  - `val plansForExport: StateFlow<List<Plan>>`（= `repo.plans` 的 stateIn）
  - `val importPreview: StateFlow<ImportPreview?>`，`data class ImportPreview(val uri: Uri, val planCount: Int)`
  - `fun previewImport(uri: Uri)`（读计划数；失败/无效文件经 `snackbarState` 提示）
  - `fun dismissImportPreview()`
  - `fun confirmImport()`（执行导入；成功 `label_import_success`，部分失败 `label_import_partial`，异常经 `snackbarState`）
  - `fun exportPlans(planIds: List<Int>, destinationUri: Uri)`（成功 `label_export_success`，失败经 `snackbarState`）
  - ⚠️ 所有提示一律走 `snackbarState.showSnackbar(...)`（PlanEdit 的 Scaffold 已接 `ErrorSnackbar`，能显示任意文本）；**不要用 `_snackbar.emit`**——该 SharedFlow 在 PlanEdit 页面无消费点，消息不会显示。

- [ ] **Step 1: StringHandler 加格式化重载**（`StringHandler.kt`）

```kotlin
    fun getString(id: Int): String {
        resources.configuration
        return resources.getString(id)
    }

    fun getString(id: Int, vararg formatArgs: Any): String {
        resources.configuration
        return resources.getString(id, *formatArgs)
    }
```

- [ ] **Step 2: 修改构造与新增状态/操作**（`PlanEditViewModel.kt`）

构造函数追加参数：

```kotlin
    private val transferManager: PlanTransferManager,
```

新增 imports：`android.net.Uri`、`com.looker.kenko.data.plan.PlanTransferManager`、`androidx.compose.runtime.Stable`（若未引入）。

新增字段（放在 `state` 定义之后）：

```kotlin
    val plansForExport: StateFlow<List<Plan>> = repo.plans
        .asStateFlow(emptyList())

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val plans = transferManager.readPlans(uri)
                _importPreview.value = ImportPreview(uri, plans.size)
            } catch (e: Exception) {
                snackbarState.showSnackbar(stringHandler.getString(R.string.label_plan_file_invalid))
            }
        }
    }

    fun dismissImportPreview() {
        _importPreview.value = null
    }

    fun confirmImport() {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            try {
                val summary = transferManager.importPlans(preview.uri)
                val message = if (summary.failed == 0) {
                    stringHandler.getString(R.string.label_import_success, summary.imported)
                } else {
                    stringHandler.getString(R.string.label_import_partial, summary.imported, summary.failed)
                }
                snackbarState.showSnackbar(message)
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            } finally {
                _importPreview.value = null
            }
        }
    }

    fun exportPlans(planIds: List<Int>, destinationUri: Uri) {
        viewModelScope.launch {
            try {
                transferManager.exportPlans(planIds, destinationUri)
                snackbarState.showSnackbar(
                    stringHandler.getString(R.string.label_export_success, planIds.size),
                )
            } catch (e: Exception) {
                snackbarState.showSnackbar(e.message ?: "An error occurred")
            }
        }
    }
```

文件底部 `PlanEditStage` 枚举前新增：

```kotlin
@Stable
data class ImportPreview(
    val uri: Uri,
    val planCount: Int,
)
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/StringHandler.kt app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEditViewModel.kt
git commit -m "feat: plan import/export actions in PlanEditViewModel"
```

---

### Task 8: PlanEdit UI 接线（top bar actions + launcher + 对话框）

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEdit.kt`

**Interfaces:**
- Consumes: `FullEdit`（本文件）、`ExportPlanDialog`/`ImportPlanConfirmDialog`（Task 6）、`viewModel.plansForExport`/`importPreview`/`exportPlans`/`previewImport`/`confirmImport`/`dismissImportPreview`（Task 7）、`@string/label_export_plan`、`label_import_plan_file`
- Produces: `FullEdit` 增加 `actions: @Composable RowScope.() -> Unit = {}` 参数透传给 `CenterAlignedTopAppBar.actions`

- [ ] **Step 1: `FullEdit` 加 actions 参数**

`FullEdit` 签名（现 `PlanEdit.kt:194-203`）加参数并在 `CenterAlignedTopAppBar` 使用：

```kotlin
private fun FullEdit(
    snackbarHostState: SnackbarHostState,
    stage: PlanEditStage,
    fab: @Composable () -> Unit,
    onBackPress: () -> Unit,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    ui: @Composable (stage: PlanEditStage) -> Unit,
) {
    Scaffold(
        ...
        topBar = {
            CenterAlignedTopAppBar(
                title = title,
                navigationIcon = { BackButton(onBackPress) },
                actions = actions,
            )
        },
    ) { innerPadding -> ... }
}
```

（需确认文件已有 `RowScope` import：`androidx.compose.foundation.layout.RowScope`。`PlanEdit.kt:23` 有 `RowScope` import，复用。）

- [ ] **Step 2: `PlanEdit` 函数体接线**

`PlanEdit`（`PlanEdit.kt:112-192`）新增状态与 launcher（放在 `FullEdit` 调用前）：

```kotlin
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportIds by remember { mutableStateOf<List<Int>?>(null) }
    val plansForExport by viewModel.plansForExport.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            pendingExportIds?.let { viewModel.exportPlans(it, uri) }
        }
        pendingExportIds = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.previewImport(it) }
    }

    if (showExportDialog) {
        ExportPlanDialog(
            plans = plansForExport,
            onConfirm = { ids ->
                pendingExportIds = ids
                showExportDialog = false
                exportLauncher.launch("kenko-plans.json")
            },
            onDismiss = { showExportDialog = false },
        )
    }

    importPreview?.let { preview ->
        ImportPlanConfirmDialog(
            planCount = preview.planCount,
            onConfirm = {
                viewModel.confirmImport()
            },
            onDismiss = { viewModel.dismissImportPreview() },
        )
    }
```

`FullEdit` 调用处（`PlanEdit.kt:123-155`）传 `actions`：

```kotlin
        actions = {
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_export_plan)) },
                    onClick = {
                        menuExpanded = false
                        showExportDialog = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_import_plan_file)) },
                    onClick = {
                        menuExpanded = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                )
            }
        },
```

新增 imports：`android.app.Activity` 不需要；需要 `androidx.activity.compose.rememberLauncherForActivityResult`、`androidx.activity.result.contract.ActivityResultContracts`、`androidx.compose.material.icons.filled.MoreVert`、`androidx.compose.material3.IconButton`（若未引入）、`androidx.compose.material3.DropdownMenu`/`DropdownMenuItem`（若未引入）、`androidx.compose.material3.Checkbox` 不需要（在 Dialog 文件）、`androidx.compose.ui.platform.LocalContext`（若未引入）、`android.net.Uri` 不需要（ImportPreview 内部）。

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: lint 验证**

Run: `./gradlew lintDebug`
Expected: BUILD SUCCESSFUL，`app/build/reports/lint-results-debug.txt` 无 Error

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEdit.kt
git commit -m "feat: wire plan import/export into plan edit top bar"
```

---

### Task 9: 全量验证 + 汇总

**Files:** 无新改动（验证 + 提交收尾）

- [ ] **Step 1: JVM 单测全绿**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL（含 `PlanTransferCodecTest`、`PlanImportApplierTest` 及既有测试）

- [ ] **Step 2: 全量构建（含 lint）**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 确认提交历史**

Run: `git log --oneline -10`
Expected: 连续看到 Task 1-8 的 8 个提交，工作区干净（除 `.agents/` 未跟踪）

- [ ] **Step 4: 汇总交付说明**

向用户报告：功能入口位置、操作路径、JSON 格式示例、验证结果、已知限制（创建动作时 tag 仅按名匹配本地、target 字段仅展示不重建）。
