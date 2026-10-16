# Kenko Body Weight Toggle, Health Connect, Theme Presets — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a body-weight on/off module, publish training sessions to Health Connect, and add a selectable color preset (Line / Material 3), all inside the `:app` module.

**Architecture:** Extend the existing DataStore-backed `SettingsRepo` for the two new toggles; hide the Profile weight card when disabled; add a `data/health` package that maps the pure domain `Session` to a testable intermediate DTO and then to a Health Connect `ExerciseSessionRecord`; refactor `ui/theme/Theme.kt` to resolve a scheme from `(Theme, ColorPalette)`.

**Tech Stack:** Kotlin 2.2.21, Jetpack Compose (BOM 2025.11.00, Material3 1.5.0-alpha08), Hilt 2.57.2, DataStore Preferences 1.1.7, Room 2.8.3, AndroidX Health Connect `connect-client`, JUnit 5 + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-13-kenko-bodyweight-healthconnect-theme-design.md`

## Global Constraints

- Work on branch `feat/bodyweight-healthconnect-theme` (already created, base `v2.0.0`). Never commit to `main`.
- Use `./gradlew` (the checked-in wrapper), never a system Gradle.
- Unit tests use JUnit 5 (`./gradlew test`); instrumented use `./gradlew connectedAndroidTest`.
- All user-facing strings go through `res/values/strings.xml` + `res/values-zh/strings.xml`; never hardcode text in composables.
- One feature per commit; do not mix the three features into one commit.
- Default `bodyWeightEnabled = true`, default `colorPalette = Line`; absent DataStore keys must not crash.
- The `Line` color scheme values must stay byte-for-byte identical to today's `CatppuccinLatte/Mocha` values.
- No Room schema change; no version bump in this plan.

## Review Focus

- **Absent DataStore keys**: a fresh install (or an upgrade) must read `body_weight_enabled` and `color_palette` as enabled/Line, not crash or flip existing behaviour.
- **Legacy sessions without timestamps**: `startedAt`/`endedAt` are nullable on old rows; these must be skipped by Health Connect, never written as a zero-length or reversed interval.
- **Non-zero-length interval**: a session whose end ≤ start must be rejected, not sent to Health Connect.
- **Default look unchanged**: switching no setting must render exactly the current Line palette; the Material preset is opt-in only.
- **Bodyweight 1RM math unchanged**: turning the module off must not change any existing estimated-1RM value.

---

## Task 1: Body weight setting persistence

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/data/local/datastore/FakePreferencesDataStore.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepoTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `Settings.bodyWeightEnabled: Boolean`; `SettingsRepo.setBodyWeightEnabled(enabled: Boolean): Unit`; DataStore key `body_weight_enabled` (boolean, default `true`).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/looker/kenko/data/local/datastore/FakePreferencesDataStore.kt`:

```kotlin
package com.looker.kenko.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state
    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
```

Create `app/src/test/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepoTest.kt`:

```kotlin
package com.looker.kenko.data.local.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatastoreSettingsRepoTest {

    @Test
    fun `body weight defaults to enabled`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        assertTrue(repo.stream.first().bodyWeightEnabled)
    }

    @Test
    fun `setBodyWeightEnabled persists the flag`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        repo.setBodyWeightEnabled(false)
        assertFalse(repo.stream.first().bodyWeightEnabled)
        repo.setBodyWeightEnabled(true)
        assertTrue(repo.stream.first().bodyWeightEnabled)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.looker.kenko.data.local.datastore.DatastoreSettingsRepoTest"`
Expected: FAIL to compile — `bodyWeightEnabled` / `setBodyWeightEnabled` unresolved.

- [ ] **Step 3: Add the field and setter**

In `Settings.kt`, add the field (keep existing order; append before `language` or after — pick one and keep it consistent with `mapSettings`):

```kotlin
data class Settings(
    val isOnboardingDone: Boolean,
    val theme: Theme,
    val lastSetTime: Instant?,
    val backupUri: String?,
    val backupInterval: BackupInterval,
    val lastBackupTime: Instant?,
    val language: Language,
    val bodyWeightEnabled: Boolean,
)
```

In `SettingsRepo.kt`, add to the interface:

```kotlin
suspend fun setBodyWeightEnabled(enabled: Boolean)
```

In `DatastoreSettingsRepo.kt`:
- add imports: `androidx.datastore.preferences.core.booleanPreferencesKey` already present; no new import.
- add setter:

```kotlin
override suspend fun setBodyWeightEnabled(enabled: Boolean) {
    BODY_WEIGHT_ENABLED.update(enabled)
}
```

- in `mapSettings`, read and pass it:

