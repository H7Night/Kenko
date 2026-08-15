# 训练动作「按添加 set 顺序排序」实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在训练视图（首页内联训练 + 训练记录详情页）中，让动作列表按「首次添加 set 的顺序」动态排序：未开始动作保持 plan 顺序置顶，已开始动作置底并加 `1..N` 序号，重复添加 set 不重排。

**Architecture:** 复用已持久化的 `sets.order` 作为插入顺序真源。给领域模型 `Set` 增加 `order` 字段，新增纯函数 `orderTrainingExercises` 把「计划顺序 + 无序 set 列表」映射为有序的 `List<TrainingExercise>`，两个 ViewModel 各调一次，UI 只改渲染循环与名字前缀。

**Tech Stack:** Kotlin、Jetpack Compose、Room、Coroutines/Flow、Hilt、JUnit5（`kotlin-test-junit5` + `useJUnitPlatform()`）。

## Global Constraints

- 复用已有 `sets.order` 列，**不新增数据库字段、不做迁移**（spec §7）。
- 序号为纯数字前缀（如 `"1 侧平举"`），**不新增字符串资源**。
- `Set` 新增字段必须带默认值，保持现有位置/命名构造与序列化兼容（spec §3.1）。
- 新 Kotlin 文件需带 GPL v3 头（照抄 `app/src/main/kotlin/com/looker/kenko/domain/model/Set.kt` 第 1–14 行）。
- 不改 `StickyHeader` / `SetItem` / `DeletableSetItem` 等共享展示组件。
- 构建/测试命令为 Windows 环境，Gradle 用 `.\gradlew.bat`。首次运行会下载依赖，可能较慢。

---

## 文件结构

| 文件 | 职责 | 变更 |
|---|---|---|
| `domain/model/Set.kt` | 领域组模型 | 新增 `order: Int = 0` |
| `data/mapper/SetMapper.kt` | Set 实体↔领域映射 | `toExternal` 补 `order` 映射 |
| `domain/model/TrainingOrder.kt`（新） | 排序纯函数 + 行数据类 | 新建 |
| `ui/feature/home/HomeViewModel.kt` | 首页 `sessionSets` 流 | 改为产出 `List<TrainingExercise>` |
| `ui/feature/home/Home.kt` | 内联训练渲染 | 遍历有序列表 + 序号前缀 |
| `ui/feature/session/SessionDetailViewModel.kt` | 详情页状态 | `SessionUiData.sets` 改为 `List<TrainingExercise>` |
| `ui/feature/session/SessionDetail.kt` | 详情页渲染 | 遍历有序列表 + 序号前缀 |
| `src/test/kotlin/com/looker/kenko/domain/model/TrainingOrderTest.kt`（新） | 纯函数单测 | 新建 |

依赖顺序：Task 1（模型 + 纯函数）→ Task 2（Home）→ Task 3（SessionDetail）。Task 2、3 只依赖 Task 1 产出的接口，彼此独立。

---

### Task 1: 排序纯函数 + `Set.order` 字段

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/model/TrainingOrder.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/domain/model/TrainingOrderTest.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/Set.kt:23-29`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/mapper/SetMapper.kt:23-29`

**Interfaces:**
- Produces（供 Task 2/3 使用）：
  - `data class TrainingExercise(val exercise: Exercise, val sets: List<Set>, val sequence: Int? = null)`（`sequence` 1 起，`null` 表示未开始）
  - `fun orderTrainingExercises(planned: List<Exercise>, sets: List<Set>): List<TrainingExercise>`

- [ ] **Step 1: 写失败测试**

新建 `app/src/test/kotlin/com/looker/kenko/domain/model/TrainingOrderTest.kt`：

