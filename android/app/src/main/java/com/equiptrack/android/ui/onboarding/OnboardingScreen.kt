package com.equiptrack.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "智能物资管理",
                description = "一站式解决物资借还、审批与审计难题，让设备流转井井有条。",
                icon = Icons.Default.Dashboard,
                color = Color(0xFF6200EE) // Primary Purple
            ),
            OnboardingPageData(
                title = "扫码借还，拍照留证",
                description = "支持二维码快速扫描录入，借还过程强制拍照上传，状态真实可见。",
                icon = Icons.Default.QrCodeScanner,
                color = Color(0xFF03DAC5) // Teal
            ),
            OnboardingPageData(
                title = "多级审批，即时通知",
                description = "灵活配置部门审批流程，申请消息实时推送，移动端随时随地轻松处理。",
                icon = Icons.Default.AssignmentTurnedIn,
                color = Color(0xFFFF5722) // Orange
            ),
            OnboardingPageData(
                title = "全程追溯，安全无忧",
                description = "详尽的操作日志与历史审计记录，让每一次物资流转都清晰透明，有迹可循。",
                icon = Icons.Default.History,
                color = Color(0xFF2196F3) // Blue
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedTextButton(onClick = {
                    viewModel.completeOnboarding()
                    onFinish()
                }) {
                    Text("跳过")
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(
                    pageData = pages[pageIndex],
                    pagerState = pagerState,
                    pageIndex = pageIndex
                )
            }

            // Bottom Section: Indicators and Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    Modifier
                        .height(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 10.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "indicatorWidth"
                        )
                        val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .height(10.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                
                AnimatedButton(
                    onClick = {
                        if (!isLastPage) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.completeOnboarding()
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Crossfade(targetState = isLastPage, label = "buttonText") { lastPage ->
                        Text(
                            text = if (lastPage) "开始使用" else "下一步",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPageContent(
    pageData: OnboardingPageData,
    pagerState: PagerState,
    pageIndex: Int
) {
    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
    
    // Animation calculations based on scroll offset
    val scale by animateFloatAsState(
        targetValue = if (pageIndex == pagerState.currentPage) 1f else 0.8f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    val alpha = lerp(
        start = 0.5f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon Circle
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    rotationZ = pageOffset * -20f // Slight rotation on scroll
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            pageData.color.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(pageData.color.copy(alpha = 0.1f), CircleShape)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = pageData.icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = pageData.color
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title with slide up animation
        Text(
            text = pageData.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer {
                translationY = pageOffset * 50f
                this.alpha = alpha
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = pageData.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    translationY = pageOffset * 100f // Parallax effect
                    this.alpha = alpha
                }
        )
    }
}
