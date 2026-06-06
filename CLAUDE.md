# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **`kotlinx.datetime.LocalDate`** is the canonical date type throughout the codebase, stored as epoch days in Room.
- **Room database** is pre-populated from `app/src/main/assets/kenko.db` and uses incremental migrations (currently at version 6).

### Key dependencies

- **Compose BOM** `2025.11.00` with Material 3 Expressive (`1.5.0-alpha08`)
- **Hilt** `2.57.2` for DI, with `hilt-navigation-compose` for ViewModel injection into composables
- **Room** `2.8.3` with KSP for compile-time codegen
- **Kotlin** `2.2.21` / KSP `2.2.21-2.0.4`
- **WorkManager** with Hilt integration for scheduled backups
- **kotlinx-datetime** and **kotlinx-serialization** for date handling and type-safe navigation routes