```kotlin
package com.looker.kenko.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TrainingOrderTest {

    private fun exercise(name: String, id: Int? = null) = Exercise(name = name, id = id)

    private fun setOf(exercise: Exercise, order: Int) = Set(
        repsOrDuration = 10,
        weight = 20F,
        exercise = exercise,
        rir = RepsInReserve(2),
        order = order,
    )

    @Test
    fun emptySetsKeepPlanOrderWithoutSequence() {
        val planned = listOf(exercise("高位下拉"), exercise("侧平举"), exercise("深蹲"))
        val result = orderTrainingExercises(planned, emptyList())
        assertEquals(listOf("高位下拉", "侧平举", "深蹲"), result.map { it.exercise.name })
        assertEquals(listOf(null, null, null), result.map { it.sequence })
    }

    @Test
    fun firstSetMovesExerciseToBottomWithSequenceOne() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        val result = orderTrainingExercises(planned, listOf(setOf(raise, 0)))
        assertEquals(listOf("高位下拉", "深蹲", "侧平举"), result.map { it.exercise.name })
        assertNull(result[0].sequence)
        assertNull(result[1].sequence)
        assertEquals(1, result[2].sequence)
    }

    @Test
    fun subsequentFirstSetsNumberInStartOrder() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 侧平举先加 set(order 0、1), 高位下拉后加 set(order 2)
        val result = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(raise, 1), setOf(pullDown, 2)),
        )
        assertEquals(listOf("深蹲", "侧平举", "高位下拉"), result.map { it.exercise.name })
        assertEquals(1, result[1].sequence)
        assertEquals(2, result[2].sequence)
    }

    @Test
    fun addingMoreSetsDoesNotReorderOrRenumber() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        val first = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(pullDown, 1)),
        )
        val second = orderTrainingExercises(
            planned,
            listOf(setOf(raise, 0), setOf(pullDown, 1), setOf(raise, 2)),
        )
        assertEquals(first.map { it.exercise.name }, second.map { it.exercise.name })
        assertEquals(first.map { it.sequence }, second.map { it.sequence })
    }

    @Test
    fun extraExerciseNotInPlanIsNumberedAndSorted() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val extra = exercise("额外动作", 9)
        val planned = listOf(pullDown, raise)
        val result = orderTrainingExercises(planned, listOf(setOf(extra, 0)))
        assertEquals(listOf("高位下拉", "侧平举", "额外动作"), result.map { it.exercise.name })
        assertEquals(1, result[2].sequence)
    }

    @Test
    fun unorderedSetsStillSortedByOrder() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 输入顺序打乱, 由 order 决定首组顺序
        val result = orderTrainingExercises(
            planned,
            listOf(setOf(pullDown, 2), setOf(raise, 0), setOf(pullDown, 3)),
        )
        assertEquals(listOf("深蹲", "侧平举", "高位下拉"), result.map { it.exercise.name })
        assertEquals(1, result[1].sequence)
        assertEquals(2, result[2].sequence)
    }

    @Test
    fun deletingAllSetsOfOneExerciseRenumbersRemaining() {
        val pullDown = exercise("高位下拉", 1)
        val raise = exercise("侧平举", 2)
        val squat = exercise("深蹲", 3)
        val planned = listOf(pullDown, raise, squat)
        // 侧平举删光全部 set 后: 回未开始组, 剩余高位下拉重编号为 1
        val result = orderTrainingExercises(planned, listOf(setOf(pullDown, 1)))
        assertEquals(listOf("侧平举", "深蹲", "高位下拉"), result.map { it.exercise.name })
        assertNull(result[0].sequence)
        assertNull(result[1].sequence)
        assertEquals(1, result[2].sequence)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.model.TrainingOrderTest"`
预期：FAIL（编译错误——`Set` 无 `order` 参数、`orderTrainingExercises`/`TrainingExercise` 不存在）。

- [ ] **Step 3: 最小实现**

**3a. `domain/model/Set.kt`** —— 把 `data class Set(...)` 改为（新增 `order` 字段，位于 `rir` 之后）：

```kotlin
@Serializable
@Immutable
data class Set(
    val repsOrDuration: Int,
    val weight: Float,
    val exercise: Exercise,
    val rir: RepsInReserve,
    val order: Int = 0,
    val id: Int? = null,
)
```

**3b. `data/mapper/SetMapper.kt`** —— `toExternal` 补 `order`：

```kotlin
fun SetEntity.toExternal(exercise: Exercise): Set = Set(
    repsOrDuration = repsOrDuration,
    weight = weight,
    exercise = exercise,
    rir = RepsInReserve(rir),
    order = order,
    id = id,
)
```

（`Set.toEntity` 保持不动：`LocalSessionRepo.addSet` 仍按 `getSetsCountBySessionId` 计算传入的 `order`。）

**3c. 新建 `domain/model/TrainingOrder.kt`** —— 顶部照抄 `Set.kt` 第 1–14 行 GPL 头，正文：

```kotlin
package com.looker.kenko.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TrainingExercise(
    val exercise: Exercise,
    val sets: List<Set>,
    val sequence: Int? = null,
)

/**
 * 训练动作排序: 未开始动作保持 [planned] 顺序置顶, 已开始动作按首次添加 set 的顺序
 * (取该动作所有 set 中最小的 order) 编号 1..N 并置底。
 */
fun orderTrainingExercises(
    planned: List<Exercise>,
    sets: List<Set>,
): List<TrainingExercise> {
    val setsByExercise: Map<Exercise, List<Set>> =
        sets.sortedBy { it.order }.groupBy { it.exercise }

    val started = setsByExercise.entries
        .sortedBy { (_, list) -> list.minOf { it.order } }
        .mapIndexed { index, (exercise, list) ->
            TrainingExercise(exercise, list, index + 1)
        }

    val notStarted = planned
        .filter { it !in setsByExercise }
        .map { TrainingExercise(it, emptyList(), null) }

    return notStarted + started
}
```

