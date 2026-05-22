# 图片加载与日志系统

本文件是 android-utils-core 技能的附属参考文件，包含 Glide 图片加载封装和文件日志系统。

## 十一、图片加载工具（完整实现，基于 Glide）

```kotlin
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ImageView 图片加载扩展（统一使用此方法加载图片）
 * @param url 图片地址（支持 String URL / Uri / Int 资源ID）
 * @param isCircle 是否圆形裁剪
 * @param roundedCorners 圆角半径（dp），0 表示无圆角
 * @param placeholder 占位图资源ID
 * @param error 错误图资源ID
 */
fun ImageView.loadImage(
    url: Any?,
    isCircle: Boolean = false,
    roundedCorners: Int = 0,
    placeholder: Int = 0,    // 替换为项目实际占位图：R.drawable.img_placeholder
    error: Int = 0,          // 替换为项目实际错误图：R.drawable.img_error
    skipMemoryCache: Boolean = false,
    diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) {
    if (url == null) return
    this.scaleType = scaleType

    val options = RequestOptions()
        .skipMemoryCache(skipMemoryCache)
        .diskCacheStrategy(diskCacheStrategy)

    if (placeholder != 0) options.placeholder(placeholder)
    if (error != 0) options.error(error)

    val builder = when (url) {
        is Uri -> Glide.with(this.context).load(url)
        is Int -> Glide.with(this.context).load(url)
        else -> Glide.with(this.context).load(url.toString())
    }

    when {
        isCircle -> builder.apply(RequestOptions.bitmapTransform(CircleCrop()))
        roundedCorners > 0 -> builder.transform(CenterCrop(), RoundedCorners(roundedCorners.dpToPx()))
        else -> builder.apply(options)
    }.apply(options).into(this)
}

/**
 * 下载图片为 Bitmap（挂起函数，可在协程中调用）
 * 用法：val bitmap = "https://...".downloadBitmap(width = 200, height = 200)
 */
suspend fun String.downloadBitmap(
    width: Int = 0,
    height: Int = 0,
    isCircle: Boolean = false,
    roundedCorners: Int = 0
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val builder = Glide.with(ContextProvider.get()).asBitmap().load(this@downloadBitmap)
        if (width > 0 && height > 0) builder.override(width, height)
        when {
            isCircle -> builder.transform(CircleCrop())
            roundedCorners > 0 -> builder.transform(CenterCrop(), RoundedCorners(roundedCorners.dpToPx()))
        }
        builder.submit().get()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ===== Compose 图片加载（依赖：com.github.bumptech.glide:compose） =====

/**
 * Compose 通用图片加载组件
 * 封装 GlideImage，统一占位图、错误图、加载效果
 *
 * @param model 图片地址（String URL / Uri / Int 资源ID）
 * @param contentDescription 无障碍描述
 * @param isCircle 是否圆形裁剪
 * @param roundedCorners 圆角半径（dp），0 表示无圆角
 * @param placeholder 占位图资源ID
 * @param error 错误图资源ID
 */
@Composable
fun AppImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    isCircle: Boolean = false,
    roundedCorners: Int = 0,
    placeholder: Int = 0,    // 替换为项目实际占位图：R.drawable.img_placeholder
    error: Int = 0,          // 替换为项目实际错误图：R.drawable.img_error
    contentScale: ContentScale = ContentScale.Crop
) {
    GlideImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier.then(
            when {
                isCircle -> Modifier.clip(CircleShape)
                roundedCorners > 0 -> Modifier.clip(RoundedCornerShape(roundedCorners.dp))
                else -> Modifier
            }
        ),
        contentScale = contentScale
    ) {
        // 占位图
        if (placeholder != 0) it.placeholder(placeholder)
        if (error != 0) it.error(error)
        it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
    }
}

// 使用示例：
// AppImage(model = "https://xxx.jpg", modifier = Modifier.size(48.dp), isCircle = true)
// AppImage(model = avatarUrl, modifier = Modifier.size(80.dp), roundedCorners = 12)
```

---

## 十二、日志系统（完整实现）

### 12.1 日志工具函数

