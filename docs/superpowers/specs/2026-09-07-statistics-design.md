# Statistics Page Expansion — Design Spec

**Date:** 2026-09-07
**Branch:** `feat/v1.9.0`
**Status:** Approved (Approach B)
**Author:** Brainstorming with user

---

## 1. Overview & Goals

**Context:** `feat/v1.9.0` currently has 4-tab navigation (`home/records/statistics/profile`) and a yearly `YearHeatmap` on the statistics page. The records page no longer shows a heatmap. The app models workouts as `Session(date, sets[Set(repsOrDuration, weight, Exercise)])`, where `Exercise.tags: List<Tag(parentName)>` maps to 7 primary body parts (胸/背/腿/手臂/肩/腹/有氧).

**User intent:** Show more on the statistics page, centered on **body-part training frequency by week / month / current plan** (7 primary parts). Frequency is the primary metric; volume/1RM are out of scope.

**Supplemental constraints from user:**
- Bottom navigation label must be Chinese `统计`.
- Heatmap defaults to **last 90 days** (GitHub style), matching `NzHelper` pattern (`calculateHeatmapData` with `weeksToShow=13` →91 days ≈90天).
- Cardio handling: `Tag.parentName=="有氧"` 的训练卡片不展示组数，仅展示时长（分钟）；统计中该部位以“分钟”而非“次”计量。

**What we will do:**
- Build a frequency-centric balanced dashboard that simultaneously motivates (streak/trend), reveals imbalance (balance ring), and shows progress (week-over-week).
- No new tables/DAOs; aggregate in ViewModel from existing `SessionRepo.streamSummaries`.

**What we will not do (YAGNI):**
- Weight/volume/1RM curves, duration/calories, secondary muscle drill-down, cardio breakdown, plan history comparison across all plans.

**Success criteria:**
- Statistics opens <1s, shows 90-day heatmap + weekly/monthly/plan cards + balance + trend + adherence.
- 7-way aggregation matches records filtering logic.
- Empty states guide to `Select Plan` or `No records`.

---

## 2. Architecture & Components

**Data layer — reuse only:**
- `SessionRepo.streamSummaries: Flow<List<SessionSummary>>` (date, planId, exerciseNames, setCount) — for频次
- `SessionRepo.stream: Flow<List<Session>>` — for有氧时长（需 `Set.repsOrDuration` + `Exercise.countType==MINUTES`）
- `ExerciseRepo.stream: Flow<List<Exercise>>` for `exerciseName → (parentName, countType)` dictionary
- `PlanRepo.current: Flow<Plan?>` for `dayCount` (adherence denominator)
- No DAO changes; no `PlanHistory` changes.

**New / modified files:**
- `ui/feature/statistics/StatisticsViewModel.kt` — extend `StatisticsUiState` to `heatmapData: HeatmapData(90d)`, `weeklyCounts/pct`, `monthlyCounts`, `planCounts`, `weeklyTrend[12]`, `adherence: Float`, `weakPart: String?`
- `ui/feature/statistics/HeatmapCard.kt` — refactor `YearHeatmap.kt` to `HeatmapCard(weeksToShow=13)` reusing `NzHelper` grid logic (Monday start, month labels, less/more legend, active days)
- `ui/feature/statistics/components/BodyPartBarCard.kt` — reusable 7-row horizontal bar (max=100%, `KenkoBorderStrong` for selected plan chip)
- `ui/feature/statistics/components/BalanceRingCard.kt` — Canvas ring / donut for monthly distribution, thin `primaryContainer` thresholds
- `ui/feature/statistics/components/TrendCard.kt` — 12-week sparkline (Path, `outlineVariant` grid)
- `ui/feature/statistics/components/AdherenceCard.kt` — progress bar `actualDays / plan.dayCount` for current month
- `res/values/strings.xml` — `label_statistics` → `统计` (replace `"Statistics"`), keep `label_less/more/active_days/max_per_day`
- `ui/component/KenkoBottomBar.kt` — already 4 tabs; only string change needed (already done)

**Isolation:**
- Pure function `aggregateByBodyPart(summaries, predicate, tagDict): Map<String,Int>` — testable without Compose.
- UI consumes `Map` only; no direct DB access from composables.

---

## 3. Data Flow & Aggregation

**Flow:**
```
SessionRepo.streamSummaries ─┐
ExerciseRepo.stream ─────────┤→ combine → StatisticsUiState (WhileSubscribed 5s, asStateFlow)
PlanRepo.current ────────────┘
```

