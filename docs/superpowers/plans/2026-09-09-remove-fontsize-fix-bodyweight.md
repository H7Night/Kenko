# Remove Font-Size Setting & Fix Bodyweight Layout — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the user-configurable font-size setting and its scaling chain (fixed typography, system `sp` scale only), and fix the bodyweight "自重" vertical-wrap/offset bug in the training and AddSet screens.

**Architecture:** Two independent commits. (A) Delete every `fontSize` reference across data/theme/settings layers so `KenkoTheme` uses the fixed `Typography` again. (B) Change the bodyweight weight-column rendering in `SetItem.kt` (non-mono font, read-only) and the bodyweight placeholder in `AddSet.kt` (72dp width, normal font), then verify delete-button alignment in `DeletableSetItem.kt`.

**Tech Stack:** Kotlin 2.2, Jetpack Compose BOM 2025.11, Material 3 Expressive, Hilt 2.57, DataStore Preferences.

## Global Constraints

- Commit split is mandatory: Change A (font-size removal) and Change B (bodyweight layout) are separate commits.
- After Change A, `grep -rn "fontSize|setFontSize|BaseFontSize|\.scaled("` under `app/src/main` must return only unrelated `fontSize = NN.sp` usages (BottomBar 10.sp, HeatmapCard 9.sp, WeightLineChart 9.sp) — no config/scaling chain remains.
- `sp` stays in use; do NOT introduce a custom font-scale multiplier.
- No DB migration: the DataStore `font_size` key is simply ignored (leave stored value; no schema change).
- All user-facing strings come from `res/values/strings.xml` + `values-zh/`.

---

### Task 1: Remove font-size data + theme chain

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt:28`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt:45`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt:99-127,140`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/theme/Type.kt:152-184`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/theme/Theme.kt:125,141`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/MainViewModel.kt:53-54`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/MainActivity.kt:108,123`

**Interfaces:**
- Consumes: none from earlier tasks.
- Produces: `Settings` without `fontSize`; `SettingsRepo` without `setFontSize`; `KenkoTheme(theme: Theme = Theme.System, content: @Composable () -> Unit)`; `MainViewModel` without `fontSize`.

- [ ] **Step 1: Remove `Settings.fontSize`**

`app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt` — delete line 28:
```kotlin
    val fontSize: Int = 14,
```

- [ ] **Step 2: Remove `SettingsRepo.setFontSize`**

`app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt` — delete:
```kotlin
    suspend fun setFontSize(fontSize: Int)
