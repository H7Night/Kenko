# AGENTS.md

## Windows 约束
**当前环境是 Windows 10 / pwsh7**
- 默认禁止使用 Bash 语法，除非确定此 shell 处在 Linux 环境
- 不要使用 Bash 引号/转义习惯，在 PowerShell 命令里，复杂正则优先用单引号包裹。
- 如果正则本身同时包含单引号和双引号，优先拆成多个简单 rg 命令。
- 执行多行 Python 禁止使用 Bash heredoc ；改用 PowerShell here-string | python -
- pwsh 中，语句块表达式（如 `foreach`、`if`）不能直接作为管道输入。 需要先使用 `$()` / `@()` 包裹，或先赋值给变量。 普通命令输出可直接进入管道，无需额外包裹。
- PowerShell 使用 `rg` 时，通配目录必须先用 `Get-ChildItem -Filter` 展开为真实路径，禁止直接把含 `*` 的搜索路径传给 `rg`。

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 工作流要求（Workflow Requirements）

- **禁止在 `main` 分支上直接修改代码，也禁止直接在 `main` 分支上提交**。
- 所有代码/文档修改必须遵循以下流程：
  1. 先切换到最新的 `main`（`git checkout main && git pull`）；
  2. 从 `main` 创建 `feat/<描述>` 分支进行修改；
  3. 在 `feat/*` 分支上完成修改并提交；
  4. **merge 时机由用户明确说明**：用户未说明"合并到 main"时，完成修改后保留在 `feat/*` 分支即可，**不要询问**是否合并/推送；用户明确说明可以合并时，再执行 merge 回 `main`（推送同样等待用户指示）。
- **其他未明确事项**（如分支命名、merge 方式、是否创建 PR）默认采用合理惯例，不逐一询问；仅当决策不可逆或涉及远程仓库时先与用户确认。
- **提交粒度**：一个 bug / 一个 feature 对应一个提交，禁止把多个 bug 或多个 feature 合并进同一个提交；不同主题的改动须拆分为多个提交（可用 `git add -p` 按 hunk 拆分）。

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install debug APK to connected device/emulator
./gradlew installDebug

# Run unit tests (JUnit 5 / JUnit Platform)
./gradlew test

# Run instrumented/android tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a single instrumented test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.looker.kenko.RepositoryTest

# Gradle wrapper is checked in — always use ./gradlew, not a system Gradle.
```

**Test structure:**
- Unit tests: use JUnit 5 (JUnit Platform) in `app/src/test/`
- Instrumented tests: in `app/src/androidTest/`, run with a custom `KenkoTestRunner` that uses `HiltTestApplication`
- Room schema files live in `app/schemas/` and are sourced into the `androidTest` assets for migration tests

## Documentation Maintenance（文档维护）

用户要求更新文档时（或功能完成后需要同步文档时），按 `.agents/skills/update-doc.md` 技能规范执行：哪些文件要更新、各文件的格式与位置、提交粒度与验证方式。涉及文件：`CHANGELOG.md`、`README.md`、`README_cn.md`、`AGENTS.md`。`CHANGELOG.md` 的发布版本化（Unreleased → 版本区块）在 Release Process 中进行。

## Release Process（版本发布流程）

- **版本号**：定义于 `app/build.gradle.kts` 的 `versionName`；`versionCode` 由 `versionCodeFor(versionName)` 自动计算（`major*100_000 + minor*1_000 + patch*10`，如 `1.7.0` → `107000`）。
- **发布前本地准备**（等价于 CI `prep_release` workflow 的步骤）：
  1. 更新 `versionName` 为发布版本（如 `1.7.0`）；
  2. 运行 `./gradlew fastlaneChangelog`（依赖 `changelogMD`）：在 `CHANGELOG.md` 的 `## [Unreleased]` 下插入 `## [X.Y.Z] - <date>` 头（原 Unreleased 条目归入该版本），并生成 `metadata/en-US/changelogs/<versionCode>.txt`（Fastlane 更新日志：条目列表，去 `### ` 前缀）；也可手动编辑达到相同效果；
  3. 按需同步更新 `README.md` / `README_cn.md`（发布流程说明、特性等有变化时）。
- **CI 发布链路**（`.github/workflows/`）：
  - `prep_release.yml`（手动触发，输入版本号）：更新 `versionName` → 跑 `fastlaneChangelog` → 创建 `release/<version>` 分支的 PR；
  - `release.yml`（`release/*` 分支 PR merge 到 `main` 后自动触发）：构建并签名 release APK/AAB → 创建 GitHub Release（draft，正文取自 `scripts/changelog.sh <version>`）→ 发布 Play Store。
