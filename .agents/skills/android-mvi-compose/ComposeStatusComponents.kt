import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * 全屏加载状态页
 * 居中显示圆形进度条
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 空数据状态页
 * 整个区域可点击重试 + 提供明显的重新加载按钮
 *
 * @param msg 自定义提示文案
 * @param iconRes 空状态图标资源ID（需在项目中定义，如 R.drawable.ic_empty_default）
 * @param onRetry 重试回调（整个区域和按钮均触发）
 */
@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    msg: String? = null,
    @DrawableRes iconRes: Int,  // 由调用方传入具体项目的 R.drawable.xxx
    onRetry: (() -> Unit)? = null
) {
    // 整个区域可点击重试
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onRetry != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRetry
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "空数据",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = msg ?: "暂无数据",  // 实际项目应使用 stringResource(R.string.empty_data)
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击页面重新加载",  // 实际项目应使用 stringResource
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重新加载")  // 实际项目应使用 stringResource
            }
        }
    }
}

/**
 * 错误状态页
 * 整个区域可点击重试 + 提供明显的重新加载按钮
 *
 * @param msg 错误信息
 * @param iconRes 错误图标资源ID（需在项目中定义，如 R.drawable.ic_error_default）
 * @param onRetry 重试回调
 */
@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    msg: String? = null,
    @DrawableRes iconRes: Int,  // 由调用方传入具体项目的 R.drawable.xxx
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onRetry != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRetry
                ) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "加载失败",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = msg ?: "加载失败，请重试",  // 实际项目应使用 stringResource(R.string.error_load_failed)
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击页面重新加载",  // 实际项目应使用 stringResource
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("重新加载")  // 实际项目应使用 stringResource
            }
        }
    }
}

/**
 * 加载中弹窗（半透明遮罩 + 居中 loading）
 * 不遮挡底层内容，适用于提交操作
 */
@Composable
fun LoadingDialog(
    msg: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                if (!msg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}