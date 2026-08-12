# 自定义训练循环周期 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把计划从"固定 7 天一周(星期几)"改为"可自定义长度的训练日序列",支持序列推进(练完自动推进、可手动覆盖、休息日占位),并同步改造 Home / 计划编辑 / Records。

**Architecture:** 方案 A(最小改造):`plan_day.dayOfWeek` 列重命名为 `dayIndex` 并重语义为"训练日序号 1..N",`plans` 新增 `dayCount`(序列长度)与 `currentDayIndex`(推进位置);休息日 = 序列中无 `plan_day` 行的位置。迁移 MIGRATION_12_13 只做 ALTER TABLE,历史数据零改动(现有 1–7 值原样成为训练日序号)。Home 今日训练由 `plan.currentDayIndex` 决定(不再由 `today().dayOfWeek` 推导),完成训练后基于实际训练日推进。

**Tech Stack:** Kotlin 2.2.21、Jetpack Compose + Material 3 Expressive、Hilt 2.57.2、Room 2.8.3(KSP)、kotlinx-datetime、JUnit 5(单测)+ androidTest(KenkoTestRunner)。

## Global Constraints

- 数据库版本从 12 → 13,必须提供 `MIGRATION_12_13` 且保持 `app/schemas/.../13.json` 与实体一致(编译时自动导出,需 git add)。
- 迁移只做 `ALTER TABLE`,禁止改写现有行数据(设计批准:数据零改动)。
- `day_of_week_short` 字符串数组保留(Heatmap 日历表头);`day_of_week`、`kenko_day_of_week` 删除。
- 所有用户可见文本走 `res/values/strings.xml` + `values-zh/`,禁止硬编码。
- 构建只用 `./gradlew`;单测 `./gradlew test`,androidTest `./gradlew connectedAndroidTest`(需设备)。
- 一个 feature 拆 8 个提交,每个任务一个提交,禁止合并多个主题。

---

