package com.equiptrack.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 加载动画类型
 */
enum class LoadingAnimationType {
    CIRCULAR,           // 圆形旋转
    DOTS,              // 跳动点
    PULSE,             // 脉冲
    WAVE,              // 波浪
    SKELETON           // 骨架屏闪烁
}

/**
 * 通用加载动画组件
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    type: LoadingAnimationType = LoadingAnimationType.CIRCULAR,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    when (type) {
        LoadingAnimationType.CIRCULAR -> CircularLoadingAnimation(modifier, size, color)
        LoadingAnimationType.DOTS -> DotsLoadingAnimation(modifier, size, color)
        LoadingAnimationType.PULSE -> PulseLoadingAnimation(modifier, size, color)
        LoadingAnimationType.WAVE -> WaveLoadingAnimation(modifier, size, color)
        LoadingAnimationType.SKELETON -> SkeletonLoadingAnimation(modifier, size, color)
    }
}

/**
 * 圆形旋转加载动画
 */
@Composable
private fun CircularLoadingAnimation(
    modifier: Modifier,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        rotate(rotation) {
            drawCircularIndicator(color)
        }
    }
}

/**
 * 跳动点加载动画
 */
@Composable
private fun DotsLoadingAnimation(
    modifier: Modifier,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_loading")
    
    Row(
        modifier = modifier.width(size * 2),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )
            
            Canvas(
                modifier = Modifier.size(size / 4)
            ) {
                drawCircle(
                    color = color,
                    radius = this.size.minDimension / 2 * scale
                )
            }
        }
    }
}

/**
 * 脉冲加载动画
 */
@Composable
private fun PulseLoadingAnimation(
    modifier: Modifier,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = this.size.minDimension / 2 * scale
        )
    }
}

/**
 * 波浪加载动画
 */
@Composable
private fun WaveLoadingAnimation(
    modifier: Modifier,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_loading")
    
    Row(
        modifier = modifier.width(size * 1.5f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_height_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(size / 8)
                    .height(size * height)
                    .clip(RoundedCornerShape(size / 16))
                    .background(color)
            )
        }
    }
}

/**
 * 骨架屏闪烁动画
 */
@Composable
private fun SkeletonLoadingAnimation(
    modifier: Modifier,
    size: Dp,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = alpha))
    )
}

/**
 * 绘制圆形指示器
 */
private fun DrawScope.drawCircularIndicator(color: Color) {
    val strokeWidth = size.minDimension * 0.1f
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // 绘制背景圆环
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
    )
    
    // 绘制进度弧
    val sweepAngle = 90f
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}

/**
 * 带文本的加载组件
 */
@Composable
fun LoadingWithText(
    text: String = "加载中...",
    modifier: Modifier = Modifier,
    animationType: LoadingAnimationType = LoadingAnimationType.CIRCULAR,
    animationSize: Dp = 32.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingAnimation(
            type = animationType,
            size = animationSize
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 全屏加载覆盖层
 */
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    text: String = "加载中...",
    modifier: Modifier = Modifier,
    animationType: LoadingAnimationType = LoadingAnimationType.CIRCULAR
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LoadingWithText(
                    text = text,
                    animationType = animationType,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}