# 训练动作「按添加 set 顺序排序」设计

日期：2026-08-15
分支：feat/v1.8.0
状态：已批准（用户确认：方案 A——领域模型暴露 `order` + 纯函数统一排序；历史只读记录页也应用相同排序与序号）

## 1. 目标

在「开始训练」的训练视图（首页 `Home` 内联训练 + 训练记录详情页 `SessionDetail`）中，让动作列表按「添加 set 的动作」动态排序：

- 尚未开始的动作（还没有任何 set）：保持默认 plan 顺序，显示在顶部。
- 某动作**第一次**添加 set：把它移到列表底部，并按「开始顺序」在名字前加序号 `1`、`2`、`3`…。
- 已经有序号的动作**再次**添加 set：序号不变、位置不重排。

示例（默认 plan 顺序：高位下拉、侧平举、深蹲）：

1. 初始：`高位下拉、侧平举、深蹲`
2. 侧平举添加 set 后：`高位下拉、深蹲、1 侧平举`
3. 高位下拉添加 set 后：`深蹲、1 侧平举、2 高位下拉`

## 2. 排序规则（精确定义）

设 `planned` 为按计划顺序的动作列表（去重），`sets` 为当日 session 的全部 set。

1. 将 `sets` 按 `Set.order` 升序，得到动作的「首次出现顺序」：每个动作的「首组顺序」= 该动作所有 set 中最小的 `order`。
2. **未开始组**：`planned` 中没有任何 set 的动作，保持 `planned` 顺序，排在前面。
3. **已开始组**：有 set 的动作（含不在 `planned` 里的额外动作），按首组顺序升序，依次编号 `1..N`，排在后面。
4. 最终列表 = 未开始组 + 已开始组。

「再次添加 set 不重排」由规则自然保证：序号只取决于首组 `order`，重复添加不改变首组 `order`。

## 3. 架构

### 3.1 领域模型 —— `domain/model/Set.kt` + `data/mapper/SetMapper.kt`

`Set` 新增字段（放在 `rir` 之后、`id` 之前，带默认值保证现有位置/命名构造与序列化兼容）：

```kotlin
data class Set(
    val repsOrDuration: Int,
    val weight: Float,
    val exercise: Exercise,
    val rir: RepsInReserve,
    val order: Int = 0,
    val id: Int? = null,
)
```

- `SetEntity.toExternal` 增加 `order = order` 映射（DB 的 `sets.order` 本就持久化）。
- `Set.toEntity` 不变：`LocalSessionRepo.addSet` 仍按 `setsDao.getSetsCountBySessionId` 计算插入 `order`，写路径不受影响。
- 序列化安全：`Set` 的 `@Serializable` 目前无实际 JSON 编解码调用点（导出用 `ExportSet`、计划传输用 `PlanTransfer*`、迁移用 `LegacySet`），新增带默认值的字段不影响旧数据读取。

### 3.2 纯函数 —— `domain/model/TrainingOrder.kt`（新文件）

```kotlin
@Immutable
data class TrainingExercise(
    val exercise: Exercise,
    val sets: List<Set>,
    val sequence: Int? = null,   // 1 起；null = 尚未开始(无 set)
)

fun orderTrainingExercises(
    planned: List<Exercise>,     // 按计划顺序，已去重
    sets: List<Set>,             // 当日 session 的全部 set（可能无序）
): List<TrainingExercise>
```

实现要点：

- `val ordered = sets.sortedBy { it.order }`（`order` 在单 session 内单调递增，`sortedBy` 稳定）。
- 按 `exercise` 分组求每组最小 `order` 作为首组顺序。
- 未开始组 = `planned` 中 `sets` 为空者；已开始组 = 有 set 者按首组顺序编号。
- 纯函数、无 I/O，可独立单测；与现有 `TrainingDayMatch` 同层同风格。

### 3.3 ViewModel 接入

**Home —— `ui/feature/home/HomeViewModel.kt`**

- `sessionSets` 类型由 `StateFlow<Map<Exercise, List<Set>>>` 改为 `StateFlow<List<TrainingExercise>>`。
- 由 `combine(sessionStream, planItemStream)` 产生，内部调用：

```kotlin
orderTrainingExercises(
    planned = planItems.map { it.exercise }.distinct(),
    sets = session?.sets ?: emptyList(),
)
```

