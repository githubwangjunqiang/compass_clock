---
name: android-project-overview
description: Android 项目总纲。技术栈、编码规范、包结构、MVI 概览。新建项目或了解架构时使用。
---

# Android 项目开发技能规范

本文件为 AI 编程工具（Claude Code、OpenCode、Cursor、Copilot 等）提供 Android 项目开发规范和技能指导。
所有生成的代码必须严格遵守本文件及 `skills/` 目录下子文件中的规范。

---

## 一、技术栈

- **语言**: Kotlin（非必要不使用 Java）
- **UI 框架**: Jetpack Compose（Material 3）
- **架构模式**: MVI（Model-View-Intent），参照 Google 官方架构指南
- **异步处理**: Kotlin Coroutines + Flow
- **事件总线**: 项目封装的 `BaseNotStickyLiveData`（非粘性）、`BaseMutableLiveData`（粘性）
- **网络库**: OkHttp3 自定义封装（禁止使用 Retrofit）
- **图片加载**: Glide（通过封装的工具类统一调用）
- **本地缓存**: 腾讯 MMKV（键值对）+ 自定义网络缓存系统
- **数据库**: Google Room
- **JSON 解析**: Gson
- **依赖注入**: 手动单例管理（暂不使用 Hilt/Koin）

---

## 技术选型理由（重要）

| 选择 | 理由 |
|------|------|
| **OkHttp 而非 Retrofit** | 项目已有统一封装，网络层高度定制；Retrofit 注解增加学习成本，且 Retrofit 底层就是 OkHttp |
| **手动单例而非 Hilt/Koin** | 项目规模适中，DI 学习成本高于收益；手动单例更直观，便于调试和追踪依赖来源 |
| **MMKV 而非 SharedPreferences** | SP 主线程 `commit()` 导致 ANR；MMKV 基于 mmap，写入更快、支持多进程、无 ANR 风险 |
| **Glide 而非 Coil** | Glide 生态成熟、缓存策略灵活；项目已有 Glide 封装，迁移成本高 |
| **Gson 而非 Moshi/Kotlin-Serialization** | Gson 兼容性好、学习成本低；项目中大量历史数据模型依赖 Gson |
| **MVI 而非 MVVM** | MVI 单向数据流更易追踪状态变化；Intent 封装用户意图，便于测试和复用 |
| **不使用 LiveData（新代码）** | LiveData 生命周期感知有时多余；Flow 更灵活、支持操作符链式调用 |

---

## 二、强制规范

### 2.1 中文注释要求

**所有代码必须添加中文注释**：

- **类**：必须添加 KDoc 注释，说明用途和职责
- **公共方法**：必须添加 KDoc，包含 `@param`、`@return`
- **关键逻辑**：复杂业务必须添加行内注释解释意图
- **常量**：必须注释说明用途
- **sealed class/interface 子类**：每个子类必须注释含义
- **禁止**：无意义废话注释（如 `// 设置文本` 配合 `setText()`）

### 2.2 包结构规范

```
com.xxx.app/
├── App.kt                        // Application 入口
├── base/                         // ===== 基础架构层（跨项目复用） =====
│   ├── baseui/                   // UI 基础设施
│   │   ├── BaseViewModel.kt      // ViewModel 基类（UIStatus 管理）
│   │   ├── UIStatus.kt           // 页面状态密封类
│   │   └── baselivedata/         // LiveData/Flow 工具
│   ├── http/                     // 网络层封装（项目自建或引入网络库）
│   ├── utils/                    // 通用工具类（参考 `android-utils-core` 技能）
│   │   ├── toputils/             // 顶层扩展函数（Toast、JSON、时间、dp 等）
│   │   ├── ScreenUtils.kt        // 屏幕工具
│   │   ├── GlideUtils.kt         // 图片加载封装
│   │   ├── CopyUtils.kt          // 剪切板
│   │   ├── DateUtils.kt          // 日期时间
│   │   ├── FileUtils.kt          // 文件操作
│   │   ├── MD5Utils.kt           // 加密工具
│   │   └── ...                   // 其他工具
│   ├── storage/                  // 本地存储（参考 `android-local-storage` 技能）
│   │   ├── mmkv/                 // MMKV 封装
│   │   ├── room/                 // Room 数据库封装
│   │   └── cache/                // 通用缓存层
│   ├── view/                     // 通用自定义 View
│   │   └── StatusViewLayout.kt   // 传统 View 状态页容器
│   └── cache/                    // 缓存层
│       └── file/                 // 文件日志管理
├── manager/                      // ===== 全局管理器 =====
│   ├── log/                      // 日志系统
│   └── CrashHandler.kt          // 崩溃捕获
├── ui/                           // ===== UI 层（Compose） =====
│   ├── theme/                    // 主题、颜色、字体
│   ├── components/               // 通用 Compose 组件
│   │   ├── StatusContainer.kt    // Compose 状态页容器
│   │   ├── AppTopBar.kt          // 通用顶部栏
│   │   ├── AppImage.kt           // 通用图片组件
│   │   └── PaginatedList.kt      // 分页列表组件
│   └── navigation/               // 导航路由定义
├── feature/                      // ===== 业务功能模块 =====
│   ├── home/                     // 首页模块
│   │   ├── HomeScreen.kt         // Compose 页面
│   │   ├── HomeViewModel.kt      // ViewModel（含 Intent/UiState/Effect）
│   │   ├── HomeRepository.kt     // 数据仓库（可选）
│   │   └── model/                // 数据模型（可选）
│   ├── user/                     // 用户模块
│   └── setting/                  // 设置模块
└── model/                        // ===== 全局数据模型 =====
    ├── response/                 // API 响应基类
    │   ├── BaseResponse.kt
    │   └── BaseDataResponse.kt
    └── entity/                   // Room 实体
```

