# 其他通用工具函数

本文件是 android-utils-core 技能的附属参考文件，包含空值判断、Map/List 扩展、数字格式化、正则验证等通用工具。

## 九、其他通用工具（完整实现）

```kotlin
import android.os.Build
import android.provider.Settings
import android.view.View
import android.graphics.Rect
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 安全枚举查找（找不到返回 null）
 * 用法：getEnumForName<StatusEnum>("ACTIVE")
 */
inline fun <reified T : Enum<T>> getEnumForName(name: String): T? {
    return try {
        enumValueOf<T>(name)
    } catch (e: Exception) {
        null
    }
}

/**
 * 安全超时协程（超时或异常返回 null）
 * 用法：withTimeoutOrNullTry(3000L) { fetchData() }
 */
suspend fun <T> withTimeoutOrNullTry(timeMillis: Long, block: suspend CoroutineScope.() -> T): T? {
    return try {
        withTimeoutOrNull(timeMillis) { block() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 安全恢复 CancellableContinuation（防止重复 resume 崩溃）
 * 用法：continuation.tryResumeYourself(value)
 */
@OptIn(InternalCoroutinesApi::class)
fun <T> CancellableContinuation<T>.tryResumeYourself(t: T) {
    this.tryResume(t)?.also { token -> this.completeResume(token) }
}

/**
 * 字节数组转十六进制字符串
 * 用法：byteArray.toHexString()  // "A1B2C3"
 */
fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { String.format("%02X", it) }
}

/**
 * 获取设备 Android ID
 * 用法：val androidId = loadAndroidId()
 */
fun loadAndroidId(): String {
    return Settings.Secure.getString(
        ContextProvider.get().contentResolver,
        Settings.Secure.ANDROID_ID
    )
}

/**
 * 字符串是否能转为大于 0 的数字
 * 用法："123".whetherItIsGreaterThan0()  // true
 */
fun String?.whetherItIsGreaterThan0(): Boolean {
    if (this.isNullOrEmpty()) return false
    return try {
        this.toDouble() > 0
    } catch (e: Exception) {
        false
    }
}

/**
 * String 保留一位小数
 * 用法："3.14159".format1FractionalPart()  // "3.1"
 */
fun String.format1FractionalPart(): String {
    return try {
        String.format(Locale.ENGLISH, "%.1f", this.toDouble())
    } catch (e: Exception) {
        this
    }
}

/**
 * 监听软键盘弹出与隐藏（兼容 Android 11+ 和低版本）
 * 用法：activity.observeKeyboard(onShow = { height -> }, onHide = { })
 */
fun android.app.Activity.observeKeyboard(
    onShow: (keyboardHeight: Int) -> Unit,
    onHide: () -> Unit
) {
    val rootView = window.decorView.findViewById<View>(android.R.id.content)
    var isKeyboardVisible = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (imeVisible && !isKeyboardVisible) {
                isKeyboardVisible = true; onShow(imeHeight)
            } else if (!imeVisible && isKeyboardVisible) {
                isKeyboardVisible = false; onHide()
            }
            insets
        }
    } else {
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) {
                if (!isKeyboardVisible) { isKeyboardVisible = true; onShow(keypadHeight) }
            } else {
                if (isKeyboardVisible) { isKeyboardVisible = false; onHide() }
            }
        }
    }
}

/**
 * RecyclerView 平滑滚动到指定位置并对齐顶部
 * 用法：recyclerView.smoothScrollToIndex(position)
 */
fun RecyclerView.smoothScrollToIndex(position: Int) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int = SNAP_TO_START
        override fun calculateTimeForScrolling(dx: Int): Int = 300
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}

/**
 * 唤起地图（优先高德，无则用通用 geo 协议）
 * 用法：openMap("androidamap://navi?...")
 */
fun openMap(amapUri: String) {
    val context = ContextProvider.get()
    val uri = android.net.Uri.parse(amapUri)
    val lat = uri.getQueryParameter("lat")?.toDoubleOrNull()
    val lon = uri.getQueryParameter("lon")?.toDoubleOrNull()
    val name = uri.getQueryParameter("poiname")
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val geoUri = android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon($name)")
        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(fallback) } catch (_: Exception) { "未检测到地图应用".show() }
    }
}
```

---

