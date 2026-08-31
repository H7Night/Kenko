# 动画流畅化与加载空白消除设计

日期：2026-08-15
分支：feat/v1.8.0
状态：已批准（用户确认：方案 C——核心页 Eagerly 预热；方案 1——短 fadeIn 过渡 + 退出瞬时移除）

## 1. 目标

1. **消除数据加载时的空白/空态闪现**：核心常驻页（Home / Records / Exercises / Plans / Profile）在数据加载期间不再先渲染空态或空白，而是数据就绪后直接显示内容。
2. **恢复平滑的页面切换过渡**：页面进入/返回有短淡入动画（当前为瞬间跳变），同时规避曾导致导航过渡被关闭的「pop 点击穿透」问题。
3. **轻量微调局部动效**：仅在现有 `tween`/`spring`/`animateItem` 存在明显生硬处时微调，不扩大改动面。

## 2. 现状与根因

- **空白闪现**：`utils/ViewModel.kt` 的 `asStateFlow` 默认 `SharingStarted.WhileSubscribed(5000)` + 空初始值。屏幕首次组合时先渲染空态（如 Records 页先闪「暂无记录」），Room 流首次发射后才替换为内容；且离开页面超过 5s 再回来会重新触发一次闪现。
- **过渡瞬间跳变**：`ui/navigation/KenkoNavHost.kt` 中 `enterTransition` / `exitTransition` / `popEnterTransition` / `popExitTransition` 全部为 `None`（当初为规避 pop 时退出页仍可点击导致点击穿透）。
- 窗口背景已在 `KenkoTheme`（`ui/theme/Theme.kt:146-152`）同步为 `colorScheme.surface`，淡入期间不会露白。

## 3. 方案

### 3.1 核心页 Eagerly 预热（方案 C）

`utils/ViewModel.kt` 的 `asStateFlow` 已暴露 `started` 参数。核心常驻页把流改为 `SharingStarted.Eagerly`，使 Room 流在 ViewModel 创建时即开始采集，屏幕首次组合前数据已就绪，且此后不因订阅变化而重放空态。

- 涉及 ViewModel 及其流：
  - `ui/feature/home/HomeViewModel.kt`：`planName`、`allExercises`、`availablePlanDays`、`planDayTitles`、`previousSessionDate`、`planExercises`、`sessionSets`（显式 `stateIn(..., WhileSubscribed(5000), ...)` 改 `Eagerly`）、`state`（`asStateFlow` 补 `started = SharingStarted.Eagerly`）。
  - `ui/feature/session/SessionsViewModel.kt`：`state`、`planDayExerciseNames`。
  - `ui/feature/exercise/ExercisesViewModel.kt`：三处 `asStateFlow(emptyList())`。
  - `ui/feature/plan/PlanViewModel.kt`：`plans`。
  - `ui/feature/profile/ProfileViewModel.kt`：`asStateFlow(emptyList())` 等。
- `SessionDetail` / `Performance` 已有 `Loading` 态，保持不变。

### 3.2 页面切换过渡（方案 1）

`KenkoNavHost.kt` 过渡改为：

```kotlin
enterTransition = { fadeIn(animationSpec = tween(220)) },
exitTransition = { ExitTransition.None },
popEnterTransition = { fadeIn(animationSpec = tween(220)) },
popExitTransition = { ExitTransition.None },
```

- 进入/返回页 220ms 淡入；退出页瞬时移除 → 平滑且退出页不残留、不复现点击穿透。
- 需补 import：`androidx.compose.animation.fadeIn`、`androidx.compose.animation.core.tween`（`EnterTransition`/`ExitTransition` 已 import）。

### 3.3 局部动效（轻量）

复查现有动画实现（`Wave`、`SwipeToDeleteBox`、`DragState`、`PlanEdit` 的 `AnimatedContent`/`tween(150)`、`PlanName` 的 `animateContentSize`、列表 `animateItem`），仅在发现明显生硬/跳变时微调时长或缓动。若无明显问题则不改。

## 4. 数据流

- **预热**：`stateIn(scope, Eagerly, initial)` / `asStateFlow(initial, started = Eagerly)` → 上游 Room 流在 ViewModel init 即开始采集 → `StateFlow` 在屏幕组合前已持有真实数据 → 首帧即渲染内容（或正确的空态），无中间空态帧。
- **过渡**：导航触发 → 目标页 `fadeIn(220ms)` 淡入；源页 `ExitTransition.None` 瞬时移除 → 窗口背景（`surface` 色）在淡入期间垫底。

## 5. 边界情况

- **点击穿透**：`exitTransition`/`popExitTransition` 保持 `None`，源页瞬时移除，不残留可点击层。
- **淡入露白/黑**：窗口背景已同步 `colorScheme.surface`，淡入期显示 surface 色而非白/黑。
- **Eagerly 资源占用**：核心常驻页的 Room 查询轻量，且这些页本身常驻/高频进入，可接受；非核心页（SessionDetail/Performance 等）保持原状。
- **真·空数据**：预热后若数据确为空，仍正常显示 `EmptyState`（这是正确语义，非「加载中空白」）。

## 6. 测试

- 编译 `:app:compileDebugKotlin` + 全量 `:app:testDebugUnitTest` + `:app:lintDebug` 通过。
- 手动冒烟（真机/模拟器）：冷启动进入 Home 无空态闪现；反复切换 Home / Records / Exercises / Plans / Profile 无「暂无数据」闪现；页面进入/返回有短淡入且返回后不会误触上一页元素；深色主题下淡入无露白。

## 7. 明确不做（YAGNI）

- 不做全局 `asStateFlow` 默认值改为 `Eagerly`（仅核心常驻页，避免影响非核心页）。
- 不新增骨架屏/加载指示（方案 C 依赖预热，不引入额外 UI）。
- 不做复杂的滑动/缩放过渡（保持短淡入即可）。
- 不重构现有动画系统，仅按需微调。

## 8. 影响文件

| 文件 | 变更 |
|---|---|
| `ui/navigation/KenkoNavHost.kt` | 过渡改为短 fadeIn + 退出 None |
| `ui/feature/home/HomeViewModel.kt` | 流改 `Eagerly` |
| `ui/feature/session/SessionsViewModel.kt` | 流改 `Eagerly` |
| `ui/feature/exercise/ExercisesViewModel.kt` | 流改 `Eagerly` |
| `ui/feature/plan/PlanViewModel.kt` | 流改 `Eagerly` |
| `ui/feature/profile/ProfileViewModel.kt` | 流改 `Eagerly` |
| （可选）局部动效微调文件 | 仅在发现生硬时 |
