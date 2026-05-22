# Compose 页面模板与通用 UI 组件

本文件是 android-mvi-compose 技能的主入口文件，包含 **Compose 页面模板、屏幕适配、Scaffold 规范、通用 UI 组件** 的完整实现。

> ## 使用说明（AI 必须遵守）
>
> 当用户需要编写 Compose 页面时，必须按以下顺序参考本文件：
>
> 1. **先看屏幕适配规范（第零章）** - 理解 375 设计稿适配规则和 ResponsiveScaffold
> 2. **再看页面模板（第一章）** - 复制 `UserListScreen` 完整模板作为起点
> 3. **遵守 Scaffold 规范（3.6 节）** - 禁止嵌套使用
> 4. **最后选 UI 组件（第二章）** - 从通用组件库中选择需要的
>
> **禁止从其他零散位置复制代码，确保代码风格统一。**

---

## 零、屏幕适配规范（375 设计稿 + WindowSizeClass）

### 0.1 核心思路

使用 Google 官方 `WindowSizeClass`（Material3）根据屏幕宽度分档，适配各种设备屏幕。

#### 为什么 375 设计稿可以直接使用 dp？

| 设备类型 | 屏幕宽度 (dp) | 与 375 差距 |
|------|:---:|:---:|
| iPhone SE / 小屏安卓 | 320~360 dp | -4% ~ -15% |
| 主流安卓（大多数） | 360~412 dp | -4% ~ +10% |
| 大屏安卓 / 折叠屏内屏 | 412~600 dp | +10% ~ +60% |

**结论**：主流手机 360~412dp 与设计稿 375dp 差距仅 ±10%，Compose Direct 布局系统可以直接使用设计稿 dp 标注值，无明显变形。大屏/折叠屏/平板可通过 `WindowSizeClass` 做响应式布局。

### 0.2 依赖配置

在 `gradle/libs.versions.toml` 中定义：
```toml
[versions]
windowSizeClass = "1.2.0"

[libraries]
compose-window-size = { group = "androidx.compose.material3", name = "material3-window-size-class", version.ref = "windowSizeClass" }
```
在 `app/build.gradle.kts` 中添加依赖：
```kotlin
dependencies {
    implementation(libs.compose.window.size)
}
```

### 0.3 ResponsiveScaffold（屏幕适配封装，推荐使用）

对 Scaffold 的封装，处理不同屏幕下的布局：

```kotlin
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.activity.ComponentActivity

/**
 * 响应式 Scaffold 组件
 * - 手机竖屏：全屏显示，使用设计稿标注值
 * - 大屏/平板：限制内容宽度并居中显示（max = 600.dp）
 */
@Composable
fun ResponsiveScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass(LocalContext.current as ComponentActivity)

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                topBar()
            }
        },
        bottomBar = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                bottomBar()
            }
        },
        floatingActionButton = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                floatingActionButton()
            }
        }
    ) { padding ->
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                // 手机竖屏：全屏内容
                content(padding)
            }
            else -> {
                // 大屏：限制内容宽度并居中
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                    ) {
                        content(PaddingValues(20.dp))
                    }
                }
            }
        }
    }
}
```

### 0.4 开发技巧

```kotlin
// ✅ 直接使用设计稿标注值
@Composable
fun UserCard() {
    Column {
        Modifier
            .fillMaxWidth()
            .height(120.dp)           // 设计稿高120dp就写120dp
            .padding(horizontal = 16.dp, vertical = 12.dp)  // 设计稿标注就直接用

        Text(
            text = "标题",
            fontSize = 16.sp,      // 设计稿16sp就用16sp
        )
    }
}

// ✅ 用 ResponsiveScaffold 作为页面根布局
@Composable
fun MainScreen() {
    ResponsiveScaffold(
        topBar = { TopBar() },
        bottomBar = { BottomBar() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // 处理状态栏等安全区域
            contentPadding = PaddingValues(16.dp)  // 设计稿间距
        ) {
            items(data) { item ->
                UserCard(item = item)  // 内部仍然使用设计稿尺寸
            }
        }
    }
}
```

### 0.5 375 设计稿适配速查表

| 场景 | 方案 |
|------|------|
| 手机竖屏 | 直接用设计稿 dp/sp 标注值 |
| 宽度 | 优先 `fillMaxWidth()` + 限制 `widthIn(max=xxx.dp)` + 居中排列 |
| 图片/卡片比例 | 用 `aspectRatio` 保持长宽比 |
| 列表网格 | 用 `GridCells.Adaptive` 自适应列数 |
| 大屏/平板 | 响应式适配 + `widthIn(max=600.dp).align(Alignment.TopCenter)` 限制最大宽度居中 |

