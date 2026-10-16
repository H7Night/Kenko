# Kenko — Body Weight Toggle, Health Connect, Theme Presets

Date: 2026-10-13
Status: Draft (awaiting review)
Scope: `:app` only (independent from the hackqing / MihealthReader work; no shared
framework is extracted — code that must be reused across the three apps is copied
and adapted per app).

## 1. Goal

Three self-contained improvements to Kenko, isolated from the other two apps:

1. **Body Weight module toggle** — let the user turn the body-weight feature off so
   its UI disappears, without deleting data.
2. **Health Connect write** — publish every training session to Health Connect as an
   `ExerciseSessionRecord` (auto + backfill; write-only).
3. **Theme presets** — keep the current look as a preset (`Line`) and add a Google
   Material 3 preset, selectable alongside light/dark/system.

Non-goals: no Health Connect reads; no version/dependency migration (Kenko already
runs the target stack); no changes to hackqing or MihealthReader in this document.

## 2. Body Weight module toggle

### Behaviour

- New setting: `bodyWeightEnabled: Boolean`, **default `true`**.
- When **false**:
  - Profile screen hides the weight card (`WeightCard`, `Profile.kt:227`, `:305`),
    the add-weight entry, the `WeightRangeDialog`, and the `WeightHistorySheet`
    entry (`Profile.kt:163-171`, `:474`).
  - Weight records are **retained** in the DB; nothing is deleted.
  - Bodyweight-exercise load calculation **keeps using the last recorded weight**
    (`ExerciseProgress.kt:85` `bodyWeightOnOrBefore`), so historical 1RM is unchanged.
    The toggle only hides recording and viewing, it does not alter math.
- When **true**: current behaviour is unchanged.

### Settings UI

The user asked for the switch under a **System** group. Settings currently has
groups `appearance` / `data` / about (`Settings.kt:149-195`). Add a new
`SettingsGroup(title = R.string.label_settings_system)` containing a row
**Body Weight** with a trailing `Switch`.

- Add a switch-capable row (e.g. `SwitchSettingsRow`) to `ui/component/`, or extend
  `SettingsRow` with an optional trailing slot. Prefer the smallest change that
  keeps existing rows untouched.
- New strings (en + zh): `label_settings_system`, `label_body_weight_toggle`,
  and a subtitle describing the effect.

### Data plumbing

| Layer | Change |
|---|---|
| `domain/model/settings/Settings.kt` | add `bodyWeightEnabled: Boolean` |
| `data/repository/SettingsRepo.kt` | add `suspend fun setBodyWeightEnabled(enabled: Boolean)` |
| `data/local/datastore/DatastoreSettingsRepo.kt` | key `body_weight_enabled` (boolean), map + setter; default `true` when absent |
| `SettingsViewModel` / `SettingsUiData` | expose + update the flag |
| `ProfileViewModel` / Profile state | expose `bodyWeightEnabled` to gate the card |

## 3. Health Connect — write ExerciseSession

### Model

Each Kenko `Session` (with its sets, `durationSeconds`, `startedAt`, `endedAt`,
resolved exercise names, plan-day title) maps to one
`ExerciseSessionRecord`:

- `metadata`: `Metadata.manualEntry()` with a stable `clientRecordId =
  "kenko:session:<sessionId>"` (idempotent re-write).
- `startTime` / `endTime`: from `startedAt` / `endedAt` epoch millis; if either is
  null (legacy sessions), skip that session during backfill and log it.