（现有「确保所有计划动作都出现（即使无 set）」「额外有 set 的动作也纳入」两条逻辑被纯函数内化。）

**SessionDetail —— `ui/feature/session/SessionDetailViewModel.kt`**

- `SessionUiData.sets` 类型由 `Map<Exercise, List<Set>>` 改为 `List<TrainingExercise>`。
- 用 `orderTrainingExercises(planned = exercises, sets = currentSession.sets)` 统一替代现有 `exerciseMap` 的 `when` 三段式分支（`exercisesToday` 已产出有序、去重的 `exercises`；`planned` 为空时全部动作落入已开始组，等价于历史只读的「按完成顺序」）。

### 3.4 UI —— `ui/feature/home/Home.kt` + `ui/feature/session/SessionDetail.kt`

- `InlineTrainingContent`（Home）与 `SetsList`（SessionDetail）的参数由 `Map<Exercise, List<Set>>` 改为 `List<TrainingExercise>`，`forEach` 遍历改为 `forEach { row -> ... }`。
- `StickyHeader(name = ...)` 传入：

```kotlin
row.sequence?.let { "$it ${row.exercise.name}" } ?: row.exercise.name
```

即 `"1 侧平举"` 的「序号 + 空格」前缀。收起/展开、删除 set、编辑等现有交互不变。

## 4. 数据流

添加 set（`AddSetViewModel.addSet` → `sessionRepo.addSet`，写入 `sets.order`）→ Room 流 `sessionStream` 重新发射 → `orderTrainingExercises` 重算 → `sessionSets` / `SessionUiData.sets` 更新 → UI 按新顺序渲染（新开始的动作带序号置底）。

## 5. 边界情况与错误处理

- **再次添加 set**：首组 `order` 不变 → 序号与位置不变。
- **删光某动作的全部 set**：该动作回到未开始组（按 plan 顺序回顶部），其余已开始动作重新连续编号 `1..N`（数据派生的自然结果，无额外状态）。
- **额外添加、不在计划里的动作**：只要出现（即已有 set）就落入已开始组，参与编号排序。
- **切换训练日**：`planItemStream` / `exercises` 变化 → 未开始组顺序自然跟随；已开始组按首组顺序不受影响。
- 本功能为纯展示排序，无新增 I/O、无新增错误路径。

## 6. 测试

- 新增 `app/src/test/kotlin/com/looker/kenko/domain/model/TrainingOrderTest.kt`（JUnit），覆盖：
  - 默认（全无 set）保持 plan 顺序；
  - 首个 set 动作置底并编号 `1`；
  - 多个动作按首组顺序连续编号 `1..N`；
  - 再次添加 set 不重排、序号不变；
  - 额外动作（不在 planned）参与编号；
  - 删光某动作 set 后回退未开始组、其余动作重编号；
  - `sets` 无序输入时结果仍按 `order` 正确排序。
- 编译 + `lintDebug` + 全量单测验证。

## 7. 明确不做（YAGNI）

- 不做新的持久化字段或数据库迁移（复用已有 `sets.order`）。
- 不做动作间的拖拽手动排序（本期排序完全由「添加 set」驱动）。
- 不改 `SetItem` / `DeletableSetItem` 等单组展示组件。
- 不改历史只读页的「可编辑」行为（仅展示排序与序号）。

## 8. 影响文件

| 文件 | 变更 |
|---|---|
| `domain/model/Set.kt` | `Set` 新增 `order: Int = 0` |
| `data/mapper/SetMapper.kt` | `toExternal` 映射 `order` |
| `domain/model/TrainingOrder.kt`（新） | `TrainingExercise` + `orderTrainingExercises` |
| `ui/feature/home/HomeViewModel.kt` | `sessionSets` 改为 `List<TrainingExercise>` |
| `ui/feature/home/Home.kt` | `InlineTrainingContent` 遍历有序列表 + 序号前缀 |
| `ui/feature/session/SessionDetailViewModel.kt` | `SessionUiData.sets` 改为 `List<TrainingExercise>` |
| `ui/feature/session/SessionDetail.kt` | `SetsList` 遍历有序列表 + 序号前缀 |
| `src/test/.../domain/model/TrainingOrderTest.kt`（新） | 单测 |