```kotlin
val bodyWeightEnabled = preferences[BODY_WEIGHT_ENABLED] ?: true
return Settings(
    isOnboardingDone = isOnboardingDone,
    theme = Theme.valueOf(theme),
    lastSetTime = lastSetTime?.let { Instant.fromEpochSeconds(it) },
    backupUri = backupUri,
    backupInterval = BackupInterval.valueOf(backupInterval),
    lastBackupTime = lastBackupTime?.let { Instant.fromEpochSeconds(it) },
    language = Language.valueOf(language),
    bodyWeightEnabled = bodyWeightEnabled,
)
```

- add the key in the `Keys` companion:

```kotlin
val BODY_WEIGHT_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("body_weight_enabled")
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.looker.kenko.data.local.datastore.DatastoreSettingsRepoTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt \
        app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt \
        app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt \
        app/src/test/kotlin/com/looker/kenko/data/local/datastore/FakePreferencesDataStore.kt \
        app/src/test/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepoTest.kt
git commit -m "feat(settings): persist body weight module toggle"
```

---

## Task 2: Settings "System" group with Body Weight switch

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/component/SettingsGroup.kt` (add a `SwitchSettingsRow`)
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

**Interfaces:**
- Consumes: `Settings.bodyWeightEnabled`, `SettingsRepo.setBodyWeightEnabled` (Task 1).
- Produces: `SettingsUiData.bodyWeightEnabled: Boolean`; `SettingsViewModel.updateBodyWeightEnabled(enabled: Boolean)`; composable `SwitchSettingsRow(icon, title, subtitle, checked, onCheckedChange)`.

- [ ] **Step 1: Add the strings**

In `values/strings.xml` (next to the Themes block, ~line 260) add:

```xml
    <string name="label_settings_system">System</string>
    <string name="label_body_weight_toggle">Body Weight</string>
    <string name="label_body_weight_toggle_subtitle">Show body weight recording and charts</string>
    <string name="label_color_style">Color style</string>
    <string name="label_color_style_line">Line</string>
    <string name="label_color_style_material">Material 3</string>
```

In `values-zh/strings.xml` (next to the 主题 block, ~line 255) add:

```xml
    <string name="label_settings_system">系统</string>
    <string name="label_body_weight_toggle">体重模块</string>
    <string name="label_body_weight_toggle_subtitle">显示体重记录与趋势图</string>
    <string name="label_color_style">配色风格</string>
    <string name="label_color_style_line">Line</string>
    <string name="label_color_style_material">Material 3</string>
```

- [ ] **Step 2: Add `SwitchSettingsRow`**

Append to `SettingsGroup.kt` (add imports for `Switch`):

```kotlin
/**
 * A single settings entry with a trailing [Switch]:
 * Layout: [icon] [title / subtitle]            [switch]
 */
@Composable
fun SwitchSettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

Add `import androidx.compose.material3.Switch` to the file's imports.

- [ ] **Step 3: Wire the ViewModel**

In `SettingsViewModel.kt`:
- In the `combine` builder add `bodyWeightEnabled = settings.bodyWeightEnabled,`
- Add to `SettingsUiData`: `val bodyWeightEnabled: Boolean,`
- Add to the initial `SettingsUiData(...)` default in `asStateFlow(...)`: `bodyWeightEnabled = true,`
- Add the updater:

```kotlin
fun updateBodyWeightEnabled(enabled: Boolean) {
    launchCatching {
        repo.setBodyWeightEnabled(enabled)
    }
}
```

- [ ] **Step 4: Render the group**

In `Settings.kt` private `Settings` composable, add a parameter `onToggleBodyWeight: (Boolean) -> Unit` and pass `viewModel::updateBodyWeightEnabled` from the public `Settings`. Then add a group between Appearance and Data:

```kotlin
SettingsGroup(title = stringResource(R.string.label_settings_system)) {
    item {
        SwitchSettingsRow(
            icon = { Icon(painter = KenkoIcons.Person, contentDescription = null) },
            title = stringResource(R.string.label_body_weight_toggle),
            subtitle = stringResource(R.string.label_body_weight_toggle_subtitle),
            checked = state.bodyWeightEnabled,
            onCheckedChange = onToggleBodyWeight,
        )
    }
}
```

Use any existing `KenkoIcons` entry (e.g. `KenkoIcons.Person` if present, else `Icons.Default.MonitorWeight`). Update the `@Preview SettingsPreview` state literal to include `bodyWeightEnabled = true` and add `onToggleBodyWeight = {}`.

- [ ] **Step 5: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/component/SettingsGroup.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat(settings): add System group with body weight switch"
```

---

## Task 3: Hide the Profile weight module when disabled

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/profile/Profile.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/testutil/FakeSettingsRepo.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModelTest.kt`

**Interfaces:**
- Consumes: `Settings.bodyWeightEnabled` (Task 1).
- Produces: `ProfileUiState.bodyWeightEnabled: Boolean` (default `true`).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/looker/kenko/testutil/FakeSettingsRepo.kt`:

```kotlin
package com.looker.kenko.testutil