- `exerciseType`: `ExerciseSessionType.STRENGTH_TRAINING` by default. Pure-cardio
  sessions (all sets' exercises are `CountType.MINUTES`) → `ExerciseSessionType.STRENGTH_TRAINING`
  is still used in v1 (HC requires one type); document as a known limitation.
- `title`: plan-day title if present, else the exercise names joined (truncated).
- v1 writes session-level records only; `ExerciseSegment` / laps / route / calories
  are out of scope.

### Timing and idempotency

- Auto-write after a session is completed and its duration persisted:
  `TrainingSessionManager.endTraining()` (`TrainingSessionManager.kt:64-100`), after
  `updateSessionEnded` + `updateSessionDuration`.
- Backfill: on the first successful permission grant and on demand from a
  "Sync now" action, iterate all sessions (newest first) and write any missing ones.
- Writes are best-effort and must never block or fail the training flow: run in an
  application scope, swallow + log HC exceptions, surface only a summary via snackbar
  on manual sync.

### Permissions & manifest

- Dependency: `androidx.health.connect:connect-client` (add to
  `gradle/libs.versions.toml` + `app/build.gradle.kts`).
- Manifest:
  - `<uses-permission android:name="android.permission.health.WRITE_EXERCISE" />`
  - privacy-policy / rationale handling and, for Android 14+, the
    `<activity-alias android:name="ViewPermissionUsageActivity">` targeting
    `android.intent.action.VIEW_PERMISSION_USAGE` with category
    `android.intent.category.HEALTH_PERMISSIONS` (same pattern as hackqing's
    `PermissionsRationaleActivity`).
- Settings → System: add a **Health Connect** row (switch or button) gated behind
  permission request. `HealthConnectManager.permissions` = `{ WRITE_EXERCISE }`.

### New code

- `data/health/HealthConnectManager.kt` (constructor-injected, `@Singleton`):
  `available()`, `hasPermissions()`, `requestPermissions(...)`,
  `write(session)`, `syncAll(sessions)`.
- Pure mapping function `Session.toExerciseSessionRecord()` (separated so it is unit
  testable without the HC runtime).
- A thin `HealthConnectViewModel`/settings entry for permission + manual sync.

## 4. Theme presets

### Setting

- New enum `domain/model/settings/ColorPalette.kt`:
  `enum class ColorPalette { Line, Material }` (extensible; `Line` = current).
- New `Settings` field `colorPalette: ColorPalette` (default `Line`) + DataStore key
  `color_palette` + `SettingsRepo.setColorPalette(...)`.
- Settings → Appearance: add a **Color style** row opening a `SelectionDialog`
  (reuse the existing theme dialog pattern, `Settings.kt:111-123`).

### Theme wiring

`ui/theme/Theme.kt` currently picks one hardcoded scheme pair
(`CatppuccinLatte/Mocha`, misnamed — actually Linear/Zinc). Change `KenkoTheme` to
take `(theme: Theme, palette: ColorPalette)` and select:

- `Line` → existing `CatppuccinLatte/Mocha` schemes (rename to `LineLight/LineDark`
  while touching them; keep values identical).
- `Material` → Google Material 3 baseline: dynamic color (`dynamicDark/LightColorScheme`)
  on Android 12+, else a static baseline M3 palette pair.

`MainActivity` reads `colorPalette` from settings and passes it in.

## 5. Testing

- **Unit**: `Session.toExerciseSessionRecord()` mapping (start/end/title/type,
  null-timestamp skip); `bodyWeightEnabled` default + round-trip through the settings
  mapper; palette selection logic.
- **Instrumented / Robolectric** where the existing harness already runs.
- **Manual**: toggle off → weight UI gone, data intact, 1RM for bodyweight exercises
  unchanged; HC permission grant → backfill visible in the Health Connect app; theme
  preset switch applies immediately.

## 6. Compatibility & migration

- DataStore: new keys read with defaults (`body_weight_enabled=true`,
  `color_palette=Line`) — no migration needed; absent keys must not crash.
- No Room schema change.
- No changes to versioning; this rides the current line.

## 7. Risks

- **HC permission/rationale**: Android 14 rationale intent is easy to get wrong
  (hackqing hit this); copy its working manifest shape.
- **Legacy sessions without timestamps** cannot produce a valid interval record —
  skipped, not corrupted.
- **Theme refactor** touches every screen; keep `Line` values byte-for-byte identical
  so the default look does not shift.

## 8. Out of scope

- Health Connect reads / cross-app display.
- Exercise segments, calories, routes.
- hackqing / MihealthReader work (separate specs).
