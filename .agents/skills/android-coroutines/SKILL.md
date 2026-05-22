---
name: android-coroutines
description: Kotlin 协程规范。协程提供者、作用域管理、异常处理、线程切换。
---

# Kotlin 协程开发规范

---

## 一、协程提供者简介

Android 协程作用域通常与生命周期绑定（`lifecycleScope`、`viewModelScope`）。但在**非生命周期组件**中需要手动创建作用域：

- 工具类（Toast、文件操作）
- 单例管理器、Repository 层、全局监听器

**CoroutineProvider** 为这些场景提供统一的全局协程作用域。

> **完整实现见 [CoroutineProvider.kt](CoroutineProvider.kt)**

---

## 二、作用域选型

| 场景 | 推荐方式 | 说明 |
|------|----------|------|
| Activity/Fragment | `lifecycleScope` | 随生命周期自动取消 |
| ViewModel | `launchTryViewModelScope()` | 安全启动，详见 `android-utils-core` |
| 工具类/单例 | `launchIO()` | 全局作用域，需手动取消 |
| UI 操作 | `launchUI()` | Main 线程 |
| CPU 密集型 | `launchCPU()` | Default 线程 |

> **ViewModel 协程扩展函数**见 `android-utils-core` skill 的 `data-flow-tools.md`

---

## 三、安全启动方法（推荐优先使用）

| 方法 | 用途 | 线程 |
|------|------|------|
| `launchUI { }` | UI 操作（Toast、更新状态） | Main |
| `launchIO { }` | 网络请求、文件读写、数据库 | IO |
| `launchCPU { }` | JSON 解析、数据排序、复杂计算 | Default |

### 使用规范

```kotlin
// ✅ 推荐：使用安全启动方法（自动 try-catch）
CoroutineProvider.launchUI { Toast.makeText(ctx, "提示", Toast.LENGTH_SHORT).show() }
CoroutineProvider.launchIO { val data = downloadFile(url) }

// ✅ 返回 Job，可用于取消控制
val job = CoroutineProvider.launchIO {
    while (true) { ensureActive(); processChunk() }
}
job.cancel()
```

### 例外场景（极少）

仅当需要 `async` 返回 `Deferred` 时才直接使用属性：

```kotlin
val deferred = CoroutineProvider.ioScope.async { fetchRemoteData() }
val result = deferred.await()
```

---

## 四、异常处理

安全方法内部自动 try-catch，异常不会传播导致协程崩溃。

---

## 五、线程切换

```kotlin
// IO → UI
CoroutineProvider.launchIO {
    val data = apiService.fetchData()
    CoroutineProvider.launchUI { showResult(data) }
}

// 或 withContext 等待结果
CoroutineProvider.launchUI {
    showLoading()
    val result = withContext(Dispatchers.IO) { repository.upload(file) }
    hideLoading()
}
```

---

## 六、取消规范

```kotlin
// ✅ ensureActive 检查取消
suspend fun processList(items: List<Item>) = items.forEach { ensureActive(); processItem(it) }

// ✅ yield 让出执行权
suspend fun heavyLoop() = while (true) { yield(); compute() }
```

---

## 七、常见陷阱

| 陷阱 | 解决方案 |
|------|----------|
| `runBlocking` 阻塞主线程 | 禁止主线程使用 |
| `GlobalScope` 无法管理 | 禁止，用 CoroutineProvider |
| 未捕获异常取消父协程 | 使用安全启动方法 |
| 协程泄漏 | 配合生命周期或手动取消 |

---

## 八、快速参考

```kotlin
// 工具类中启动协程
CoroutineProvider.launchUI { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
CoroutineProvider.launchIO { downloadFile(url) }
CoroutineProvider.launchCPU { parseJson(largeString) }

// 需要取消控制
val job = CoroutineProvider.launchIO { while (true) { ensureActive(); task() } }
job.cancel()

// 单例管理器
object CacheManager {
    fun preload(urls: List<String>) = CoroutineProvider.launchIO {
        urls.forEach { url -> ensureActive(); download(url) }
    }
}
```