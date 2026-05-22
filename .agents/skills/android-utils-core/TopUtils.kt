/**
 * Android 通用顶层扩展函数集合
 * 包含 Toast、JSON、时间、距离、DP/PX、资源获取、SpannableString 等工具
 *
 * 使用前需在 Application.onCreate() 中初始化 ContextProvider
 */

package com.xxx.app.base.utils.toputils

import android.content.res.Resources
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

// ==================== 全局 Context 提供者 ====================

/**
 * 全局 Context 提供者
 * 在 Application.onCreate() 中调用 init(this) 初始化
 */
object ContextProvider {

    private lateinit var appContext: android.content.Context

    /** 初始化（Application.onCreate 中调用） */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    /** 获取全局 Application Context */
    fun get(): android.content.Context = appContext
}

// ==================== Toast 扩展 ====================

/**
 * 底部 Toast 提示
 * 用法："提示信息".show()
 *
 * 使用 CoroutineProvider.uiScope 确保在主线程显示
 * 参见 android-coroutines skill
 */
fun String?.show() {
    if (this.isNullOrEmpty()) return
    com.xxx.app.base.coroutines.CoroutineProvider.launchUI {
        Toast.makeText(ContextProvider.get(), this@show, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 居中 Toast 提示（使用系统默认 Toast + Gravity）
 * 用法："提示信息".showCenter()
 * 注意：如需自定义布局的居中 Toast，需自行提供布局文件
 */
fun String?.showCenter(isLengthLong: Boolean = false) {
    if (this.isNullOrEmpty()) return
    com.xxx.app.base.coroutines.CoroutineProvider.launchUI {
        val toast = Toast.makeText(
            ContextProvider.get(), this@showCenter,
            if (isLengthLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        )
        toast.setGravity(android.view.Gravity.CENTER, 0, 0)
        toast.show()
    }
}

/**
 * 仅 Debug 模式下的居中 Toast
 * 用法："调试信息".showCenterDebug()
 *
 * 注意：BuildConfig.DEBUG 需要在具体项目中定义
 * 此方法建议在实际项目中根据 BuildConfig 配置调整
 */
fun String?.showCenterDebug(isLengthLong: Boolean = false) {
    // BuildConfig.DEBUG 在通用工具类中不可用
    // 实际使用时应在具体项目中实现，或通过 ContextProvider 暴露 debug 状态
    // 此处保留方法签名，实际逻辑需项目自行实现
    this?.showCenter(isLengthLong)
}

// ==================== JSON 处理（基于 Gson） ====================

/** 全局 Gson 实例（线程安全） */
private val gson = Gson()

/**
 * 将任意对象序列化为 JSON 字符串
 * 用法：val jsonStr = anyObject.json()
 */
fun Any.json(): String = gson.toJson(this)

/**
 * 将 JSON 字符串反序列化为指定类型
 * 用法：val obj = jsonString.fromJson(MyData::class.java)
 * @return 解析失败返回 null
 */
fun <T> String.fromJson(classOfT: Class<T>): T? {
    return try {
        gson.fromJson(this, classOfT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 通过 JSON 序列化实现深拷贝
 * 用法：val copy = originalObj.copyFromJson()
 */
fun <T : Any> T?.copyFromJson(): T? {
    if (this == null) return null
    val json = gson.toJson(this)
    @Suppress("UNCHECKED_CAST")
    return gson.fromJson(json, this::class.java) as? T
}

/**
 * 格式化 JSON 字符串（美化输出，用于日志打印）
 * 用法：val pretty = jsonString.formatJson()
 */
fun String.formatJson(indent: Int = 4): String {
    return try {
        val element = JsonParser.parseString(this)
        GsonBuilder().setPrettyPrinting().create().toJson(element)
    } catch (e: Exception) {
        this
    }
}

// ==================== 时间格式化（Long 扩展） ====================

/**
 * 时间戳格式化
 * 用法：timestamp.format("yyyy-MM-dd HH:mm:ss")
 */
fun Long.format(format: String? = null): String {
    return SimpleDateFormat(format ?: "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(this)
}

/**
 * 毫秒值 → "mm:ss" 格式
 * 用法：millis.formatMillisToMmSs()  // "05:30"
 */
fun Long.formatMillisToMmSs(): String {
    if (this < 0) return "00:00"
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * 毫秒值 → "HH:mm:ss" 格式
 * 用法：millis.formatHMS()  // "01:05:30"
 */
fun Long.formatHMS(): String {
    if (this <= 0) return "00:00:00"
    val h = this / 3600000
    val m = (this % 3600000) / 60000
    val s = (this % 60000) / 1000
    return String.format("%02d:%02d:%02d", h, m, s)
}

/**
 * 毫秒值 → 分钟数（取整）
 * 用法：millis.formatM()  // "5"
 */
fun Long.formatM(): String {
    return "${max(this / 60000, 0)}"
}

/**
 * 毫秒值 → 小时数（保留1位小数）
 * 用法：millis.formatH()  // "1.5"
 */
fun Long.formatH(): String {
    val hours = this.toDouble() / 3600000.0
    return String.format(Locale.ENGLISH, "%.1f", hours)
}

/**
 * 秒 → "HH:mm:ss" 格式（Int 扩展）
 * 用法：seconds.formatS2Time()  // "00:02:35"
 */
fun Int.formatS2Time(): String {
    if (this < 0) return "00:00:00"
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

// ==================== 距离格式化 ====================

/**
 * 米数格式化为 km/m
 * 用法：meters.formatKm()  // "1.5 km" 或 "800 m"
 * @param showM true 时小于 1000m 显示 m 单位，false 直接算 km
 */
fun Long.formatKm(showM: Boolean = true): String {
    return try {
        if (showM && this < 1000) {
            "$this m"
        } else {
            val km = this.toDouble() / 1000.0
            String.format(Locale.ENGLISH, "%.1f km", km)
        }
    } catch (e: Exception) {
        this.toString()
    }
}

// ==================== DP/PX 转换 ====================

/**
 * Float dp → px（Float 返回）
 * 用法：16f.dp2px()
 */
fun Float.dp2px(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this,
        Resources.getSystem().displayMetrics
    )
}

/**
 * Float dp → px（Int 返回，四舍五入）
 * 用法：16f.dp2pxInt()
 */
fun Float.dp2pxInt(): Int = (this.dp2px() + 0.5f).toInt()

/**
 * Int dp → px（Float 返回）
 * 用法：16.dp2px()
 */
fun Int.dp2px(): Float = this.toFloat().dp2px()

/**
 * Int dp → px（Int 返回，四舍五入）
 * 用法：16.dp2pxInt()
 */
fun Int.dp2pxInt(): Int = this.toFloat().dp2pxInt()

/**
 * Int dp → px（Int 返回，密度计算）
 * 用法：16.dpToPx()
 */
fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

// ==================== 资源获取 ====================

/**
 * 通过资源 ID 获取字符串
 * 用法：R.string.app_name.getResString()
 */
fun Int.getResString(): String {
    return ContextProvider.get().getString(this)
}

/**
 * 通过资源 ID 获取尺寸值
 * 用法：R.dimen.margin.getResDimension()
 */
fun Int.getResDimension(): Int {
    return try {
        ContextProvider.get().resources.getDimension(this).toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * 通过资源 ID 获取颜色值
 * 用法：R.color.primary.getResColor()
 */
fun Int.getResColor(): Int {
    return ContextCompat.getColor(ContextProvider.get(), this)
}

// ==================== SpannableString 富文本 ====================

/**
 * 字符串分段设置不同字体大小和颜色
 * @param index 分割位置（0~index 为前半段，index~末尾 为后半段）
 * @param textSizeStartDp 前半段字体大小（dp）
 * @param textSizeEndDp 后半段字体大小（dp）
 * @param textColorStart 前半段颜色（可选）
 * @param textColorEnd 后半段颜色（可选）
 */
fun String.spannable(
    index: Int,
    textSizeStartDp: Float,
    textSizeEndDp: Float,
    textColorStart: Int? = null,
    textColorEnd: Int? = null,
): SpannableStringBuilder {
    val builder = SpannableStringBuilder(this)
    builder.setSpan(AbsoluteSizeSpan(textSizeStartDp.toInt(), true), 0, index, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    builder.setSpan(AbsoluteSizeSpan(textSizeEndDp.toInt(), true), index, this.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    textColorStart?.let {
        builder.setSpan(ForegroundColorSpan(it), 0, index, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }
    textColorEnd?.let {
        builder.setSpan(ForegroundColorSpan(it), index, this.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }
    return builder
}

/**
 * 字符串分段设置不同颜色（字体大小不变）
 * @param index 分割位置
 * @param textColorStart 前半段颜色
 * @param textColorEnd 后半段颜色
 */
fun String.spannableColor(
    index: Int,
    textColorStart: Int? = null,
    textColorEnd: Int? = null,
): SpannableStringBuilder {
    val builder = SpannableStringBuilder(this)
    textColorStart?.let {
        builder.setSpan(ForegroundColorSpan(it), 0, index, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }
    textColorEnd?.let {
        builder.setSpan(ForegroundColorSpan(it), index, this.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
    }
    return builder
}