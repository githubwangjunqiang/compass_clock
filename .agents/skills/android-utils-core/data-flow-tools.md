# LiveData / Flow 工具与协程规范

本文件是 android-utils-core 技能的附属参考文件，包含 LiveData/Flow 工具类和协程使用规范。

## 十、LiveData / Flow 工具（完整实现）

```kotlin
import android.os.SystemClock
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.util.concurrent.ConcurrentHashMap

/**
 * 非粘性 LiveData（新订阅者不会收到历史值）
 * 原理：记录 setValue/postValue 时间戳，observe 时跳过订阅前已存在的值
 * 额外特性：Observer 自动包装 try-catch，异常通过回调统一处理
 *
 * @param onHandlerError 必需的回调，用于处理捕获到的异常
 * @param onHandlerErrorTask 可选的回调，当异常发生时，提供导致异常的数据
 *
 * 所在包：com.xq.video.social.base.baseui.baselivedata
 */
open class BaseNotStickyLiveData<T>(
    private val onHandlerError: ((e: Exception) -> Unit) = { it.printStackTrace() },
    private val onHandlerErrorTask: ((t: T) -> Unit)? = null
) : MutableLiveData<T>() {
    private val observerProcessedHashCodes = ConcurrentHashMap<Int, Long>()
    private val observerMap = ConcurrentHashMap<Observer<in T>, Observer<in T>>()

    /** 带有生命周期感知的观察 */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrappedObserver = observerMap.getOrPut(observer) {
            createWrappedObserver(observer)
        }
        super.observe(owner, wrappedObserver)
    }

    /** 重写移除观察者的逻辑，确保能正确移除被包装的 Observer */
    override fun removeObserver(observer: Observer<in T>) {
        val wrappedObserver: Observer<in T> = observerMap.remove(observer) ?: observer
        val identityHashCode = System.identityHashCode(wrappedObserver)
        observerProcessedHashCodes.remove(identityHashCode)
        super.removeObserver(wrappedObserver)
    }

    /** 持续观察，直到手动移除 */
    override fun observeForever(observer: Observer<in T>) {
        val wrappedObserver = observerMap.getOrPut(observer) {
            createWrappedObserver(observer)
        }
        super.observeForever(wrappedObserver)
    }

    override fun postValue(value: T) {
        lastUpdateTime = SystemClock.elapsedRealtime()
        super.postValue(value)
    }
    private var lastUpdateTime: Long = 0

    override fun setValue(value: T) {
        lastUpdateTime = SystemClock.elapsedRealtime()
        super.setValue(value)
    }

    /**
     * 创建一个包装了 try-catch 逻辑的新 Observer
     * 只处理观察者创建之后更新的数据（实现非粘性）
     */
    protected open fun createWrappedObserver(originalObserver: Observer<in T>): Observer<T> {
        val eventually = object : Observer<T> {
            override fun onChanged(value: T) {
                try {
                    val identityHashCode = System.identityHashCode(this)
                    val observerCreateTime = observerProcessedHashCodes.get(identityHashCode) ?: 0
                    // 只处理观察者创建之后更新的数据
                    if (lastUpdateTime >= observerCreateTime) {
                        originalObserver.onChanged(value)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onHandlerError.invoke(e)
                    onHandlerErrorTask?.invoke(value)
                }
            }
        }
        val identityHashCode = System.identityHashCode(eventually)
        observerProcessedHashCodes[identityHashCode] = SystemClock.elapsedRealtime()
        return eventually
    }
}

/** 创建非粘性 LiveData */
fun <T> createNonStickyLiveData(): BaseNotStickyLiveData<T> = BaseNotStickyLiveData()

/** 创建非粘性 SharedFlow（replay = 0） */
fun <T> createNonStickyShareFlow(): MutableSharedFlow<T> = MutableSharedFlow(replay = 0)

/**
 * ViewModel 安全协程（自动 try-catch，异常仅打印日志）
 * 用法：launchTryViewModelScope { fetchData() }
 */
fun ViewModel.launchTryViewModelScope(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * ViewModel 安全协程（带自定义异常回调）
 * 用法：launchTryViewModelScopeError(catchBlock = { e -> handleError(e) }) { fetchData() }
 */
fun ViewModel.launchTryViewModelScopeError(
    catchBlock: (Exception) -> Unit = {},
    block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit
) {
    viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            catchBlock(e)
        }
    }
}

/**
 * Flow 生命周期感知观察（Compose 页面中优先用 collectAsStateWithLifecycle）
 * 用法：flow.observe(lifecycleOwner) { value -> updateUI(value) }
 */
fun <T> Flow<T>.observe(
    owner: LifecycleOwner,
    minState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minState) {
            this@observe.collect { collector(it) }
        }
    }
}
```

---


---

## 十五、协程与 Flow 规范

### 15.1 协程作用域

```kotlin
launchTryViewModelScope { }                           // ViewModel 安全启动
launchTryViewModelScopeError(catchBlock = {}) { }    // 带错误回调
```

### 15.2 回调转挂起函数

将第三方 SDK 回调转为协程挂起函数时，**统一使用 `suspendCancellableCoroutine` + `tryResumeYourself`**，避免协程取消后回调触发导致崩溃。

```kotlin
import kotlinx.coroutines.suspendCancellableCoroutine
import com.xq.video.social.base.utils.toputils.tryResumeYourself

/**
 * tryResumeYourself 扩展函数（已封装在项目工具类中）
 * 安全恢复挂起协程，避免协程取消后重复 resume 崩溃
 */
@OptIn(InternalCoroutinesApi::class)
fun <T> CancellableContinuation<T>.tryResumeYourself(t: T) {
    this.tryResume(t)?.also { value ->
        this.completeResume(value)
    }
}

// 用法示例：将 IM SDK 回调转为挂起函数
suspend fun getUserInfo(userId: String): UserInfo? = suspendCancellableCoroutine { continuation ->
    SdkManager.getUserInfo(userId, object : Callback<UserInfo> {
        override fun onSuccess(data: UserInfo?) {
            continuation.tryResumeYourself(data)
        }
        override fun onError(code: Int, msg: String?) {
            continuation.tryResumeYourself(null)
        }
    })
}
```

**禁止事项：**
- 禁止使用 `suspendCoroutine` + `resumeWith`（不支持取消，不安全）
- 禁止混用 `tryResumeYourself` 和 `resumeWith`（会导致重复 resume 崩溃）

### 15.3 线程切换

```kotlin
withContext(Dispatchers.IO) { }       // 网络、文件、数据库
withContext(Dispatchers.Default) { }  // CPU 密集计算
withContext(Dispatchers.Main) { }     // UI 更新
```

### 15.4 Flow 操作符

```kotlin
// 防抖搜索
searchFlow.debounce(300L).distinctUntilChanged()
    .filter { it.isNotBlank() }
    .flatMapLatest { flow { emit(search(it)) } }
    .catch { emit(emptyList()) }
    .launchIn(viewModelScope)

// 合并多 Flow
combine(f1, f2, f3) { a, b, c -> UiState(a, b, c) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

// 定时轮询
flow { while (true) { emit(fetch()); delay(30_000L) } }.flowOn(Dispatchers.IO)
```

---