import com.looker.kenko.data.repository.SettingsRepo
import com.looker.kenko.domain.model.settings.BackupInterval
import com.looker.kenko.domain.model.settings.Language
import com.looker.kenko.domain.model.settings.Settings
import com.looker.kenko.domain.model.settings.Theme
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeSettingsRepo(
    initial: Settings = Settings(
        isOnboardingDone = true,
        theme = Theme.System,
        lastSetTime = null,
        backupUri = null,
        backupInterval = BackupInterval.Off,
        lastBackupTime = null,
        language = Language.System,
        bodyWeightEnabled = true,
    ),
) : SettingsRepo {
    private val state = MutableStateFlow(initial)
    override val stream: Flow<Settings> = state
    override fun <T> get(block: Settings.() -> T): Flow<T> = state.map { it.block() }
    override suspend fun setOnboardingDone() = Unit
    override suspend fun setTheme(theme: Theme) { state.value = state.value.copy(theme = theme) }
    override suspend fun setLastSetTime(instant: Instant?) = Unit
    override suspend fun setBackupUri(uri: String?) = Unit
    override suspend fun setBackupInterval(interval: BackupInterval) = Unit
    override suspend fun setLastBackupTime(instant: Instant?) = Unit
    override suspend fun setLanguage(language: Language) = Unit
    override suspend fun setBodyWeightEnabled(enabled: Boolean) {
        state.value = state.value.copy(bodyWeightEnabled = enabled)
    }
}
```

Create `app/src/test/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModelTest.kt`:

```kotlin
package com.looker.kenko.ui.feature.profile

import com.looker.kenko.testutil.FakeExerciseRepo
import com.looker.kenko.testutil.FakePlanRepo
import com.looker.kenko.testutil.FakeSettingsRepo
import com.looker.kenko.testutil.FakeWeightRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `body weight enabled reflects settings`() = runTest(dispatcher) {
        val settings = FakeSettingsRepo()
        val vm = ProfileViewModel(FakePlanRepo(), FakeWeightRepo(), FakeExerciseRepo(), settings)
        assertTrue(vm.state.value.bodyWeightEnabled)

        settings.setBodyWeightEnabled(false)
        assertFalse(vm.state.value.bodyWeightEnabled)
    }
}
```

This needs a `FakeWeightRepo`. Create it in `testutil/FakeRepos.kt` (append):

```kotlin
internal class FakeWeightRepo : WeightRepo {
    private val _weights = MutableStateFlow(emptyList<Weight>())
    override val weights: Flow<List<Weight>> = _weights
    override suspend fun addWeight(weight: Weight) { _weights.value = _weights.value + weight }
    override suspend fun updateWeight(weight: Weight) = Unit
    override suspend fun deleteWeight(id: Int) = Unit
}
```

Add imports in `FakeRepos.kt`: `com.looker.kenko.data.repository.WeightRepo`, `com.looker.kenko.domain.model.Weight`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.looker.kenko.ui.feature.profile.ProfileViewModelTest"`
Expected: FAIL to compile — `ProfileViewModel` has no `bodyWeightEnabled` state / extra constructor arg.

- [ ] **Step 3: Thread settings into ProfileViewModel**

In `ProfileViewModel.kt`:
- Add constructor param `private val settingsRepo: SettingsRepo,` (last).
- In `state`, add the settings flow to the outer `combine`:

```kotlin
val state: StateFlow<ProfileUiState> = combine(
    combine(currentPlan, weightRepo.weights, exerciseRepo.numberOfExercise, plans) { plan, weights, number, allPlans ->
        Bundle(plan, weights, number, allPlans)
    },
    _selectedMonth,
    _customRange,
    settingsRepo.get { bodyWeightEnabled },
) { bundle, month, range, bodyWeightEnabled ->
    val view = computeWeightChartView(bundle.weights, month, range)
    ProfileUiState(
        numberOfExercises = bundle.numberOfExercises,
        weights = bundle.weights,
        isPlanAvailable = bundle.plan != null,
        planName = bundle.plan?.name ?: "",
        planStat = bundle.plan?.stat,
        planDayCount = bundle.plan?.dayCount ?: 7,
        plans = bundle.plans,
        filteredWeights = view.visibleWeights,
        selectedMonthLabel = view.monthLabel,
        currentMonth = view.currentMonth,
        canGoPrev = view.canGoPrev,
        canGoNext = view.canGoNext,
        customRangeActive = range != null,
        bodyWeightEnabled = bodyWeightEnabled,
    )
}.asStateFlow(ProfileUiState(), started = SharingStarted.Eagerly)
```

- Add `import com.looker.kenko.data.repository.SettingsRepo`.
- Add to `ProfileUiState`: `val bodyWeightEnabled: Boolean = true,`