### Task 1: 训练周期推进纯逻辑 + JVM 单测

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/model/PlanCycle.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/domain/model/PlanCycleTest.kt`

**Interfaces:**
- Produces: `object PlanCycle { fun nextDayIndex(actualDayIndex: Int, dayCount: Int): Int }` — 实际训练日 `actualDayIndex`(1..dayCount)推进到下一个序号;`actualDayIndex == dayCount` 时回绕到 1。Task 4 的 `advanceDay` 依赖它。

- [ ] **Step 1: 写失败测试**

`app/src/test/kotlin/com/looker/kenko/domain/model/PlanCycleTest.kt`(JUnit 5,与现有 `WeightChartFilterTest` 同风格):

```kotlin
package com.looker.kenko.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlanCycleTest {

    @Test
    fun advancesToNextDay() {
        assertEquals(2, PlanCycle.nextDayIndex(1, 7))
    }

    @Test
    fun wrapsAroundAtCycleEnd() {
        assertEquals(1, PlanCycle.nextDayIndex(7, 7))
    }

    @Test
    fun supportsCustomCycleLength() {
        assertEquals(2, PlanCycle.nextDayIndex(1, 3))
        assertEquals(1, PlanCycle.nextDayIndex(3, 3))
    }

    @Test
    fun advancesFromRestDaySelection() {
        // 休息日当天选了 Day2 训练 → 推进到 Day3
        assertEquals(3, PlanCycle.nextDayIndex(2, 8))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew test --tests "com.looker.kenko.domain.model.PlanCycleTest"`
Expected: 编译失败,`PlanCycle` unresolved。

- [ ] **Step 3: 最小实现**

`app/src/main/kotlin/com/looker/kenko/domain/model/PlanCycle.kt`:

```kotlin
package com.looker.kenko.domain.model

/** 训练周期推进规则:基于实际训练日序号(1..dayCount)推进到下一个序号,越界回绕。 */
object PlanCycle {
    fun nextDayIndex(actualDayIndex: Int, dayCount: Int): Int = (actualDayIndex % dayCount) + 1
}
```

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew test --tests "com.looker.kenko.domain.model.PlanCycleTest"`
Expected: PASS(4 个测试)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/model/PlanCycle.kt app/src/test/kotlin/com/looker/kenko/domain/model/PlanCycleTest.kt
git commit -m "feat: add training cycle advance logic"
```

---

### Task 2: 数据模型与映射层从 weekday 切换为 dayIndex

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/model/PlanEntity.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/Plan.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/mapper/PlanMapper.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/Session.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/model/SessionEntity.kt`(含 `SessionDataEntity`)
- Modify: `app/src/main/kotlin/com/looker/kenko/data/mapper/SessionMapper.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/PlanRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/SessionRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalPlanRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalSessionRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/PlanDao.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/SessionDao.kt`

**Interfaces:**
- Consumes: Task 1 的 `PlanCycle`(本任务不用,Task 4 用)。
- Produces(后续任务依赖的精确签名):
  - `Plan` 增加字段 `dayCount: Int = 7`、`currentDayIndex: Int = 1`。
  - `PlanItem` 字段 `dayOfWeek: DayOfWeek` → `dayIndex: Int`。
  - `Plan.titlesMap: Map<Int, String>`(key = dayIndex);`Plan.withDayTitle(dayIndex: Int, title: String?): Plan`。
  - `Session` 字段 `planDayOverride: DayOfWeek?` → `dayIndexOverride: Int?`。
  - `PlanRepo.planItems(day: Int)`、`planItems(id: Int, day: Int)`、`activeExercises(day: Int)`、`updateOrder(planId: Int, day: Int, exercises: List<Exercise>)`。
  - `SessionRepo.updateDayIndex(date: LocalDate, dayIndex: Int)`、`previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?>`。
  - `PlanDao.getWorkDaysByPlanId` 保留(语义:有动作的训练日数量)。

- [ ] **Step 1: 改 Room 实体**

`PlanEntity.kt`:

```kotlin
data class PlanEntity(
    val name: String,
    @ColumnInfo(defaultValue = "NULL")
    val description: String?,
    @ColumnInfo(defaultValue = "NULL")
    val difficulty: Difficulty?,
    @ColumnInfo(defaultValue = "NULL")
    val focus: Focus?,
    @ColumnInfo(defaultValue = "NULL")
    val equipment: Equipment?,
    @ColumnInfo(defaultValue = "NULL")
    val time: Time?,
    @ColumnInfo(defaultValue = "NULL")
    val dayTitles: String? = null,
    @ColumnInfo(defaultValue = "7")
    val dayCount: Int = 7,
    @ColumnInfo(defaultValue = "1")
    val currentDayIndex: Int = 1,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
```

`PlanDayEntity`:`val dayOfWeek: Int` → `val dayIndex: Int`(其余不变)。

- [ ] **Step 2: 改 Session 实体**

`SessionEntity.kt` 中 `SessionDataEntity` 的 `planDayOverride: Int?` → `dayIndexOverride: Int?`(列名随字段名,迁移见 Task 3)。

- [ ] **Step 3: 改 domain 模型**

`Plan.kt`:
- `Plan` 加 `val dayCount: Int = 7`、`val currentDayIndex: Int = 1`(放在 `dayTitles` 之后、`id` 之前)。
- `PlanItem`:`val dayOfWeek: DayOfWeek` → `val dayIndex: Int`。
- `titlesMap`:

```kotlin
val Plan.titlesMap: Map<Int, String>
    get() = try {
        if (dayTitles.isNullOrBlank()) emptyMap()
        else Json.decodeFromString<Map<Int, String>>(dayTitles)
    } catch (e: Exception) {
        emptyMap()
    }

fun Plan.withDayTitle(dayIndex: Int, title: String?): Plan {
    val currentMap = titlesMap.toMutableMap()
    if (title.isNullOrBlank()) {
        currentMap.remove(dayIndex)
    } else {
        currentMap[dayIndex] = title
    }
    val newDayTitles = if (currentMap.isEmpty()) null else Json.encodeToString(currentMap)
    return copy(dayTitles = newDayTitles)
}
```

- 删除 `val week = DatePeriod(days = 7)`(已无用)以及 `import kotlinx.datetime.DatePeriod`。
- 保留 `import kotlinx.datetime.isoDayNumber` 相关代码的清理:确认 `Plan.kt` 不再引用 `isoDayNumber`、`DayOfWeek` 后删除对应 import。

`Session.kt`:`val planDayOverride: DayOfWeek? = null` → `val dayIndexOverride: Int? = null`;删除 `import kotlinx.datetime.DayOfWeek`(确认无其他使用)。

- [ ] **Step 4: 改 mapper**

`PlanMapper.kt`:

```kotlin
fun PlanItem.toEntity() = PlanDayEntity(
    id = id ?: 0,
    planId = planId,
    exerciseId = requireNotNull(exercise.id) { "Exercise id cannot be null" },
    dayIndex = dayIndex,
)

inline fun PlanDayEntity.toExternal(block: (exerciseId: Int) -> Exercise?) = PlanItem(
    planId = planId,
    dayIndex = dayIndex,
    exercise = block(exerciseId) ?: DefaultExercise,
    id = id,
)
```

`PlanEntity.toExternal` / `Plan.toEntity` 补 `dayCount`、`currentDayIndex` 透传。删除 `import kotlinx.datetime.DayOfWeek` / `isoDayNumber`。

`SessionMapper.kt`:

```kotlin
fun Session.data(): SessionDataEntity = SessionDataEntity(
    date = EpochDays(date.toEpochDays().toInt()),
    planId = planId,
    dayIndexOverride = dayIndexOverride,
    durationSeconds = durationSeconds,
    id = id ?: 0,
)

fun SessionEntity.toExternal(setsMap: List<Set>): Session = Session(
    planId = data.planId,
    date = LocalDate.fromEpochDays(data.date.value),
    sets = setsMap,
    dayIndexOverride = data.dayIndexOverride,
    durationSeconds = data.durationSeconds,
    id = data.id,
)
```

删除 `import kotlinx.datetime.DayOfWeek` / `isoDayNumber`。

- [ ] **Step 5: 改 DAO**

`PlanDao.kt`:
- `currentPlanItemsByDayFlow(day: Int)`、`planItemsByPlanIdAndDayFlow(planId: Int, day: Int)`、`getPlanItemsByPlanIdAndDay`、`deleteItemsByPlanIdAndDay` 的 SQL 中 `dayOfWeek = :day` → `dayIndex = :day`(列名)。函数签名已用 `day: Int`,保持不变。

`SessionDao.kt`:
- `updatePlanDayOverride(date: EpochDays, day: Int)` → `updateDayIndexOverride(date: EpochDays, dayIndex: Int)`,`SET planDayOverride = :day` → `SET dayIndexOverride = :dayIndex`。
- `getPreviousSessionDate` 的 SQL 改为(去掉 weekday 推导,改为按实际训练日):

```sql
SELECT date
FROM sessions
WHERE (planId = :planId OR :planId IS NULL)
AND dayIndexOverride = :dayIndex
AND date < :date
ORDER BY date DESC
LIMIT 1
```

函数签名 `getPreviousSessionDate(date: Int, planId: Int?, dayIndex: Int)`。

- [ ] **Step 6: 改 repo 接口与实现**

`PlanRepo.kt`:`planItems(day: DayOfWeek)` → `planItems(day: Int)`;`planItems(id: Int, day: DayOfWeek)` → `planItems(id: Int, day: Int)`;`activeExercises(day: DayOfWeek)` → `activeExercises(day: Int)`;`updateOrder(planId: Int, day: DayOfWeek, exercises)` → `updateOrder(planId: Int, day: Int, exercises)`。删除 `import kotlinx.datetime.DayOfWeek`。

`LocalPlanRepo.kt`:所有 `day.isoDayNumber` 调用改为直接传 `day`(`dao.currentPlanItemsByDayFlow(day)`、`dao.planItemsByPlanIdAndDayFlow(id, day)`、`dao.getPlanItemsByPlanIdAndDay(id, day)`、`dao.deleteItemsByPlanIdAndDay` 所在处);`createPlan` 无需改(实体默认 dayCount=7/currentDayIndex=1);`updateOrder` 参数改 `day: Int`。删除 `import kotlinx.datetime.DayOfWeek` / `isoDayNumber`。

`SessionRepo.kt`:`updatePlanDay(date, day: DayOfWeek)` → `updateDayIndex(date: LocalDate, dayIndex: Int)`;`previousSessionDate(date, planId, day: DayOfWeek)` → `previousSessionDate(date, planId, dayIndex: Int)`。删除 DayOfWeek import。

`LocalSessionRepo.kt`:

```kotlin
override suspend fun updateDayIndex(date: LocalDate, dayIndex: Int) {
    getSessionIdOrCreate(date)
    dao.updateDayIndexOverride(date.toLocalEpochDays(), dayIndex)
}

override fun previousSessionDate(date: LocalDate, planId: Int?, dayIndex: Int): Flow<LocalDate?> {
    return dao.getPreviousSessionDate(date.toLocalEpochDays().value, planId, dayIndex)
        .map { it?.let(LocalDate::fromEpochDays) }
}
```

删除 `import kotlinx.datetime.DayOfWeek` / `isoDayNumber`。

- [ ] **Step 7: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: 通过。此阶段 UI 层(HomeViewModel/PlanEditViewModel/SessionsViewModel 等)仍引用旧 `dayOfWeek` 字段会编译失败——**本任务先只改数据层,UI 层引用在 Task 5/6/7 逐个修复,因此本步允许 UI 层编译失败**。改完全部 Task 5/6/7 后再全量编译。若你按任务顺序执行,本步可运行 `./gradlew compileDebugKotlin -x compileDebugKotlin` 以外的数据层编译检查:`./gradlew :app:compileDebugKotlin` 会失败,改用逐文件 IDE 检查或继续 Task 3(迁移与 schema 需要实体一致)。

> 说明:为保证任务可独立验证,本任务的"验证"是实体/DAO/Repo 签名一致(IDE 或 `./gradlew kspDebugKotlin` 观察 Room 错误消失),全量编译放 Task 8 之后。若中途需编译,可临时注释 UI 引用。

- [ ] **Step 8: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/local/model/PlanEntity.kt app/src/main/kotlin/com/looker/kenko/data/local/model/SessionEntity.kt app/src/main/kotlin/com/looker/kenko/domain/model/Plan.kt app/src/main/kotlin/com/looker/kenko/domain/model/Session.kt app/src/main/kotlin/com/looker/kenko/data/mapper/PlanMapper.kt app/src/main/kotlin/com/looker/kenko/data/mapper/SessionMapper.kt app/src/main/kotlin/com/looker/kenko/data/repository/PlanRepo.kt app/src/main/kotlin/com/looker/kenko/data/repository/SessionRepo.kt app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalPlanRepo.kt app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalSessionRepo.kt app/src/main/kotlin/com/looker/kenko/data/local/dao/PlanDao.kt app/src/main/kotlin/com/looker/kenko/data/local/dao/SessionDao.kt
git commit -m "refactor: switch plan/session models from weekday to dayIndex"
```

---

### Task 3: 数据库 v13 迁移 + schema 迁移测试

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/KenkoDatabase.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/Migrations.kt`
- Create: `app/schemas/com.looker.kenko.data.local.KenkoDatabase/13.json`(编译后自动生成)
- Modify: `app/src/androidTest/kotlin/com/looker/kenko/RoomDatabaseTesting.kt`

**Interfaces:**
- Consumes: Task 2 的实体(dayCount/currentDayIndex/dayIndex/dayIndexOverride)。
- Produces: `MIGRATION_12_13`(Task 4 的 repo 测试与后续任务依赖 v13 schema)。

- [ ] **Step 1: 加迁移并升版本**

`KenkoDatabase.kt`:`version = 12` → `13`;`addMigrations(...)` 追加 `MIGRATION_12_13`。

`Migrations.kt` 末尾:

```kotlin
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plans ADD COLUMN dayCount INTEGER NOT NULL DEFAULT 7")
        db.execSQL("ALTER TABLE plans ADD COLUMN currentDayIndex INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE plan_day RENAME COLUMN dayOfWeek TO dayIndex")
        db.execSQL("ALTER TABLE sessions RENAME COLUMN planDayOverride TO dayIndexOverride")
    }
}
```

- [ ] **Step 2: 编译导出 schema 13.json 并提交**

Run: `./gradlew compileDebugKotlin`(Room KSP 导出 schema)
Expected: 生成 `app/schemas/com.looker.kenko.data.local.KenkoDatabase/13.json`;验证其中 `plans` 含 `dayCount`/`currentDayIndex`,`plan_day` 含 `dayIndex`,`sessions` 含 `dayIndexOverride`。

```bash
git add app/schemas/com.looker.kenko.data.local.KenkoDatabase/13.json
```

- [ ] **Step 3: 写迁移测试**

`RoomDatabaseTesting.kt` 末尾追加两个测试:

```kotlin
@Test
fun schemaMigration12To13() = runTest {
    val db = helper.createDatabase(DB_NAME, 12)
    db.execSQL("INSERT INTO plans (name, dayTitles) VALUES ('Test Plan', '{\"1\":\"Chest\"}')")
    db.execSQL("INSERT INTO plan_day (planId, exerciseId, dayOfWeek, sortOrder) VALUES (1, 1, 3, 0)")
    db.execSQL("INSERT INTO sessions (date, planId, planDayOverride) VALUES (20000, 1, 5)")
    helper.runMigrationsAndValidate(DB_NAME, 13, true, MIGRATION_12_13)
}

@Test
fun dataMigration12To13() = runTest {
    val db = helper.createDatabase(DB_NAME, 12)
    db.execSQL("INSERT INTO plans (name, dayTitles) VALUES ('Test Plan', '{\"1\":\"Chest\"}')")
    db.execSQL("INSERT INTO plan_day (planId, exerciseId, dayOfWeek, sortOrder) VALUES (1, 1, 3, 0)")
    db.execSQL("INSERT INTO sessions (date, planId, planDayOverride) VALUES (20000, 1, 5)")
    val updatedDb = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        KenkoDatabase::class.java,
        DB_NAME,
    ).addMigrations(MIGRATION_12_13).build()
    val plan = updatedDb.planDao().getPlanById(1)
    assertEquals(7, plan?.dayCount)
    assertEquals(1, plan?.currentDayIndex)
    val items = updatedDb.planDao().getPlanItemsByPlanId(1)
    assertEquals(3, items.single().dayIndex)
    val session = updatedDb.sessionDao().getSession(EpochDays(20000))
    assertEquals(5, session?.data?.dayIndexOverride)
    updatedDb.close()
}
```

注意:`dataMigration12To13` 需与既有测试一致的 imports(`Room`、`InstrumentationRegistry`、`EpochDays`、`assertEquals` 均已在文件内;若 `getSession` 返回类型含 `data` 字段,按 `SessionEntity` 结构取 `?.data?.dayIndexOverride`)。

- [ ] **Step 4: 运行迁移测试**

Run: `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.looker.kenko.RoomDatabaseTesting`
Expected: 新增两个测试 PASS(需要设备/模拟器;无设备时记录并留待 CI)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/local/KenkoDatabase.kt app/src/main/kotlin/com/looker/kenko/data/local/Migrations.kt app/schemas/com.looker.kenko.data.local.KenkoDatabase/13.json app/src/androidTest/kotlin/com/looker/kenko/RoomDatabaseTesting.kt
git commit -m "feat: migrate DB to v13 with training cycle fields"
```

---

### Task 4: Repo 层推进与手动覆盖 + 仓库测试更新

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/PlanRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalPlanRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/dao/PlanDao.kt`
- Modify: `app/src/androidTest/kotlin/com/looker/kenko/RepositoryTest.kt`

**Interfaces:**
- Consumes: Task 1 `PlanCycle.nextDayIndex`、Task 2 的 `Plan.dayCount/currentDayIndex`。
- Produces(Home/编辑页依赖):
  - `PlanRepo.updateDayIndex(planId: Int, dayIndex: Int)`(手动覆盖当前训练日)
  - `PlanRepo.advanceDay(planId: Int, actualDayIndex: Int)`(完成训练后推进)
  - `PlanRepo.addDay(planId: Int)`(序列末尾新增一天)
  - `PlanRepo.deleteDay(planId: Int, dayIndex: Int)`(删除该天动作并重编号,currentDayIndex 越界回退到 1)
  - `PlanRepo.moveDay(planId: Int, from: Int, to: Int)`(训练日重排,dayIndex 联动)

- [ ] **Step 1: PlanDao 新增查询**

```kotlin
@Query("UPDATE plans SET currentDayIndex = :dayIndex WHERE id = :planId")
suspend fun updateCurrentDayIndex(planId: Int, dayIndex: Int)

@Query("UPDATE plans SET dayCount = dayCount + 1 WHERE id = :planId")
suspend fun incrementDayCount(planId: Int)

@Query("UPDATE plan_day SET dayIndex = dayIndex - 1 WHERE planId = :planId AND dayIndex > :day")
suspend fun decrementDayIndexes(planId: Int, day: Int)

@Query(
    """
    UPDATE plans SET
    dayCount = dayCount - 1,
    currentDayIndex = CASE
        WHEN currentDayIndex = :day THEN 1
        WHEN currentDayIndex > :day THEN currentDayIndex - 1
        ELSE currentDayIndex END
    WHERE id = :planId
    """,
)
suspend fun decrementDayCount(planId: Int, day: Int)

@Query("UPDATE plan_day SET dayIndex = :to WHERE planId = :planId AND dayIndex = :from")
suspend fun setDayIndex(planId: Int, from: Int, to: Int)

@Query("UPDATE plan_day SET dayIndex = dayIndex - 1 WHERE planId = :planId AND dayIndex > :from AND dayIndex <= :to")
suspend fun decrementDayRange(planId: Int, from: Int, to: Int)

@Query("UPDATE plan_day SET dayIndex = dayIndex + 1 WHERE planId = :planId AND dayIndex >= :from AND dayIndex < :to")
suspend fun incrementDayRange(planId: Int, from: Int, to: Int)

@Transaction
suspend fun deleteDay(planId: Int, day: Int) {
    deleteItemsByPlanIdAndDay(planId, day)
    decrementDayIndexes(planId, day)
    decrementDayCount(planId, day)
}

@Transaction
suspend fun moveDay(planId: Int, from: Int, to: Int) {
    if (from == to) return
    if (from < to) {
        decrementDayRange(planId, from + 1, to)
        setDayIndex(planId, from, to)
    } else {
        incrementDayRange(planId, to, from - 1)
        setDayIndex(planId, from, to)
    }
}
```

- [ ] **Step 2: PlanRepo 接口新增**

```kotlin
suspend fun updateDayIndex(planId: Int, dayIndex: Int)

suspend fun advanceDay(planId: Int, actualDayIndex: Int)

suspend fun addDay(planId: Int)

suspend fun deleteDay(planId: Int, dayIndex: Int)

suspend fun moveDay(planId: Int, from: Int, to: Int)
```

- [ ] **Step 3: LocalPlanRepo 实现**

```kotlin
override suspend fun updateDayIndex(planId: Int, dayIndex: Int) {
    val plan = dao.getPlanById(planId) ?: return
    dao.updateCurrentDayIndex(planId, dayIndex.coerceIn(1, plan.dayCount))
}

override suspend fun advanceDay(planId: Int, actualDayIndex: Int) {
    val plan = dao.getPlanById(planId) ?: return
    dao.updateCurrentDayIndex(planId, PlanCycle.nextDayIndex(actualDayIndex, plan.dayCount))
}

override suspend fun addDay(planId: Int) {
    dao.incrementDayCount(planId)
}

override suspend fun deleteDay(planId: Int, dayIndex: Int) {
    dao.deleteDay(planId, dayIndex)
}

override suspend fun moveDay(planId: Int, from: Int, to: Int) {
    val plan = dao.getPlanById(planId) ?: return
    dao.moveDay(planId, from, to)
    // dayTitles 的 key 同步搬移
    val shifted = plan.titlesMap.entries.mapNotNull { (day, title) ->
        val newDay = when {
            day == from -> to
            from < to && day in (from + 1)..to -> day - 1
            from > to && day in to until from -> day + 1
            else -> null
        }
        newDay?.let { it to title }
    }.toMap()
    val newDayTitles = if (shifted.isEmpty()) null else Json.encodeToString(shifted)
    dao.upsertPlan(plan.copy(dayTitles = newDayTitles))
}
```

补充:LocalPlanRepo 需 `import com.looker.kenko.domain.model.PlanCycle`、`kotlinx.serialization.json.Json`、`com.looker.kenko.domain.model.titlesMap`(确认已 import 或补全)。

- [ ] **Step 4: 更新 RepositoryTest**

`RepositoryTest.kt` 的 `checkPlanDeletion`:`PlanItem(dayOfWeek = DayOfWeek(Random.nextInt(1, 5)), ...)` 两处改为 `PlanItem(dayIndex = Random.nextInt(1, 5), ...)`;删除 `import kotlinx.datetime.DayOfWeek`。

追加测试:

```kotlin
@Test
fun planCycleAdvanceAndOverride() = runTest {
    val planId = planRepo.createPlan("cycle")
    assertEquals(7, planRepo.plan(planId)?.dayCount)
    assertEquals(1, planRepo.plan(planId)?.currentDayIndex)
    planRepo.advanceDay(planId, 7)
    assertEquals(1, planRepo.plan(planId)?.currentDayIndex) // 回绕
    planRepo.updateDayIndex(planId, 3)
    assertEquals(3, planRepo.plan(planId)?.currentDayIndex)
    planRepo.addDay(planId)
    assertEquals(8, planRepo.plan(planId)?.dayCount)
    planRepo.deleteDay(planId, 2)
    assertEquals(7, planRepo.plan(planId)?.dayCount)
}
```

(若 `planRepo.plan()` 每次查库,直接断言即可;`runTest` 与既有测试一致。)

- [ ] **Step 5: 运行测试**

Run: `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.looker.kenko.RepositoryTest`
Expected: `checkPlanDeletion` 与 `planCycleAdvanceAndOverride` PASS(需设备)。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/repository/PlanRepo.kt app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalPlanRepo.kt app/src/main/kotlin/com/looker/kenko/data/local/dao/PlanDao.kt app/src/androidTest/kotlin/com/looker/kenko/RepositoryTest.kt
git commit -m "feat: add plan day advance/override/restructure in repos"
```

---

### Task 5: Home 今日训练改造(推进、休息日、手动覆盖)

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/home/HomeViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/home/Home.kt`

**Interfaces:**
- Consumes: Task 2/4 的 `Plan.dayCount/currentDayIndex`、`Session.dayIndexOverride`、`PlanRepo.updateDayIndex/advanceDay`、`SessionRepo.updateDayIndex/previousSessionDate`。
- Produces:`HomeUiData` 字段变更(dayIndex/dayCount/isRestDay,移除 dayOfWeek)、`selectTrainingDay(dayIndex: Int)`。

- [ ] **Step 1: 新增字符串资源(Home/编辑页共用,先行定义避免编译失败)**

`values/strings.xml` 与 `values-zh/strings.xml` 同步新增(中文文案参考既有风格):

```xml
<!-- en -->
<string name="label_day_n">Day %1$d</string>
<string name="label_day_progress">Day %1$d/%2$d</string>
<string name="label_rest_day">Rest Day</string>
<string name="label_rest_short">Rest</string>
<string name="label_today_rest">Rest Day — train another day from the menu</string>
<string name="label_switch_training_day">Switch training day</string>
<string name="label_add_day">Add training day</string>
<string name="label_rename_day">Rename</string>
<string name="label_set_as_rest_day">Set as rest day</string>
<string name="label_delete_day">Delete this day</string>
<string name="label_train_other_day">Train another day</string>

<!-- zh -->
<string name="label_day_n">第 %1$d 天</string>
<string name="label_day_progress">第 %1$d/%2$d 天</string>
<string name="label_rest_day">休息日</string>
<string name="label_rest_short">休</string>
<string name="label_today_rest">今日休息 — 可从菜单选择其他训练日训练</string>
<string name="label_switch_training_day">切换训练日</string>
<string name="label_add_day">新增训练日</string>
<string name="label_rename_day">重命名</string>
<string name="label_set_as_rest_day">设为休息日</string>
<string name="label_delete_day">删除该训练日</string>
<string name="label_train_other_day">选择训练日训练</string>
```

- [ ] **Step 2: 改 HomeViewModel 状态流**

`HomeViewModel.kt`:
- `planItemStream`:

```kotlin
private val planItemStream = combine(
    sessionStream,
    planStream,
    planRepo.planItems
) { session, plan, planItems ->
    val day = session?.dayIndexOverride ?: plan?.currentDayIndex
    planItems.filter { day != null && it.dayIndex == day }
}
```

- `availablePlanDays`: `Map<Int, List<PlanItem>>`,`items.groupBy { it.dayIndex }`。
- `planDayTitles`: `Map<Int, String>`(plan?.titlesMap)。
- `previousSessionDate`:

```kotlin
val previousSessionDate: StateFlow<LocalDate?> = combine(
    planStream,
    sessionStream,
) { plan, session ->
    val day = session?.dayIndexOverride ?: plan?.currentDayIndex
    if (day == null) null else sessionRepo.previousSessionDate(today(), plan?.id, day)
}.flatMapLatest { it ?: flowOf(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

(需 `import kotlinx.coroutines.flow.flowOf`。)

- `state` 的 combine 与 `HomeUiData`:

```kotlin
val dayIndex = currentSession?.dayIndexOverride ?: currentPlan?.currentDayIndex
val dayCount = currentPlan?.dayCount ?: 7
val dayTitle = currentPlan?.titlesMap?.get(dayIndex)
val isRestDay = dayIndex != null && planItems.isEmpty()
HomeUiData(
    isPlanSelected = currentPlan != null,
    isSessionStarted = currentSession != null && currentSession.sets.isNotEmpty(),
    isTodayEmpty = planItems.isEmpty(),
    isFirstSession = isFirstSession,
    currentPlanId = currentPlan?.id,
    sessionDates = sessions.map { it.date }.toSet(),
    timerState = timerState,
    trainingState = trainingState,
    planName = currentPlan?.name,
    dayIndex = dayIndex,
    dayCount = dayCount,
    isRestDay = isRestDay,
    dayTitle = dayTitle,
    todayExercises = planItems.mapNotNull { it.exercise },
)
```

`HomeUiData` 字段:`val dayOfWeek: DayOfWeek = today().dayOfWeek` → `val dayIndex: Int? = null`、新增 `val dayCount: Int = 7`、`val isRestDay: Boolean = false`。删除 `import kotlinx.datetime.DayOfWeek`(若不再使用)。

- 函数:`importPlanFromDay(day: DayOfWeek)` → `selectTrainingDay(dayIndex: Int)`:

```kotlin
fun selectTrainingDay(dayIndex: Int) {
    viewModelScope.launch {
        try {
            val planId = planStream.first()?.id ?: return@launch
            sessionRepo.updateDayIndex(today(), dayIndex)
            planRepo.updateDayIndex(planId, dayIndex)
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}
```

- `endWorkout()` 增加推进:

```kotlin
fun endWorkout() {
    trainingSessionManager.endTraining()
    viewModelScope.launch {
        try {
            val plan = planStream.first() ?: return@launch
            val session = sessionStream.first()
            if (session?.sets?.isNotEmpty() == true) {
                val day = session.dayIndexOverride ?: plan.currentDayIndex
                planRepo.advanceDay(requireNotNull(plan.id), day)
            }
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}
```

- [ ] **Step 3: 改 Home.kt**

`Home.kt`:
- `Home` 外层(`onSelectPlanClick` 弹层,约 199–212 行):`availablePlanDays.toSortedMap().forEach { (day, _) -> ... }` 中 `val displayName = if (title.isNullOrBlank()) dayName(day) else "$title (${dayName(day)})"` → `val displayName = if (title.isNullOrBlank()) stringResource(R.string.label_day_n, day) else title`;点击回调改 `onSelectDay(day)`(签名改为 `(Int) -> Unit`)。
- `PlanInfoCard`(393–442 行):参数 `dayOfWeek: DayOfWeek` → `dayIndex: Int?`、`dayCount: Int`、新增 `isRestDay: Boolean`;日期行删除 `dayName(today().dayOfWeek)`(只显示日期);训练标题:

```kotlin
Text(
    text = when {
        isRestDay -> stringResource(R.string.label_today_rest)
        else -> "${stringResource(R.string.label_today_plan)}: ${dayTitle ?: stringResource(R.string.label_day_n, dayIndex ?: 1)}"
    },
    style = MaterialTheme.typography.titleMedium,
)
```

- 进度行新增:`"${stringResource(R.string.label_day_progress, dayIndex ?: 1, dayCount)}"`(label_day_progress 格式 `第 %1$d/%2$d 天`),置于训练标题下或日期行中。
- 训练卡菜单:在训练卡(或休息卡)右上角加 `IconButton`(`Icons.Default.MoreVert`)→ `DropdownMenu` 项 `label_switch_training_day`,点击打开"选择训练日"弹层(复用 199–212 行的弹层,提取为 `TrainingDayPickerDialog(availableDays: Map<Int, String>, onSelect: (Int) -> Unit, onDismiss)`);休息卡上直接显示"选择训练日训练"按钮调用同一弹层。
- `label_today_plan` 的调用处保留;`dayName` 不再在 Home 使用(确认后删除 Home.kt 中相关 import)。

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: Home 相关错误消除(其余 UI 引用错误会在 Task 6/7 修复;若仍有其他文件报错,先继续,最后统一验证)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/home/HomeViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/home/Home.kt
git commit -m "feat: rework Home for training cycle with rest days and manual override"
```

---

### Task 6: 计划编辑改造(训练日标签条 + 增删/排序/命名/休息)

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEditViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanEdit.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanExercise.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanItem.kt`
- Delete: `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/DaySwitcher.kt`
- Delete: `app/src/main/kotlin/com/looker/kenko/ui/component/Days.kt`

**Interfaces:**
- Consumes: Task 2/4 的 `Plan.dayCount`、`PlanRepo.addDay/deleteDay/moveDay/updateOrder(planId, day: Int, ...)`、`Plan.withDayTitle(dayIndex, title)`。
- Produces: `PlanEditState` 字段变更(`currentDay: Int`、`dayCount: Int`,移除 selectionMode),`setCurrentDay(dayIndex: Int)`、`addDay()`、`deleteCurrentDay()`、`moveDay(from: Int, to: Int)`。

- [ ] **Step 1: 改 PlanEditViewModel**

- `_dayOfWeek: MutableStateFlow<DayOfWeek> = MutableStateFlow(today().dayOfWeek)` → `_dayIndex: MutableStateFlow<Int> = MutableStateFlow(1)`。
- `state` combine:参数 `day` 换 `dayIndex`;`planItems = items.filter { it.dayIndex == dayIndex }`;`planTitles = plan?.titlesMap ?: emptyMap()`;新增 `dayCount = plan?.dayCount ?: 7`;`PlanEditState(currentDay = dayIndex, dayCount = ..., ...)`。
- init 中 dayTitleState 同步:`plan?.titlesMap?.get(day)` → `plan?.titlesMap?.get(dayIndex)`(combine 用 `_dayIndex`)。
- dayTitle 保存:`val day = _dayOfWeek.value` → `val day = _dayIndex.value`;`currentPlan.withDayTitle(day, title)` 不变(签名已改)。
- `setCurrentDay(dayOfWeek: DayOfWeek)` → `setCurrentDay(dayIndex: Int)`:emit `_dayIndex`;删除 `_fullDaySelection` 相关逻辑(连同 `_fullDaySelection`、`openFullDaySelection` 一起删除)。
- `addExercise`:`dayOfWeek = _dayOfWeek.value` → `dayIndex = _dayIndex.value`。
- `updateOrder`:`repo.updateOrder(planIdStream.value, _dayOfWeek.value, exercises)` → `_dayIndex.value`。
- 新增:

```kotlin
fun addDay() {
    viewModelScope.launch {
        try {
            repo.addDay(planIdStream.value)
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}

fun deleteCurrentDay() {
    viewModelScope.launch {
        try {
            repo.deleteDay(planIdStream.value, _dayIndex.value)
            _dayIndex.emit(1)
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}

fun setDayAsRest() {
    viewModelScope.launch {
        try {
            // 清空当天动作即成为休息日
            repo.getPlanItems(planIdStream.value, _dayIndex.value).forEach { repo.removeItem(requireNotNull(it.id)) }
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}

fun moveDay(from: Int, to: Int) {
    viewModelScope.launch {
        try {
            repo.moveDay(planIdStream.value, from, to)
            _dayIndex.emit(to)
        } catch (e: Exception) {
            _snackbar.emit(e.message ?: "An error occurred")
        }
    }
}
```

- `PlanEditState`:

```kotlin
@Stable
data class PlanEditState(
    val currentDay: Int,
    val dayCount: Int,
    val exerciseSheetVisible: Boolean,
    val planItems: List<PlanItem>,
    val planTitles: Map<Int, String> = emptyMap(),
)
```

删除 `selectionMode` 字段。删除 `import kotlinx.datetime.DayOfWeek`、`today`(确认不再使用)。

- [ ] **Step 2: 新建训练日标签条组件**

Create `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/components/TrainingDayBar.kt`:

```kotlin
package com.looker.kenko.ui.feature.plan.components

// 横向可滚动标签条:
// - 每个序列位置一个标签(1..dayCount):文本 = title ?: "第 N 天",休息日(isRest)灰显 + "休"角标
// - 末尾 "+" 按钮(onAddDay)
// - 点击标签 onSelectDay(dayIndex)
// - 长按标签弹出 DropdownMenu:重命名(回调聚焦标题输入框)/设为休息日(onSetAsRest)/删除该天(onDeleteDay)
// - 标签长按拖拽:参考 PlanEdit.kt 动作列表现有拖拽实现,松手调用 onMoveDay(from, to)
// 签名:
@Composable
fun TrainingDayBar(
    dayCount: Int,
    selectedDay: Int,
    titles: Map<Int, String>,
    restDays: Set<Int>, // dayIndex 集合(无 plan_day 行的位置由调用方计算)
    onSelectDay: (Int) -> Unit,
    onAddDay: () -> Unit,
    onMoveDay: (Int, Int) -> Unit,
    onRename: () -> Unit,
    onSetAsRest: () -> Unit,
    onDeleteDay: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`restDays` 计算规则:某 dayIndex 在 `1..dayCount` 内但对应 `planItems`(全计划)为空 → 休息日。调用方(PlanEdit.kt)用 `(1..dayCount).filter { day -> items.none { it.dayIndex == day } }.toSet()`。

标签文本统一用字符串资源:`R.string.label_day_n`("第 %1$d 天")、`R.string.label_rest_short`("休")、`R.string.label_rename_day`、`R.string.label_set_as_rest_day`、`R.string.label_delete_day`。

核心实现(标签行 + 菜单;拖拽重排参照 PlanEdit.kt 动作列表的现成 `detectDragGesturesAfterLongPress` 模式,松手时按目标位调用 `onMoveDay`):

```kotlin
@Composable
fun TrainingDayBar(
    dayCount: Int,
    selectedDay: Int,
    titles: Map<Int, String>,
    restDays: Set<Int>,
    onSelectDay: (Int) -> Unit,
    onAddDay: () -> Unit,
    onMoveDay: (Int, Int) -> Unit,
    onRename: () -> Unit,
    onSetAsRest: () -> Unit,
    onDeleteDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..dayCount).forEach { day ->
            val title = titles[day]
            val isRest = day in restDays
            DayTab(
                label = title ?: stringResource(R.string.label_day_n, day),
                isRest = isRest,
                selected = day == selectedDay,
                onClick = { onSelectDay(day) },
                onRename = onRename,
                onSetAsRest = onSetAsRest,
                onDeleteDay = onDeleteDay,
            )
        }
        FilledTonalIconButton(
            onClick = onAddDay,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.label_add_day))
        }
    }
}

@Composable
private fun DayTab(
    label: String,
    isRest: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onSetAsRest: () -> Unit,
    onDeleteDay: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                if (isRest) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.label_rest_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { menuExpanded = true },
        ),
    )
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.label_rename_day)) }, onClick = { menuExpanded = false; onRename() })
        DropdownMenuItem(text = { Text(stringResource(R.string.label_set_as_rest_day)) }, onClick = { menuExpanded = false; onSetAsRest() })
        DropdownMenuItem(text = { Text(stringResource(R.string.label_delete_day)) }, onClick = { menuExpanded = false; onDeleteDay() })
    }
}
```

若 `FilterChip` + `combinedClickable` 组合触控冲突,退化为:短按选择、长按仅弹菜单,拖拽重排通过菜单"移到前/后"两项替代(`onMoveDay(day, day-1)` / `onMoveDay(day, day+1)`),并把 `onMoveDay` 回调接入菜单。

- [ ] **Step 3: 改 PlanEdit.kt**

- `PlanEdit` 签名:`onSelectDay` 类型 `(DayOfWeek) -> Unit` → `(Int) -> Unit`;删除 `onFullDaySelection` 参数;新增 `onAddDay`、`onDeleteDay`、`onSetAsRest`、`onMoveDay` 参数;`state.currentDay`/`dayTitle` 用法相应改为 `Int`。
- 头部(约 299–343 行):删除 `HorizontalDaySelector` 与 `DaySwitcher` 分支,替换为 `TrainingDayBar(...)`;`state.planTitles[state.currentDay]` 用 Int key。
- 删除 `if (_fullDaySelection) ...` 逻辑与 `DaySwitcher` 引用。
- `AddExerciseSheet` 的 `title = viewModel.dayTitleState.text.ifBlank { name }`:`name = dayName(state.currentDay)` → `name = stringResource(R.string.label_day_n, state.currentDay)`。
- 删除 `import ... DaySwitcher`、`HorizontalDaySelector`、`dayName`(如无其他使用)。
- `onBackPress` 逻辑保留(空计划二次返回删除)。

- [ ] **Step 4: 改 PlanExercise.kt**

Header 参数从 `DayOfWeek` 切换改为 `TrainingDayBar`(传 `state.dayCount`、`state.currentDay`、`state.planTitles`、restDays 与回调)。

- [ ] **Step 5: 改 PlanItem.kt 文案**

`PlanItem.kt` 约 130–138 行:

```kotlin
Text(
    text = stringResource(
        R.string.label_plan_description,
        stats.exercises,
        normalizeInt(stats.workDays),
        normalizeInt((plan.dayCount - stats.workDays).coerceAtLeast(0)),
    ),
)
```

(需 `Plan` 有 `dayCount`;确认 `plan` 变量在作用域内。)

- [ ] **Step 6: 删除 DaySwitcher.kt 与 Days.kt**

删除 `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/DaySwitcher.kt`(含 `dayName`/`kenkoDayName`)与 `app/src/main/kotlin/com/looker/kenko/ui/component/Days.kt`(含 `HorizontalDaySelector`)。用 grep 确认无残留引用(`dayName(`, `kenkoDayName`, `HorizontalDaySelector`, `DaySwitcher`),Heatmap/Sessions 等若引用 `dayName` 属 Records 范畴,Task 7 处理(可先删除,Task 7 一并清理编译错误)。

- [ ] **Step 7: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: 计划编辑相关错误消除。

- [ ] **Step 8: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/plan app/src/main/kotlin/com/looker/kenko/ui/component/Days.kt
git commit -m "feat: rework plan editor with training-day bar and dynamic cycle"
```

---

### Task 7: Records / SessionDetail 显示训练日名

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionsViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/Sessions.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetailViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetail.kt`

**Interfaces:**
- Consumes: Task 2 的 `Session.dayIndexOverride`、`Plan.titlesMap: Map<Int, String>`。
- Produces: `SessionsViewModel.addSession(date: LocalDate, dayIndex: Int, onComplete)`、`availablePlanDays: Map<Int, List<Exercise>>`、`dayTitles: Map<Int?, Map<Int, String>>`;`SessionSummary.dayTitles: Map<Int?, Map<Int, String>>`。

- [ ] **Step 1: 改 SessionsViewModel**

- `availablePlanDays: Map<DayOfWeek, List<Exercise>>` → `Map<Int, List<Exercise>>`,构建处 `items.groupBy { it.dayIndex }`。
- `dayTitles: Map<Int?, Map<DayOfWeek, String>>` → `Map<Int?, Map<Int, String>>`(titlesMap 已是 Int key)。
- `addSession(date: LocalDate, day: DayOfWeek, ...)` → `addSession(date: LocalDate, dayIndex: Int, ...)`,内部 `sessionRepo.updateDayIndex(date, dayIndex)`。
- 删除 DayOfWeek import。

- [ ] **Step 2: 改 Sessions.kt**

- `selectedDay: DayOfWeek?` → `Int?`;`selectedDayName = selectedDay?.let { day -> selectedPlan?.titlesMap?.get(day) ?: dayName(day) }` → `selectedDay?.let { day -> selectedPlan?.titlesMap?.get(day) ?: stringResource(R.string.label_day_n, day) }`。
- `availableDays: Map<DayOfWeek, String>` → `Map<Int, String>`。
- `AddHistoryDialog`:参数 `availablePlanDays: Map<DayOfWeek, List<Exercise>>` → `Map<Int, List<Exercise>>`;`selectedDay: DayOfWeek?` → `Int?`;`LaunchedEffect(date) { if (date.dayOfWeek in availablePlanDays) ... }` → 删除按 weekday 的自动选中逻辑(改为 `selectedDay = availablePlanDays.keys.firstOrNull()`);`date.dayOfWeek` 相关逻辑删除。
- `SessionCard`:`dayTitles: Map<Int?, Map<DayOfWeek, String>>` → `Map<Int?, Map<Int, String>>`;`effectiveDay` 逻辑 `session.planDayOverride ?: session.date.dayOfWeek` → `session.dayIndexOverride`;`displayName = if (dayTitle.isNullOrBlank()) dayName else "$dayTitle ($dayName)"` → `if (dayTitle.isNullOrBlank()) stringResource(R.string.label_day_n, effectiveDay) else dayTitle`(注意 SessionCard 是 composable,可用 `stringResource`;`remember` keys 相应改)。
- 删除 `import kotlinx.datetime.DayOfWeek`、`dayName` 引用。
- `SessionsUiData` 相关字段类型随 ViewModel 改。

- [ ] **Step 3: 改 SessionDetailViewModel / SessionDetail.kt**

- `availablePlanDays: Map<DayOfWeek, List<Exercise>>`、`dayTitles: Map<DayOfWeek, String>` → `Map<Int, ...>`(titlesMap 直接使用)。
- `dayTitle` 取值:`currentSession.dayIndexOverride` 查 `titlesMap`。
- `Header`(538–541 行):`val name = dayName(performedOn.dayOfWeek)`;`dayText = if (dayTitle.isNullOrBlank()) name else "$dayTitle ($name)"` → `dayText = dayTitle ?: stringResource(R.string.label_day_n, dayIndexOverride ?: 1)`(dayIndexOverride 从 state 传入,Header 加参数)。
- 内部"切换计划日"弹层(238–246、374–385 行):`title.isNullOrBlank() -> dayName(day)` → `stringResource(R.string.label_day_n, day)`;类型改 Int。
- 删除 DayOfWeek/dayName 引用。

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: 全项目编译通过(若 Home.kt 还有 `dayName` 残留,一并清理)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/session
git commit -m "refactor: show training-day names in Records"
```

---

### Task 8: 字符串与死代码清理 + 全量验证

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/Plan.kt`(确认 `week` 已删)

**Interfaces:**
- Consumes: 全部前序任务的资源 key。
- Produces: 最终可编译产物。

- [ ] **Step 1: strings.xml 清理**

`values/strings.xml` 与 `values-zh/strings.xml`:
- 删除 `<string-array name="kenko_day_of_week">`(7 项)与 `<string-array name="day_of_week">`(7 项)。**保留** `day_of_week_short`(Heatmap 用)。
- 新字符串已在 Task 5 Step 1 添加,此处不再重复。

- [ ] **Step 2: 检查全项目编译**

Run: `./gradlew compileDebugKotlin`
Expected: 通过。若失败,按错误定位修复(多为 Task 6/7 遗留的 `dayName`/`DayOfWeek` 引用或未替换字符串 key)。

- [ ] **Step 3: 运行 JVM 单测**

Run: `./gradlew test`
Expected: `PlanCycleTest` 及既有测试全部 PASS。

- [ ] **Step 4: 残留引用检查**

Run: `grep -rn "dayOfWeek\|planDayOverride\|kenkoDayName\|HorizontalDaySelector\|DaySwitcher\|kenko_day_of_week\|R.array.day_of_week" app/src/main app/src/test app/src/androidTest`
Expected: 无匹配(除 `day_of_week_short`、`DayOfWeek` 在无计划相关场景的合法使用,如有则人工确认)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml app/src/main/kotlin/com/looker/kenko/domain/model/Plan.kt
git commit -m "chore: clean up weekday strings and unused cycle references"
```

---

## 验收清单(全部任务完成后)

- [ ] `./gradlew test` 全绿(PlanCycleTest 4 项 + 既有)。
- [ ] `./gradlew connectedAndroidTest` 全绿(迁移 12→13 ×2、RepositoryTest 更新 + 新增)。
- [ ] 手动验证:新建计划默认 7 天 → 增删/排序/命名/设休息 → Home 显示"第 N/7 天"与训练日名 → 完成训练后推进 → 休息日显示休息卡并可选择训练日 → 菜单切换训练日 → Records 显示训练日名。
- [ ] 老数据升级:安装 v12 数据 → 升级后计划仍 7 天、历史记录训练日序号保留。
