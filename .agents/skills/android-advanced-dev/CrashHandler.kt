/**
 * 全局未捕获异常处理器
 * 在 Application.onCreate 中初始化，捕获应用崩溃并记录日志
 */

package com.xxx.app.manager

import com.tencent.mmkv.MMKV
import com.xxx.app.base.utils.toputils.ContextProvider
import com.xxx.app.base.utils.toputils.format
import com.xxx.app.base.utils.toputils.json
import com.xxx.app.base.utils.toputils.fromJson
import com.xxx.app.base.utils.toputils.loge

/**
 * 全局未捕获异常处理器
 * 在 Application.onCreate 中初始化
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    /** 系统默认的异常处理器 */
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /** 初始化（Application.onCreate 调用） */
    fun init() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. 记录崩溃日志到文件
        val crashLog = buildString {
            appendLine("===== CRASH LOG =====")
            appendLine("时间: ${System.currentTimeMillis().format("yyyy-MM-dd HH:mm:ss")}")
            appendLine("线程: ${thread.name}")
            appendLine("异常: ${throwable.stackTraceToString()}")
        }
        crashLog.loge()

        // 2. 保存崩溃信息到本地（列表形式，不覆盖历史记录）
        val mmkv = MMKV.mmkvWithID("crash")
        // 读取已有崩溃记录列表
        val existingList = mmkv.decodeString("crash_list", "[]")
            ?.fromJson(Array<String>::class.java)?.toMutableList() ?: mutableListOf()
        // 追加新崩溃记录（按时间排序，最新在前）
        existingList.add(0, crashLog)
        // 最多保留 50 条崩溃记录，防止无限膨胀
        if (existingList.size > 50) {
            existingList.subList(50, existingList.size).clear()
        }
        mmkv.encode("crash_list", existingList.json())
        mmkv.encode("last_crash_time", System.currentTimeMillis())

        // 3. 交给系统默认处理器（弹出崩溃弹窗）
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * 获取所有崩溃记录（最新在前）
     * 用于下次启动时上报或在调试面板展示
     */
    fun getCrashList(): List<String> {
        val mmkv = MMKV.mmkvWithID("crash")
        return mmkv.decodeString("crash_list", "[]")
            ?.fromJson(Array<String>::class.java)?.toList() ?: emptyList()
    }

    /** 清空崩溃记录（上报成功后调用） */
    fun clearCrashList() {
        MMKV.mmkvWithID("crash").encode("crash_list", "[]")
    }
}

/**
 * 全局协程异常处理器
 * 捕获所有未被 try-catch 的协程异常
 */
val globalCoroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
    "全局协程异常: ${throwable.message}".loge()
    throwable.loge()
}

// 在全局 CoroutineScope 中使用
val appScope = kotlinx.coroutines.CoroutineScope(
    kotlinx.coroutines.SupervisorJob() +
    kotlinx.coroutines.Dispatchers.IO +
    globalCoroutineExceptionHandler
)