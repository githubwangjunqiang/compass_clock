# 单元测试规范

本文件是 android-advanced-dev 技能的附属参考文件，包含 ViewModel 单元测试模板、测试辅助工具和命名规范。

## 六、单元测试规范

### 6.1 测试依赖

在 `gradle/libs.versions.toml` 中定义：
```toml
[versions]
junit = "4.13.2"
mockk = "1.13.8"
turbine = "1.0.0"
archCoreTesting = "2.2.0"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-arch-core-testing = { group = "androidx.arch.core", name = "core-testing", version.ref = "archCoreTesting" }
```
在 `app/build.gradle.kts` 中添加依赖：
```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test) // 假设已在 toml 中定义
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
}
```

### 6.2 ViewModel 测试模板

```kotlin
/**
 * 用户列表 ViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    /** 替换主线程调度器为测试调度器 */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Mock 依赖 */
    private val repository: UserRepository = mockk()

    /** 被测对象 */
    private lateinit var viewModel: UserListViewModel

    @Before
    fun setup() {
        viewModel = UserListViewModel(repository)
    }

    @Test
    fun `加载数据成功时应更新列表状态`() = runTest {
        // Given - 模拟网络返回
        val mockUsers = listOf(
            UserInfoData(userId = "1", nickname = "张三"),
            UserInfoData(userId = "2", nickname = "李四")
        )
        coEvery { repository.getUserList(1, 20) } returns Result.success(mockUsers)

        // When - 触发加载
        viewModel.handleIntent(UserListIntent.LoadData)
        advanceUntilIdle() // 等待协程执行完毕

        // Then - 验证状态
        val state = viewModel.uiState.value
        assertEquals(2, state.userList.size)
        assertEquals("张三", state.userList[0].nickname)
        assertEquals(false, state.isLoadingMore)
    }

    @Test
    fun `加载数据失败时应显示错误状态`() = runTest {
        // Given
        coEvery { repository.getUserList(any(), any()) } returns
            Result.failure(Exception("网络异常"))

        // When
        viewModel.handleIntent(UserListIntent.LoadData)
        advanceUntilIdle()

        // Then - 验证 UIStatus 变为 Error
        val uiStatus = viewModel.uiStatus.value
        assertTrue(uiStatus is UIStatus.Error)
    }

    @Test
    fun `删除用户后应从列表中移除并发送 Toast 事件`() = runTest {
        // Given - 先加载列表
        val mockUsers = listOf(
            UserInfoData(userId = "1", nickname = "张三"),
            UserInfoData(userId = "2", nickname = "李四")
        )
        coEvery { repository.getUserList(1, 20) } returns Result.success(mockUsers)
        viewModel.handleIntent(UserListIntent.LoadData)
        advanceUntilIdle()

        // Mock 删除接口
        coEvery { repository.deleteUser("1") } returns Result.success(Unit)

        // When - 执行删除
        viewModel.handleIntent(UserListIntent.Delete("1"))
        advanceUntilIdle()

        // Then - 验证列表更新（删除后只剩1条）
        assertEquals(1, viewModel.uiState.value.userList.size)
        assertEquals("2", viewModel.uiState.value.userList[0].userId)
        // 注意：Toast 直接在 VM 中 .show()，不走 Effect，无需测试
    }

    @Test
    fun `分页加载应追加数据而非替换`() = runTest {
        // Given - 首页数据
        val page1 = (1..20).map { UserInfoData(userId = "$it", nickname = "用户$it") }
        val page2 = (21..30).map { UserInfoData(userId = "$it", nickname = "用户$it") }

        coEvery { repository.getUserList(1, 20) } returns Result.success(page1)
        coEvery { repository.getUserList(2, 20) } returns Result.success(page2)

        // When - 加载第一页
        viewModel.handleIntent(UserListIntent.LoadData)
        advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.userList.size)

        // When - 加载第二页
        viewModel.handleIntent(UserListIntent.LoadMore)
        advanceUntilIdle()

        // Then - 数据追加
        assertEquals(30, viewModel.uiState.value.userList.size)
        assertEquals(2, viewModel.uiState.value.pageNo)
        // 第二页不足 20 条，hasMore 为 false
        assertEquals(false, viewModel.uiState.value.hasMore)
    }
}
```

### 6.3 MainDispatcherRule（测试辅助）

```kotlin
/**
 * JUnit Rule：将协程主调度器替换为测试调度器
 * 所有 ViewModel 测试都需要使用
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### 6.4 测试命名规范

- 使用反引号 + 中文描述测试场景
- 格式：`` `当xxx时应该xxx` ``
- 遵循 Given-When-Then 结构

```kotlin
@Test fun `加载数据成功时应更新列表状态`() { }
@Test fun `加载数据失败时应显示错误状态`() { }
@Test fun `空列表时应显示空数据页面`() { }
@Test fun `搜索防抖300ms内仅触发一次请求`() { }
```

### 6.5 运行测试

```bash
# 全部单元测试
./gradlew test

# 指定变体
./gradlew testGoogleDebugUnitTest

# 指定测试类
./gradlew :app:testGoogleDebugUnitTest --tests "com.xq.video.social.ui.vm.UserListViewModelTest"

# 指定单个测试方法
./gradlew :app:testGoogleDebugUnitTest --tests "*.UserListViewModelTest.加载数据成功时应更新列表状态"
```
