# 动画流畅化与加载空白消除实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 核心常驻页数据流改为 `SharingStarted.Eagerly` 预热以消除加载空白闪现，并为 `NavHost` 恢复 220ms 淡入过渡（退出瞬时移除）。

**Architecture:** 复用 `utils/ViewModel.kt` 的 `asStateFlow(initial, started = …)` 扩展与 `stateIn` 的 `started` 参数；`NavHost` 过渡改为 `fadeIn(tween(220))` + `ExitTransition.None`。

**Tech Stack:** Kotlin、Jetpack Compose、Room/Flow、Navigation Compose。

## Global Constraints

- 只改核心常驻页（Home / Sessions / Exercises / Plans / Profile），不改 `asStateFlow` 全局默认值。
- `exitTransition` / `popExitTransition` 必须保持 `ExitTransition.None`（规避 pop 点击穿透）。
- 不改 SessionDetail / Performance 的 Loading 态。
- 新 import 需按字母序插入。
- 构建/测试命令 Windows 环境：`.\gradlew.bat`。

---

### Task 1: 页面切换过渡

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/navigation/KenkoNavHost.kt:18-19,72-75`

- [ ] **Step 1: 改 import**

在 `import androidx.compose.animation.EnterTransition` 后加：

```kotlin
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
```

- [ ] **Step 2: 改过渡**

把 `NavHost(...)` 的四个过渡参数改为：

```kotlin
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { ExitTransition.None },
```

- [ ] **Step 3: 编译验证** `.\gradlew.bat :app:compileDebugKotlin`

- [ ] **Step 4: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/navigation/KenkoNavHost.kt
git commit -m "feat: restore short fade transition between screens"
```

---

### Task 2: 核心页 Eagerly 预热

**Files:**
- Modify: `ui/feature/home/HomeViewModel.kt`
- Modify: `ui/feature/session/SessionsViewModel.kt`
- Modify: `ui/feature/exercise/ExercisesViewModel.kt`
- Modify: `ui/feature/plan/PlanViewModel.kt`
- Modify: `ui/feature/profile/ProfileViewModel.kt`

- [ ] **Step 1: HomeViewModel**

把 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), …)` 中所有 `WhileSubscribed(5000)` 改为 `Eagerly`（`planName`、`allExercises`、`availablePlanDays`、`planDayTitles`、`previousSessionDate`、`planExercises`、`sessionSets`）。`state` 的 `.asStateFlow(HomeUiData(...))` 改为 `.asStateFlow(HomeUiData(...), started = SharingStarted.Eagerly)`。

- [ ] **Step 2: SessionsViewModel**

`planDayExerciseNames` 的 `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())` → `SharingStarted.Eagerly`；`state` 的 `.asStateFlow(SessionsUiData(emptyList(), false))` → `.asStateFlow(SessionsUiData(emptyList(), false), started = SharingStarted.Eagerly)`。

- [ ] **Step 3: ExercisesViewModel**

加 `import kotlinx.coroutines.flow.SharingStarted`；`parentTags`、`allTags`、`exercises` 三处 `.asStateFlow(emptyList())` → `.asStateFlow(emptyList(), started = SharingStarted.Eagerly)`。

- [ ] **Step 4: PlanViewModel**

加 `import kotlinx.coroutines.flow.SharingStarted`；`repo.plans.asStateFlow(emptyList())` → `repo.plans.asStateFlow(emptyList(), started = SharingStarted.Eagerly)`。

- [ ] **Step 5: ProfileViewModel**

加 `import kotlinx.coroutines.flow.SharingStarted`；`plans` 的 `.asStateFlow(emptyList())` 与 `state` 的 `.asStateFlow(ProfileUiState())` 补 `started = SharingStarted.Eagerly`。

- [ ] **Step 6: 编译验证** `.\gradlew.bat :app:compileDebugKotlin`

- [ ] **Step 7: 提交**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/home/HomeViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/session/SessionsViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/exercise/ExercisesViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/plan/PlanViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModel.kt
git commit -m "perf: eager-load core screens to avoid blank flash"
```

---

## 收尾验证

- [ ] 全量单测：`.\gradlew.bat :app:testDebugUnitTest`
- [ ] Lint：`.\gradlew.bat :app:lintDebug`
- [ ] 手动冒烟：冷启动 Home 无空态闪现；反复切底部导航无「暂无数据」闪现；进入/返回有淡入；深色主题淡入无露白。

## Self-Review 记录

- Spec §3.1 → Task 2；§3.2 → Task 1；§3.3（局部动效）为可选项，无明确生硬处则不改。
- 无占位符；类型/参数名与 spec 一致（`started = SharingStarted.Eagerly`、`fadeIn(tween(220))`）。
