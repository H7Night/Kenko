<div align="center">

<img width="" src="metadata/en-US/images/featureGraphic.png" alt="Kenko" align="center">

Kenko is a workout journal which will provide you with appropriate progressive-overload and well
thought-out plans

</div>

<div align="left">

## 构建包说明 (Build Packages)

### 测试 / 日常使用（默认）
- **本地构建 debug 包**：运行 `./gradlew assembleDebug`，产物位于 `app/build/outputs/apk/debug/`。
- 使用 **debug 签名**（已入库的 `app/kenko-debug.keystore`，团队通用），所有开发者本地构建的调试包签名一致，覆盖安装不会出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 签名冲突。

### main 分支（正式包）
- 每次 push / merge 到 `main` 分支，GitHub Actions（`.github/workflows/auto-build.yml`）自动构建 **release 签名包**，并提交回仓库：
  - 下载位置：`apk/Kenko-release.apk`（仓库根目录，自动更新）
- 使用独立的 **release 签名**（凭证保存在 GitHub Secrets，与 debug 签名区分），用于正式安装与发布。

## Features
- **Workout Journal**: Track your training sessions (Resistance and Cardio) with ease.
- **Inline Training Timer**: Start, pause, and resume workouts directly from the Home page with a foreground service timer.
- **Custom Plans**: Create and manage your own workout plans with custom name and day titles.
- **Two-Level Exercise Tags**: Classify exercises by body part → specific muscle groups. Multi-tag support with tag management in Settings.
- **Interactive Heatmap**: View your training consistency with a scrollable monthly/yearly heatmap and day-of-week labels.
- **Progressive Overload**: Monitor your progress over time with detailed session history.
- **Body Weight Tracking**: Record your body weight and view progress on interactive line charts.
- **Modern Navigation**: Effortlessly switch between Home, Training, and Profile using the new bottom navigation bar.
- **Material Design 3**: Fully rounded and unified UI components following the latest MD3 guidelines.
- **Backup & Restore**: Keep your data safe with local backup and restore.
- **Multi-language**: Support for English, Chinese, and Turkish.

## Screenshots

<img src="metadata/en-US/images/phoneScreenshots/1.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/2.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/3.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/4.png" width="25%" />

## CHANGELOGS
- Full changelog: [here](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md)
- Unreleased changes: [here](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md#unreleased)

## TODO


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