```kotlin
import android.util.Log

/** 日志 TAG */
private const val LOG_TAG = "12345"
/** 日志开关（Release 关闭 Logcat 输出，文件日志始终写入） */
private val LOG_ENABLED: Boolean get() = BuildConfig.DEBUG

/**
 * Debug 级别日志
 * 用法："请求成功".logd()
 */
fun String?.logd(tag: String = LOG_TAG) {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) Log.d(tag, this)
    LogManager.log(message = "[$tag] $this", type = "debug")
}

/**
 * Info 级别日志
 * 用法："用户登录".logi()
 */
fun String?.logi(tag: String = LOG_TAG) {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) Log.i(tag, this)
    LogManager.log(message = "[$tag] $this", type = "info")
}

/**
 * Error 级别日志（字符串）
 * 用法："解析失败".loge()
 */
fun String?.loge(tag: String = LOG_TAG) {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) Log.e(tag, this)
    LogManager.log(message = "[ERROR][$tag] $this", type = "error")
}

/**
 * Error 级别日志（异常，自动输出 stackTrace）
 * 用法：exception.loge()
 */
fun Exception?.loge(tag: String = LOG_TAG) {
    if (this == null) return
    val msg = this.stackTraceToString()
    if (LOG_ENABLED) Log.e(tag, msg)
    LogManager.log(message = "[EXCEPTION][$tag] $msg", type = "error")
}

/**
 * HTTP 请求日志
 * 用法："POST /api/user".logHttp()
 */
fun String?.logHttp(tag: String = "HTTP") {
    if (this.isNullOrEmpty()) return
    if (LOG_ENABLED) splitAndLog(tag)
    LogManager.log(message = "[$tag] $this", type = "http")
}

/**
 * 长日志分段打印（Logcat 单条最大约 4000 字符）
 */
private fun String.splitAndLog(tag: String, maxLength: Int = 4000) {
    if (this.length <= maxLength) {
        Log.d(tag, this)
        return
    }
    var start = 0
    while (start < this.length) {
        val end = minOf(start + maxLength, this.length)
        Log.d(tag, this.substring(start, end))
        start = end
    }
}
```

### 12.2 文件日志管理器（完整实现）

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 异步文件日志管理器
 * 特性：
 * - 异步非阻塞（Channel + Dispatchers.IO）
 * - 按小时轮转（type/yyyy-MM-dd/HH.log）
 * - 单类型目录上限 100MB，自动清理最旧日期
 * - 每 100 条触发容量检查
 */
object LogManager {

    /** 日志消息数据类 */
    private data class LogEntry(val message: String, val type: String)

    /** 日志通道（无限缓冲） */
    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)

    /** 日志根目录 */
    private lateinit var baseLogDir: String

    /** 是否已初始化 */
    private var isInitialized = false

    /** 写入计数器（用于触发清理检查） */
    private var writeCount = 0

    /** 单类型目录最大大小（100MB） */
    private const val MAX_DIR_SIZE = 100L * 1024 * 1024

    /** 清理检查间隔（每 100 条日志检查一次） */
    private const val CLEAN_CHECK_INTERVAL = 100

    /** 后台协程作用域 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化日志管理器
     * @param context Application Context
     * @param appSpecificBasePath 日志存储根路径，如 "${filesDir}/logs"
     */
    fun init(context: android.content.Context, appSpecificBasePath: String) {
        if (isInitialized) return
        baseLogDir = appSpecificBasePath
        isInitialized = true

        // 启动后台消费协程
        scope.launch {
            for (entry in logChannel) {
                writeLogToFile(entry)
            }
        }
    }

    /**
     * 写入日志（非阻塞）
     * @param message 日志内容
     * @param type 日志类型（决定子目录名，如 "http"、"debug"、"error"）
     */
    fun log(message: String, type: String = "default") {
        if (!isInitialized) return
        logChannel.trySend(LogEntry(message, type))
    }

    /** 实际写入文件 */
    private fun writeLogToFile(entry: LogEntry) {
        try {
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

            // 日志路径：baseLogDir/type/yyyy-MM-dd/HH.log
            val logDir = File(baseLogDir, "${entry.type}/${dateFormat.format(now)}")
            if (!logDir.exists()) logDir.mkdirs()

            val logFile = File(logDir, "${hourFormat.format(now)}.log")
            val logLine = "[${timeFormat.format(now)}] ${entry.message}\n"
            logFile.appendText(logLine)

            // 每 100 条检查一次目录大小
            writeCount++
            if (writeCount >= CLEAN_CHECK_INTERVAL) {
                writeCount = 0
                checkAndCleanLogDirectory(entry.type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 检查并清理超大日志目录 */
    private fun checkAndCleanLogDirectory(type: String) {
        try {
            val typeDir = File(baseLogDir, type)
            if (!typeDir.exists()) return

            val totalSize = calculateDirectorySize(typeDir)
            if (totalSize <= MAX_DIR_SIZE) return

            // 按日期文件夹排序，删除最旧的
            val dateDirs = typeDir.listFiles()?.filter { it.isDirectory }
                ?.sortedBy { it.name } ?: return

            for (dir in dateDirs) {
                dir.deleteRecursively()
                if (calculateDirectorySize(typeDir) <= MAX_DIR_SIZE * 0.8) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 计算目录总大小（字节） */
    private fun calculateDirectorySize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** 关闭日志管理器 */
    fun close() {
        logChannel.close()
        scope.cancel()
    }
}
```

---

