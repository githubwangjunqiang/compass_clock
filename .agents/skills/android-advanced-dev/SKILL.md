---
name: android-advanced-dev
description: 进阶开发规范。Repository、异常处理、多语言、深色模式、动画、单元测试。
---

# 进阶开发规范

## 附属参考文件目录

| 文件 | 内容 |
|------|------|
| [CrashHandler.kt](CrashHandler.kt) | 全局崩溃捕获完整实现、协程异常处理器 |
| [RepositoryTemplate.kt](RepositoryTemplate.kt) | Repository 模板完整代码、ViewModel 使用示例 |
| [ThemeConfig.kt](ThemeConfig.kt) | Material3 主题色方案、深色模式适配代码 |
| [testing.md](testing.md) | 单元测试规范 |

---

## 一、Repository 模式

### 1.1 什么时候需要 Repository

| 场景 | 是否需要 Repository |
|------|-------------------|
| 简单的单接口 CRUD 页面 | 不需要，ViewModel 直接调 HTTP |
| 多数据源合并（网络 + 缓存 + 数据库） | **需要** |
| 多个 ViewModel 共享同一业务逻辑 | **需要** |
| 需要复杂数据转换/映射 | **需要** |
| 离线优先策略 | **需要** |

### 1.2 Repository 核心职责

- 统一管理数据获取、缓存、持久化
- ViewModel 不直接调用网络层
- 返回 `Result<T>` 类型，便于处理成功/失败

> 完整模板见 [RepositoryTemplate.kt](RepositoryTemplate.kt)

---

## 二、全局异常处理

### 2.1 崩溃捕获初始化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init()  // 崩溃捕获
        MMKV.initialize(this)
    }
}
```

### 2.2 功能列表

| 功能 | 说明 |
|------|------|
| 崩溃日志记录 | 自动记录时间、线程、异常栈到文件 |
| 本地保存 | 保存到 MMKV，最多 50 条记录 |
| 崩溃列表读取 | `CrashHandler.getCrashList()` |
| 清空记录 | `CrashHandler.clearCrashList()` |
| 协程异常处理 | `globalCoroutineExceptionHandler` |

> 完整实现见 [CrashHandler.kt](CrashHandler.kt)

---

## 三、多语言 / 国际化

### 3.1 基本规范

- **禁止**在代码中硬编码中文字符串用于 UI 显示
- 所有用户可见文本必须放在 `res/values/strings.xml`

```xml
<!-- res/values/strings.xml（默认/英文） -->
<string name="loading">Loading...</string>
<string name="empty_data">No data available</string>

<!-- res/values-zh/strings.xml -->
<string name="loading">加载中...</string>
<string name="empty_data">暂无数据</string>
```

### 3.2 Compose 中使用

```kotlin
Text(text = stringResource(R.string.empty_data))
```

### 3.3 Android 13+ 应用内语言设置

```xml
<!-- res/xml/locales_config.xml -->
<locale-config>
    <locale android:name="en" />
    <locale android:name="zh" />
</locale-config>
```

### 3.4 RTL 布局适配

- `AndroidManifest.xml` 中 `android:supportsRtl="true"`
- 使用 `start/end` 代替 `left/right`
- Compose 中使用 `Arrangement.Start` 而非 `Arrangement.Left`

---

## 四、深色模式适配

### 4.1 Application 初始化

```kotlin
setContent {
    AppTheme {
        // 应用内容
    }
}
```

### 4.2 使用规范

| 规范 | 示例 |
|------|------|
| 使用主题颜色 | `MaterialTheme.colorScheme.onSurface` |
| 禁止硬编码颜色 | ❌ `Color(0xFF000000)` |
| 图片适配 | `Modifier.alpha(if (isSystemInDarkTheme()) 0.8f else 1f)` |

> 完整实现见 [ThemeConfig.kt](ThemeConfig.kt)

---

## 五、Compose 动画规范

### 5.1 常用动画

| 动画类型 | 代码示例 |
|----------|----------|
| 显隐动画 | `AnimatedVisibility(visible = isVisible) { Text("内容") }` |
| 尺寸变化 | `Modifier.animateContentSize()` |
| 属性动画 | `animateDpAsState(targetValue = if (expanded) 0.dp else (-100).dp)` |
| 无限循环 | `rememberInfiniteTransition().animateFloat(...)` |

### 5.2 页面切换动画

```kotlin
NavHost(
    enterTransition = { slideInHorizontally { it } + fadeIn() },
    exitTransition = { slideOutHorizontally { -it } + fadeOut() },
    popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
    popExitTransition = { slideOutHorizontally { it } + fadeOut() }
) { ... }
```

### 5.3 列表 Item 动画

```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->
        ItemCard(item, modifier = Modifier.animateItem())
    }
}