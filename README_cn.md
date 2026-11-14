[English](README.md)

# Tore

Tore是一款专注于力量训练和有氧训练的训练日志应用。

名字取自日语「筋トレ」，即「筋肉トレーニング」的简称，意为力量训练。Tore帮助你建立稳定的训练节奏，记录每次训练，并观察长期训练是否均衡。

## 功能

- 记录力量训练和有氧训练
- 创建包含训练日、休息日和自定义日标题的训练计划
- 以JSON格式导入和导出训练计划
- 使用身体部位和肌肉群标签整理训练动作
- 通过热力图、趋势、平衡度和计划执行率查看训练统计
- 记录体重并查看长期变化
- 使用支持暂停和继续的训练计时器
- 删除数据前明确确认
- 备份完整数据库，或按范围导出训练数据
- 支持英文和简体中文

## 技术栈

- Kotlin2.2.x
- Jetpack Compose和Material 3 Expressive
- Hilt依赖注入
- Room与KSP本地持久化
- WorkManager定时备份
- 使用可序列化路由的Navigation Compose

## 构建

```bash
# 构建debug APK
./gradlew assembleDebug

# 在已连接的设备或模拟器上构建并安装
./gradlew installDebug

# 运行单元测试
./gradlew test

# 在已连接的设备或模拟器上运行仪器化测试
./gradlew connectedAndroidTest
```

`scripts/`目录提供了适用于Windows和Unix的常用构建命令脚本，所有脚本均为非交互式。

## 发布

发布准备由GitHub Actions管理。准备发布工作流会更新版本号、更新日志和Fastlane元数据。发布PR合并到`main`后，会启动签名构建和发布流程。

## 截图

Tore重设计完成并安装到已连接设备后，再补充最新截图。

## 许可证

Tore使用GNU通用公共许可证第3版或更高版本发布。

完整许可证文本请参阅[LICENSE](LICENSE)。

## 更新日志

- [完整更新日志](CHANGELOG.md)
- [未发布改动](CHANGELOG.md#unreleased)
