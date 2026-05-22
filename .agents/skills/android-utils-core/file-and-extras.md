# 常用工具类、ZIP 压缩与键盘工具

本文件是 android-utils-core 技能的附属参考文件，包含文件操作、MD5 加密、剪贴板、状态栏工具、ZIP 压缩和键盘管理。

## 十六、常用工具类（完整实现）

### 16.1 剪切板工具

```kotlin
import android.content.ClipData
import android.content.ClipboardManager

/**
 * 复制文本到系统剪切板
 * 用法："要复制的内容".copyToClipboard()
 */
fun String.copyToClipboard(showToast: Boolean = true) {
    val clipboard = ContextProvider.get()
        .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("text", this)
    clipboard.setPrimaryClip(clip)
    if (showToast) "已复制到剪切板".show()
}
```

### 16.2 日期时间工具

```kotlin
import java.util.Calendar

object DateUtils {
    /** 获取今天 0:00 的时间戳 */
    fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** 判断时间戳是否为今天 */
    fun isToday(timestamp: Long): Boolean {
        val todayStart = getTodayStartMillis()
        return timestamp >= todayStart && timestamp < todayStart + 86400000L
    }

    /** 获取 N 天前的时间戳 */
    fun daysAgoMillis(days: Int): Long {
        return System.currentTimeMillis() - days * 86400000L
    }

    /** 获取本周一 0:00 的时间戳 */
    fun getWeekStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** 获取本月1号 0:00 的时间戳 */
    fun getMonthStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
```

### 16.3 文件工具

```kotlin
import java.io.File

object FileUtils {
    /** 获取应用私有缓存目录下的子目录 */
    fun getCacheDir(subDir: String): File {
        val dir = File(ContextProvider.get().cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 获取应用私有文件目录下的子目录 */
    fun getFilesDir(subDir: String): File {
        val dir = File(ContextProvider.get().filesDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 删除文件或目录（递归） */
    fun delete(file: File): Boolean {
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    /** 计算目录大小（字节） */
    fun calculateSize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** 格式化文件大小 */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
```

### 16.4 加密工具

```kotlin
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    /** MD5 加密 */
    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }

    /** SHA-256 加密 */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }

    /** HMAC-SHA256 签名 */
    fun hmacSha256(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { String.format("%02x", it) }
    }
}
```

### 16.5 应用信息工具

```kotlin
object AppUtils {
    /** 获取应用版本名称 */
    fun getVersionName(): String {
        return try {
            val context = ContextProvider.get()
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
    }

    /** 获取应用版本号 */
    fun getVersionCode(): Long {
        return try {
            val context = ContextProvider.get()
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
            else info.versionCode.toLong()
        } catch (e: Exception) { 0L }
    }

    /** 检查应用是否已安装 */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            ContextProvider.get().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) { false }
    }

    /** 用浏览器打开 URL */
    fun openBrowser(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextProvider.get().startActivity(intent)
        } catch (e: Exception) {
            "未找到浏览器应用".show()
        }
    }

    /** 判断应用是否在前台 */
    fun isAppForeground(): Boolean {
        val am = ContextProvider.get().getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val processes = am.runningAppProcesses ?: return false
        return processes.any {
            it.processName == ContextProvider.get().packageName &&
            it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
}
```

### 16.6 设备品牌识别

```kotlin
import android.os.Build

object DeviceBrandUtil {
    /** 设备品牌枚举 */
    enum class Brand { XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, OTHER }

    /** 获取当前设备品牌 */
    fun getBrand(): Brand {
        val manufacturer = Build.MANUFACTURER.uppercase()
        return when {
            manufacturer.contains("XIAOMI") || manufacturer.contains("REDMI") -> Brand.XIAOMI
            manufacturer.contains("HUAWEI") || manufacturer.contains("HONOR") -> Brand.HUAWEI
            manufacturer.contains("OPPO") || manufacturer.contains("REALME") -> Brand.OPPO
            manufacturer.contains("VIVO") || manufacturer.contains("IQOO") -> Brand.VIVO
            manufacturer.contains("SAMSUNG") -> Brand.SAMSUNG
            manufacturer.contains("ONEPLUS") -> Brand.ONEPLUS
            else -> Brand.OTHER
        }
    }
}
```

---


---

## 十八、ZIP 压缩工具

```kotlin
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    /**
     * 压缩文件夹为 ZIP
     * @param sourceDir 要压缩的目录
     * @param outputFile 输出的 ZIP 文件
     */
    fun zipDirectory(sourceDir: File, outputFile: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                addFilesToZip(sourceDir, sourceDir.name, zos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFilesToZip(file: File, parentPath: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addFilesToZip(child, "$parentPath/${child.name}", zos)
            }
        } else {
            val entry = ZipEntry(parentPath)
            zos.putNextEntry(entry)
            BufferedInputStream(FileInputStream(file), 8192).use { bis ->
                bis.copyTo(zos, 8192)
            }
            zos.closeEntry()
        }
    }
}
```

---

## 十九、键盘工具

```kotlin
import android.view.inputmethod.InputMethodManager

object KeyboardUtils {
    /** 显示软键盘 */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /** 隐藏软键盘 */
    fun hideKeyboard(activity: android.app.Activity) {
        val view = activity.currentFocus ?: activity.window.decorView
        val imm = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