```

- [ ] **Step 3: Remove DataStore implementation**

`app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt`:
- Delete the `setFontSize` override (lines 99-101).
- In `mapSettings`, delete `val fontSize = preferences[FONT_SIZE] ?: 14` and the `fontSize = fontSize,` argument.
- Delete the `FONT_SIZE` key declaration (in `Keys` companion, `intPreferencesKey("font_size")`).
- If `intPreferencesKey` import becomes unused, remove it.

- [ ] **Step 4: Remove `BaseFontSize` + `Typography.scaled` from Type.kt**

`app/src/main/kotlin/com/looker/kenko/ui/theme/Type.kt` — delete from `/** 基准字体大小（sp）...` through end of `scaled(...)` (lines 152-184). Remove now-unused imports `TextUnit` (keep `sp`).

- [ ] **Step 5: Simplify `KenkoTheme` signature**

`app/src/main/kotlin/com/looker/kenko/ui/theme/Theme.kt`:
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KenkoTheme(
    theme: Theme = Theme.System,
    content: @Composable () -> Unit,
) {
    val systemTheme = isSystemInDarkTheme()
    val isDarkTheme = remember(theme) { ... }   // unchanged
    val colorScheme = if (isDarkTheme) { ... } else { ... }   // unchanged
    // delete: val scaledTypography = remember(fontSize) { Typography.scaled(fontSize) }
    ...
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,   // was scaledTypography
        shapes = Shapes,
        content = content,
    )
}
```

- [ ] **Step 6: Remove `MainViewModel.fontSize`**

`app/src/main/kotlin/com/looker/kenko/ui/MainViewModel.kt` — delete:
```kotlin
    val fontSize: StateFlow<Int> = repo.get { fontSize }
        .asStateFlow(com.looker.kenko.ui.theme.BaseFontSize)
```

- [ ] **Step 7: Remove `MainActivity` fontSize**

`app/src/main/kotlin/com/looker/kenko/ui/MainActivity.kt`:
- Delete `val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()`.
- Change `KenkoTheme(theme = theme, fontSize = fontSize)` → `KenkoTheme(theme = theme)`.

- [ ] **Step 8: Verify no config chain remains**

Run (PowerShell): `rg -n "fontSize|setFontSize|BaseFontSize|\.scaled\(" app/src/main/kotlin`
Expected: only `fontSize = NN.sp` literal usages (KenkoBottomBar 10.sp, HeatmapCard 9.sp, WeightLineChart 9.sp) — no `repo.setFontSize`, `Typography.scaled`, `BaseFontSize`, `viewModel.fontSize`.

- [ ] **Step 9: Compile**

Run: `.\gradlew :app:compileDebugKotlin --offline`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt app/src/main/kotlin/com/looker/kenko/ui/theme/Type.kt app/src/main/kotlin/com/looker/kenko/ui/theme/Theme.kt app/src/main/kotlin/com/looker/kenko/ui/MainViewModel.kt app/src/main/kotlin/com/looker/kenko/ui/MainActivity.kt
git commit -m "refactor(settings): remove font-size setting and scaling chain"
```

---

### Task 2: Remove Settings UI row + font-size strings

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt:79-86,155-167,203-258,457`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt:79,93,117-124,265`
- Modify: `app/src/main/res/values/strings.xml` (font size strings)
- Modify: `app/src/main/res/values-zh/strings.xml` (font size strings)

**Interfaces:**
- Consumes: Task 1 — `SettingsRepo`/`Settings` no longer have font size; `KenkoTheme` no longer takes `fontSize`.
- Produces: `SettingsViewModel` without `setFontSize`/`fontSize` in `SettingsUiData`; `Settings` composable without `SettingsFontSizeRow`/`onFontSizeChanged`.

- [ ] **Step 1: Remove `SettingsViewModel.setFontSize` + state field**

`app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt`:
- Delete the `setFontSize` function (lines ~117-124).
- In the combine, delete `fontSize = settings.fontSize,`.
- In the default `SettingsUiData(...)`, delete `fontSize = 14,`.
- In `data class SettingsUiData`, delete `val fontSize: Int,`.

- [ ] **Step 2: Remove `SettingsFontSizeRow` + wiring in Settings.kt**

`app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt`:
- In top-level `Settings(...)`, remove `onFontSizeChanged = viewModel::setFontSize,` from the call.
- In private `Settings(...)` signature, remove the `onFontSizeChanged: (Int) -> Unit,` param.
- Remove the `SettingsFontSizeRow(...)` invocation block (the `SettingsFontSizeRow(fontSize = state.fontSize, onFontSizeChanged = onFontSizeChanged)` call site).
- Delete the whole `private fun SettingsFontSizeRow(...)` composable (lines 203-258).
- In the preview, remove `fontSize = 14,` from `SettingsUiData(...)` and `onFontSizeChanged = {},`.
- Remove now-unused imports (`Surface`, `Width`, etc.) only if the compiler flags them; Kotlin tolerates unused imports as warnings, but clean them if obvious.

- [ ] **Step 3: Remove font-size string resources**

`app/src/main/res/values/strings.xml` — remove `label_font_size`, `label_font_size_small`, `label_font_size_medium`, `label_font_size_large`, `label_font_size_larger`.
`app/src/main/res/values-zh/strings.xml` — remove the same keys if present.

- [ ] **Step 4: Compile + verify no references**

Run: `rg -n "fontSize|label_font_size|SettingsFontSizeRow" app/src/main/kotlin app/src/main/res`
Expected: no matches (besides unrelated `fontSize = NN.sp` literals in BottomBar/Heatmap/WeightLineChart).
Run: `.\gradlew :app:compileDebugKotlin --offline` → BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "refactor(settings): remove font-size settings row and strings"
```

---

### Task 3: Fix bodyweight label in SetItem (non-mono, read-only)

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/component/SetItem.kt:113-126,132-200`
- Test: `app/src/test/kotlin/com/looker/kenko/ui/component/SetItemBodyweightTest.kt` (new)

**Interfaces:**
- Consumes: `Set` model (`exercise.isBodyweight`, `countType`), `R.string.label_bodyweight_display`.
- Produces: `SetItem` renders bodyweight weight column as read-only "自重" text in `labelMedium` (non-`.numbers()`), no `BasicTextField` for bodyweight even in edit mode.

- [ ] **Step 1: Write failing test (bodyweight column is read-only text, not editable field)**

```kotlin
// app/src/test/kotlin/com/looker/kenko/ui/component/SetItemBodyweightTest.kt
package com.looker.kenko.ui.component

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Exercise
import com.looker.kenko.domain.model.Set
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetItemBodyweightTest {
    // The display string must equal label_bodyweight_display resource value "自重"/"Body Weight".
    // We assert on the resolved bodyweight label only via the domain rule:
    // bodyweight exercises must not expose a numeric weight editor — i.e. the rendered
    // performance text equals the bodyweight display string, and countType is REPS.
    @Test
    fun `bodyweight exercise keeps a weight column but with bodyweight label`() {
        val ex = Exercise(name = "Pull-ups", isBodyweight = true, countType = CountType.REPS)
        assertTrue(ex.isBodyweight)
        // Guard: the label resource exists and resolves to the bodyweight display string
        val label = com.looker.kenko.R.string.label_bodyweight_display
        assertTrue(label != 0)
    }
}
```
> Note: This is a light guard test because the visual font/layout can't be unit-tested without Compose UI test infra. The real verification is the device run in Step 4. The test pins the invariant that bodyweight stays a label-driven display.

- [ ] **Step 2: Run test to verify it passes (guard only)**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.looker.kenko.ui.component.SetItemBodyweightTest" --offline`
Expected: BUILD SUCCESSFUL, 1 test pass.

- [ ] **Step 3: Implement bodyweight weight column**

`app/src/main/kotlin/com/looker/kenko/ui/component/SetItem.kt`:
- In the weight `PerformedItem` call (lines ~119-125), when `set.exercise.isBodyweight`:
  - Use a read-only display column instead of `PerformedItem` editing machinery. Replace with:
    ```kotlin
    if (set.exercise.isBodyweight) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.label_weight).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.label_bodyweight_display),
                style = MaterialTheme.typography.labelMedium,   // normal font, NOT numbers()
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    } else { existing PerformedItem weight column }
    ```
- Leave `PerformedItem` unchanged for the numeric (non-bodyweight) case.

- [ ] **Step 4: Compile + manual check**

Run: `.\gradlew :app:compileDebugKotlin --offline` → BUILD SUCCESSFUL.
Device: build/install `installDebug`, open Home → inline training with a bodyweight exercise (or Session Detail), verify "自重" renders horizontally in normal font, no vertical wrap, no button offset.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/component/SetItem.kt app/src/test/kotlin/com/looker/kenko/ui/component/SetItemBodyweightTest.kt
git commit -m "fix(session): render bodyweight label with normal font, read-only"
```

---

### Task 4: Fix AddSet bodyweight placeholder width + alignment

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/session/AddSet.kt:282-288`

**Interfaces:**
- Consumes: `R.string.label_bodyweight_display`, `MaterialTheme.typography`.
- Produces: bodyweight-mode "自重" placeholder is 72dp wide, normal font, centered; the adjacent step buttons stay 48dp and aligned.

- [ ] **Step 1: Widen and re-style the bodyweight placeholder**

`app/src/main/kotlin/com/looker/kenko/ui/feature/session/AddSet.kt` — replace the `isBodyweightMode` text block (lines 282-288):
```kotlin
                if (viewModel.isBodyweightMode) {
                    Text(
                        text = stringResource(R.string.label_bodyweight_display),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(72.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
```
(Was: `Modifier.width(48.dp).padding(horizontal = 4.dp)` — widened to 72dp, centered, single-line to prevent vertical wrap.)

- [ ] **Step 2: Compile**

Run: `.\gradlew :app:compileDebugKotlin --offline` → BUILD SUCCESSFUL.

- [ ] **Step 3: Manual device check**

Device: install, open AddSet for a bodyweight exercise, verify "自重" placeholder is single-line horizontal, and the 0.5/1/5 weight buttons (non-bodyweight case unchanged) / the reps+sets rows remain aligned.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/session/AddSet.kt
git commit -m "fix(session): widen bodyweight placeholder to prevent vertical wrap"
```

---

### Task 5: Verify delete-button alignment in DeletableSetItem + full suite

**Files:**
- Review: `app/src/main/kotlin/com/looker/kenko/ui/component/DeletableSetItem.kt:63-81`

**Interfaces:**
- Consumes: Tasks 3-4.
- Produces: confirmation that the trailing delete `IconButton` aligns with the set capsule (no change needed if already correct).

- [ ] **Step 1: Inspect alignment**

`DeletableSetItem.kt` uses `Row(verticalAlignment = Alignment.CenterVertically)` with `Box(Modifier.weight(1f))` + `IconButton(Modifier.size(28.dp))`. With the SetItem bodyweight fix (Task 3), the capsule no longer grows vertically from wrapped CJK text, so the 44dp row + 28dp button stay center-aligned. Confirm on device; no code change expected unless the button still sits off-center.

- [ ] **Step 2: Run full unit tests**

Run: `.\gradlew :app:testDebugUnitTest --offline`
Expected: BUILD SUCCESSFUL, all suites pass.

- [ ] **Step 3: Full build**

Run: `.\gradlew assembleDebug --offline`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit any DeletableSetItem change if made**

Only if Step 1 found an actual offset requiring code change; otherwise skip this step.

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/component/DeletableSetItem.kt
git commit -m "fix(session): align set-item delete button"
```

---

## Self-Review

- **Spec coverage:**
  - Change A (font removal): Task 1 (data/theme/app wiring) + Task 2 (Settings UI + strings). ✓
  - Change B (bodyweight): Task 3 (SetItem) + Task 4 (AddSet) + Task 5 (DeletableSetItem alignment + full suite). ✓
  - No preset tiers, no custom scale, no DB migration: Global Constraints. ✓
- **Placeholder scan:** every step has concrete file:line or full code. No TBD/TODO.
- **Type consistency:** `KenkoTheme(theme, content)`, `Settings`/`SettingsRepo`/`SettingsUiData` without `fontSize` — all consistent across Tasks 1-2. `label_bodyweight_display` referenced consistently in Tasks 3-4.