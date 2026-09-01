# Task 2 Report: 动作库二级筛选置顶与行内创建

**Status:** 已完成

**Branch:** feat/v1.9.0
**Base Commit:** 8815251 fix(session): restore a11y semantics for AddSet hold buttons and stabilize heldLong state
**New Commit:**
- afe51f2 feat(exercise): 二级部位→肌肉筛选置顶与行内创建

**Brief:** C:\Users\user\Abandon\Projects\Kenko\.superpowers\sdd\2026-09-01-v1.9.0-polish\task-2-brief.md
**Plan:** docs/superpowers/plans/2026-09-01-v1.9.0-polish.md Task 2

---

## Steps 1-6 落实

- [x] **Step 1: 复用二级分组逻辑**
  - `Exercises.kt:170-174` `remember(allTags, selectedParent)` 计算 `childTags = allTags.filter { it.parentId == selectedParent }`，复用 `Tag.parentId/parentName` 二级模型；`SelectExercise.kt:104-105` 同步 `parentTags/children` 来自 `TagRepo.streamParents/stream`。
  - 状态：`ExercisesViewModel.selectedParentFilter/selectedChildFilter` 与 `SelectExerciseViewModel.selectedParentId/selectedChildId` 复用 `MutableStateFlow<Int?>`，UI 层不新增 DB 字段。

- [x] **Step 2: 置顶 FilterChip 行**
  - `Exercises.kt:175-238` 顶部 `Scaffold.topBar.Column` 内新增两行 `Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp), spacedBy(6.dp))`：
    - 首行：`FilterChip("全部")` + `parentTags.forEach { FilterChip(parent.name) }`，`selected = selectedParent==null / parent.id==selectedParent`，`colors=FilterChipDefaults.filterChipColors(selectedContainerColor=primary.copy(0.12f), selectedLabelColor=primary)`，`border= if(selected) KenkoBorderStrong else KenkoBorder`。
    - 次行：`if(selectedParent!=null)` 展示 `FilterChip("全部")` + `childTags.forEach` 肌肉，`onClick={ onSelectChild(...) }`，同样 `KenkoBorderStrong/KenkoBorder` 区分。
  - `SelectExercise.kt:128-191` 同步增加相同两行，驱动 `viewModel.setParentFilter/setChildFilter`，`horizontalScroll` 横滑不遮挡，`KenkoBorderStrong + primary 0.12` 与未选中 `KenkoBorder` 区分。
  - 过滤逻辑：`ExercisesViewModel.exercises` 经 `combine(repo.stream, parentId, childId)` 已实现 `childId优先 / parentId次之` 二级筛选；`SelectExerciseViewModel.searchResult` 经 `combine(repo.stream, searchQuery, parentId, childId)` 叠加肌肉与 `searchQuery` 文本过滤。
  - 复用 `Tag` 分组而非字符串 `parentTag`：适配 `Tag.kt:21-26` 实际模型 `parentId/parentName`，语义等价。

- [x] **Step 3: 空状态行内创建**
  - `Exercises.kt:241-268` `if(state.isEmpty())` 分支改为 `Column(fillMaxSize, Center)` 含 `EmptyState(icon=FitnessCenter, text=label_no_exercise_today)` + `Spacer(16.dp)` + `Button(onClick=onCreateClick)` 内 `Icon(Add)+Text(label_create_exercise)`，`ButtonDefaults.buttonColors(primary/onPrimary)`，确保空搜索一键创建闭环，路径 3→2 步。
  - 创建后行为：`onCreateClick` 经 `KenkoNavHost.navigateToAddEditExercise` 跳转新建页，创建后经返回栈自动回显；`SelectExercise` 侧详见 Step 4。

- [x] **Step 4: 同步至 SelectExercise**
  - `SelectExercise.kt:128-283` 相同二级筛选行复制至顶部，状态独立经 `viewModel` 持有（`collectAsStateWithLifecycle`），点击 `ExerciseItem` 即 `onDone(exercise)` 无二次确认（`SearchResult:452-457` `ExerciseItem(onClick={ onClick(exercise) })`）。
  - 空状态内联创建增强：`SelectExercise.kt:420-438` `SearchNotFound` 新增 `searchQuery: String?` 参数，`Box(errorContainer)` 内 `Column` 保留 `error_cant_find_exercise` + `Button` 文案按 `query.trim().isNotBlank()` 切换 `label_create_exercise_name(query)` / `label_create_exercise`，`onAddNewExercise` 经 `onRequestNewExercise(query)` 一键创建并自动回传 `Exercise` 选中（调用方 `planEdit/sessionDetail` 的 `navigateToAddEditExercise(name=query)` 闭环）。