**关键原则**：
- `base/` 放基础架构和工具，跨项目可复用
- `feature/` 按业务功能分包，每个模块内聚
- `ui/components/` 放通用 Compose 组件
- `model/response/` 放 API 响应基类，业务模型跟随 feature

### 2.3 代码风格

- **缩进**: 4 空格
- **行宽**: 120 字符
- **换行符**: LF（Unix 风格）
- **代码风格**: Kotlin official（`kotlin.code.style=official`）
- **注释语言**: 中文
- **命名**:
  - 类名/接口名/枚举名/密封类：**PascalCase**（大驼峰），如 `HomeViewModel`、`UIStatus`、`NetworkState`
  - 函数/变量/参数：**camelCase**（小驼峰），如 `loadData`、`userName`、`pageNo`
  - 常量（`const val` / `companion object` 中）：**UPPER_SNAKE_CASE**，如 `MAX_RETRY_COUNT`、`PAGE_SIZE`
  - 资源文件/XML ID：**lowercase_underscores**，如 `ic_home_tab`、`tv_user_name`
  - Compose 组件函数：**PascalCase**（大驼峰，与类名一致），如 `HomeScreen`、`AppTopBar`、`StatusContainer`
  - 包名：**全小写**，如 `com.xq.video.social.feature.home`
  - 泛型参数：单个大写字母，如 `T`、`E`、`K`、`V`
- **导入**: 显式导入，禁止通配符，按组字母排序
- **SDK 兼容**: 新 API 必须用 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.XXX` 守卫

### 2.4 依赖管理规范 (Gradle Version Catalog)

为保证项目依赖版本统一，必须使用 Gradle Version Catalog (`libs.versions.toml`) 进行管理。

> **依赖版本统一见 `references/dependencies.md`**，各 Skill 不再重复定义版本号。

**配置示例（版本号仅供参考，实际使用 dependencies.md 中的版本）：**

```toml
[versions]
# 编译环境 — 详见 references/dependencies.md
compileSdk = "35"
minSdk = "23"
targetSdk = "35"
agp = "8.7.3"
kotlin = "2.0.21"
coreKtx = "1.12.0"
appcompat = "1.6.1"
material = "1.11.0"
lifecycle = "2.7.0"
composeBom = "2024.02.01"
activityCompose = "1.8.2"
navigationCompose = "2.7.7"
windowSizeClass = "1.2.0"

# Coroutines
coroutines = "1.7.3"

# Network
okhttp = "4.12.0"
gson = "2.10.1"

# Storage
room = "2.6.1"
mmkv = "1.3.4"

# Image Loading
glide = "4.16.0"
glideCompose = "1.0.0-beta01"

# Testing
junit = "4.13.2"
mockk = "1.13.8"
turbine = "1.0.0"

[libraries]
# Android & Jetpack
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-activity = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
compose-window-size = { group = "androidx.compose.material3", name = "material3-window-size-class", version.ref = "windowSizeClass" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Network
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }

# Storage
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
mmkv = { group = "com.tencent", name = "mmkv", version.ref = "mmkv" }

# Image Loading
glide-core = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
glide-compose = { group = "com.github.bumptech.glide", name = "compose", version.ref = "glideCompose" }

# Testing
# ...

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
# ...
```

**2. 在 `build.gradle.kts` 中使用类型安全的方式引用依赖：**

```kotlin
// app/build.gradle.kts
dependencies {
    // 统一导入 Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    
    // 其他依赖
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.mmkv)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

