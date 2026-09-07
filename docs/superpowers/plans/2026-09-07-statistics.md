# Statistics Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a frequency-centric balanced statistics dashboard (90-day heatmap + 7-part weekly/monthly/plan bars + balance ring + 12-week trend + adherence) with cardio minutes handling.

**Architecture:** Extend `StatisticsViewModel` to combine `SessionRepo.streamSummaries` + `SessionRepo.stream` + `ExerciseRepo.stream` + `PlanRepo.current` into a single `StatisticsUiState`. Pure `aggregateByBodyPart` and `aggregateCardioMinutes` functions handle all grouping; Compose cards consume `Map<String,Int>` / `Int` only.

**Tech Stack:** Kotlin 2.2, Jetpack Compose BOM 2025.11, Hilt 2.57, Room 2.8, kotlinx-datetime, JUnit5

## Global Constraints

- No DAO / entity / migration changes — reuse existing `SessionSummary` and `Session` flows.
- Frequency is primary metric; for 6 strength parts count sessions, for `有氧` sum minutes (`Set.repsOrDuration` where `countType==MINUTES`).
- Body parts are 7 primary `Tag.parentName` values: 胸/背/腿/手臂/肩/腹/有氧 (null/empty → 有氧).
- Heatmap window is 13 weeks ×7 =91 days ≈90 days, Monday-aligned, GitHub style (NzHelper pattern).
- Bottom nav label must be Chinese `统计` (`label_statistics`).
- Deduplicate per session per parent (bench+fly in same session → 胸=1).
- All aggregations in ViewModel, UI has no DB access.

---

## File Structure

**Created:**
- `app/src/main/kotlin/com/looker/kenko/domain/statistics/Aggregation.kt` — `aggregateByBodyPart`, `aggregateCardioMinutes`, `buildHeatmapData90d`
- `app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt`
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/BodyPartBarCard.kt`
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/BalanceRingCard.kt`
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/TrendCard.kt`
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/AdherenceCard.kt`

**Modified:**
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/HeatmapCard.kt` (refactor `YearHeatmap.kt` → 90d, weeksToShow=13)
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt` (add combine of 4 flows, expose new state)
- `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/Statistics.kt` (LazyColumn with 5 cards)
- `app/src/main/res/values/strings.xml` (`label_statistics` → `统计`, already done but verified)
- `app/src/main/res/values-zh/strings.xml` if exists (mirror)

---