- [ ] **Step 4: Gate the UI**

In `Profile.kt`, wrap the weight card (and the weight dialogs/sheet) so they only render when enabled. Around line 227:

```kotlin
if (state.bodyWeightEnabled) {
    WeightCard(
        weights = state.weights,
        filteredWeights = state.filteredWeights,
        // ...existing args...
    )
}
```

Also guard the dialog/sheet block (`Profile.kt:137-172`) with `if (state.bodyWeightEnabled)` so a stale dialog cannot render. Leave `WeightLineChart` in `ExerciseProgress.kt` untouched — it is the exercise-progress chart, not the body-weight module.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.looker.kenko.ui.feature.profile.ProfileViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModel.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/profile/Profile.kt \
        app/src/test/kotlin/com/looker/kenko/testutil/FakeSettingsRepo.kt \
        app/src/test/kotlin/com/looker/kenko/testutil/FakeRepos.kt \
        app/src/test/kotlin/com/looker/kenko/ui/feature/profile/ProfileViewModelTest.kt
git commit -m "feat(profile): hide body weight module when disabled"
```

---

## Task 4: Health Connect dependency and permissions

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/looker/kenko/ui/health/PermissionsRationaleActivity.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `HealthConnectClient` on the classpath; manifest permission `android.permission.health.WRITE_EXERCISE`; rationale activity declared.

- [ ] **Step 1: Add the dependency**

In `gradle/libs.versions.toml`:

```toml
[versions]
healthConnect = "1.1.0"

[libraries]
health-connect-client = { module = "androidx.health.connect:connect-client", version.ref = "healthConnect" }
```

In `app/build.gradle.kts` dependencies block:

```kotlin
implementation(libs.health.connect.client)
```

- [ ] **Step 2: Add the rationale activity**

Create `app/src/main/kotlin/com/looker/kenko/ui/health/PermissionsRationaleActivity.kt`:

```kotlin
/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.ui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.looker.kenko.R
import com.looker.kenko.ui.theme.KenkoTheme

/**
 * Target of the Health Connect rationale intent. Health Connect finishes this page
 * after showing the app's privacy policy link; a minimal static screen is enough to
 * satisfy the platform contract.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KenkoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = getString(R.string.label_health_connect_rationale))
                    }
                }
            }
        }
    }
}
```

Add the string in both locales:

`values/strings.xml`:
```xml
    <string name="label_health_connect_rationale">Kenko writes your workout sessions to Health Connect. Your data stays on this device.</string>
```
`values-zh/strings.xml`:
```xml
    <string name="label_health_connect_rationale">Kenko 会把训练记录写入 Health Connect，数据仅保存在本机。</string>
```

- [ ] **Step 3: Declare permission + activity in the manifest**

In `AndroidManifest.xml`, add the permission next to the other `<uses-permission>` entries:

```xml
    <uses-permission android:name="android.permission.health.WRITE_EXERCISE" />
```

Inside `<activity android:name=".ui.MainActivity" .../>` add the privacy-policy intent filter so the rationale deep link works:

```xml
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
```

Then register the rationale activity and the Android-14 alias (sibling of `CrashSaveActivity`):

```xml
        <activity
            android:name=".ui.health.PermissionsRationaleActivity"
            android:exported="true"
            android:theme="@style/Theme.Kenko">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
        </activity>

        <activity-alias
            android:name="ViewPermissionUsageActivity"
            android:exported="true"
            android:targetActivity=".ui.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
        </activity-alias>
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Health Connect classes resolve).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml \
        app/src/main/kotlin/com/looker/kenko/ui/health/PermissionsRationaleActivity.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat(health): add health connect dependency and permissions"
```

---

## Task 5: Pure Session → Health Connect DTO mapping

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/health/HealthExerciseSession.kt`
- Create: `app/src/test/kotlin/com/looker/kenko/domain/health/HealthExerciseSessionTest.kt`

**Interfaces:**
- Consumes: `Session` (`startedAt`, `endedAt`, `id`, `dayTitleOverride`).
- Produces: `data class HealthExerciseSession(clientRecordId, startMillis, endMillis, title)`; `fun Session.toHealthExerciseSession(exerciseNames: List<String>): HealthExerciseSession?`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/looker/kenko/domain/health/HealthExerciseSessionTest.kt`:

```kotlin
package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthExerciseSessionTest {

    private fun session(
        startedAt: Long?,
        endedAt: Long?,
        id: Int? = 7,
        dayTitle: String? = null,
    ) = Session(
        date = LocalDate(2026, 10, 13),
        sets = emptyList(),
        planId = null,
        dayTitleOverride = dayTitle,
        startedAt = startedAt,
        endedAt = endedAt,
        id = id,
    )

    @Test
    fun `valid session maps with stable id and interval`() {
        val result = session(1_000L, 5_000L).toHealthExerciseSession(listOf("Bench Press"))
        assertEquals("kenko:session:7", result?.clientRecordId)
        assertEquals(1_000L, result?.startMillis)
        assertEquals(5_000L, result?.endMillis)
        assertEquals("Bench Press", result?.title)
    }

    @Test
    fun `day title overrides exercise names`() {
        val result = session(1L, 2L, dayTitle = "Push Day").toHealthExerciseSession(listOf("Bench"))
        assertEquals("Push Day", result?.title)
    }

    @Test
    fun `missing timestamps are skipped`() {
        assertNull(session(null, 5L).toHealthExerciseSession(emptyList()))
        assertNull(session(1L, null).toHealthExerciseSession(emptyList()))
    }

    @Test
    fun `non positive interval is skipped`() {
        assertNull(session(5L, 5L).toHealthExerciseSession(emptyList()))
        assertNull(session(5L, 4L).toHealthExerciseSession(emptyList()))
    }

    @Test
    fun `missing id is skipped`() {
        assertNull(session(1L, 2L, id = null).toHealthExerciseSession(emptyList()))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.looker.kenko.domain.health.HealthExerciseSessionTest"`
Expected: FAIL to compile — `toHealthExerciseSession` unresolved.

- [ ] **Step 3: Implement the mapper**

Create `app/src/main/kotlin/com/looker/kenko/domain/health/HealthExerciseSession.kt`:

```kotlin
/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.domain.health

import com.looker.kenko.domain.model.Session

/** A Health-Connect-ready training session, decoupled from the HC SDK for testing. */
data class HealthExerciseSession(
    val clientRecordId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
)