**Mapping:**
- Build `tagDict: Map<exerciseName, (parentName, countType)>` from `ExerciseRepo` once per emission.
- For strength (胸/背/腿/手臂/肩/腹): for each `SessionSummary`, resolve `exerciseNames → parentNames` via dict; `null/empty → "有氧"` 已排除；Deduplicate per session per parent (bench+fly → 胸=1).
- For cardio `"有氧"`: from `Session.stream` 聚合 `sum(repsOrDuration)` where `parentName=="有氧"` 或 `countType==MINUTES`；按 `predicate(date)` 过滤后求和，得到 `minutesByPeriod: Int`。
- Result: `countByBodyPart: Map<String,Int>` (6 项) + `cardioMinutes: Int` 单独；UI 对有氧显示“X 分钟”，其余显示“X 次”。柱长按各自最大值归一（有氧按分钟最大值，力量按次数最大值分尺度，避免 30 分钟压制 3 次）。

**Time predicates:**
- Week: `date in [mondayOfThisWeek, today]` via `isoDayNumber`
- Month: `date.year==now.year && date.month==now.month`
- Plan: `session.planId == currentPlan?.id`
- Trend: iterate 12 weeks back, each Monday-Sunday bucket
- Heatmap: `startMonday = today.minus((dow-1) days).minus(12 weeks)`, 13 weeks ×7 =91 days

**Performance:** Single pass over `summaries` (<5k) per emission, `remember(summaries, currentPlan, tagDict)` caching; `DefaultDispatcher` for aggregation if >1k.

---

## 4. Layout & Interaction

**Page structure — `Scaffold(LargeFlexibleTopAppBar("统计"))` + `LazyColumn` (16dp padding, 16dp spacing, NzHelper spacing):**
1. `HeatmapCard(90d)` — horizontally scrollable, month labels on top, weekday `一/三/五/日` on left, `active X/90` + `max Y` footer (already in `YearHeatmap`)
2. `BalanceRingCard` — monthly 7-part donut, <8% slice red dot + tooltip “薄弱”（有氧按分钟占比折算为次数等效，或单独灰段）
3. `BodyPartBarCard` ×3 — **本周 / 本月 / 本计划** (each 7 rows; 6 项力量 `X 次`，有氧 `Y 分钟`；柱长分尺度：力量按 `maxCount`、有氧按 `maxMinutes` 各自归一，降序中力量与有氧分别排序，weakest 力量项标 `error` tint)
4. `TrendCard` — 12-week column/sparkline, tap bar → `navigateToSessions(dateOfWeek)` (optional, no-op if no navigation)
5. `AdherenceCard` — `Text("本月达成 12/20 天")` + `LinearProgressIndicator`

**Bottom nav:** `KenkoBottomBar` 4 tabs equal width, `label_statistics="统计"`, `ic_show_chart` unchanged.

**Empty states:**
- Global `summaries.isEmpty()` → `EmptyState("暂无训练，去完成第一次")`
- Single card empty → card内 `Text("暂无记录")` + 0-width bars (no crash)
- No active plan → `planCounts` card shows `Button("选择计划") → navigateToPlans`

---

## 5. Error & Edge Cases

- **No plan:** `PlanRepo.current` null → `adherence = 0f`, `planCounts` empty with CTA.
- **90d no data:** heatmap renders grey grid, `active=0`.
- **Tag missing:** `parentName` null → bucket `"有氧"`; never throw.
- **有氧无时长:** `Session.stream` 中有氧 `repsOrDuration==0` → 计 0 分钟，卡片显示“0 分钟”。
- **Cross-year heatmap:** Monday alignment may start in prior month/year; month labels handle it.
- **Large data:** Aggregation on `DefaultDispatcher`; UI observes `StateFlow`.

---

## 6. Testing & Rollout

**Unit:**
- `aggregateByBodyPart` cases: empty, single session multi-tag, duplicate parent in same session, null tag → 有氧, week/month/plan predicates boundaries (Monday, month-end).
- `aggregateCardioMinutes` cases: 0 分钟、单次多组有氧累加、混合力量+有氧同会话、有氧 `countType==MINUTES` 判定。

**UI preview:**
- `StatisticsPreview` with 3 fixtures: empty, balanced (each part 4×), imbalanced (胸 10, 腿 0).

**Manual:**
- Scroll 90d, change system date, switch plan, verify weekly/monthly counts match Records filter.

**Rollout:** `feat/v1.9.0` branch, no DB migration, `assembleDebug` must pass. Follow-up `writing-plans` will break into tasks: ViewModel aggregation → Heatmap 90d refactor → Bar/Ring/Trend/Adherence components.

---

## Open Questions (resolved)
- Body part granularity:一级部位 7类
- Metric:频次为主
- Plan scope:当前激活计划
- Heatmap window:90天 (NzHelper 14周)
- Nav label:统计
