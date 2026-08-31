# 计划导入导出功能设计

日期：2026-08-12
分支：feat/v1.8.0
状态：已批准（用户确认：缺失动作自动创建、导入全部非激活、单文件多计划数组）

## 1. 目标

在编辑计划页面（`PlanEdit`）右上角提供入口，支持：

- **导出计划**：从已有计划中多选（显示计划名 + 训练/休息天数，可全选），导出为单个 JSON 文件。
- **导入计划**：从 JSON 文件导入计划，允许计划名重复，缺失动作自动创建。

## 2. JSON 格式

单文件，顶层对象带版本号便于扩展：

```json
{
  "version": 1,
  "plans": [{
    "name": "Push Pull Leg",
    "description": null,
    "difficulty": "ADAPTABLE",
    "focus": null,
    "equipment": "FULL_GYM",
    "time": "NORMAL",
    "dayCount": 6,
    "dayTitles": {"1": "推", "2": "拉"},
    "days": [
      {"dayIndex": 1, "exercises": [
        {"name": "杠铃卧推", "target": "胸", "tags": [], "countType": "REPS", "isBodyweight": false}
      ]},
      {"dayIndex": 2, "exercises": [
        {"name": "引体向上", "target": "背", "tags": [], "countType": "REPS", "isBodyweight": true}
      ]}
    ]
  }]
}
```

规则：

- 动作按 **name** 引用（沿用项目现有导出约定 `ExportSet.exerciseName` / `ExportExercise.name`），不导出本地 id。
- `days` 数组内顺序即动作顺序（映射到 `plan_day.sortOrder`，`addItem` 自动追加）。
- **不含**本地 id、isActive、currentDayIndex。导入时强制非激活、`currentDayIndex = 1`。
- `difficulty/focus/equipment/time` 用枚举 `name` 字符串；导入时 `valueOf` 还原，未知值按 null 处理。

## 3. 架构

模仿现有 `ExportManager` / `BackupManager` 分层模式。

### 3.1 DTO —— `domain/model/PlanTransferData.kt`（新文件）

全部 `@Serializable`：

```kotlin
PlanTransferFile(version: Int = 1, plans: List<PlanTransfer>)
PlanTransfer(
    name: String,
    description: String? = null,
    difficulty: String? = null,   // Labels.Difficulty.name
    focus: String? = null,        // Labels.Focus.name
    equipment: String? = null,    // Labels.Equipment.name
    time: String? = null,         // Labels.Time.name
    dayCount: Int = 7,
    dayTitles: Map<Int, String> = emptyMap(),
    days: List<PlanDayTransfer>,
)
PlanDayTransfer(dayIndex: Int, exercises: List<PlanExerciseTransfer>)
PlanExerciseTransfer(
    name: String,
    target: String? = null,       // 第一个 tag 的 parentName（与现有导出一致）
    tags: List<String> = emptyList(),
    countType: String = "REPS",   // CountType.name
    isBodyweight: Boolean = false,
)
```

### 3.2 业务 —— `data/plan/PlanTransferManager.kt`（新文件）

注入 `@ApplicationContext context`、`PlanRepo`、`ExerciseRepo`。`Json { prettyPrint = true; encodeDefaults = true }`（与 `ExportManagerImpl` 一致）。

```kotlin
suspend fun exportPlans(planIds: List<Int>, destinationUri: Uri): Result<Unit>
suspend fun importPlans(uri: Uri): ImportSummary   // 成功数 / 失败数
data class ImportSummary(val imported: Int, val failed: Int)
```

**exportPlans**：按 `planRepo.plan(id)` 取 Plan（含 stat、dayTitles）、`getPlanItems(id)` 取 PlanItem（按 dayIndex 分组、保持返回顺序），组装 DTO；id 无效（`plan(id)` 返回 null）的计划跳过。`contentResolver.openOutputStream(uri)` + `OutputStreamWriter` 写入（复用 `ExportManagerImpl.kt:76-81` 模式）。写入失败抛异常，由 ViewModel 捕获报 snackbar。

**importPlans**：`contentResolver.openInputStream(uri)` 读文本 → `Json.decodeFromString<PlanTransferFile>` → 解析失败或 `version != 1` 即抛异常（ViewModel 报"无效的计划文件"）。逐计划执行，每个计划独立 try-catch：

1. `planRepo.createPlan(name, description, difficulty, focus, equipment, time)` → planId（允许重名，无 `planNameExists` 校验）
2. `planRepo.updatePlan(Plan(id=planId, name, description, difficulty, focus, equipment, time, isActive=false, dayTitles=encode, dayCount, currentDayIndex=1))`——注意不调用 `setCurrent`，天然非激活
3. 按 `days` 数组序（day 升序、组内动作按序）逐个：`exerciseRepo.getOrCreate(exercise)` → `planRepo.addItem(PlanItem(dayIndex, exercise, planId))`

dayTitles 编码：`Json.encodeToString(map)`（复用 `Plan.kt:55-72` 模式），空 map 存 null。

### 3.3 Repo 扩展

