# Task 3 Report: 显示与查询字阶统一与空状态

**Status:** 已完成

**Branch:** feat/v1.9.0
**Base Commit:** 95f0efd fix(exercise): unify Exercise card borders to KenkoBorder and clarify Border aliases
**New Commit:**
- b81a00b style(ui): 统一卡片三档字阶与空状态

**Brief:** C:\Users\user\Abandon\Projects\Kenko\.superpowers\sdd\2026-09-01-v1.9.0-polish\task-3-brief.md
**Plan:** docs/superpowers/plans/2026-09-01-v1.9.0-polish.md Task 3

**Global Constraints:** 离线单 APK / M3 Expressive 参数化 / 无 DB migration 13→14 / 不做最近/收藏置顶 / 不大改样式

---

## Steps 1-4 落实

- [x] **Step 1: 定义三档规范并替换**
  - 规范：标题 `MaterialTheme.typography.titleSmall`，数值 `titleMedium.copy(fontFamily=numbers())` (via `numbers()`), 辅助 `labelSmall` + `color = onSurfaceVariant` (不带 alpha)。
  - `SetItem.kt:165-199` `PerformedItem`：
    - 标题 `title.uppercase()` 已为 `labelSmall onSurfaceVariant` (保留)。
    - 数值 `performance` 由 `labelLarge.copy(bodyFont, Medium)` 改为 `titleMedium.numbers()`，符合三档数值档；移除 `bodyFont/FontWeight` 未使用导入。
  - `StickyHeader.kt:67-91`：
    - `sequence` 已为 `labelSmall.numbers() onSurfaceVariant` (保留)，
    - `name` 已为 `titleSmall onSurface` (保留)，
    - `setCount` 已为 `labelSmall onSurfaceVariant` (保留)，
    - Icon `KeyboardArrowRight` tint 由 `onSurfaceVariant.copy(alpha=0.6f)` 改为 `onSurfaceVariant` 去除散落 alpha。
  - `Sessions.kt:539-551` `SessionCard`：
    - `durationSeconds` 文本保持 `labelSmall`，颜色由 `onSurfaceVariant.copy(alpha=0.7f)` 改为 `onSurfaceVariant` 去除 alpha，注释标注三档规范。
    - `exerciseNames` 由 `bodySmall onSurfaceVariant` 改为 `labelSmall onSurfaceVariant` (maxLines=2 保留)。
  - `PlanItem.kt:97-142` 已符合三档：`plan.name` `titleSmall`，`label_selected` `labelSmall primary`，`stat` 描述 `labelSmall onSurfaceVariant` — 保留背景选中动画与 `borderColor`。
  - `PlanCards.kt:86-151` H标题字阶统一：
    - `CurrentPlanCard`：`label_current_plan` 由 `titleMedium` → `titleSmall` (标题档)，`name` 由 `titleLarge` → `titleMedium` (标题档收敛)，`content` 的 `LocalTextStyle` 由 `bodyLarge` → `labelSmall` (辅助档 `labelSmall onSurfaceVariant`)。
    - `SelectPlanCard`：`label_select_plan` 由 `headlineLarge` → `titleLarge` (H标题收敛至 title 系)。

- [x] **Step 2: 空状态补充**
  - `Sessions.kt:321-346` 在 `LazyColumn` 内 `if(filteredSessions.isEmpty()) item { EmptyState(icon=Icons.Rounded.History, text=label_no_sessions) } else items(...)`：
    - 覆盖 `hasAnySessions==true` 但 `filteredSessions.isEmpty()`（按月/Plan/Day 筛选无结果）分支；`hasAnySessions==false` 已有顶部 `EmptyState` (line 221) 保留。
    - 复用既有 `import EmptyState` 与 `Icons.Rounded.History`。
  - `WeightHistorySheet.kt:27-88` 新增 `import Icons.Rounded.History`，`weights.isEmpty()` 分支展示 `EmptyState(icon=History, text=label_no_weight_in_period, modifier=fillMaxWidth+padding vertical 24.dp)`，否则展示原 `LazyColumn`；与 `Profile.kt:477-490` 的 `weights.isEmpty()` 占位互补。

