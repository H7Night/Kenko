[简体中文](README_cn.md)

# Tore

Tore is a focused workout journal for strength training and cardio.

The name comes from the Japanese term `筋トレ`, short for `筋肉トレーニング`, meaning strength training。 Tore helps you build a repeatable routine, record each session, and see whether your training is balanced over time。

## Features

- Log resistance and cardio sessions
- Build plans with training days, rest days, and custom day titles
- Import and export plans as JSON
- Organize exercises with body-part and muscle-group tags
- Track consistency with heatmaps, trends, balance, and adherence statistics
- Record body weight and review progress over time
- Use an inline workout timer with pause and resume
- Delete data through explicit confirmation dialogs
- Back up the complete database or export selected training data
- English and Simplified Chinese localization

## Tech stack

- Kotlin 2.2.x
- Jetpack Compose and Material 3 Expressive
- Hilt for dependency injection
- Room and KSP for local persistence
- WorkManager for scheduled backups
- Navigation Compose with serializable routes

## Build

```bash
# Build a debug APK
./gradlew assembleDebug

# Build and install on a connected device or emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests on a connected device or emulator
./gradlew connectedAndroidTest
```

The `scripts/` directory contains non-interactive wrappers for common Windows and Unix build commands.

## Release

Release preparation is managed by GitHub Actions. The preparation workflow updates the version, changelog, and Fastlane metadata. Merging the release pull request into `main` starts the signed release build and publishing workflow.

## Screenshots

Screenshots will be added after the Tore redesign is installed on a connected device.

## License

Tore is free software licensed under the GNU General Public License, version 3 or later.

See [LICENSE](LICENSE) for the full license text.

## Changelog

- [Full changelog](CHANGELOG.md)
- [Unreleased changes](CHANGELOG.md#unreleased)
