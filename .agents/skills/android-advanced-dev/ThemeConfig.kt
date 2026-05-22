/**
 * Material3 主题配置
 * 包含浅色/深色主题色方案定义和应用主题封装
 *
 * 使用前需：
 * 1. 创建 Typography.kt 定义 AppTypography
 * 2. 在 Activity 的 setContent 中包裹 AppTheme
 */

package com.xxx.app.ui.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp

// ==================== 颜色定义 ====================

// 浅色主题颜色
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ==================== Typography 定义 ====================

/**
 * 应用字体样式定义
 * 实际项目应根据设计稿调整字号、字重
 */
val AppTypography = androidx.compose.material3.Typography(
    // 标题样式
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 57.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 45.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontSize = 36.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    // 正文样式
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    // 标签样式
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
)

// ==================== 主题色方案 ====================

/** 浅色主题色方案 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = PurpleGrey80,
    secondary = Pink40,
    onSecondary = Color.White,
    secondaryContainer = Pink80,
    tertiary = Purple40,
    onTertiary = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    error = Color(0xFFB3261E),
    onError = Color.White,
    // ... 根据设计稿定义其他颜色
)

/** 深色主题色方案 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple40,
    primaryContainer = PurpleGrey40,
    secondary = PurpleGrey80,
    onSecondary = PurpleGrey40,
    secondaryContainer = PurpleGrey40,
    tertiary = Pink80,
    onTertiary = Pink40,
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

// ==================== 主题应用 ====================

/**
 * 应用主题封装
 * 自动跟随系统深色模式
 *
 * 使用方式（在 Activity 的 setContent 中）：
 * ```
 * setContent {
 *     AppTheme {
 *         // 应用内容
 *     }
 * }
 * ```
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 动态颜色支持（Android 12+）
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// ==================== 使用规范 ====================

/**
 * 使用规范：
 * 1. 始终使用 MaterialTheme.colorScheme 取色，禁止硬编码颜色
 * 2. 图片适配：深色模式下降低亮度
 * 3. 在 Activity/Fragment 的 setContent 中包裹 AppTheme（不是 Application）
 */

// 正确用法示例
/**
@Composable
fun SampleScreen() {
    // 使用主题颜色
    Text(
        text = "标题",
        color = MaterialTheme.colorScheme.onSurface
    )

    // 使用主题背景
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        // 内容
    }

    // 图片深色模式适配
    val alpha = if (isSystemInDarkTheme()) 0.8f else 1f
    Image(
        painter = painterResource(id = R.drawable.ic_logo),  // 实际项目传入具体资源ID
        modifier = Modifier.alpha(alpha),
        contentDescription = null
    )
}
*/