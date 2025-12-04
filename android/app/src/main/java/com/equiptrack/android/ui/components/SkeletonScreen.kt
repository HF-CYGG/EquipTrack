package com.equiptrack.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 骨架屏基础组件
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
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
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
    )
}

/**
 * 闪烁效果骨架屏组件
 */
@Composable
fun ShimmerSkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_skeleton")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.horizontalGradient(
                    colors = shimmerColors,
                    startX = translateAnim - 150f,
                    endX = translateAnim + 150f
                )
            )
    )
}

/**
 * 圆形头像骨架屏
 */
@Composable
fun SkeletonAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    SkeletonBox(
        modifier = modifier.size(size),
        height = size,
        cornerRadius = size / 2
    )
}

/**
 * 文本行骨架屏
 */
@Composable
fun SkeletonTextLine(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 16.dp
) {
    SkeletonBox(
        modifier = modifier.width(width),
        height = height
    )
}

/**
 * 多行文本骨架屏
 */
@Composable
fun SkeletonTextBlock(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    lineHeight: Dp = 16.dp,
    lineSpacing: Dp = 8.dp
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(lineSpacing)
    ) {
        repeat(lines) { index ->
            val width = when (index) {
                lines - 1 -> 0.7f // 最后一行较短
                else -> 1f
            }
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(width),
                height = lineHeight
            )
        }
    }
}

/**
 * 卡片骨架屏
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
    showImage: Boolean = false,
    imageHeight: Dp = 120.dp
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部信息（头像 + 标题）
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAvatar) {
                    SkeletonAvatar(size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SkeletonTextLine(width = 120.dp, height = 18.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonTextLine(width = 80.dp, height = 14.dp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 图片占位
            if (showImage) {
                SkeletonBox(
                    modifier = Modifier.fillMaxWidth(),
                    height = imageHeight,
                    cornerRadius = 8.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 内容文本
            SkeletonTextBlock(lines = 2)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 底部操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier.width(60.dp),
                    height = 32.dp,
                    cornerRadius = 16.dp
                )
                SkeletonBox(
                    modifier = Modifier.width(60.dp),
                    height = 32.dp,
                    cornerRadius = 16.dp
                )
            }
        }
    }
}

/**
 * 列表项骨架屏
 */
@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
    showTrailing: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            SkeletonAvatar(size = 48.dp)
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            SkeletonTextLine(width = 140.dp, height = 18.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonTextLine(width = 100.dp, height = 14.dp)
        }
        
        if (showTrailing) {
            SkeletonBox(
                modifier = Modifier.size(24.dp),
                height = 24.dp,
                cornerRadius = 4.dp
            )
        }
    }
}

/**
 * 设备列表骨架屏
 */
@Composable
fun EquipmentListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(itemCount) {
            SkeletonCard(
                showAvatar = false,
                showImage = true,
                imageHeight = 100.dp
            )
        }
    }
}

/**
 * 用户列表骨架屏
 */
@Composable
fun UserListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(itemCount) {
            SkeletonListItem(
                showAvatar = true,
                showTrailing = true
            )
            if (it < itemCount - 1) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        }
    }
}

/**
 * 历史记录骨架屏
 */
@Composable
fun HistoryListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonTextLine(width = 100.dp, height = 16.dp)
                        SkeletonBox(
                            modifier = Modifier.width(60.dp),
                            height = 24.dp,
                            cornerRadius = 12.dp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SkeletonTextLine(width = 150.dp, height = 18.dp)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    SkeletonTextLine(width = 120.dp, height = 14.dp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SkeletonTextLine(width = 80.dp, height = 12.dp)
                        SkeletonTextLine(width = 80.dp, height = 12.dp)
                    }
                }
            }
        }
    }
}

/**
 * 审批列表骨架屏
 */
@Composable
fun ApprovalListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonAvatar(size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            SkeletonTextLine(width = 120.dp, height = 16.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonTextLine(width = 80.dp, height = 14.dp)
                        }
                        SkeletonBox(
                            modifier = Modifier.width(60.dp),
                            height = 24.dp,
                            cornerRadius = 12.dp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SkeletonTextBlock(lines = 2, lineHeight = 14.dp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBox(
                            modifier = Modifier.width(60.dp),
                            height = 32.dp,
                            cornerRadius = 16.dp
                        )
                        SkeletonBox(
                            modifier = Modifier.width(60.dp),
                            height = 32.dp,
                            cornerRadius = 16.dp
                        )
                    }
                }
            }
        }
    }
}