/**
 * Maps a Kenko [Session] to [HealthExerciseSession], or null when it cannot become a
 * valid HC interval: no id, missing start/end, or end <= start.
 */
fun Session.toHealthExerciseSession(exerciseNames: List<String>): HealthExerciseSession? {
    val sessionId = id ?: return null
    val start = startedAt ?: return null
    val end = endedAt ?: return null
    if (end <= start) return null
    val title = dayTitleOverride?.takeIf { it.isNotBlank() }
        ?: exerciseNames.joinToString(", ").take(100)
    return HealthExerciseSession(
        clientRecordId = "kenko:session:$sessionId",
        startMillis = start,
        endMillis = end,
        title = title,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.looker.kenko.domain.health.HealthExerciseSessionTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/health/HealthExerciseSession.kt \
        app/src/test/kotlin/com/looker/kenko/domain/health/HealthExerciseSessionTest.kt
git commit -m "feat(health): map training sessions to health connect DTO"
```

---

## Task 6: HealthConnectManager (HC adapter)

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/data/health/HealthConnectManager.kt`

**Interfaces:**
- Consumes: `HealthExerciseSession` (Task 5).
- Produces: `HealthConnectManager` (`@Singleton`) with `val permissions: Set<String>`, `fun isAvailable(): Boolean`, `suspend fun hasAllPermissions(): Boolean`, `suspend fun write(session: HealthExerciseSession)`, `suspend fun syncAll(sessions: List<HealthExerciseSession>): Int`.

- [ ] **Step 1: Implement the manager**

Create `app/src/main/kotlin/com/looker/kenko/data/health/HealthConnectManager.kt`:

```kotlin
/*
 * Copyright (C) 2026 H7Night <h7night@gmail.com>
 * This file is part of Kenko (GPL-3.0-or-later).
 */

package com.looker.kenko.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import com.looker.kenko.domain.health.HealthExerciseSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Best-effort single write; returns true on success. */
    suspend fun write(session: HealthExerciseSession): Boolean {
        val client = client() ?: return false
        return runCatching {
            client.insertRecords(listOf(session.toRecord()))
            true
        }.getOrDefault(false)
    }

    /** Writes every session not yet present, using clientRecordId idempotency. Returns the count written. */
    suspend fun syncAll(sessions: List<HealthExerciseSession>): Int {
        val client = client() ?: return 0
        var written = 0
        for (session in sessions) {
            val ok = runCatching {
                client.insertRecords(listOf(session.toRecord()))
                true
            }.getOrDefault(false)
            if (ok) written++
        }
        return written
    }
}

private fun HealthExerciseSession.toRecord(): ExerciseSessionRecord {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis)
    val end = Instant.ofEpochMilli(endMillis)
    // ExerciseSessionType.EXERCISE_TYPE_STRENGTH_TRAINING == 70
    return ExerciseSessionRecord(
        startTime = start,
        startZoneOffset = zone.rules.getOffset(start),
        endTime = end,
        endZoneOffset = zone.rules.getOffset(end),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        title = title.takeIf { it.isNotBlank() },
        notes = null,
        metadata = Metadata.manualEntry(clientRecordId = clientRecordId),
    )
}
```

If the installed `connect-client:1.1.0` `ExerciseSessionRecord` constructor also requires `segments`/`laps`/`route` without defaults, pass `segments = emptyList(), laps = emptyList(), route = null` (verify by compiling; the compiler will flag missing params). `Energy` import is unused — drop it if the build warns as an error.

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix constructor parameter names to match the compiler if it complains.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/data/health/HealthConnectManager.kt
git commit -m "feat(health): add health connect manager writing exercise sessions"
```

---

## Task 7: Wire HC write on session end + Settings switch + manual sync

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/component/timer/TrainingSessionManager.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

**Interfaces:**
- Consumes: `HealthConnectManager` (Task 6), `Session.toHealthExerciseSession` (Task 5), `SessionRepo` (existing), `SettingsRepo` (Task 1).
- Produces: side-effect only.

- [ ] **Step 1: Add strings**

`values/strings.xml`:
```xml
    <string name="label_health_connect">Health Connect</string>
    <string name="label_health_connect_sync">Sync workouts to Health Connect</string>
    <string name="label_health_connect_synced">Synced %1$d sessions</string>
```
`values-zh/strings.xml`:
```xml
    <string name="label_health_connect">Health Connect</string>
    <string name="label_health_connect_sync">同步训练到 Health Connect</string>
    <string name="label_health_connect_synced">已同步 %1$d 条训练</string>
```

- [ ] **Step 2: Write the session after it ends**

In `TrainingSessionManager.kt`, inject `HealthConnectManager` and `SettingsRepo`, and after the duration update inside `endTraining` write the completed session (only when body weight/timers produced a valid session and the user enabled sync — gate on `SettingsRepo.get { healthConnectEnabled }`; add that key in Task 7 Step 4). Minimal wiring:

```kotlin
@Singleton
class TrainingSessionManager @Inject constructor(
    private val sessionRepo: SessionRepo,
    private val timerManager: TimerManager,
    private val healthConnectManager: HealthConnectManager,
    @com.looker.kenko.di.ApplicationScope private val scope: CoroutineScope,
) {
    // ...
    private suspend fun syncSessionToHealth(sessionId: Int) {
        val session = sessionRepo.getSession(sessionId) ?: return
        val names = session.performExercises.map { it.name }
        session.toHealthExerciseSession(names)?.let { healthConnectManager.write(it) }
    }
}
```

Call `syncSessionToHealth(sessionId)` at the end of the active-session branch in `endTraining` (after `updateSessionEnded`/`updateSessionDuration`), wrapped so it never throws. If `SessionRepo` has no `getSession(id)`, add one (`suspend fun getSession(id: Int): Session?`) in `SessionRepo` + `LocalSessionRepo` backed by `sessionDao`, mirroring `streamByDate`.

> Note: `Session.performExercises` requires `sets`; if `LocalSessionRepo.getSession` returns sets, names resolve. If not, pass the summary's `exerciseNames` from `SessionSummary` instead and map `SessionSummary.toHealthExerciseSession()` with the same rules — either is acceptable; keep the pure mapper the single source of the skip rules.

- [ ] **Step 3: Settings switch + manual sync**

In `SettingsViewModel.kt`: inject `HealthConnectManager`, `SessionRepo` (already present). Add a `healthConnectEnabled: Boolean` field to `Settings` (default `false`) plus DataStore key `health_connect_enabled`, `SettingsRepo.setHealthConnectEnabled`, and the `DatastoreSettingsRepo` setter + `mapSettings` read — the same pattern as Task 1. Update `FakeSettingsRepo` (Task 3) and every `Settings(...)` construction to include `healthConnectEnabled = false`. Expose it in `SettingsUiData`, and add:

```kotlin
fun setHealthConnectEnabled(enabled: Boolean) {
    launchCatching {
        repo.setHealthConnectEnabled(enabled)
        if (enabled) syncHealthConnect()
    }
}

fun syncHealthConnect() {
    viewModelScope.launch {
        if (!healthConnectManager.hasAllPermissions()) return@launch
        val sessions = sessionRepo.getAllSessions() // add if missing
        val count = healthConnectManager.syncAll(
            sessions.mapNotNull { it.toHealthConnectDto() },
        )
        emitSnackbar(count)
    }
}
```

Permission request UI lives in the composable via `rememberLauncherForActivityResult(RequestPermission())` — request `healthConnectManager.permissions` from `Settings.kt` when the switch is toggled on, via an injected `ActivityResultContract` target. In `Settings.kt` add to the System group:

```kotlin
item {
    SwitchSettingsRow(
        icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = null) },
        title = stringResource(R.string.label_health_connect),
        subtitle = stringResource(R.string.label_health_connect_sync),
        checked = state.healthConnectEnabled,
        onCheckedChange = onToggleHealthConnect,
    )
}
```

- [ ] **Step 4: Build + manual smoke**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.
Manual: on a device with Health Connect, toggle the switch on, grant WRITE_EXERCISE, end a workout, and confirm the session appears in the Health Connect app. Toggle off → no new writes.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/component/timer/TrainingSessionManager.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt \
        app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt \
        app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt \
        app/src/main/kotlin/com/looker/kenko/data/repository/SessionRepo.kt \
        app/src/main/kotlin/com/looker/kenko/data/repository/local/LocalSessionRepo.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat(health): sync completed sessions to health connect"
```