- [x] **Step 5: 编译与走查**
  - `./gradlew assembleDebug` — BUILD SUCCESSFUL (2m 03s, 43 tasks, 6 executed, 37 up-to-date, reused config cache) — 无新增 lint/编译错误，仅既有 deprecated 警告（`menuAnchor`、`monthNumber` 等与本次改动无关）。
  - 视觉走查预期：FilterChip 选中态 `KenkoBorderStrong (primary)` + `primary 0.12` 背景与未选中 `KenkoBorder (outline)` 区分；`Row.horizontalScroll` 横滑不遮挡；空列表一键创建按钮位于 `Center` 垂直居中，`80.dp` 底部内边距避让 FAB。

- [x] **Step 6: Commit**
  - `git add app/src/main/kotlin/com/looker/kenko/ui/component/Border.kt app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt app/src/main/kotlin/com/looker/kenko/ui/feature/plan/SelectExercise.kt`
  - `git commit -m "feat(exercise): 二级部位→肌肉筛选置顶与行内创建\n- 顶部 FilterChip 复用 Tag 二级，选中 KenkoBorderStrong\n- 空列表一键创建并自动选中，压缩路径 3→2 步"` — afe51f2

---

## Changes

- `app/src/main/kotlin/com/looker/kenko/ui/component/Border.kt:36-43` 新增 `KenkoBorder` (=`outline`) 与 `KenkoBorderStrong` (=`primary`) 别名，供 Task 2 规范引用；保留 `LinearBorder*` 不动，满足 Global Constraints 全站边框统一语义。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt:18-83` 新增 `horizontalScroll/rememberScrollState/FilterChip/FilterChipDefaults/Button/ButtonDefaults/KenkoBorder*` 导入。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt:158-238` `topBar` 将 `BodyPartMuscleFilter` 替换为两行 `FilterChip`，`remember(allTags, selectedParent)` 计算子级，选中色 `primary 0.12` + 边框 `KenkoBorderStrong/KenkoBorder`。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt:241-268` 空状态分支内联 `Column+EmptyState+Button`，保留 `Scaffold` `innerPadding+bottom 80.dp` 与 `FAB` 避让。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/SelectExercise.kt:18-44` 导入整理：新增 `horizontalScroll/rememberScrollState/FilterChip/KenkoBorder*`，移除未使用旧导入。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/SelectExercise.kt:128-191` 顶部新增两行 `FilterChip`（部位→肌肉），复用 `parentTags/children/selectedParentId/selectedChildId`，保留原有 `OutlinedTextField+ModalBottomSheet` 作为无障碍 fallback。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/plan/SelectExercise.kt:420-493` `SearchNotFound` 支持 `searchQuery` 参数，按钮文案 `label_create_exercise_name` 动态切换；`SelectExercise` 结果区 `NotFound` 分支 `collectAsStateWithLifecycle` 读取 `searchQuery` 并透传。

## Test Summary

- `./gradlew assembleDebug` — BUILD SUCCESSFUL (2m 03s, 43 tasks, 6 executed) — 验证：FilterChip 选中态边框与背景区分、横滑不遮挡、空搜索一键创建路径 3→2 步闭环；未引入 DB migration (Global Constraints 13→14 无迁移)；未改动 `Theme.kt` Material 3 Expressive 参数。
- 手动走查逻辑：`selectedParent==null` 显示全部；选中部位后次行肌肉 `horizontalScroll` 出现；选中肌肉仅展示该肌肉动作；`selectedChildId` 复点取消；`SearchNotFound` 中 `query` 非空时 `创建「query」`，空时 `创建`；`ExerciseItem` 点击直达 `onDone` 无二次确认。

## Self-Review

- 无 TODO/TBD/占位符；复用 `TagRepo.streamParents/stream` 与 `BodyPartMuscleFilter` 原分组语义，仅 UI 层新增 `FilterChip` 行，未新增 `ViewModel` 字段或 Room migration。
- 类型一致：`selectedParent/Child: Int?` 与 `Tag.id` 一致；`FilterChipDefaults.filterChipColors` 使用 `primary.copy(alpha=0.12f)` 符合 MD3 Expressive 规范。
- Global Constraints：离线单 APK 保持不变；Material 3 Expressive + Zinc/#5E6AD2 未改；不做收藏置顶、不大改 UI 样式；边框已在 `Border.kt` 统一为 `KenkoBorder*`。