- **发布提交**：本地准备完成后提交（提交信息风格 `chore: prepare release X.Y.Z`），按工作流要求保留在 `feat/*` 分支或按用户指示合并。
- `scripts/changelog.sh <version>` 输出 `CHANGELOG.md` 中指定版本（或 `Unreleased`）的区块内容。

## Architecture

Kenko is an Android workout journal app. It uses **Jetpack Compose** with **Hilt** for DI, **Room** for local persistence, and **Navigation Compose** with type-safe `@Serializable` routes.

### Layered structure

```
UI (Compose screens + ViewModels)
  └─ data/repository/ (interfaces: SessionRepo, PlanRepo, ExerciseRepo, etc.)
       └─ data/repository/local/ (impls backed by Room DAOs)
            └─ data/local/dao/ (Room DAO interfaces)
                 └─ data/local/model/ (Room @Entity classes)
```

The `data/model/` package holds the **domain/external** models (e.g. `Session`, `Exercise`, `Plan`, `Set`) that are exposed through repository interfaces and consumed by ViewModels. Room entities live separately in `data/local/model/` and are mapped to external models via `toEntity()` / `toExternal()` extension functions.

### DI modules (`di/`)

- `DatabaseModule` — provides `KenkoDatabase` singleton and individual DAOs
- `AppModule` — provides `CoroutineDispatcher` qualifiers (`@IoDispatcher`, `@DefaultDispatcher`) and an `@ApplicationScope` `CoroutineScope`
- `RepositoryModule` — binds repository interfaces to their `Local*` implementations
- `DatastoreModule` — provides `DataStore<Preferences>` for settings
- `BackupModule` / `HandlersModule` — backup-related bindings

### Navigation pattern

Each screen has its own package under `ui/<screen>/` with:
- The composable screen file(s)
- A `navigation/` subpackage containing a `@Serializable` route object (or data class) and extension functions on `NavController` (for `navigateTo*`) and `NavGraphBuilder` (for the `composable` destination)

Example for the "home" screen:
- `ui/home/Home.kt` — the composable
- `ui/home/HomeViewModel.kt` — the ViewModel
- `ui/home/navigation/HomeNavigation.kt` — `HomeRoute`, `NavController.navigateToHome()`, `NavGraphBuilder.home()`

The top-level nav graph is assembled in `ui/navigation/KenkoNavHost.kt`. Bottom-bar navigation is managed in `MainActivity.kt`.

### Key patterns

