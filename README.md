<div align="center">

<img width="" src="metadata/en-US/images/featureGraphic.png" alt="Kenko" align="center">

Kenko is a workout journal which will provide you with appropriate progressive-overload and well
thought-out plans

</div>

<div align="left">

## Features

- **Workout Journal**: Track your training sessions (Resistance and Cardio) with ease.
- **Inline Training Timer**: Start, pause, resume, and end workouts directly from the Home page with a foreground service timer and notification.
- **Custom Plans**: Create and manage your own workout plans with custom names and per-day titles.
- **Two-Level Exercise Tags**: Classify exercises by body part → specific muscle groups. Multi-tag support with tag management in Settings.
- **Interactive Heatmap**: View your training consistency with a scrollable monthly heatmap and day-of-week labels on the Session History screen.
- **Progressive Overload**: Monitor your progress over time with detailed session history and performance insights.
- **Body Weight Tracking**: Record your body weight and view progress on interactive line charts.
- **Safe Deletion**: Delete exercises, plans, sets, weight records, and sessions via explicit delete buttons with confirmation dialogs.
- **Modern Navigation**: Effortlessly switch between Home, Training, and Profile using the bottom navigation bar.
- **Material Design 3**: Fully rounded and unified UI components following the latest MD3 guidelines, with dynamic color on Android 12+.
- **Backup & Export**: Full app backup (ZIP, auto-scheduled via WorkManager) and selective training data export (JSON, with date range filtering).
- **Multi-language**: Support for English and Chinese (Simplified).

## Tech Stack

- **Language**: Kotlin 2.2.x
- **UI**: Jetpack Compose (BOM 2025.11.00), Material 3 Expressive
- **DI**: Hilt 2.57
- **Persistence**: Room 2.8 (with KSP), pre-populated database, incremental migrations
- **Background**: WorkManager with Hilt integration (scheduled backups, foreground timer service)
- **Navigation**: Navigation Compose with type-safe `@Serializable` routes
- **Other**: kotlinx-datetime, kotlinx-serialization, DataStore Preferences

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install debug APK to a connected device/emulator
./gradlew installDebug

# Run unit tests (JUnit 5 / JUnit Platform)
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

Convenience scripts are available in [`scripts/`](scripts/) (`build_debug`, `install_debug`, `build_and_install`) for Windows (`.bat`/`.ps1`) and Unix (`.sh`). Use `scripts/changelog.sh <version>` to print the changelog section for a given version (e.g. `Unreleased`).

## Screenshots

<img src="metadata/en-US/images/phoneScreenshots/1.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/2.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/3.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/4.png" width="25%" />

## CHANGELOGS

- Full changelog: [here](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md)
- Unreleased changes: [here](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md#unreleased)

## LICENSE

```
Kenko

Copyright (C) 2025 LooKeR & Contributors
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

</div>
