# Android 依赖版本参考

本文件提供依赖库选型建议。**优先使用最新稳定版本**，遇到兼容性问题再降级。

---

## 版本选型建议

| 库 | 建议 | 降级备选 | 说明 |
|----|------|----------|------|
| compose-bom | 最新稳定版 | `2024.12.01` | Compose 组件统一版本管理 |
| compose-material3 | 最新稳定版 | `1.3.1` | Material 3（BOM 外独立版本） |
| lifecycle | 最新稳定版 | `2.8.7` | ViewModel、Lifecycle、LiveData |
| navigation-compose | 最新稳定版 | `2.8.5` | Compose Navigation |
| room | 最新稳定版 | `2.6.1` | Room 数据库 |
| mmkv | 最新稳定版 | `1.3.4` | 腾讯 MMKV |
| okhttp | 最新稳定版 | `4.12.0` | OkHttp 网络库 |
| gson | 最新稳定版 | `2.10.1` | Google Gson |
| glide | 最新稳定版 | `4.16.0` | Glide 图片加载 |
| coroutines | 最新稳定版 | `1.9.0` | Kotlin 协程 |
| kotlin | 最新稳定版 | `2.0.21` | Kotlin 编译器 |

> **降级备选**：遇到兼容性问题时的参考版本（曾验证稳定）

---

## 测试库

| 库 | 建议 |
|----|------|
| junit | 最新稳定版 |
| mockk | 最新稳定版 |
| turbine | 最新稳定版 |

---

## libs.versions.toml 模板

```toml
[versions]
# 编译环境（根据项目需要调整）
agp = "最新稳定版"
kotlin = "最新稳定版"
ksp = "匹配 kotlin 版本"
compileSdk = "35"
targetSdk = "35"
minSdk = "23"

# Jetpack Compose（使用最新 BOM）
compose-bom = "最新稳定版"
compose-material3 = "最新稳定版"

# 其他 Jetpack
lifecycle = "最新稳定版"
navigation-compose = "最新稳定版"
room = "最新稳定版"

# Coroutines
coroutines = "最新稳定版"

# Network
okhttp = "最新稳定版"
gson = "最新稳定版"

# Storage
mmkv = "最新稳定版"

# Image
glide = "最新稳定版"

[libraries]
# Compose（BOM 管理）
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "compose-material3" }

# Lifecycle
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

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

# Image
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
glide-compose = { group = "com.github.bumptech.glide", name = "compose", version.ref = "glide" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

---

## 版本查询方式

```bash
# compose-bom 最新版本
https://developer.android.com/jetpack/compose/setup

# kotlinx-coroutines 最新版本
https://github.com/Kotlin/kotlinx.coroutines/releases

# OkHttp 最新版本
https://github.com/square/okhttp/releases

# Room/Lifecycle 最新版本
https://developer.android.com/jetpack/androidx/releases
```