### Framework 规范（重要）

#### Compose 屏幕适配与页面模板

**统一参考** `android-mvi-compose` 技能文件，包含：
- 页面模板（MVI + Compose 完整示例）
- 屏幕适配规范（375 设计稿 + WindowSizeClass + ResponsiveScaffold）
- Scaffold 使用规范（禁止嵌套）
- 通用 UI 组件（TopBar、Loading、Error 等）

> 详细规范见 → `android-mvi-compose/compose-templates-and-components.md`

---

## 三、MVI 架构概览

MVI 架构的完整规范、基类实现和页面模板见 `android-mvi-compose` 技能文件。

**核心要点**：
- 数据流：View → Intent → ViewModel → State/Effect → View
- View → ViewModel：直接函数调用 `handleIntent()`
- ViewModel → View 状态：`StateFlow`（持续状态）
- ViewModel → View 事件：`Channel`（一次性事件，如导航、Toast 等）

> 详细实现见 → `android-mvi-compose/SKILL.md` 和 `android-mvi-compose/compose-templates-and-components.md`

---

## 四、AndroidManifest 通用配置

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- ==================== 常用权限 ==================== -->
    <!-- 网络 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- 存储（Android 13+ 细分权限） -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <!-- Android 12 及以下 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />

    <!-- 相机 -->
<!--    <uses-permission android:name="android.permission.CAMERA" />-->

    <!-- 定位 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- 通知（Android 13+） -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- 前台服务 -->
<!--    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />-->

    <!-- 振动 -->
    <uses-permission android:name="android.permission.VIBRATE" />

    <!--
        tools:replace 防止第三方库的 AndroidManifest 合并时覆盖本项目的重要属性
        只有本项目代码自己配置的值才生效，第三方库无法替换
    -->
    <application
        android:name=".App"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:largeHeap="true"
        android:networkSecurityConfig="@xml/network_security_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.App"
        android:localeConfig="@xml/locales_config"
        android:enableOnBackInvokedCallback="true"
        android:resizeableActivity="true"
        tools:replace="android:name,android:allowBackup,android:icon,android:label,android:theme,android:networkSecurityConfig">

        <!--
            支持的屏幕宽高比范围
            maxAspectRatio = 2.2：最大比例，覆盖 20:9 全面屏（如三星 S24 = 2.11）
            不设置 minAspectRatio：默认 1.33（4:3），兼容平板和折叠屏
            注意：大部分手机比例在 1.78~2.17 之间
        -->
        <meta-data android:name="android.max_aspect" android:value="2.2" />

        <!-- FileProvider（拍照/分享文件） -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

**network_security_config.xml**（`res/xml/`）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Debug 允许明文 HTTP（测试环境） -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
    <!-- Release 强制 HTTPS -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**file_paths.xml**（`res/xml/`）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="external_files" path="." />
    <cache-path name="cache" path="." />
    <files-path name="files" path="." />
</paths>
```

**构建环境**: JDK 21 / Compile SDK 35 / Target SDK 35 / **Min SDK 23（Android 6.0）** / Gradle 8.13 / AGP 8.13.0 / Kotlin 2.2.10

---

## 五、buildSrc 构建工具模块

`buildSrc/` 是 Gradle 预编译构建逻辑模块，用 Kotlin DSL 编写，提供版本管理和 APK 自动上传能力。

- **VersionConfig** — 基于 `version.properties` 的版本号自增管理、蒲公英 Key 读取
- **ApkUpLoadUtils** — 注册 Gradle 任务（`versionCode++`、`uploadPgyer`），构建后自动上传 APK 到蒲公英
- **UploadResponse** — 蒲公英 API 响应数据模型

> 详细说明见 → `android-build-publish` 技能文件

---

## 六、Skills 文件索引

| 技能 | 内容 |
|------|------|
| `android-mvi-compose` | MVI 完整模板（Intent/State/Effect/ViewModel/Compose 页面）、状态页组件、屏幕适配（375 设计稿 + ResponsiveScaffold）、通用 UI 组件、Compose 开发规范 |
| `android-local-storage` | MMKV 键值存储、Room 数据库、存储选型 |
| `android-utils-core` | toputils 工具函数、LiveData/Flow 工具、图片加载、日志系统、权限管理 |
| `android-advanced-dev` | Repository 模式、全局异常处理、多语言国际化、深色模式、Compose 动画、单元测试 |
| `android-build-publish` | buildSrc 模块、VersionConfig 版本管理、ApkUpLoadUtils 蒲公英上传、APK 命名规则 |
| `android-legacy-view` | Activity 启动模板、传统 View 状态页容器、View-Compose 互操作 |
