# 权限管理与防连续点击

本文件是 android-utils-core 技能的附属参考文件，包含 Compose 运行时权限管理和防连续点击工具。

**屏幕适配规范已迁移至 `android-mvi-compose/compose-templates-and-components.md`，请统一参考该文件。**

---

## 十三、权限管理（Compose 运行时权限）

```kotlin
// 单权限请求
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) { /* 已授予 */ } else { /* 被拒绝 */ }
}
// 触发：launcher.launch(Manifest.permission.CAMERA)

// 多权限请求
val multiLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { perms ->
    if (perms.values.all { it }) { /* 全部授予 */ }
}
// 触发：multiLauncher.launch(arrayOf(CAMERA, READ_MEDIA_IMAGES))

// 跳转系统权限设置页
fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
```

---

## 十七、防连续点击

### 传统 View 扩展函数（完整实现）

```kotlin
import android.os.SystemClock
import android.view.View

/** 全局共享的最后点击时间（防止跨 View 连续点击） */
@Volatile
private var lastGlobalClickTime = 0L

/**
 * 防连续点击核心监听器
 * @param onClick 点击回调
 * @param interval 防抖间隔（毫秒）
 */
class OnClickDelayListener(
    private val onClick: (View?) -> Unit,
    private val interval: Long
) : View.OnClickListener {
    override fun onClick(v: View?) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastGlobalClickTime > interval) {
            lastGlobalClickTime = now
            try {
                onClick(v)
            } catch (e: Exception) {
                e.printStackTrace()
                e.message.show()
            }
        }
    }
}

/**
 * View 防连续点击扩展函数（多种间隔可选）
 * 用法：
 *   btnSubmit.setOnClickListener500 { doSubmit() }    // 500ms 防抖（最常用）
 *   btnLike.setOnClickListener200 { toggleLike() }    // 200ms 防抖（轻操作）
 *   btnPay.setOnClickListener800 { startPay() }       // 800ms 防抖（重操作）
 */
fun View?.setOnClickListener200(click: (View?) -> Unit) {
    this?.setOnClickListener(OnClickDelayListener(click, 200))
}
fun View?.setOnClickListener400(click: (View?) -> Unit) {
    this?.setOnClickListener(OnClickDelayListener(click, 400))
}
fun View?.setOnClickListener500(click: (View?) -> Unit) {
    this?.setOnClickListener(OnClickDelayListener(click, 500))
}
fun View?.setOnClickListener600(click: (View?) -> Unit) {
    this?.setOnClickListener(OnClickDelayListener(click, 600))
}
fun View?.setOnClickListener800(click: (View?) -> Unit) {
    this?.setOnClickListener(OnClickDelayListener(click, 800))
}
```

### Compose 防连续点击 Modifier（完整实现）

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Compose 防连续点击 Modifier
 * 用法：
 *   Modifier.throttleClick { doSomething() }            // 默认 500ms
 *   Modifier.throttleClick(interval = 800L) { doPay() } // 自定义间隔
 *
 * 示例：
 *   Button(onClick = {}, modifier = Modifier.throttleClick { viewModel.handleIntent(Submit) })
 *   // 或直接用在 Box/Card 等任意组件上
 *   Card(modifier = Modifier.throttleClick { navigateToDetail() }) { ... }
 */
fun Modifier.throttleClick(
    interval: Long = 500L,
    indication: androidx.compose.foundation.Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    this.clickable(
        indication = indication,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= interval) {
            lastClickTime = now
            onClick()
        }
    }
}

/**
 * Compose 防连续点击回调（用于 Button 等自带 onClick 参数的组件）
 * 用法：
 *   Button(onClick = throttleClick { submit() }) { Text("提交") }
 */
@Composable
fun throttleClick(interval: Long = 500L, onClick: () -> Unit): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return {
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= interval) {
            lastClickTime = now
            onClick()
        }
    }
}
```

---

## 附录：375 设计稿适配速查表

> 详细适配方案、ResponsiveScaffold 实现见 `android-mvi-compose/compose-templates-and-components.md`

| 场景 | 方案 |
|------|------|
| 手机竖屏 | 直接用设计稿 dp/sp 标注值 |
| 宽度 | 优先 `fillMaxWidth()` + 限制 `widthIn(max=xxx.dp)` + 居中排列 |
| 图片/卡片比例 | 用 `aspectRatio` 保持长宽比 |
| 列表网格 | 用 `GridCells.Adaptive` 自适应列数 |
| 大屏/平板 | 响应式适配 + `widthIn(max=600.dp).align(Alignment.TopCenter)` 限制最大宽度居中 |
