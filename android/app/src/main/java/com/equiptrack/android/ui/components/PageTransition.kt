@file:OptIn(ExperimentalAnimationApi::class)

package com.equiptrack.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PageTransition(
    visible: Boolean,
    transitionType: PageTransitionType = PageTransitionType.SLIDE_HORIZONTAL,
    durationMillis: Int = 300,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (transitionType) {
        PageTransitionType.SLIDE_HORIZONTAL -> {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        PageTransitionType.SLIDE_VERTICAL -> {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        PageTransitionType.FADE -> {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis, easing = LinearEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis, easing = LinearEasing)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        PageTransitionType.SCALE -> {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis)),
                exit = scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        PageTransitionType.SLIDE_UP -> {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis)),
                modifier = modifier
            ) {
                content()
            }
        }
        
        PageTransitionType.SLIDE_DOWN -> {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis)),
                modifier = modifier
            ) {
                content()
            }
        }
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
    durationMillis: Int = 300,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    PageTransition(
        visible = visible,
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
                        slideInHorizontally { it } + fadeIn() with
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() with
                        slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
                
                PageTransitionType.SLIDE_VERTICAL -> {
                    if (targetState > initialState) {
                        slideInVertically { it } + fadeIn() with
                        slideOutVertically { -it } + fadeOut()
                    } else {
                        slideInVertically { -it } + fadeIn() with
                        slideOutVertically { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }
                
                PageTransitionType.FADE -> {
                    fadeIn(animationSpec = tween(durationMillis)) with
                    fadeOut(animationSpec = tween(durationMillis))
                }
                
                PageTransitionType.SCALE -> {
                    scaleIn(initialScale = 0.8f) + fadeIn() with
                    scaleOut(targetScale = 1.2f) + fadeOut()
                }
                
                PageTransitionType.SLIDE_UP -> {
                    slideInVertically { it } + fadeIn() with
                    slideOutVertically { it } + fadeOut()
                }
                
                PageTransitionType.SLIDE_DOWN -> {
                    slideInVertically { -it } + fadeIn() with
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
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    
    // Calculate staggered delay based on index
    // Limit delay to first 10 items to avoid long waits for large lists
    val delay = (index % 10) * 50
    
    val enterTransition = when (listAnimationType) {
        "Fade" -> fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = delay)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 300, delayMillis = delay)
        )
        "Slide" -> slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 350, delayMillis = delay, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = delay)
        )
        else -> fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = delay)
        )
    }
    
    AnimatedVisibility(
        visibleState = transitionState,
        enter = enterTransition,
        exit = ExitTransition.None
    ) {
        content()
    }
}
