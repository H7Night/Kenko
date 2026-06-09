# Android 跨机器构建包冲突问题

## 问题现象

在 macOS 和 Windows 两台机器上开发同一个 Android 项目，当在一台机器上构建并安装 APK 到手机后，另一台机器构建的 APK 安装时提示**包冲突无法安装**。

## 问题原因

Android 通过 **APK 签名证书**来验证应用的来源身份。即使包名（applicationId）完全相同，如果签名不同，系统也会拒绝覆盖安装。

Android 调试构建默认使用 `~/.android/debug.keystore`（或 Windows 上的 `C:\Users\<user>\.android\debug.keystore`）。这个文件是在首次使用 Android Studio 或 SDK 工具时**本地自动生成**的，每台机器的密钥都不同。

```
macOS 机器：    ~/.android/debug.keystore  ← 密钥 A
Windows 机器：  ~/.android/debug.keystore  ← 密钥 B（不同！）
```

因此，macOS 构建的 APK 用密钥 A 签名，Windows 构建的 APK 用密钥 B 签名。Android 系统检测到签名不一致，拒绝安装。

## 解决方案

创建一个**项目共享的调试密钥库**，提交到 Git 仓库，并在 `build.gradle.kts` 中配置使用它。这样所有机器上的构建都使用同一个签名。

### 步骤 1：生成共享调试密钥库

在项目根目录执行（任选一台机器执行一次即可）：

```bash
keytool -genkeypair -v \
  -keystore app/kenko-debug.keystore \
  -alias androiddebugkey \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug, O=Kenko, C=US"
```

参数说明：
- **keystore**：密钥库文件路径（放在 app/ 目录下）
- **alias**：密钥别名，沿用 Android 默认的 `androiddebugkey`
- **storepass/keypass**：密码，沿用 Android 默认的 `android`
- **validity**：有效期，10000 天足够覆盖整个开发周期
- **dname**：证书持有者信息（仅用于调试，随意填写即可）

### 步骤 2：配置构建脚本

在 `app/build.gradle.kts` 中添加 `signingConfigs` 并引用共享密钥库：

```kotlin
android {
    signingConfigs {
        create("sharedDebug") {
            storeFile = file("kenko-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }
}
```

> **注意**：不要命名为 `debug`，因为 Android Gradle Plugin 已经内置了一个同名的签名配置，会导致冲突。

### 步骤 3：将密钥库提交到 Git

```bash
git add app/kenko-debug.keystore
git commit -m "feat: use shared debug keystore for cross-machine builds"
```

确保 `.gitignore` 不会排除 `.keystore` 文件（常见的 `*.jks` 规则不会影响 `.keystore` 扩展名）。

### 步骤 4：在其他机器上拉取

```bash
git pull
./gradlew assembleDebug
```

此后从任意机器构建的 APK 签名一致，可以互相覆盖安装。

## 安全性说明

- 调试密钥库的密码是公开的（`android`），**仅用于开发调试**，不具备任何安全保护能力。
- **切勿**将此密钥库用于生产发布。发布版本应使用独立的、妥善保管的发布密钥库。
- 本项目使用 `applicationIdSuffix = ".debug"`，因此调试版本的包名是 `com.looker.kenko.debug`，与正式版本 `com.looker.kenko` 相互独立，不会互相干扰。

## 扩展知识

### Android 签名验证机制

Android 在安装 APK 时会检查签名：

```
1. 已安装的 APK → 提取签名证书指纹
2. 新 APK        → 提取签名证书指纹
3. 比较指纹
   ├── 相同 → 允许覆盖安装（或更新）
   └── 不同 → 拒绝安装，提示签名冲突
```

### v1 / v2 / v3 签名方案

| 方案 | 引入版本 | 特点 |
|------|---------|------|
| v1 (JAR) | Android 1.0 | 对 APK 中的每个文件进行签名，验证较慢 |
| v2 (APK) | Android 7.0 | 对整个 APK 进行签名，安装更快 |
| v3 (APK) | Android 9.0 | 支持密钥轮转，允许更换签名密钥 |

现代 Android 构建通常同时使用 v1 + v2 签名。

### 其他解决方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| **共享调试密钥库（推荐）** | 一次配置，永久解决 | 需要在构建脚本中配置 |
| 每次安装前先卸载 | 无需任何改动 | 繁琐，每个设备都要操作 |
| 使用 adb install -r -t | 同上 | `-t` 只允许 testOnly，不影响签名 |
| 手动复制 `~/.android/debug.keystore` | 不修改项目 | 多台机器间难以同步保持一致 |
