package com.equiptrack.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility

/**
 * 通用空状态卡片组件
 *
 * 用于列表为空时展示占位提示，支持：
 * - 自定义图标和提示文案
 * - 可选的操作按钮（如"清除搜索条件"），通过 AnimatedVisibility 实现淡入淡出
 * - 卡片背景颜色动画过渡 + 内容尺寸变化动画（animateContentSize）
 *
 * @param message  提示文案，支持 \n 换行
 * @param icon     顶部图标，默认为搜索无结果图标
 * @param onRetry  操作按钮回调，为 null 时隐藏按钮
 * @param retryText 操作按钮文案
 */
@Composable
fun EmptyStateCard(
    message: String,
    icon: ImageVector = Icons.Outlined.SearchOff,
    onRetry: (() -> Unit)? = null,
    retryText: String = "清除搜索条件"
) {
    // 背景色动画：主题切换时平滑过渡
    val containerTarget = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val containerColor by animateColorAsState(containerTarget, tween(220), label = "emptyContainer")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // 按钮显示/隐藏时卡片高度平滑变化
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 操作按钮区域：仅在 onRetry 非空时淡入显示
            AnimatedVisibility(visible = onRetry != null, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { onRetry?.invoke() }) {
                        Text(retryText)
                    }
                }
            }
        }
    }
}
