# Remove Font-Size Setting & Fix Bodyweight Layout — Design

**Date:** 2026-09-09
**Branch:** `feat/v1.9.0`
**Status:** Approved
**Author:** Brainstorming with user

---

## 1. Overview & Goals

**Context:** The app currently exposes a manual font-size setting in Settings (Small 10 / Medium 14 / Large 18) that scales the whole Material typography via `Typography.scaled()`. Scaling every size uniformly produces misalignment / overlap / broken layouts across screens. Meanwhile the training screen renders the bodyweight label "自重" in a Latin monospace font (`.numbers()` = `maplemono_bold`, which has no CJK glyphs) inside a 48dp slot, so the two Chinese characters wrap vertically and push the adjacent buttons out of alignment.

**User intent:**
1. Remove the user-configurable font size entirely (Linear style + NzHelper reference). The app uses one carefully-tuned fixed typography; Android's system font scale (accessibility) already adapts `sp` automatically, so no custom scaling is needed.
2. Fix the bodyweight "自重" vertical-wrap/offset bug in the training/AddSet screens.

**What we will do:**
- Remove the entire font-size data + UI chain: DataStore `font_size` key, `Settings.fontSize` field, `SettingsRepo.setFontSize`, `SettingsViewModel.setFontSize` + `SettingsUiData.fontSize`, `MainViewModel.fontSize`, `KenkoTheme.fontSize` param, `Type.kt` `BaseFontSize` + `Typography.scaled()`, the Settings UI row, and the related string resources.
- Fix bodyweight display in `SetItem.kt` (weight column) and `AddSet.kt` (bodyweight placeholder width/font), and align the trailing delete-button offset in `DeletableSetItem.kt`.

**What we will not do (YAGNI):**
- No preset font-size tiers (Compact/Normal/Large). One fixed typography.
- No changes to the typography gradient itself (10–28sp values stay as-is).
- No behavior change to the system font scale (`sp` already responds to it).

**Success criteria:**
- No `fontSize` references remain in main/test source; app builds and unit tests pass.
- Settings screen no longer shows the font-size row.
- Bodyweight exercise in training inline list + AddSet shows "自重" horizontally in a normal font, no vertical wrap, no button misalignment.
- Android system font scale (large) still works via `sp` (no regression).

---

## 2. Font-Size Removal (Change A)

**Data layer:**
- `domain/model/settings/Settings.kt:28` — remove `val fontSize: Int = 14`.
- `data/repository/SettingsRepo.kt:45` — remove `suspend fun setFontSize(fontSize: Int)`.
- `data/local/datastore/DatastoreSettingsRepo.kt` — remove `setFontSize` override, `FONT_SIZE` key, and the `fontSize = preferences[FONT_SIZE] ?: 14` read / `Settings(..., fontSize = fontSize)`.

**Theme layer:**
- `ui/theme/Type.kt` — remove `BaseFontSize` const, `Typography.scaled(fontSize)` fn, `TextUnit`/`TextStyle` scaled helpers and now-unused imports (`TextUnit`, `toSp`-style arithmetic).
- `ui/theme/Theme.kt` — `KenkoTheme(theme, fontSize=BaseFontSize, content)` → `KenkoTheme(theme, content)`; remove `scaledTypography` and use plain `Typography`.

**App wiring:**
- `ui/MainViewModel.kt` — remove `fontSize: StateFlow<Int>`.
- `ui/MainActivity.kt` — remove `fontSize` collect + `fontSize = fontSize` arg.

**Settings UI:**
- `ui/feature/settings/Settings.kt` — remove `SettingsFontSizeRow`, `onFontSizeChanged` param, `fontSize = state.fontSize` call, and preview references.
- `ui/feature/settings/SettingsViewModel.kt` — remove `setFontSize` fn, `fontSize = settings.fontSize` / default `fontSize = 14` in state, and `SettingsUiData.fontSize` field.

**Strings:**
- `res/values/strings.xml` + `values-zh/strings.xml` — remove `label_font_size`, `label_font_size_small/medium/large`, `label_font_size_larger`.

---

## 3. Bodyweight Layout Fix (Change B)

**Root cause:** "自重" is CJK text rendered with `MaterialTheme.typography.titleMedium.numbers()` where `numbers()` = `FontFamily.Numbers` = `maplemono_bold` (Latin mono, no CJK glyphs). The glyph falls back to a system font, and the 48dp width + mono metrics cause the two characters to wrap vertically; the row/button alignment then shifts.

**`ui/component/SetItem.kt`:**
- Weight column `PerformedItem` when `set.exercise.isBodyweight`:
  - Non-edit display: show `label_bodyweight_display` ("自重") using a normal label style (e.g. `MaterialTheme.typography.labelMedium`) — NOT `.numbers()` mono font.
  - Editing: bodyweight offers no numeric weight input; keep it read-only display (no `BasicTextField`). This matches current behavior intent (bodyweight has no weight), just fixes font+wrap.

**`ui/feature/session/AddSet.kt`:**
- `isBodyweightMode` placeholder Text ("自重") currently `Modifier.width(48.dp).padding(horizontal = 4.dp)` + `titleMedium` → widen to `width(72.dp)` (enough for two CJK chars at titleMedium) and keep normal font; centered in the row so the remaining step buttons keep equal widths and stay aligned.

**`ui/component/DeletableSetItem.kt`:**
- Trailing delete `IconButton` fixed `size(28.dp)` — ensure `Box(weight(1f))` + button align with `Alignment.CenterVertically`; no layout change if already correct, otherwise align the 28dp button with the 44dp capsule. (Reported "right button offset" is largely the SetItem wrap; verify after Change B and adjust if still shifted.)

**Verification of Change B:** exercise tagged `isBodyweight` (e.g. Pull-ups) in Home inline training and in AddSet sheet — "自重" renders horizontally, buttons aligned; non-bodyweight unaffected.

---

## 4. Commit Split

Per repo workflow (one bug / one feature per commit):

1. **`refactor(settings): remove font-size setting and scaling chain`** — Change A (all font-size removal; may touch ~9 files + 2 string files).
2. **`fix(session): render bodyweight label without mono-font wrap`** — Change B (`SetItem.kt`, `AddSet.kt`, `DeletableSetItem.kt`).

---

## 5. Testing & Rollout

- `./gradlew assembleDebug` + `:app:testDebugUnitTest` must pass.
- Manual on device: Settings shows no font-size row; bodyweight exercise in Home inline training and AddSet shows horizontal "自重", buttons aligned; system font scale (Settings → Display → Font size Large) still scales text via `sp`.
- No DB migration: DataStore `font_size` key is simply no longer read (existing stored value ignored).