- [ ] **Step 4: 运行测试确认通过**

运行：`.\gradlew.bat :app:testDebugUnitTest --tests "com.looker.kenko.domain.model.TrainingOrderTest"`
预期：PASS（7 个用例全绿）。

- [ ] **Step 5: 提交**

```bash
git add -f app/src/main/kotlin/com/looker/kenko/domain/model/TrainingOrder.kt app/src/main/kotlin/com/looker/kenko/domain/model/Set.kt app/src/main/kotlin/com/looker/kenko/data/mapper/SetMapper.kt app/src/test/kotlin/com/looker/kenko/domain/model/TrainingOrderTest.kt
git commit -m "feat: add training exercise ordering by first set"
```

---

### Task 2: 首页内联训练接线

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/home/HomeViewModel.kt:21-46,115-128`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/home/Home.kt:73,309,333-374`

**Interfaces:**
- Consumes: Task 1 的 `TrainingExercise`、`orderTrainingExercises`。
- Produces: `HomeViewModel.sessionSets: StateFlow<List<TrainingExercise>>`；`InlineTrainingContent` 参数改为 `List<TrainingExercise>`。

- [ ] **Step 1: 改 `HomeViewModel.sessionSets` 类型与实现**

在 `HomeViewModel.kt` 顶部 import 区（第 21 行 `today` 之后）加两行：

```kotlin
import com.looker.kenko.domain.model.TrainingExercise
import com.looker.kenko.domain.model.orderTrainingExercises
```

把 `sessionSets` 声明（现第 115–128 行）整体替换为：

```kotlin
val sessionSets: StateFlow<List<TrainingExercise>> =
    combine(sessionStream, planItemStream) { session, planItems ->
        orderTrainingExercises(
            planned = planItems.map { it.exercise }.distinct(),
            sets = session?.sets ?: emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

（删除原 `fromPlan` / `fromSession` / `result` 逻辑，其「计划动作即使无 set 也展示」「额外有 set 的动作也纳入」两条语义已由纯函数内化。）

- [ ] **Step 2: 改 `Home.kt` 渲染**

在 import 区，删除 `import com.looker.kenko.domain.model.Set`（第 73 行，改后不再显式使用），新增：

```kotlin
import com.looker.kenko.domain.model.TrainingExercise
```

把 `InlineTrainingContent` 签名（第 309 行）改为：

```kotlin
private fun InlineTrainingContent(
    exerciseSets: List<TrainingExercise>,
    onAddSet: (Exercise) -> Unit,
    onRemoveSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
```

把函数体内的遍历块（第 333–374 行）替换为：

```kotlin
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        exerciseSets.forEach { row ->
            val exercise = row.exercise
            val sets = row.sets
            val isCollapsed = exercise.id in collapsedExercises
            StickyHeader(
                name = row.sequence?.let { "$it ${exercise.name}" } ?: exercise.name,
                setCount = sets.size,
                isCollapsed = isCollapsed,
                onCollapseToggle = {
                    if (isCollapsed) {
                        exercise.id?.let { collapsedExercises.remove(it) }
                    } else {
                        exercise.id?.let { collapsedExercises.add(it) }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onAddSet(exercise) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(stringResource(R.string.label_add), style = MaterialTheme.typography.labelSmall)
                    }
                },
            )
            if (!isCollapsed) {
                sets.forEachIndexed { index, set ->
                    DeletableSetItem(
                        set = set,
                        onDelete = { set.id?.let { setToDelete = it } },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        title = {
                            Text(
                                text = normalizeInt(index + 1),
                                style = MaterialTheme.typography.displayMedium.numbers(),
                            )
                        },
                    )
                }
            }
        }
    }
