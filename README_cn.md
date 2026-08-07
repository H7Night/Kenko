<div align="center">

<img width="" src="metadata/en-US/images/featureGraphic.png" alt="Kenko" align="center">

Kenko 是一款健身日志应用，为你提供合适的渐进超负荷训练与精心设计的训练计划

</div>

<div align="left">

## 构建包说明

### 测试 / 日常使用（默认）
- **本地构建 debug 包**：运行 `./gradlew assembleDebug`，产物位于 `app/build/outputs/apk/debug/`。
- 使用 **debug 签名**（本地 `keystore/debug.keystore`，缺失时自动回退开发者本机默认 debug keystore；keystore 目录已被 .gitignore 忽略，不入库）。

### main 分支（正式包）
- 每次 push / merge 到 `main` 分支，GitHub Actions（`.github/workflows/auto-build.yml`）自动构建 **release 签名包**，并提交回仓库：
  - 下载位置：`apk/Kenko-release.apk`（仓库根目录，自动更新）
- 使用独立的 **release 签名**（凭证保存在 GitHub Secrets，与 debug 签名区分），用于正式安装与发布。

## 功能特性

- **训练日志**：轻松记录训练组（抗阻训练与有氧训练）。
- **内联训练计时器**：在首页直接开始、暂停、继续、结束训练，基于前台服务计时并常驻通知栏。
- **自定义计划**：创建并管理自定义训练计划，支持自定义计划名与每日标题。
- **两级动作标签**：按 身体部位 → 具体肌群 分类动作，支持多标签，可在设置中管理标签。
- **交互式热力图**：在训练历史页查看带月份滚动和星期标签的训练一致性热力图。
- **渐进超负荷**：通过详细的训练历史与表现洞察跟踪你的长期进步。
- **体重记录**：记录体重并通过交互式折线图查看变化趋势。
- **安全删除**：动作、计划、训练组、体重记录与训练会话均通过显式删除按钮 + 确认对话框删除，防止误删。
- **现代导航**：通过底部导航栏在 首页 / 训练 / 我的 之间轻松切换。
- **Material Design 3**：全面遵循最新 MD3 规范、圆角统一的 UI 组件，Android 12+ 支持动态取色。
- **备份与导出**：完整应用备份（ZIP，WorkManager 自动定时）与选择性训练数据导出（JSON，支持日期范围过滤）。
- **多语言**：支持英文与简体中文。

## 技术栈

- **语言**：Kotlin 2.2.x
- **UI**：Jetpack Compose（BOM 2025.11.00）、Material 3 Expressive
- **依赖注入**：Hilt 2.57
- **持久化**：Room 2.8（KSP 代码生成）、预填充数据库、增量迁移
- **后台任务**：WorkManager（Hilt 集成，定时备份、前台计时服务）
- **导航**：Navigation Compose，类型安全的 `@Serializable` 路由
- **其他**：kotlinx-datetime、kotlinx-serialization、DataStore Preferences

## 构建

```bash
# 构建 debug APK
./gradlew assembleDebug

# 构建并安装 debug APK 到已连接的设备/模拟器
./gradlew installDebug

# 运行单元测试（JUnit 5 / JUnit Platform）
./gradlew test

# 运行仪器化测试（需要设备/模拟器）
./gradlew connectedAndroidTest
```

便捷脚本位于 [`scripts/`](scripts/)（`build_debug`、`install_debug`、`build_and_install`），提供 Windows（`.bat`/`.ps1`）与 Unix（`.sh`）版本。使用 `scripts/changelog.sh <版本>` 可输出指定版本的更新日志段落（如 `Unreleased`）。

## 截图

<img src="metadata/en-US/images/phoneScreenshots/1.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/2.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/3.png" width="25%" /><img src="metadata/en-US/images/phoneScreenshots/4.png" width="25%" />

## 更新日志

- 完整更新日志：[此处](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md)
- 未发布改动：[此处](https://github.com/H7Night/Kenko/blob/main/CHANGELOG.md#unreleased)

## 开源许可

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