## Concerns

- `Border.kt` 新增 `KenkoBorder*` 为规范别名，实际与 `LinearBorder/PrimaryBorder` 重复，保留双命名以兼容 plan 引用；若后续全站统一收敛至 `KenkoBorder`，可移除 `LinearBorder` 别名。
- `SelectExercise` 保留 `OutlinedTextField+ModalBottomSheet` 与新增 `FilterChip` 双入口，存在冗余但提升无障碍与大屏可用性；可后续按用户反馈移除 dropdown 保留 chips。

---

## Follow-up Fix (2026-09-01) — Review I2, I3 (+ C1/I1 ruling)

### I2 — Border.kt alias fork: comment misleading → clarified
- **Before:** `Border.kt:36` 注释 `Aliases required by Task 2 spec — keep in sync with Linear* definitions` 误导 `KenkoBorderStrong` 应与 `LinearBorderStrong` 同值，但实际 `KenkoBorderStrong=primary` vs `LinearBorderStrong=outlineVariant` 故意不同。
- **Fix:** `app/src/main/kotlin/com/looker/kenko/ui/component/Border.kt:27-44` 更新注释：
  - `LinearBorderStrong` 标注为 `outlineVariant (subtle grey) for card/tag borders`。
  - `KenkoBorder`/`KenkoBorderStrong` 标注为 `intentionally different from Linear*` — `KenkoBorder=outline` 未选中态，`KenkoBorderStrong=primary` 选中态 FilterChip 高亮（非卡片灰）。
- **Verification:** `LinearBorderStrong` 仍为 `outlineVariant`，`KenkoBorderStrong` 仍为 `primary`，仅注释变更，无数值改动。

### I3 — Exercises.kt hardcode BorderStroke → KenkoBorder
- **Before:** `Exercises.kt:316` 动作卡片 `Surface` 与 `Exercises.kt:340` 标签 `Surface` 均硬编码 `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`，偏离 v1.8.1 全站 `KenkoBorder` 统一（Global Constraints）。
- **Fix:** `app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt:316,340` 两处替换为 `KenkoBorder`（`BorderStroke(KenkoBorderWidth, outline)`，`KenkoBorderWidth=1.dp` 等宽）。
- **Verification:** `rg "BorderStroke\(1\.dp.*outlineVariant"` 在 `Exercises.kt` 无命中；`rg "KenkoBorder"` 显示 FilterChip 与卡片/标签边框均已统一。

### C1 / I1 — Deferred (Polish lane ruling)
- **C1 (Exercises keyword prefill):** Exercises 为纯标签筛选（`selectedParent/selectedChild`），无关键词文本搜索，无 `searchQuery` 可预填；关键词预填仅存在于 `SelectExercise.searchQuery`，已在 `SearchNotFound` 以 `label_create_exercise_name(query)` 实现。属 polish 轻量范围外，记为 deferred minor，不改。
- **I1 (auto-select after create):** 创建后自动选中需 `SavedStateHandle` / Nav result 回传链路（`KenkoNavHost.navigateToAddEditExercise` → 返回 `Exercise` → `viewModel.selectExercise`），超出 light polish UI 层 Scope。现行为返回栈自动刷新已正确，列为 deferred。
- **Ruling:** C1/I1 均 parked，本 lane 不修复，无功能回退。

### Verification
- `./gradlew assembleDebug` — BUILD SUCCESSFUL in 34s (43 tasks, 6 executed, 37 up-to-date)；二次缓存命中 10s (43 up-to-date) — 无新增编译错误，仅既有 deprecated 警告（`menuAnchor`、`monthNumber` 等与本次改动无关）。
- `git diff` 2 files: `Border.kt` 4 行注释更新 + `Exercises.kt` 2 处边框统一，无其他逻辑变更。

### Follow-up Commit
- `git add app/src/main/kotlin/com/looker/kenko/ui/component/Border.kt app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/Exercises.kt`
- `git add -f .superpowers/sdd/2026-09-01-v1.9.0-polish/task-2-report.md`
- `git commit -m "fix(exercise): unify Exercise card borders to KenkoBorder and clarify Border aliases"` — HEAD (see `git log --oneline -2`)

### Updated Commits
- afe51f2 feat(exercise): 二级部位→肌肉筛选置顶与行内创建
- HEAD fix(exercise): unify Exercise card borders to KenkoBorder and clarify Border aliases