---

## Task 8: Color palette setting persistence

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/model/settings/ColorPalette.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt`
- Modify: `app/src/test/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepoTest.kt`

**Interfaces:**
- Consumes: Task 1's fake DataStore test file.
- Produces: `enum class ColorPalette { Line, Material }`; `Settings.colorPalette: ColorPalette`; `SettingsRepo.setColorPalette(palette)`; DataStore key `color_palette` default `Line`.

- [ ] **Step 1: Write the failing test**

Append to `DatastoreSettingsRepoTest.kt`:

```kotlin
    @Test
    fun `color palette defaults to Line`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        assertEquals(ColorPalette.Line, repo.stream.first().colorPalette)
    }

    @Test
    fun `setColorPalette persists Material`() = runTest {
        val repo = DatastoreSettingsRepo(FakePreferencesDataStore())
        repo.setColorPalette(ColorPalette.Material)
        assertEquals(ColorPalette.Material, repo.stream.first().colorPalette)
    }
```

Add imports: `com.looker.kenko.domain.model.settings.ColorPalette`, `kotlin.test.assertEquals`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.looker.kenko.data.local.datastore.DatastoreSettingsRepoTest"`
Expected: FAIL to compile — `ColorPalette` / `colorPalette` unresolved.

- [ ] **Step 3: Implement**

