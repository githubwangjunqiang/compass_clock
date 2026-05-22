---
name: android-legacy-view
description: 传统 View 与互操作。Activity 启动模板、状态页容器、View-Compose 互操作。
---

# Android 传统 View 与互操作规范

本文件包含所有与 Android 传统 View（XML 布局、Activity）相关的组件、工具和开发规范。

在以 Jetpack Compose 为主的技术栈中，本文件内容主要用于处理 Activity 创建、历史代码迁移、或需要嵌入第三方传统 View 组件的场景。

---

## 一、Activity 启动模板规范

所有新建 Activity 必须在 `companion object` 中提供 `startActivity()` 方法作为标准启动入口。

### 1.1 基础模板

```kotlin
class SettingActivity : BaseVMActivity<SettingVm>() {

    companion object {
        /**
         * 启动此界面
         */
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }
}
```

### 1.2 带参数启动

如需传递数据，通过 `putExtra` 添加参数：

```kotlin
class UserDetailActivity : BaseVMActivity<UserDetailVm>() {

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USER_NAME = "extra_user_name"

        /**
         * 启动此界面
         * @param userId 用户ID
         * @param userName 用户名
         */
        fun startActivity(context: Context, userId: String, userName: String) {
            context.startActivity(Intent(context, UserDetailActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
            })
        }
    }
}
```

### 1.3 使用规范

1. **新建 Activity 时必须** 同步创建 `startActivity()` 方法
2. **启动 Activity 时** 优先使用此方法，而非直接 `context.startActivity(Intent(...))`
3. **Intent Flags 固定** 必须包含 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`
4. **参数扩展** 通过添加方法参数和 `putExtra` 实现数据传递

---

## 二、StatusViewLayout（传统 View 状态页容器）

当使用传统 View（非 Compose）布局时，使用此容器管理 Loading/Error/Empty/Content 状态切换。

```kotlin
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 传统 View 状态页容器
 * 支持 4 种状态：Loading / Error / Empty / Content
 * 支持淡入淡出动画，支持点击重试
 *
 * 用法（XML）：
 * <StatusViewLayout android:id="@+id/statusView" ...>
 *     <!-- 正常内容放这里 -->
 *     <RecyclerView ... />
 * </StatusViewLayout>
 *
 * 用法（代码）：
 * statusView.showLoading()
 * statusView.showContent()
 * statusView.showEmpty("暂无数据") { retryLoad() }
 * statusView.showError("加载失败") { retryLoad() }
 */
class StatusViewLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 状态枚举 */
    enum class Status { LOADING, CONTENT, ERROR, EMPTY }

    /** 当前状态 */
    private var currentStatus: Status = Status.CONTENT

    /** 状态视图缓存 */
    private var loadingView: View? = null
    private var errorView: View? = null
    private var emptyView: View? = null

    /** 状态监听器 */
    var statusListener: StatusListener? = null

    /** 动画时长（毫秒） */
    var animDuration: Long = 200L

    /** 显示加载中 */
    fun showLoading() {
        if (currentStatus == Status.LOADING) return
        hideAllStatusViews()
        if (loadingView == null) {
            loadingView = ProgressBar(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
            addView(loadingView)
        }
        loadingView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.LOADING
        statusListener?.onStatusChanged(Status.LOADING)
    }

    /** 显示正常内容 */
    fun showContent() {
        if (currentStatus == Status.CONTENT) return
        hideAllStatusViews()
        setContentVisible(true)
        currentStatus = Status.CONTENT
        statusListener?.onStatusChanged(Status.CONTENT)
    }

    /**
     * 显示错误状态
     * @param msg 错误提示文案
     * @param onRetry 点击重试回调
     */
    fun showError(msg: String? = "加载失败，点击重试", onRetry: (() -> Unit)? = null) {
        if (currentStatus == Status.ERROR) return
        hideAllStatusViews()
        if (errorView == null) {
            errorView = createStatusView(msg ?: "加载失败，点击重试")
            addView(errorView)
        } else {
            errorView?.findViewById<TextView>(android.R.id.text1)?.text = msg
        }
        onRetry?.let { retry -> errorView?.setOnClickListener { retry() } }
        errorView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.ERROR
        statusListener?.onStatusChanged(Status.ERROR)
    }

    /**
     * 显示空数据状态
     * @param msg 空状态提示文案
     * @param onRetry 点击重试回调
     */
    fun showEmpty(msg: String? = "暂无数据", onRetry: (() -> Unit)? = null) {
        if (currentStatus == Status.EMPTY) return
        hideAllStatusViews()
        if (emptyView == null) {
            emptyView = createStatusView(msg ?: "暂无数据")
            addView(emptyView)
        } else {
            emptyView?.findViewById<TextView>(android.R.id.text1)?.text = msg
        }
        onRetry?.let { retry -> emptyView?.setOnClickListener { retry() } }
        emptyView?.fadeIn()
        setContentVisible(false)
        currentStatus = Status.EMPTY
        statusListener?.onStatusChanged(Status.EMPTY)
    }

    /** 创建状态提示视图（居中文字） */
    private fun createStatusView(msg: String): View {
        return TextView(context).apply {
            text = msg
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(32, 32, 32, 32)
        }
    }

    /** 隐藏所有状态视图 */
    private fun hideAllStatusViews() {
        loadingView?.fadeOut()
        errorView?.fadeOut()
        emptyView?.fadeOut()
    }

    /** 设置原始内容子 View 的可见性 */
    private fun setContentVisible(visible: Boolean) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != loadingView && child != errorView && child != emptyView) {
                child.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }
    }

    /** 淡入动画 */
    private fun View.fadeIn() {
        this.visibility = View.VISIBLE
        this.startAnimation(AlphaAnimation(0f, 1f).apply { duration = animDuration })
    }

    /** 淡出动画 */
    private fun View.fadeOut() {
        if (this.visibility != View.VISIBLE) return
        this.startAnimation(AlphaAnimation(1f, 0f).apply { duration = animDuration })
        this.visibility = View.GONE
    }

    /** 状态变化监听器 */
    interface StatusListener {
        fun onStatusChanged(status: Status)
    }
}
```