---

## 一、Compose 页面模板（使用 ResponsiveScaffold）

```kotlin
/**
 * 用户列表页面
 * 集成了响应式布局 ResponsiveScaffold，确保屏幕适配
 */
@Composable
fun UserListScreen(
    viewModel: UserListViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    // 1. 状态和 Effect 的监听代码
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pageData = state.data // 页面专属数据

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserListEffect.NavigateToDetail -> onNavigateToDetail(effect.userId)
                is UserListEffect.NavigateBack -> onNavigateBack()
                is UserListEffect.ScrollToTop -> { /* 滚动到顶部 */ }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(UserListIntent.LoadData)
    }

    // 2. 使用 ResponsiveScaffold 作为页面根布局，自动处理屏幕适配
    ResponsiveScaffold(
        topBar = {
            AppTopBar(
                title = "用户列表",
                onBack = onNavigateBack
            )
        }
    ) { padding -> // ResponsiveScaffold 会提供正确的内边距

        // 3. Box 用于处理 DialogLoading 的叠加
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // 使用来自 Scaffold 的 padding
        ) {
            // 4. 根据页面状态显示不同内容
            when (val status = state.status) {
                is MviPageStatus.Content,
                is MviPageStatus.DialogLoading -> UserListContent( // DialogLoading时也显示内容
                    pageData = pageData,
                    onIntent = viewModel::handleIntent
                )
                is MviPageStatus.FullScreenLoading -> LoadingScreen(text = status.text)
                is MviPageStatus.Error -> ErrorScreen(
                    msg = status.message,
                    iconRes = status.icon,
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
                is MviPageStatus.Empty -> EmptyScreen(
                    msg = status.message,
                    iconRes = status.icon,
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
            }

            // 5. 单独处理 DialogLoading 的叠加显示
            if (state.status is MviPageStatus.DialogLoading) {
                LoadingDialog(msg = (state.status as MviPageStatus.DialogLoading).text)
            }
        }
    }
}

### 1.1 内容组件模板 `UserListContent`

`UserListScreen` 在显示正常内容时，会将页面专属数据 `pageData` 传递给 `UserListContent`。这个组件负责渲染页面的核心 UI。

**重要：内容组件不再使用 Scaffold，直接使用父组件传入的 Modifier，避免边距翻倍问题（详见 3.6 节）。**

```kotlin
/**
 * 用户列表页面的内容区域
 * @param pageData 页面专属数据
 * @param onIntent 向 ViewModel 发送用户意图
 */
@Composable
fun UserListContent(
    pageData: UserListData,
    onIntent: (UserListIntent) -> Unit
) {
    // 示例：使用下拉刷新和列表
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            onIntent(UserListIntent.Refresh)
        }
    }
    // 当 ViewModel 处理完刷新逻辑后，isRefreshing 会变为 false
    if (!pageData.isRefreshing) {
        LaunchedEffect(Unit) {
            pullRefreshState.endRefresh()
        }
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = pageData.isRefreshing,
        onRefresh = { pullRefreshState.startRefresh() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 搜索栏 (示例)
            item {
                SearchBar(
                    query = pageData.searchQuery,
                    onQueryChange = { query -> onIntent(UserListIntent.Search(query)) }
                )
            }
            
            // 用户列表
            items(items = pageData.userList, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    onDelete = { onIntent(UserListIntent.Delete(user.id)) }
                )
            }
        }
    }
}

/**
 * 单个用户卡片 (示例)
 */
@Composable
fun UserCard(user: UserData, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ... 显示用户头像、名称等 ...
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除用户")
            }
        }
    }
}
```
---

## 二、Compose 通用 UI 组件

### 2.1 通用 TopBar

```kotlin
/**
 * 通用页面顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        actions = actions
    )
}
```

### 2.2 通用确认弹窗

```kotlin
/**
 * 通用确认弹窗
 */