- **`asStateFlow()` extension** (`utils/ViewModel.kt`): a `context(viewModel)` extension on `Flow<T>` that calls `stateIn()` with `WhileSubscribed(5000)`. Used throughout ViewModels to convert flows to `StateFlow`.
- **Settings**: stored in `DataStore<Preferences>` via `SettingsRepo` (interface) / `DatastoreSettingsRepo` (impl). Theme, language, color palette, backup URI, etc. use typed keys in `data/model/settings/`.
- **Kotlin context parameters**: enabled via `-Xcontext-parameters` compiler flag. `asStateFlow()` is a context-parameter function.
- **ViewModel base** (`ui/base/KenkoViewModel.kt`): feature ViewModels extend `KenkoViewModel`, which owns the `snackbar` `SharedFlow` and a `launchCatching { }` helper (try/catch → snackbar emit). Do not declare a per-ViewModel `_snackbar`; use `launchCatching` (or `emitSnackbar`) instead. ViewModels already exposing `SnackbarHostState` (e.g. Plan, Exercises) keep using `snackbarState`.
- **`kotlinx.datetime.LocalDate`** is the canonical date type throughout the codebase, stored as epoch days in Room.
- **Room database** is pre-populated from `app/src/main/assets/kenko.db` and uses incremental migrations (currently at version 12). When changing `@Entity` classes, bump the version in `KenkoDatabase.kt`, add a migration in `data/local/Migrations.kt` that matches the generated schema (`app/schemas/…/NN.json` is exported on compile), and add a `schemaMigrationXToY` test in `androidTest/.../RoomDatabaseTesting.kt`.
- **Destructive actions** (deleting exercises, plans, sets, weight records, sessions) use an explicit `DeleteIconButton` (`ui/component/DeleteIconButton.kt`) + `ConfirmDialog` (in `ui/component/ConfirmDialog.kt`) — never swipe-to-delete. Show feedback via `Context.toast()` (`utils/Toast.kt`) with `R.string.label_deleted`. Deleting a set from Home/Session detail goes through the shared `SetDeleteDialog`.
- **Shared UI components**: `ConfirmDialog` (confirmation dialogs), `SetDeleteDialog` (set deletion confirm + toast), `DeleteIconButton`, `SelectionDialog` (single-choice dialogs), `KenkoDatePickerDialog`, `EmptyState` (empty list placeholder), `Toast.kt` (`Context.toast` helper). All user-facing strings go through `res/values/strings.xml` + `values-zh/` — no hardcoded text in composables.
- **Backup** has two modes: DB backup (ZIP, restore-capable, WorkManager-scheduled) and JSON export (selective data with date range). The backup UI lives in `ui/feature/backup/BackupScreen.kt` + `ui/feature/settings/BackupSection.kt`.
- **Statistics page** (`ui/feature/statistics/`): bottom-nav "Statistics" tab. 90-day heatmap (`HeatmapCard.kt`, `WeeksToShow=13`) + body-part frequency bars (`BodyPartBarCard.kt`, weekly/monthly/plan) + balance ring (`BalanceRingCard.kt`) + 12-week trend (`TrendCard.kt`) + adherence (`AdherenceCard.kt`). Aggregation is pure in `domain/statistics/Aggregation.kt` (`aggregateByBodyPart`, `aggregateCardioMinutes`, `buildTagDict`, `bodyPartByName`, `CARDIO_PART`). Body-part display order is **derived from DB top-level tags** (`parentId==null`, sorted by `sortOrder`, cardio last) in `StatisticsViewModel.buildBodyParts` — never hardcode part names. `buildTagDict` returns `Pair<String?, CountType>`: resolve parent via `parentId` against the full tag list (`Tag.parentName` is never populated by `TagMapper`); `parentName == CARDIO_PART` → cardio, null/unknown → excluded (no part ≠ cardio). Balance ring covers **six strength parts only**; cardio is a separate "有氧 X 分钟" row. Cardio minutes come from `CountType.MINUTES`, strength from session counts.
- **Fixed typography** (`ui/theme/Type.kt`): a single tuned `Typography` (10–28sp gradient), no user font-size setting and no `Typography.scaled()`. `sp` naturally follows the system accessibility font scale. `KenkoTheme(theme, content)` takes no fontSize param.
- **Home timer** (`ui/component/timer/TimerManager.kt` + `TimerCard.kt`): `TimerState` IDLE/RUNNING/PAUSED with a singleton `TimerManager`; `stop()` resets to 0. `TimerCard` exposes `onReset` (Replay button, shown while active or with accumulated time) wired to a confirm dialog in `Home.kt` → `HomeViewModel.resetTimer()`.
- **Bodyweight display**: the "自重" label must NOT use the mono `.numbers()` font (it has no CJK glyphs → vertical wrap). `SetItem.kt` renders bodyweight as read-only `labelMedium` text; `AddSet.kt` bodyweight placeholder uses `width(72.dp)` + `maxLines=1`. A bodyweight exercise has no numeric weight editor.
- **Profile weight chart** (`ui/feature/profile/`): no plan filter. `computeWeightChartView(weights, selectedMonth, customRange)` — month paging by default; when a `customRange: Pair<LocalDate,LocalDate>?` is set, shows all records in range (no month paging, `currentMonth=null`). Range picked via `WeightRangeDialog` (Material3 `DatePickerDialog`).
- **Plan import/export** (`data/plan/`): `PlanTransferCodec` (JSON DTOs in `domain/model/PlanTransferData.kt`), `PlanImportApplier` (creates plans, allows duplicate names, auto-creates missing exercises via `ExerciseRepo.getOrCreate`, imports inactive), and `PlanTransferManager` (SAF read/write, file IO on `Dispatchers.IO`). UI entry: `⋮` menu on the plan list page (`ui/feature/plan/Plan.kt`); dialogs in `ui/feature/plan/PlanTransferDialogs.kt`. Import/export feedback uses `snackbarState` (never the un-consumed `snackbar` SharedFlow inherited from `KenkoViewModel`).
- **Build scripts** in `scripts/` mirror the three Gradle tasks (`build_debug`, `install_debug`, `build_and_install`) for Windows (`.bat`/`.ps1`) and Unix (`.sh`); all scripts are non-interactive (no "Press Enter to exit" pause). `scripts/changelog.sh <version>` prints a version's section from `CHANGELOG.md`. Manual SQL migration/seed scripts live in `scripts/sql/`. All project scripts and SQL files live under `scripts/`.

### Key dependencies

- **Compose BOM** `2025.11.00` with Material 3 Expressive (`1.5.0-alpha08`)
- **Hilt** `2.57.2` for DI, with `hilt-navigation-compose` for ViewModel injection into composables
- **Room** `2.8.3` with KSP for compile-time codegen
- **Kotlin** `2.2.21` / KSP `2.2.21-2.0.4`
- **WorkManager** with Hilt integration for scheduled backups
- **kotlinx-datetime** and **kotlinx-serialization** for date handling and type-safe navigation routes
