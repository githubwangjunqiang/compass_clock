---
name: android-build-publish
description: 构建与发布规范。buildSrc、版本管理、蒲公英上传、APK 命名。
---

# 构建工具与发布

本文件描述 `buildSrc/` 模块的构建工具能力。**完整实现代码见附属文件**。

> **AI 使用说明**：标注 `⚠️ 项目相关` 的部分需根据实际项目替换。

---

## 一、buildSrc 目录结构

```
buildSrc/                                ✅ 通用代码
├── build.gradle.kts                     // Gradle 配置（kotlin-dsl + Gson）
└── src/main/kotlin/
    ├── VersionConfig.kt                  // 版本号管理
    ├── ApkUpLoadUtils.kt                 // APK 上传蒲公英
    └── entity/
        └── UploadResponse.kt            // 蒲公英响应模型
```

---

## 二、buildSrc/build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
```

---

## 三、VersionConfig — 版本号管理

基于 `version.properties` 文件的版本配置管理单例。

**核心功能**：
- `getNextVersionCode(project)` — 获取版本号并自增
- `getVersionName(project)` — 获取语义化版本名（如 "1.0.0"）
- `getPGyerApiKey(project)` — 获取蒲公英 API Key

> **完整实现见 [VersionConfig.kt](VersionConfig.kt)**

---

## 四、ApkUpLoadUtils — 蒲公英自动上传

### 4.1 ⚠️ 需替换的常量

```kotlin
// ⚠️ 替换为 assemble + {Flavor首字母大写} + {BuildType首字母大写}
private val taskName = "assembleChinaRelease"        // ← ⚠️

// ⚠️ 替换为 outputs/apk/{flavor}/{buildType}
private val apkPath = "outputs/apk/china/release"    // ← ⚠️

// ⚠️ 替换为 "{rootProject.name}_"
private val apkPrefix = "YangBo_"                    // ← ⚠️
```

**渠道对应速查**：

| 场景 | taskName | apkPath |
|------|----------|---------|
| 无 flavor + Release | `assembleRelease` | `outputs/apk/release` |
| china + Release | `assembleChinaRelease` | `outputs/apk/china/release` |

> **完整实现见 [ApkUpLoadUtils.kt](ApkUpLoadUtils.kt)**

---

## 五、UploadResponse — 蒲公英响应模型

> **完整实现见 [UploadResponse.kt](UploadResponse.kt)**

---

## 六、APK 输出命名规则

**命名格式**：
```
{rootProject.name}_{versionName}_{versionCode}_{yyyy.MM.dd_HH.mm.ss}_{flavor}_{buildType}.apk
```

**示例**：
```
YangBo_1.0.0_1745_2026.03.05_10.41.43_china_release.apk
```

**配置代码**（✅ 通用）：
```kotlin
// app/build.gradle.kts → android {} 块内
applicationVariants.all {
    outputs.all {
        val ext = outputFile.extension
        val date = Date()
        val formattedDate = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(date)
        val outputFileName =
            "${rootProject.name}_${versionName}_${versionCode}_${formattedDate}_${flavorName}_${buildType.name}.$ext"
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            outputFileName
    }
}
```

---

## 七、工程配置总览

### 7.1 version.properties — ⚠️ 项目相关

项目根目录创建：
```properties
# 版本配置
versionCode=1
versionName=1.0.0        # ← ⚠️ 手动维护

# 蒲公英配置
PGYER_API_KEY=你的api_key  # ← ⚠️ 替换
PGYER_APP_KEY=你的app_key  # ← ⚠️ 替换
```

### 7.2 settings.gradle.kts — ⚠️ 项目相关

```kotlin
rootProject.name = "YangBo"  // ← ⚠️ 替换为实际项目名
```

### 7.3 app/build.gradle.kts 集成 — ✅ 通用

```kotlin
import java.text.SimpleDateFormat
import java.util.Date

afterEvaluate {
    ApkUpLoadUtils.registerTaskForDandelion(this, VersionConfig.getPGyerApiKey(project))
}

android {
    defaultConfig {
        versionCode = VersionConfig.getNextVersionCode(project)
        versionName = VersionConfig.getVersionName(project)
        buildConfigField("String", "PGYER_API_KEY", "\"${VersionConfig.getPGyerApiKey(project)}\"")
        buildConfigField("String", "PGYER_APP_KEY", "\"${VersionConfig.getPGyerAppKey(project)}\"")
    }
    // APK 命名配置见第六节
}
```

---

## 八、使用命令

```bash
# 通用命令
./gradlew uploadPgyer           # 单独上传已有 APK
./gradlew clean                 # 清理构建产物

# ⚠️ 项目相关（按实际 flavor/buildType 替换）
./gradlew assembleChinaRelease  # 构建 + 自动上传
./gradlew assembleGoogleRelease
```

---

## 九、新项目接入清单

| 步骤 | 操作 | 类型 |
|------|------|------|
| 1 | 复制 `buildSrc/` 目录 | ✅ 通用 |
| 2 | 创建 `version.properties` | ⚠️ 项目相关 |
| 3 | 修改 `rootProject.name` | ⚠️ 项目相关 |
| 4 | 修改 `ApkUpLoadUtils` 中常量 | ⚠️ 项目相关 |
| 5 | 添加 `app/build.gradle.kts` 配置 | ✅ 通用 |