- [x] **Step 3: 编译与视觉走查**
  - `./gradlew assembleDebug` — BUILD SUCCESSFUL in 23s (43 tasks, 6 executed, 37 up-to-date) — 仅既有 deprecated 警告 (`menuAnchor`, `monthNumber/dayOfMonth`) 与本次改动无关；无新增 lint/编译错误。
  - 预期：`PlanItem/SessionCard/SetItem/StickyHeader` 三档一致 (`titleSmall` 标题 / `titleMedium.numbers()` 数值 / `labelSmall onSurfaceVariant` 辅助)，Light/Dark 对比度≥4.5:1（`onSurfaceVariant` 非 alpha 半透明），空状态文案统一 `label_no_sessions` / `label_no_weight_in_period`。

- [x] **Step 4: Commit**
  - `git add app/src/main/kotlin/com/looker/kenko/ui/component/SetItem.kt app/src/main/kotlin/com/looker/kenko/ui/component/StickyHeader.kt app/src/main/kotlin/com/looker/kenko/ui/feature/session/Sessions.kt app/src/main/kotlin/com/looker/kenko/ui/feature/profile/PlanCards.kt app/src/main/kotlin/com/looker/kenko/ui/feature/profile/WeightHistorySheet.kt`
  - `git add -f .superpowers/sdd/2026-09-01-v1.9.0-polish/task-3-report.md`
  - `git commit -m "style(ui): 统一卡片三档字阶与空状态\n- titleSmall/numbers/labelSmall 三档替换散落 bodySmall 0.6f\n- Sessions/WeightHistory 空状态统一 EmptyState"`

---

## Changes

- `app/src/main/kotlin/com/looker/kenko/ui/component/SetItem.kt:52-67` 移除 `bodyFont/FontWeight` 导入，保留 `numbers`；`PerformedItem:196-199` performance 样式改为 `titleMedium.numbers()`。
- `app/src/main/kotlin/com/looker/kenko/ui/component/StickyHeader.kt:92` Icon tint 去 alpha `copy(0.6f)` → `onSurfaceVariant`。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/session/Sessions.kt:321-346` 新增 `filteredSessions.isEmpty()` EmptyState 分支；`539` 注释更新；`542` duration 颜色去 alpha；`554` exerciseNames `bodySmall` → `labelSmall`。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/profile/PlanCards.kt:89` `titleMedium` → `titleSmall`；`111` `titleLarge` → `titleMedium`；`115` `bodyLarge` → `labelSmall`；`151` `headlineLarge` → `titleLarge`。
- `app/src/main/kotlin/com/looker/kenko/ui/feature/profile/WeightHistorySheet.kt:28` 新增 `History` 导入；`81-88` 新增 `weights.isEmpty()` EmptyState 分支。

## Test Summary

- `./gradlew assembleDebug` — BUILD SUCCESSFUL (23s, 43 tasks, 6 executed) — 验证：三档字阶统一、辅助色无 alpha 0.6-0.7f 散落、Sessions 筛选空结果与 WeightHistory 空列表均展示 `EmptyState`；未引入 DB migration；未改动 `Theme.kt`，符合 Global Constraints。
- 手动逻辑：`filteredSessions.isEmpty()` 时 LazyColumn 单 item EmptyState 可滚动居中；`weights.isEmpty()` 时 ModalBottomSheet 内 EmptyState 占位 24dp 垂直边距。

## Self-Review

- 无 TODO/TBD/占位符；复用 `MaterialTheme.typography.titleSmall/labelSmall/titleMedium.numbers()` 与 `EmptyState` 组件，未新增新组件或 DB 字段。
- 类型一致：`EmptyState(icon: ImageVector, text: String)` 复用 `Icons.Rounded.History`，`stringResource(R.string.label_no_sessions/label_no_weight_in_period)` 已存在。
- Global Constraints：离线单 APK 保持不变；Material 3 Expressive + Zinc/#5E6AD2 未改；不做最近/收藏置顶；App 入口与页面显示保持不变符合不大改样式约束。

## Concerns

- `PlanCards` H标题收敛至 `titleSmall/titleMedium/titleLarge`，若设计需保留 `headlineLarge` 视觉冲击，可回调 `headlineLarge` 但需同步更新 `Type.kt` 字阶比例。
- `WeightHistorySheet` EmptyState 使用 `History` 图标与 Sessions 统一，语义稍弱于 `MonitorWeight`，可按 UX 反馈替换图标不影响结构。
