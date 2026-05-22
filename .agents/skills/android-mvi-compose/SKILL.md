---
name: android-mvi-compose
description: MVI 架构与 Compose 核心规范。创建页面、编写 ViewModel 时使用。
---

# MVI 架构与 Compose 开发规范

本文件包含 MVI 核心定义模板。**完整基类实现和状态页组件见附属文件**。

---

## 一、Compose 依赖配置

> **依赖版本统一见 `references/dependencies.md`**，本文件不再重复定义版本号。

在 `gradle/libs.versions.toml` 中定义（版本号见 dependencies.md）：
```toml
[versions]
# Compose 版本见 references/dependencies.md
compose = "2024.12.01"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "compose-material3" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle-viewmodel-compose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
```

在 `app/build.gradle.kts` 中：
```kotlin
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
}
```

---

## 二、MVI 基础设施

### 2.1 核心概念

| 组件 | 说明 |
|------|------|
| `MviPageStatus` | 页面框架状态密封接口 |
| `MviUiState<T>` | 通用状态容器（status + data） |
| `MviBaseViewModel` | ViewModel 基类 |

### 2.2 状态类型

- `Content` — 内容正常显示
- `FullScreenLoading` — 全屏加载
- `DialogLoading` — 弹窗加载
- `Error` — 错误状态
- `Empty` — 空状态

> **完整基类实现见 [MviInfrastructure.kt](MviInfrastructure.kt)**

---

## 三、Intent/PageData/Effect 定义模板

### 3.1 Intent（用户意图）

```kotlin
sealed interface UserListIntent {
    data object LoadData : UserListIntent
    data object Refresh : UserListIntent
    data class Search(val query: String) : UserListIntent
    data class Delete(val userId: String) : UserListIntent
}
```

### 3.2 PageData（页面专属数据）

```kotlin
data class UserListData(
    val isRefreshing: Boolean = false,
    val userList: List<UserData> = emptyList(),
    val searchQuery: String = ""
) {
    val isEmpty: Boolean get() = userList.isEmpty()
}
```

### 3.3 Effect（一次性副作用）

```kotlin
sealed interface UserListEffect {
    data class NavigateToDetail(val userId: String) : UserListEffect
    data object NavigateBack : UserListEffect
    data object ScrollToTop : UserListEffect
}
// 注意：Toast 直接在 ViewModel 调用 "msg".show()
```

---

## 四、ViewModel 模板

```kotlin
class UserListViewModel : MviBaseViewModel<UserListIntent, UserListData, UserListEffect>(UserListData()) {

    override fun handleIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadData -> loadData(isRefresh = false)
            is UserListIntent.Refresh -> loadData(isRefresh = true)
            is UserListIntent.Search -> searchUser(intent.query)
            is UserListIntent.Delete -> deleteUser(intent.userId)
        }
    }

    private fun loadData(isRefresh: Boolean) {
        launchTryViewModelScope {
            if (isRefresh) {
                setState { it.copy(data = it.data.copy(isRefreshing = true)) }
            } else {
                showFullScreenLoading("正在加载...")
            }
            // 网络请求...
        }
    }
}
```

---

## 五、Compose 状态页组件

| 组件 | 用途 |
|------|------|
| `LoadingScreen` | 全屏加载页 |
| `EmptyScreen` | 空数据页（支持重试） |
| `ErrorScreen` | 错误页（支持重试） |
| `LoadingDialog` | 弹窗加载 |

> **完整组件实现见 [ComposeStatusComponents.kt](ComposeStatusComponents.kt)**

---

## 六、页面状态渲染示例

```kotlin
@Composable
fun UserListPage(viewModel: UserListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.status) {
        MviPageStatus.Content -> UserListContent(uiState.data)
        MviPageStatus.FullScreenLoading -> LoadingScreen()
        MviPageStatus.DialogLoading -> LoadingDialog(uiState.status.text)
        is MviPageStatus.Error -> ErrorScreen(
            msg = uiState.status.message,
            onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
        )
        is MviPageStatus.Empty -> EmptyScreen(
            msg = uiState.status.message,
            onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
        )
    }
}
```

---

## 七、更多 Compose 资源

**页面模板、屏幕适配、Scaffold 规范、通用 UI 组件见 [compose-templates-and-components.md](compose-templates-and-components.md)**（这是编写 Compose 页面的主入口文件）。