Create `ColorPalette.kt`:

```kotlin
package com.looker.kenko.domain.model.settings

import androidx.annotation.StringRes
import com.looker.kenko.R

enum class ColorPalette(@StringRes val nameRes: Int) {
    /** The current Kenko accent (Linear-style violet on Zinc neutrals). */
    Line(R.string.label_color_style_line),

    /** Google Material 3 baseline / dynamic color. */
    Material(R.string.label_color_style_material),
}
```

Add `val colorPalette: ColorPalette,` to `Settings`; add `suspend fun setColorPalette(palette: ColorPalette)` to `SettingsRepo`; in `DatastoreSettingsRepo` add the key `color_palette` (string), the setter, and `colorPalette = ColorPalette.valueOf(preferences[COLOR_PALETTE] ?: ColorPalette.Line.name)` in `mapSettings`. Update every `Settings(...)` construction (including the `FakeSettingsRepo` default in testutil) to include `colorPalette = ColorPalette.Line`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.looker.kenko.data.local.datastore.DatastoreSettingsRepoTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/model/settings/ColorPalette.kt \
        app/src/main/kotlin/com/looker/kenko/domain/model/settings/Settings.kt \
        app/src/main/kotlin/com/looker/kenko/data/repository/SettingsRepo.kt \
        app/src/main/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepo.kt \
        app/src/test/kotlin/com/looker/kenko/data/local/datastore/DatastoreSettingsRepoTest.kt
git commit -m "feat(theme): persist color palette preset"
```

---

## Task 9: Resolve the color scheme from the palette

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/theme/Theme.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/MainViewModel.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `ColorPalette` (Task 8).
- Produces: `KenkoTheme(theme: Theme, palette: ColorPalette, content)`; `MainViewModel.palette: StateFlow<ColorPalette>`.

- [ ] **Step 1: Refactor Theme.kt**

- Rename `CatppuccinLatteColorScheme` → `LineLightColorScheme` and `CatppuccinMochaColorScheme` → `LineDarkColorScheme` **without changing any color value** (delete the now-wrong "Catppuccin" comments or update them to say "Line"). Add:

```kotlin
private val MaterialBaselineLight = lightColorScheme()
private val MaterialBaselineDark = darkColorScheme()
```

- Change the signature and selection:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KenkoTheme(
    theme: Theme = Theme.System,
    palette: ColorPalette = ColorPalette.Line,
    content: @Composable () -> Unit,
) {
    val systemTheme = isSystemInDarkTheme()
    val isDarkTheme = remember(theme) {
        when (theme) {
            Theme.System -> systemTheme
            Theme.Light -> false
            Theme.Dark -> true
        }
    }
    val context = LocalContext.current
    val colorScheme = when (palette) {
        ColorPalette.Line ->
            if (isDarkTheme) LineDarkColorScheme else LineLightColorScheme

        ColorPalette.Material ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) MaterialBaselineDark else MaterialBaselineLight
            }
    }
    // ...unchanged SideEffect + MaterialExpressiveTheme...
}
```