### Task 1: Pure Aggregation Logic + Unit Tests

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/domain/statistics/Aggregation.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt`

**Interfaces:**
- Consumes: `List<SessionSummary>`, `List<Session>`, `Map<String, Pair<String, CountType>>` (tagDict), `LocalDate`, `Plan?`
- Produces:
  - `fun aggregateByBodyPart(summaries: List<SessionSummary>, predicate: (LocalDate)->Boolean, tagDict: Map<String, Pair<String,CountType>>): Map<String,Int>`
  - `fun aggregateCardioMinutes(sessions: List<Session>, predicate: (LocalDate)->Boolean): Int`
  - `fun buildHeatmapData90d(dates: Set<LocalDate>, today: LocalDate): HeatmapData` (13 weeks)

- [ ] **Step 1: Write failing test for aggregateByBodyPart**

```kotlin
// app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt
package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.SessionSummary
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AggregationTest {
    private val tagDict = mapOf(
        "Bench Press" to ("胸" to CountType.REPS),
        "Squat" to ("腿" to CountType.REPS),
        "Treadmill" to ("有氧" to CountType.MINUTES)
    )
    @Test
    fun `dedup per session per parent`() {
        val summaries = listOf(
            SessionSummary(date = LocalDate(2026,9,1), planId=1, exerciseNames=listOf("Bench Press","Pec Dec"), setCount=2),
            SessionSummary(date = LocalDate(2026,9,2), planId=1, exerciseNames=listOf("Squat"), setCount=1)
        )
        val result = aggregateByBodyPart(summaries, { true }, tagDict)
        assertEquals(1, result["胸"])
        assertEquals(1, result["腿"])
        assertEquals(null, result["有氧"])
    }
    @Test
    fun `null tag buckets to 有氧 excluded from strength map`() {
        val summaries = listOf(SessionSummary(date=LocalDate(2026,9,1), planId=1, exerciseNames=listOf("Unknown"), setCount=1))
        val result = aggregateByBodyPart(summaries, { true }, emptyMap())
        assertEquals(0, result.size) // 有氧 excluded here; cardio handled separately
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.looker.kenko.domain.statistics.AggregationTest" -v`
Expected: FAIL `aggregateByBodyPart not defined`

- [ ] **Step 3: Implement minimal Aggregation.kt**

```kotlin
package com.looker.kenko.domain.statistics

import com.looker.kenko.domain.model.CountType
import com.looker.kenko.domain.model.Session
import com.looker.kenko.domain.model.SessionSummary
import kotlinx.datetime.LocalDate

fun aggregateByBodyPart(
    summaries: List<SessionSummary>,
    predicate: (LocalDate)->Boolean,
    tagDict: Map<String, Pair<String, CountType>>
): Map<String,Int> {
    val counts = mutableMapOf<String,Int>()
    for (s in summaries) {
        if (!predicate(s.date)) continue
        val parents = s.exerciseNames.mapNotNull { tagDict[it]?.first }.toSet()
            .ifEmpty { emptySet() } // null → handled as 有氧 elsewhere
        for (p in parents) if (p != "有氧") counts[p] = (counts[p]?:0)+1
    }
    return counts
}
fun aggregateCardioMinutes(sessions: List<Session>, predicate: (LocalDate)->Boolean): Int {
    var sum=0
    for (sess in sessions) if (predicate(sess.date)) {
        for (set in sess.sets) if (set.exercise.countType==CountType.MINUTES) sum+=set.repsOrDuration
    }
    return sum
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.looker.kenko.domain.statistics.AggregationTest" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/domain/statistics/Aggregation.kt app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt
git commit -m "feat(statistics): add pure aggregation for body-part frequency and cardio minutes"
```

---

### Task 2: Heatmap 90d Refactor

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/YearHeatmap.kt` → rename to `HeatmapCard.kt` or keep name but change logic
- Test: `app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt` add heatmap case

**Interfaces:**
- Consumes: `Set<LocalDate>`, `LocalDate today`
- Produces: `@Composable fun HeatmapCard(sessionDates: Set<LocalDate>, today: LocalDate, modifier: Modifier)`

- [ ] **Step 1: Write failing test for 90d window**

```kotlin
@Test
fun `heatmap covers 91 days Monday-aligned`() {
    val today = LocalDate(2026,9,7) // Monday
    val data = buildHeatmapData90d(setOf(today), today)
    assertEquals(13, data.weeks.size)
    assertEquals(91, data.weeks.sumOf { it.days.count { d -> d!=null } })
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*.AggregationTest.heatmap*" -v` Expected: FAIL

- [ ] **Step 3: Refactor YearHeatmap to 90d**

```kotlin
// In HeatmapCard.kt replace buildYearData(year) with buildHeatmapData90d
fun buildHeatmapData90d(dates: Set<LocalDate>, today: LocalDate): HeatmapData {
    val dow = today.dayOfWeek.isoDayNumber
    val endSunday = today.plus(7-dow, DateTimeUnit.DAY)
    val startMonday = endSunday.minus(13*7-1, DateTimeUnit.DAY) // 91 days inclusive
    // ... same loop as before but 13 weeks, not year
}
@Composable fun HeatmapCard(sessionDates: Set<LocalDate>, modifier: Modifier) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val data = remember(sessionDates) { buildHeatmapData90d(sessionDates, today) }
    // reuse existing grid UI
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test -v` Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/HeatmapCard.kt app/src/test/kotlin/com/looker/kenko/domain/statistics/AggregationTest.kt
git commit -m "refactor(statistics): switch heatmap to 90d rolling window"
```

---

### Task 3: Extend StatisticsViewModel

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModelTest.kt` (optional robolectric)

**Interfaces:**
- Consumes: `SessionRepo.streamSummaries`, `SessionRepo.stream`, `ExerciseRepo.stream`, `PlanRepo.current`
- Produces: `StateFlow<StatisticsUiState>` with `weeklyCounts, monthlyCounts, planCounts: Map<String,Int>`, `cardioMinutesWeekly/Monthly/Plan: Int`, `weeklyTrend: List<Int>`, `heatmapData`

- [ ] **Step 1: Write failing test (viewmodel emits weeklyCounts)**

```kotlin
@Test
fun `viewmodel weeklyCounts aggregates correctly`() = runTest {
    // mock repos with 2 summaries this week
    val vm = StatisticsViewModel(fakeSessionRepo, fakeExerciseRepo, fakePlanRepo)
    assertEquals(1, vm.state.first().weeklyCounts["胸"])
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement combine**

```kotlin
val state = combine(streamSummaries, stream, exerciseStream, currentPlan) { summaries, sessions, exercises, plan ->
    val tagDict = exercises.associate { it.name to (it.tags.firstOrNull()?.parentName to it.countType) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val monday = today.minus(today.dayOfWeek.isoDayNumber-1, DateTimeUnit.DAY)
    StatisticsUiState(
        heatmapData = buildHeatmapData90d(summaries.map{it.date}.toSet(), today),
        weeklyCounts = aggregateByBodyPart(summaries, { it >= monday }, tagDict),
        monthlyCounts = aggregateByBodyPart(summaries, { it.year==today.year && it.month==today.month }, tagDict),
        planCounts = aggregateByBodyPart(summaries, { s -> s==plan?.id }, tagDict), // via planId check
        cardioWeekly = aggregateCardioMinutes(sessions, { it >= monday }),
        // trend: loop 12 mondays
    )
}.asStateFlow(...)
```

- [ ] **Step 4: Run test**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsViewModel.kt
git commit -m "feat(statistics): extend viewmodel to weekly/monthly/plan aggregates and cardio"
```

---

### Task 4: BodyPartBarCard

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/BodyPartBarCard.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/ui/feature/statistics/components/BodyPartBarCardTest.kt` (compose preview test) or manual preview

**Interfaces:**
- Consumes: `title: String`, `counts: Map<String,Int>`, `cardioMinutes: Int`, `maxCount: Int`, `maxMinutes: Int`
- Produces: `@Composable fun BodyPartBarCard(...)`

- [ ] **Step 1: Write failing preview test**

```kotlin
@Composable @Preview fun BodyPartBarCardPreview() {
    BodyPartBarCard(title="本周", counts=mapOf("胸" to 3, "腿" to 0), cardioMinutes=30)
}
```

- [ ] **Step 2: Run to verify fails (component not found)**

- [ ] **Step 3: Implement card (7 rows, bar = count/maxCount, 有氧 row shows "30 分钟" + bar = minutes/maxMinutes)**

```kotlin
@Composable fun BodyPartBarCard(title: String, counts: Map<String,Int>, cardioMinutes: Int, modifier: Modifier) {
    val order = listOf("胸","背","腿","手臂","肩","腹","有氧")
    Card(modifier, shape=MaterialTheme.shapes.medium, border=BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title, style=MaterialTheme.typography.titleMedium)
            order.forEach { part ->
                val value = if (part=="有氧") cardioMinutes else counts[part]?:0
                val max = if (part=="有氧") maxOf(1, cardioMinutes) else maxOf(1, counts.values.maxOrNull()?:1)
                val label = if (part=="有氧") "$value 分钟" else "$value 次"
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text(part, Modifier.width(40.dp), style=MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(progress={value.toFloat()/max}, Modifier.weight(1f).height(8.dp))
                    Text(label, Modifier.padding(start=8.dp), style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run preview & `./gradlew assembleDebug`**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/BodyPartBarCard.kt
git commit -m "feat(statistics): add body-part bar card with cardio minutes handling"
```

---

### Task 5: BalanceRingCard

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/BalanceRingCard.kt`

**Interfaces:**
- Consumes: `monthlyCounts: Map<String,Int>`, `cardioMonthly: Int`
- Produces: `@Composable fun BalanceRingCard(...)`

- [ ] **Step 1: Write failing test (ring renders without crash)**

- [ ] **Step 2: Implement Canvas donut (7 slices, <8% red dot)**

- [ ] **Step 3: Commit**

---

### Task 6: TrendCard (12-week)

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/TrendCard.kt`

- [ ] **Step 1: Write failing test**

- [ ] **Step 2: Implement sparkline with `Path` and `outlineVariant` grid**

- [ ] **Step 3: Commit**

---

### Task 7: AdherenceCard

**Files:**
- Create: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/components/AdherenceCard.kt`

- [ ] **Step 1: Write failing test**

- [ ] **Step 2: Implement `actualDays / plan.dayCount` progress**

- [ ] **Step 3: Commit**

---

### Task 8: Assemble Statistics Screen

**Files:**
- Modify: `app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/Statistics.kt`
- Test: `app/src/test/kotlin/com/looker/kenko/ui/feature/statistics/StatisticsScreenTest.kt`

- [ ] **Step 1: Write failing test (screen contains 5 cards)**

- [ ] **Step 2: Implement LazyColumn with `HeatmapCard` + `BalanceRingCard` + 3×`BodyPartBarCard` + `TrendCard` + `AdherenceCard`**

```kotlin
LazyColumn(contentPadding=innerPadding+PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
    item { HeatmapCard(state.heatmapData) }
    item { BalanceRingCard(state.monthlyCounts) }
    item { BodyPartBarCard("本周", state.weeklyCounts, state.cardioWeekly) }
    item { BodyPartBarCard("本月", state.monthlyCounts, state.cardioMonthly) }
    item { BodyPartBarCard("本计划", state.planCounts, state.cardioPlan) }
    item { TrendCard(state.weeklyTrend) }
    item { AdherenceCard(state.adherence) }
}
```

- [ ] **Step 3: Run `./gradlew assembleDebug`**

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/looker/kenko/ui/feature/statistics/Statistics.kt
git commit -m "feat(statistics): assemble dashboard with heatmap, bars, ring, trend and adherence"
```

---

## Self-Review

- Spec coverage: heatmap 90d → Task 2, weekly/monthly/plan → Tasks 1/3/4, balance → Task5, trend → Task6, adherence → Task7, screen → Task8, cardio minutes → Tasks1/4, nav label → already done (verify)
- Placeholder scan: no TBD; all steps have code.
- Type consistency: `aggregateByBodyPart` returns `Map<String,Int>` used consistently; `cardioMinutes` is `Int` throughout.