@Composable
fun ConfirmDialog(
    title: String,
    content: String,
    confirmText: String = "确定",
    cancelText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelText) } }
    )
}
```

### 2.3 通用 BottomSheet

```kotlin
/**
 * 通用底部弹出面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                HorizontalDivider()
            }
            content()
        }
    }
}
```

### 2.4 通用搜索栏

```kotlin
/**
 * 通用搜索输入框
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
    onSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(query) })
    )
}
```

---

## 三、Compose 开发规范

### 3.1 Navigation（类型安全路由）

```kotlin
// Compose Navigation 2.8+，使用 Kotlin Serialization 定义路由
@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: String)

NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> { HomeScreen(navController) }
    composable<DetailRoute> { backStackEntry ->
        DetailScreen(id = backStackEntry.toRoute<DetailRoute>().id)
    }
}

navController.navigate(DetailRoute(id = "123"))
```

### 3.2 Compose 与 View 互操作

```kotlin
// Compose 中嵌入传统 View（如 ExoPlayer）
AndroidView(
    factory = { context -> PlayerView(context) },
    modifier = Modifier.fillMaxSize(),
    update = { playerView -> playerView.player = exoPlayer }
)

// 传统 Activity/Fragment 中嵌入 Compose
setContent { MaterialTheme { XxxScreen() } }
```

### 3.3 主题与样式

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```

### 3.4 性能优化要点

```kotlin
// 1. LazyColumn 必须提供 key
items(items = list, key = { it.id }) { item -> ItemView(item) }

// 2. remember 缓存计算结果
val formatted = remember(timestamp) { timestamp.format("yyyy-MM-dd") }

// 3. derivedStateOf 减少重组
val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

// 4. Modifier.drawBehind 替代 background 减少重组
Modifier.drawBehind { drawRect(color) }
```

### 3.5 Scaffold 使用规范

**原则：Scaffold 组件仅在页面入口级组件中使用，禁止嵌套使用。**

#### 原因
Scaffold 提供的 `paddingValues` 参数已包含状态栏、导航栏等安全区域的内边距。如果多层组件都使用 Scaffold 并应用该 padding，会导致边距重复叠加，造成界面布局异常（如顶部出现双倍状态栏高度空白）。

#### 正确用法
```kotlin
@Composable
fun UserListScreen() {
    // ✅ 只在入口组件使用 Scaffold
    Scaffold(
        topBar = { AppTopBar(title = "用户列表") }
    ) { paddingValues ->
        // 将 padding 传递给内容区域
        UserListContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun UserListContent(modifier: Modifier = Modifier) {
    // ✅ 子组件不再使用 Scaffold，直接使用传入的 modifier
    LazyColumn(modifier = modifier) {
        // ...
    }
}
```

#### 错误示例
```kotlin
@Composable
fun UserListScreen() {
    Scaffold { padding ->  // 第一层 padding
        UserListContent(Modifier.padding(padding))
    }
}

@Composable
fun UserListContent(modifier: Modifier) {
    // ❌ 错误：子组件又嵌套 Scaffold
    Scaffold(modifier = modifier) { innerPadding ->  // 第二层 padding，导致边距翻倍
        LazyColumn(modifier = Modifier.padding(innerPadding)) { }
    }
}
```

### 3.6 数据流选型

| 类型 | 方向 | 用途 | 使用场景 |
|------|------|------|----------|
| 直接函数调用 | View → VM | Intent | `handleIntent()` |
| `StateFlow` | VM → View | 持续状态 | `uiState` |
| `Channel` | VM → View | 一次性事件 | `effect`（导航、滚动等） |
| `SharedFlow` | 跨模块 | 广播事件 | 全局事件总线 |

---

## 四、反模式与禁止事项（重要）

> **核心原则**：反模式是具体可执行的"禁止做 X"，比抽象的正面规范更有效。

### 4.1 协程与异步反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| `GlobalScope.launch { ... }` | 内存泄漏，无法随 ViewModel 销毁而取消 | `viewModelScope.launch { ... }` |
| `lifecycleScope.launch { ... }` 在 View 中 | 无法感知页面状态，可能后台执行 | 用 ViewModel 的 `viewModelScope` 或 `repeatOnLifecycle` |
| `Thread { ... }.start()` | 无法取消，资源浪费 | `withContext(Dispatchers.Default) { ... }` |

```kotlin
// ❌ 反模式：内存泄漏
GlobalScope.launch {
    fetchData()  // ViewModel 销毁后协程仍在执行
}

// ✅ 正确：跟随 ViewModel 生命周期
viewModelScope.launch {
    fetchData()  // ViewModel 销毁时自动取消
}
```

### 4.2 ViewModel 设计反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| ViewModel 直接持有 `Context` | 内存泄漏，Context 生命周期比 VM 长 | `AndroidViewModel.application` 或 `SavedStateHandle` |
| ViewModel 存 `Activity`/`Fragment` 引用 | 内存泄漏，严重崩溃风险 | 通过参数传递，不持有引用 |
| `_binding` 未在 `onDestroyView` 置空 | Fragment View 销毁后访问崩溃 | 必须置空 |

```kotlin
// ❌ 反模式：ViewModel 存 Context
class UserViewModel(private val context: Context) : ViewModel() {
    // context 生命周期可能比 VM 长，导致泄漏
}

// ✅ 正确：用 AndroidViewModel
class UserViewModel(application: Application) : AndroidViewModel(application) {
    // application 是全局单例，不会泄漏
}

// ✅ 正确：用 SavedStateHandle（推荐）
class UserViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val userId: String? = savedState["user_id"]
}
```

### 4.3 Fragment ViewBinding 反模式

```kotlin
// ❌ 反模式：未置空导致崩溃
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!  // onDestroyView 后访问会崩溃

    override fun onDestroyView() {
        super.onDestroyView()
        // 缺少：_binding = null
    }
}

// ✅ 正确：必须置空
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // 必须置空，防止 View 销毁后访问
    }
}
```

### 4.4 数据层设计反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| Repository 返回 `LiveData<User>` | 耦合 UI 层，无法在非 UI 场景复用 | `suspend fun getUser(): Result<User>` 或 `Flow<User>` |
| Dao 返回 `LiveData<List<Entity>>` | 同上，耦合 LiveData | `suspend fun getAll(): List<Entity>` 或 `Flow<List<Entity>>` |
| Repository 直接调用 `show()` Toast | 跨层调用 UI，违反架构原则 | 返回错误信息，由 ViewModel/View 处理 |

```kotlin
// ❌ 反模式：Repository 返回 LiveData
class UserRepository {
    fun getUser(id: String): LiveData<User> {  // 耦合 UI 层
        // ...
    }
}

// ✅ 正确：返回 suspend fun 或 Flow
class UserRepository {
    suspend fun getUser(id: String): Result<User> = runCatching {
        // ...
    }

    fun userFlow(id: String): Flow<User> = flow {
        // ...
    }
}
```

### 4.5 SharedPreferences 反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| 主线程调用 `prefs.edit().commit()` | 阻塞主线程，导致 ANR | `prefs.edit().apply()` 或用 MMKV |
| 存储 大 JSON/列表 到 SP | 性能差，读取慢 | MMKV 或 Room |

```kotlin
// ❌ 反模式：主线程 commit 导致 ANR
prefs.edit().putString("token", token).commit()  // 同步阻塞主线程

// ✅ 正确：异步 apply
prefs.edit().putString("token", token).apply()  // 异步写入

// ✅ 推荐：使用 MMKV（项目统一方案）
MMKVUtil.put("token", token)
```

### 4.6 Compose 性能反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| `Modifier.background(Color.Red)` | 每次重组都创建新对象 | `Modifier.drawBehind { drawRect(Color.Red) }` |
| LazyColumn 无 `key` 参数 | 列表更新时全量重组 | `items(list, key = { it.id })` |
| `@Composable` 函数内大量计算 | 每次重组都重新计算 | `remember` 缓存计算结果 |

```kotlin
// ❌ 反模式：无 key 导致全量重组
LazyColumn {
    items(userList) { user ->  // 缺少 key，列表变化时全部重组
        UserCard(user)
    }
}

// ✅ 正确：提供 key，只重组变化的项
LazyColumn {
    items(userList, key = { it.id }) { user ->
        UserCard(user)
    }
}
```

### 4.7 协程挂起函数转换反模式

| ❌ 反模式 | 风险 | ✅ 正确替代 |
|----------|------|-------------|
| `suspendCoroutine` + `resumeWith` | 不支持取消，协程取消后回调仍执行导致崩溃 | `suspendCancellableCoroutine` + `tryResumeYourself` |
| 混用 `tryResume` 和 `resumeWith` | 重复 resume 崩溃 | 只用 `tryResumeYourself` |

```kotlin
// ❌ 反模式：suspendCoroutine 不安全
suspend fun getUser(id: String): User? = suspendCoroutine { cont ->
    api.getUser(id) { user ->
        cont.resumeWith(Result.success(user))  // 协程取消后仍会执行，可能崩溃
    }
}

// ✅ 正确：suspendCancellableCoroutine + tryResumeYourself
suspend fun getUser(id: String): User? = suspendCancellableCoroutine { cont ->
    api.getUser(id) { user ->
        cont.tryResumeYourself(user)  // 安全恢复，已取消时不会执行
    }
}
```

> 详细实现见 `android-utils-core/data-flow-tools.md`
