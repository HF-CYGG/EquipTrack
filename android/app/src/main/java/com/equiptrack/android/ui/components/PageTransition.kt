@file:OptIn(ExperimentalAnimationApi::class)

package com.equiptrack.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 页面过渡动画类型
 */
enum class PageTransitionType {
    SLIDE_HORIZONTAL,  // 水平滑动
    SLIDE_VERTICAL,    // 垂直滑动
    FADE,              // 淡入淡出
    SCALE,             // 缩放
    SLIDE_UP,          // 从底部滑入
    SLIDE_DOWN         // 从顶部滑入
}

/**
 * 页面过渡动画组件
 */
private fun getEnterTransition(type: PageTransitionType, duration: Int): EnterTransition {
    val easing = FastOutSlowInEasing
    return when (type) {
        PageTransitionType.SLIDE_HORIZONTAL -> slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeIn(animationSpec = tween(duration))
        PageTransitionType.SLIDE_VERTICAL -> slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeIn(animationSpec = tween(duration))
        PageTransitionType.FADE -> fadeIn(animationSpec = tween(duration, easing = LinearEasing))
        PageTransitionType.SCALE -> scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(duration, easing = easing)
        ) + fadeIn(animationSpec = tween(duration))
        PageTransitionType.SLIDE_UP -> slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeIn(animationSpec = tween(duration))
        PageTransitionType.SLIDE_DOWN -> slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeIn(animationSpec = tween(duration))
    }
}

private fun getExitTransition(type: PageTransitionType, duration: Int): ExitTransition {
    val easing = FastOutSlowInEasing
    return when (type) {
        PageTransitionType.SLIDE_HORIZONTAL -> slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeOut(animationSpec = tween(duration))
        PageTransitionType.SLIDE_VERTICAL -> slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeOut(animationSpec = tween(duration))
        PageTransitionType.FADE -> fadeOut(animationSpec = tween(duration, easing = LinearEasing))
        PageTransitionType.SCALE -> scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(duration, easing = easing)
        ) + fadeOut(animationSpec = tween(duration))
        PageTransitionType.SLIDE_UP -> slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeOut(animationSpec = tween(duration))
        PageTransitionType.SLIDE_DOWN -> slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(duration, easing = easing)
        ) + fadeOut(animationSpec = tween(duration))
    }
}

/**
 * 页面过渡动画组件 (State version)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PageTransition(
    visibleState: MutableTransitionState<Boolean>,
    transitionType: PageTransitionType = PageTransitionType.SLIDE_HORIZONTAL,
    durationMillis: Int = 300,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visibleState = visibleState,
        enter = getEnterTransition(transitionType, durationMillis),
        exit = getExitTransition(transitionType, durationMillis),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 页面过渡动画组件 (Boolean version)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PageTransition(
    visible: Boolean,
    transitionType: PageTransitionType = PageTransitionType.SLIDE_HORIZONTAL,
    durationMillis: Int = 300,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = getEnterTransition(transitionType, durationMillis),
        exit = getExitTransition(transitionType, durationMillis),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 带有过渡动画的页面容器
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedPage(
    modifier: Modifier = Modifier,
    transitionType: PageTransitionType = PageTransitionType.FADE,
    durationMillis: Int = 200,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    
    PageTransition(
        visibleState = visibleState,
        transitionType = transitionType,
        durationMillis = durationMillis,
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}

/**
 * 页面切换过渡动画
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PageSwitchTransition(
    currentPage: Int,
    modifier: Modifier = Modifier,
    transitionType: PageTransitionType = PageTransitionType.SLIDE_HORIZONTAL,
    durationMillis: Int = 300,
    content: @Composable (Int) -> Unit
) {
    val transition = updateTransition(
        targetState = currentPage,
        label = "page_switch"
    )
    
    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            when (transitionType) {
                PageTransitionType.SLIDE_HORIZONTAL -> {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
                
                PageTransitionType.SLIDE_VERTICAL -> {
                    if (targetState > initialState) {
                        slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
                    } else {
                        slideInVertically { -it } + fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
                
                PageTransitionType.FADE -> {
                    fadeIn(animationSpec = tween(durationMillis)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis))
                }
                
                PageTransitionType.SCALE -> {
                    scaleIn(initialScale = 0.8f) + fadeIn() togetherWith
                    scaleOut(targetScale = 1.2f) + fadeOut()
                }
                
                PageTransitionType.SLIDE_UP -> {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { it } + fadeOut()
                }
                
                PageTransitionType.SLIDE_DOWN -> {
                    slideInVertically { -it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                }
            }.using(SizeTransform(clip = false))
        }
    ) { page ->
        content(page)
    }
}

@Composable
fun AnimatedListItem(
    enabled: Boolean,
    listAnimationType: String,
    index: Int = 0,
    // 移除 lazyListState，避免滚动时的全量重组
    content: @Composable () -> Unit
) {
    if (!enabled || listAnimationType == "None") {
        content()
        return
    }

    // 仅前几项应用级联延迟，避免长列表滚动时的等待
    val isInitialItem = index < 6
    
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (isInitialItem) {
            // 大幅降低延迟时间，提升响应速度
            // 从 80ms/item 降至 30ms/item，最大延迟限制在 200ms
            val delay = (index * 30).toLong().coerceAtMost(200) 
            if (delay > 0) kotlinx.coroutines.delay(delay)
        }
        visible = true
    }

    // 统一使用中等刚度，既有弹性又不会感觉拖沓
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "itemAlpha"
    )
    
    // 减小位移距离，从 1000f 降至 400f，减少视觉负担和渲染压力
    val offsetX by animateFloatAsState(
        targetValue = if (visible) 0f else 400f, 
        animationSpec = animationSpec,
        label = "itemOffset"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = animationSpec,
        label = "itemScale"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            if (listAnimationType == "Slide") {
                this.translationX = offsetX
                this.scaleX = scale
                this.scaleY = scale
            }
        }
    ) {
        content()
    }
}