```

（`normalizeInt` 为 `Home.kt` 末尾私有函数，保持不动。）

- [ ] **Step 3: 编译验证**

运行：`.\gradlew.bat :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/home/HomeViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/home/Home.kt
git commit -m "feat: order home inline training exercises by first set"
```

---

### Task 3: 训练记录详情页接线

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetailViewModel.kt:29,232-244,338`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetail.kt:90,304,457-505,713-727`

**Interfaces:**
- Consumes: Task 1 的 `TrainingExercise`、`orderTrainingExercises`。
- Produces: `SessionUiData.sets: List<TrainingExercise>`；`SetsList` 参数改为 `List<TrainingExercise>`。

- [ ] **Step 1: 改 `SessionDetailViewModel`**

import 区：删除 `import com.looker.kenko.domain.model.Set`（第 29 行），新增：

```kotlin
import com.looker.kenko.domain.model.TrainingExercise
import com.looker.kenko.domain.model.orderTrainingExercises
```

把 `exerciseMap` 分支（现第 232–239 行）整体替换为：

```kotlin
val sets = orderTrainingExercises(
    planned = exercises,
    sets = currentSession.sets,
)
```

把 `SessionUiData(...)` 构造里的 `sets = exerciseMap` 改为 `sets = sets`（现第 244 行）。

把 `SessionUiData` 数据类字段（现第 338 行）改为：

```kotlin
val sets: List<TrainingExercise>,
```

- [ ] **Step 2: 改 `SessionDetail.kt` 渲染**

import 区：删除 `import com.looker.kenko.domain.model.Set`（第 90 行），新增：

```kotlin
import com.looker.kenko.domain.model.TrainingExercise
```

把 `SetsList` 签名（第 304 行）改为：

```kotlin
private fun SetsList(
    date: LocalDate,
    exerciseSets: List<TrainingExercise>,
    ...
) {
```

把遍历块（第 457–505 行）替换为：

```kotlin
        exerciseSets.forEach { row ->
            val exercise = row.exercise
            val sets = row.sets
            val isCollapsed = exercise.id in collapsedExercises
            item(
                span = { GridItemSpan(maxLineSpan) },
            ) {
                StickyHeader(
                    name = row.sequence?.let { "$it ${exercise.name}" } ?: exercise.name,
                    setCount = sets.size,
                    isCollapsed = isCollapsed,
                    onCollapseToggle = {
                        collapsedExercises = if (isCollapsed) {
                            exercise.id?.let { collapsedExercises - it } ?: collapsedExercises
                        } else {
                            exercise.id?.let { collapsedExercises + it } ?: collapsedExercises
                        }
                    }
                ) {
                    if (isEditMode) {
                        FilledTonalIconButton(
                            shapes = IconButtonShapes(
                                shape = MaterialShapes.Circle.toShape(),
                                pressedShape = MaterialShapes.Cookie6Sided.toShape(),
                            ),
                            onClick = { onSelectBottomSheet(exercise) },
                        ) {
                            Icon(painter = KenkoIcons.Add, contentDescription = null)
                        }
                    }
                }
            }
            if (!isCollapsed) {
                itemsIndexed(items = sets) { index, set ->
                    DeletableSetItem(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        set = set,
                        isToday = isToday,
                        isEditMode = isEditMode,
                        onRepsUpdate = { onUpdateSet(set.id, it, set.weight) },
                        onWeightUpdate = { onUpdateSet(set.id, set.repsOrDuration, it) },
                        onDelete = if (isEditMode) { { setToDelete = set.id } } else null,
                        title = {
                            Text(normalizeInt(index + 1))
                        },
                    )
                }
            }
        }
```

把 `SessionDetailPreview`（第 713–727 行）里 `sets = emptyMap()` 改为 `sets = emptyList()`。

- [ ] **Step 3: 编译验证**

运行：`.\gradlew.bat :app:compileDebugKotlin`
预期：BUILD SUCCESSFUL。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetailViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionDetail.kt
git commit -m "feat: order session detail exercises by first set"
```

---

## 收尾验证（三个 Task 全部完成后）

- [ ] 运行全量单测：`.\gradlew.bat :app:testDebugUnitTest` → 全部 PASS（含新增 `TrainingOrderTest`）。
- [ ] 运行 lint：`.\gradlew.bat :app:lintDebug` → 无新增 error（若报未使用 import，删除对应 `Set` import）。
- [ ] 手动冒烟（真机/模拟器，`Home` 开始训练）：默认 plan 顺序 → 给某动作加 set 后它置底并显示 `1` 前缀 → 给另一动作加 set 显示 `2` → 再次加 set 序号与位置不变 → 训练记录详情页表现一致。
- [ ] 提交前确认 `git status` 无意外文件。

## Self-Review 记录

- **Spec 覆盖**：§3.1 模型 → Task 1；§3.2 纯函数 → Task 1；§3.3 Home → Task 2、SessionDetail → Task 3；§3.4 UI → Task 2/3；§6 测试 → Task 1（纯函数单测）+ 收尾验证。
- **占位符扫描**：无 TBD/TODO/「类似 Task N」。
- **类型一致性**：`TrainingExercise`、`orderTrainingExercises(planned: List<Exercise>, sets: List<Set>): List<TrainingExercise>` 在 Task 1 定义，Task 2/3 引用一致；`sessionSets`/`SessionUiData.sets` 均统一为 `List<TrainingExercise>`。
