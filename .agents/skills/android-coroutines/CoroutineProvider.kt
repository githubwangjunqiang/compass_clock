/**
 * 全局协程提供者
 * 为非生命周期组件（工具类、单例管理器、Repository）提供协程作用域
 *
 * 使用场景：
 * - 工具类中需要执行异步操作（如 Toast、文件操作）
 * - 单例管理器需要后台任务（如缓存管理、日志上传）
 * - Repository 层（非 ViewModel 内）需要协程
 * - 全局监听器需要异步处理
 *
 * 禁止场景：
 * - Activity/Fragment → 使用 lifecycleScope
 * - ViewModel → 使用 viewModelScope
 * - Fragment 视图 → 使用 viewLifecycleOwner.lifecycleScope
 */

package com.xxx.app.base.coroutines

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 协程提供者单例
 * 提供全局可用的协程作用域（Main 和 IO）
 */
object CoroutineProvider {

    private const val TAG = "CoroutineProvider"

    // ==================== 全局异常处理器 ====================

    /**
     * 全局协程异常处理器
     * 捕获未处理的协程异常，防止协程崩溃
     */
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程未捕获异常: ${throwable.message}", throwable)
        // 可接入 CrashHandler 记录
        // CrashHandler.recordException(throwable)
    }

    // ==================== 协程作用域 ====================

    /**
     * UI 作用域（主线程）
     * 用于需要在非生命周期组件中执行 UI 操作的场景
     * 如：工具类中显示 Toast、更新 UI 状态
     *
     * 特性：
     * - 使用 SupervisorJob：子协程异常不会取消其他子协程
     * - 使用 Main dispatcher：在主线程执行
     * - 内置异常处理器：捕获未处理异常
     */
    val uiScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + errorHandler)

    /**
     * IO 作用域（后台线程）
     * 用于需要执行 IO 操作的场景
     * 如：文件读写、网络请求、数据库操作
     *
     * 特性：
     * - 使用 SupervisorJob：子协程异常不会取消其他子协程
     * - 使用 IO dispatcher：在 IO 线程池执行
     * - 内置异常处理器：捕获未处理异常
     */
    val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)

    /**
     * 计算作用域（CPU 密集型）
     * 用于需要执行 CPU 密集型操作的场景
     * 如：JSON 解析、数据排序、复杂计算
     */
    val cpuScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + errorHandler)

    // ==================== 作用域管理 ====================

    /**
     * 取消所有全局协程
     * 通常在 Application.onTerminate() 或特定场景调用
     * 注意：Android 中 onTerminate 不会被调用，通常不取消全局作用域
     */
    fun cancelAll() {
        uiScope.cancel("CoroutineProvider cancelled")
        ioScope.cancel("CoroutineProvider cancelled")
        computeScope.cancel("CoroutineProvider cancelled")
        Log.d(TAG, "所有全局协程已取消")
    }

    /**
     * 创建新的作用域（用于特定任务组）
     * 可手动管理取消
     *
     * @param dispatcher 调度器，默认 IO
     * @return 新的协程作用域
     */
    fun createScope(dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcher + errorHandler)
    }

    // ==================== 安全启动方法 ====================

    /**
     * 安全启动 UI 协程
     * 自动包裹 try-catch，防止异常传播
     *
     * @param block 协程执行块
     * @return Job 可用于取消协程
     */
    fun launchUI(block: suspend CoroutineScope.() -> Unit): Job {
        return uiScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "UI协程异常: ${e.message}", e)
            }
        }
    }

    /**
     * 安全启动 IO 协程
     * 自动包裹 try-catch，防止异常传播
     *
     * @param block 协程执行块
     * @return Job 可用于取消协程
     */
    fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return ioScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "IO协程异常: ${e.message}", e)
            }
        }
    }
    /**
     * 安全启动 IO 协程
     * 自动包裹 try-catch，防止异常传播
     *
     * @param block 协程执行块
     * @return Job 可用于取消协程
     */
    fun launchCPU(block: suspend CoroutineScope.() -> Unit): Job {
        return cpuScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "IO协程异常: ${e.message}", e)
            }
        }
    }
}

// ==================== 使用示例 ====================

/**
 * 使用示例：工具类中显示 Toast
 */
// fun String?.showToast() {
//     if (this.isNullOrEmpty()) return
//     CoroutineProvider.launchUI {
//         Toast.makeText(ContextProvider.get(), this, Toast.LENGTH_SHORT).show()
//     }
// }

/**
 * 使用示例：单例缓存管理器
 */
// object CacheManager {
//     private val scope = CoroutineProvider.ioScope
//
//     fun preload(urls: List<String>) {
//         scope.launch {
//             urls.forEach { url ->
//                 ensureActive()  // 检查取消
//                 downloadAndCache(url)
//             }
//         }
//     }
// }