---
name: android-utils-core
description: 通用工具类。Toast、JSON、时间、DP/PX、图片加载、日志、权限。
---

# Android 通用工具类扩展

## 附属参考文件目录

| 文件 | 内容 |
|------|------|
| [TopUtils.kt](TopUtils.kt) | **核心工具完整代码**（Toast、JSON、时间、距离、DP/PX、资源获取、SpannableString） |
| [system-utils.md](system-utils.md) | 其他通用工具（空值判断、Map/List 扩展、数字格式化、正则验证等） |
| [data-flow-tools.md](data-flow-tools.md) | LiveData/Flow 工具类、协程与 Flow 规范 |
| [image-and-log.md](image-and-log.md) | 图片加载工具（Glide 封装）、日志系统（文件日志） |
| [permissions-and-click.md](permissions-and-click.md) | 权限管理、防连续点击 |
| [file-and-extras.md](file-and-extras.md) | 常用工具类（File/MD5/Clipboard/StatusBar 等）、ZIP 压缩、键盘工具 |

---

## 一、全局 Context 提供者（新项目必须首先初始化）

所有工具函数需要 Context 时，统一通过此单例获取，避免到处传参。

**Application 中初始化**：
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextProvider.init(this)  // 必须最先调用
        MMKV.initialize(this)
    }
}
```

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 二、Toast 与提示

Toast 工具函数使用 `CoroutineProvider.uiScope` 确保在主线程显示，参见 `android-coroutines` skill。

| 函数 | 用法 | 说明 |
|------|------|------|
| `String?.show()` | `"提示信息".show()` | 底部 Toast |
| `String?.showCenter()` | `"提示信息".showCenter()` | 居中 Toast |
| `String?.showCenterDebug()` | `"调试信息".showCenterDebug()` | 仅 Debug 模式显示 |

> 完整实现见 [TopUtils.kt](TopUtils.kt)
> 协程相关见 `android-coroutines` skill

---

## 三、JSON 处理（基于 Gson）

| 函数 | 用法 | 说明 |
|------|------|------|
| `Any.json()` | `obj.json()` | 对象 → JSON 字符串 |
| `String.fromJson()` | `json.fromJson(Data::class.java)` | JSON → 对象（失败返回 null） |
| `T?.copyFromJson()` | `obj.copyFromJson()` | 通过 JSON 深拷贝 |
| `String.formatJson()` | `json.formatJson()` | 格式化 JSON（日志打印用） |

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 四、时间格式化

| 函数 | 用法 | 结果示例 |
|------|------|----------|
| `Long.format()` | `timestamp.format("yyyy-MM-dd")` | "2024-01-15" |
| `Long.formatMillisToMmSs()` | `millis.formatMillisToMmSs()` | "05:30" |
| `Long.formatHMS()` | `millis.formatHMS()` | "01:05:30" |
| `Long.formatM()` | `millis.formatM()` | "5"（分钟数） |
| `Long.formatH()` | `millis.formatH()` | "1.5"（小时数） |
| `Int.formatS2Time()` | `seconds.formatS2Time()` | "00:02:35" |

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 五、距离格式化

| 函数 | 用法 | 结果示例 |
|------|------|----------|
| `Long.formatKm()` | `meters.formatKm()` | "1.5 km" 或 "800 m" |

参数：`showM = true` 时小于 1000m 显示 m 单位。

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 六、DP/PX 转换

| 函数 | 用法 | 返回类型 |
|------|------|----------|
| `Float.dp2px()` | `16f.dp2px()` | Float |
| `Float.dp2pxInt()` | `16f.dp2pxInt()` | Int（四舍五入） |
| `Int.dp2px()` | `16.dp2px()` | Float |
| `Int.dp2pxInt()` | `16.dp2pxInt()` | Int |
| `Int.dpToPx()` | `16.dpToPx()` | Int（密度计算） |

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 七、资源获取

| 函数 | 用法 | 说明 |
|------|------|------|
| `Int.getResString()` | `R.string.app_name.getResString()` | 获取字符串资源 |
| `Int.getResDimension()` | `R.dimen.margin.getResDimension()` | 获取尺寸值 |
| `Int.getResColor()` | `R.color.primary.getResColor()` | 获取颜色值 |

> 完整实现见 [TopUtils.kt](TopUtils.kt)

---

## 八、SpannableString 富文本

| 函数 | 用法 | 说明 |
|------|------|------|
| `String.spannable()` | `"123元".spannable(3, 14f, 12f, color1, color2)` | 分段设置字体大小和颜色 |
| `String.spannableColor()` | `"123元".spannableColor(3, color1, color2)` | 分段设置颜色（字体大小不变） |

> 完整实现见 [TopUtils.kt](TopUtils.kt)