Add imports: `androidx.compose.material3.dynamicDarkColorScheme`, `dynamicLightColorScheme`, `androidx.compose.ui.platform.LocalContext`, `com.looker.kenko.domain.model.settings.ColorPalette`. Add `ColorPalette` to other call sites' imports as needed (previews use the default, so no change required).

- [ ] **Step 2: Expose the palette**

`MainViewModel.kt`:

```kotlin
val palette: StateFlow<ColorPalette> = repo.get { colorPalette }
    .asStateFlow(ColorPalette.Line)
```

Add `import com.looker.kenko.domain.model.settings.ColorPalette`.

`MainActivity.kt`: collect and pass:

```kotlin
val palette by viewModel.palette.collectAsStateWithLifecycle()
KenkoTheme(theme = theme, palette = palette) { ... }
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Toggling the palette setting (Task 10) switches colors; with no setting, the app looks identical to before.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/theme/Theme.kt \
        app/src/main/kotlin/com/looker/kenko/ui/MainViewModel.kt \
        app/src/main/kotlin/com/looker/kenko/ui/MainActivity.kt
git commit -m "feat(theme): resolve scheme from color palette preset"
```

---

## Task 10: Settings "Color style" selector

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt`
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `ColorPalette` (Task 8), `SettingsRepo.setColorPalette` (Task 8), strings from Task 2.
- Produces: `SettingsUiData.colorPalette`; `SettingsViewModel.updateColorPalette(palette)`.

- [ ] **Step 1: ViewModel**

In `SettingsViewModel.kt`: add `colorPalette = settings.colorPalette,` to `SettingsUiData(...)`, `val colorPalette: ColorPalette,` to the data class, `colorPalette = ColorPalette.Line,` to the initial default, and:

```kotlin
fun updateColorPalette(palette: ColorPalette) {
    launchCatching {
        repo.setColorPalette(palette)
    }
}
```

- [ ] **Step 2: UI row**

In `Settings.kt`, mirror the theme dialog pattern (a `var showColorStyleDialog` + `SelectionDialog` over `ColorPalette.entries` with `label = { stringResource(it.nameRes) }`). Give `ColorPalette` a `nameRes: Int` property using the same `@StringRes` convention as `Theme` (`ColorPalette.kt`), with `Line → R.string.label_color_style_line`, `Material → R.string.label_color_style_material`. Add a row in the Appearance group:

```kotlin
item {
    SettingsRow(
        icon = { Icon(painter = KenkoIcons.Lightbulb, contentDescription = null) },
        title = stringResource(R.string.label_color_style),
        value = stringResource(state.colorPalette.nameRes),
        onClick = { showColorStyleDialog = true },
    )
}
```

Update `SettingsPreview` state literal (`colorPalette = ColorPalette.Line`) and callback.

- [ ] **Step 3: Build + manual**

Run: `./gradlew :app:assembleDebug`
Manual: open Settings → Color style → Material 3; confirm the app re-tints (dynamic color on Android 12+); switch back to Line → identical to before.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/settings/Settings.kt \
        app/src/main/kotlin/com/looker/kenko/ui/feature/settings/SettingsViewModel.kt \
        app/src/main/kotlin/com/looker/kenko/domain/model/settings/ColorPalette.kt
git commit -m "feat(settings): add color style selector"
```

---

## Task 11: Full verification

**Files:** none.

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew test`
Expected: PASS (all pre-existing tests plus the new ones).

- [ ] **Step 2: Build debug + release**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual matrix**

- Body weight ON → Profile shows the weight card; OFF → hidden, existing weight still exported by `ExportManager` and still used for bodyweight 1RM (compare an exercise-progress value before/after toggling — unchanged).
- Health Connect ON → grant → end a workout → session appears in HC; "Sync now" backfills older sessions; no duplicate records after re-sync (clientRecordId).
- Color style → Material 3 vs Line; Line is pixel-identical to before.

- [ ] **Step 4: Update docs**

Add a CHANGELOG `[Unreleased]` entry per the project's `.agents/skills/update-doc.md` convention; note the body weight toggle, Health Connect sync, and color presets. Commit:

```bash
git add CHANGELOG.md
git commit -m "docs: changelog for body weight, health connect, theme presets"
```

---

## Self-Review Notes

- **Spec coverage:** Body weight toggle → Tasks 1-3; HC write → Tasks 4-7; theme presets → Tasks 8-10; verification → Task 11. Spec's "no Room change / no version bump" honored.
- **Task 7.2 note:** if `LocalSessionRepo` cannot return sets for a session cheaply, map `SessionSummary.exerciseNames` through the same pure skip rules instead of adding a heavy query — the skip rules stay in one place.
- **HC 1.1.0 constructor:** Task 6 flags that `segments`/`laps`/`route` may need explicit empty/null defaults; the compiler is the source of truth here.