`ExerciseRepo` 新增（`LocalExerciseRepo` + `ExerciseDao` 实现）：

```kotlin
suspend fun getOrCreate(exercise: Exercise): Exercise
```

- `ExerciseDao` 补 `@Query("SELECT * FROM exercises WHERE name = :name LIMIT 1") suspend fun getByName(name: String): ExerciseEntity?`
- 存在 → 转 external 返回（带本地 id）；不存在 → `dao.upsert(entity)` 拿新 id 返回。

### 3.4 UI —— `ui/feature/plan/PlanTransferDialogs.kt`（新文件）+ `PlanEdit.kt` 接线

**入口**：`FullEdit` 的 `CenterAlignedTopAppBar` 增加 `actions` 槽位：`IconButton(Icons.Default.MoreVert)` → `DropdownMenu` 两项：导出计划 / 导入计划。两阶段（NameEdit / PlanEdit）均显示（导入导出与当前编辑对象无关，是全局操作）。

**导出选择器 `ExportPlanDialog`**（`AlertDialog`）：

- 顶部一行："全选" `Checkbox` + 文本（勾选全部/取消全部）。
- 计划列表：`LazyColumn`（限高，计划多时可滚动），每项 `Row(Checkbox + Column(计划名, 训练/休息天数))`。
- 训练/休息天数显示：`"%1$s 天训练 %2$s 天休息"`（`workDays` 取 `Plan.stat.workDays`，休息 = `(dayCount - workDays).coerceAtLeast(0)`，即新规则）。
- 底部：取消 / 导出。**导出按钮在未选中任何计划时禁用**。
- 确认后触发 `CreateDocument("application/json")` launcher，选好位置后调用 `exportPlans(selectedIds, uri)`。

**导入确认框**：`OpenDocument(["application/json"])` 选文件后先解析（仅解析头部/计数），显示"将导入 N 个计划"，确认后执行 `importPlans(uri)`，完成后 snackbar"已导入 N 个计划"（失败时"成功 X 个，失败 Y 个"）。

**launcher 放哪**：`PlanEdit.kt`（Composable）用 `rememberLauncherForActivityResult`（`BackupSection.kt:94-123` 模式），`CreateDocument`/`OpenDocument` 均在 Composable 层注册；确认对话框的中间状态（待导出 ids / 待导入 uri）用 `mutableStateOf` 暂存。

### 3.5 ViewModel —— `PlanEditViewModel`

新增状态/操作：

- `plansForExport: StateFlow<List<Plan>>`（= `repo.plans`，供选择器显示名字 + 训练/休息天数）
- `exportPlans(ids, uri)` / `importPlans(uri)`：`viewModelScope.launch { withContext(Dispatchers.IO) ... }`，成功/失败经现有 `snackbarState.showSnackbar(...)` 提示，错误消息经现有 `_snackbar` 通道。
- Manager 实例经构造函数注入（`@Inject`，Hilt 提供）。

### 3.6 字符串（中英各新增）

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

⚠️ 现有 `label_import_plan` 已被 SessionDetail 的"从其他计划日导入"弹窗占用，**不可复用**，新导入入口用 `label_import_plan_file`。

## 4. 数据流

**导出**：MoreVert 菜单"导出计划" → 读 `repo.plans` → `ExportPlanDialog`（多选 + 全选）→ 确认 → `CreateDocument` 选位置 → `exportPlans(ids, uri)` → snackbar"已导出 N 个计划"。

**导入**：MoreVert 菜单"导入计划" → `OpenDocument` 选 JSON → 解析计数 → 确认框"将导入 N 个计划" → `importPlans(uri)` → snackbar"已导入 N 个计划"（部分失败时用 partial 文案）。

## 5. 错误处理

- 导出：无计划可选时弹窗列表为空、导出按钮禁用；写入失败 → snackbar 错误。
- 导入：JSON 解析失败 → snackbar"无效的计划文件"；**逐计划独立 try-catch**，部分成功时报告"成功 X，失败 Y"；单个计划失败不影响其余。
- 动作创建失败（`getOrCreate` 异常）随所属计划失败，计入 failed。

## 6. 测试

- `PlanTransferDataTest`（JUnit）：DTO 序列化往返（含 dayTitles、多计划、动作字段）。
- `PlanTransferManagerTest`（JUnit，fake `PlanRepo`/`ExerciseRepo`）：
  - 导入创建计划且允许重名、导入后非激活（无 `setCurrent` 调用）
  - dayCount / dayTitles / currentDayIndex=1 正确
  - 动作顺序保持（addItem 调用顺序）
  - 缺失动作自动创建（`getOrCreate` 被调用）、已有动作复用
  - 部分失败：坏计划计入 failed，其余继续
- 编译 + `lintDebug` + 全量单测验证。

## 7. 明确不做（YAGNI）

- 不做覆盖/合并导入（总是创建新计划）。
- 不做导入详情预览（仅确认数量）。
- 不导出 isActive / currentDayIndex / 本地 id / 计划统计（workDays 由导入后 DB 重新计算）。
- 不做导入到指定目录选择（沿用系统文件选择器）。
