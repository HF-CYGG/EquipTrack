package com.equiptrack.android.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val pages = remember(primary, secondary, tertiary, error) {
        listOf(
            OnboardingPageData(
                title = "智能物资管理",
                description = "一站式解决物资借还、审批与审计难题，让设备流转井井有条。",
                icon = Icons.Default.Dashboard,
                color = primary
            ),
            OnboardingPageData(
                title = "扫码借还，拍照留证",
                description = "支持二维码快速扫描录入，借还过程强制拍照上传，状态真实可见。",
                icon = Icons.Default.QrCodeScanner,
                color = secondary
            ),
            OnboardingPageData(
                title = "多级审批，即时通知",
                description = "灵活配置部门审批流程，申请消息实时推送，移动端随时随地轻松处理。",
                icon = Icons.Default.AssignmentTurnedIn,
                color = tertiary
            ),
            OnboardingPageData(
                title = "全程追溯，安全无忧",
                description = "详尽的操作日志与历史审计记录，让每一次物资流转都清晰透明，有迹可循。",
                icon = Icons.Default.History,
                color = error
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Fluid Background Layer
        FluidBackground(pagerState = pagerState, pages = pages)

        // 2. Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedTextButton(onClick = {
                    viewModel.completeOnboarding()
                    onFinish()
                }) {
                    Text(
                        "跳过",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 0.dp
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
                    .padding(24.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Liquid Page Indicator
                LiquidPageIndicator(
                    pagerState = pagerState,
                    pageCount = pages.size,
                    activeColor = pages[pagerState.currentPage].color
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Action Button with Morphing Effect
                val isLastPage = pagerState.currentPage == pages.size - 1
                
                Button(
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
                        .height(56.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(16.dp)
                            clip = true
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[pagerState.currentPage].color,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp) // Squircle
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
fun FluidBackground(
    pagerState: PagerState,
    pages: List<OnboardingPageData>
) {
    val color = pages[pagerState.currentPage].color
    val nextColor = pages.getOrElse(pagerState.currentPage + 1) { pages.last() }.color
    
    // Smooth color transition based on scroll
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(500),
        label = "bgColor"
    )

    // Infinite animation for blobs
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val movement1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "m1"
    )
    val movement2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "m2"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(80.dp) // Heavy blur for "Aurora" effect
            .graphicsLayer { alpha = 0.4f }
    ) {
        val width = size.width
        val height = size.height
        
        // Blob 1: Top Left
        drawCircle(
            color = animatedColor.copy(alpha = 0.5f),
            radius = width * 0.6f,
            center = Offset(
                x = width * 0.2f + (movement1 * 100f),
                y = height * 0.2f + (movement2 * 50f)
            )
        )
        
        // Blob 2: Bottom Right
        drawCircle(
            color = animatedColor.copy(alpha = 0.4f),
            radius = width * 0.5f,
            center = Offset(
                x = width * 0.8f - (movement2 * 100f),
                y = height * 0.8f - (movement1 * 50f)
            )
        )
        
        // Blob 3: Center Moving
        drawCircle(
            color = animatedColor.copy(alpha = 0.3f),
            radius = width * 0.4f,
            center = Offset(
                x = width * 0.5f + (cos(movement1 * Math.PI).toFloat() * 100f),
                y = height * 0.5f + (sin(movement2 * Math.PI).toFloat() * 100f)
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidPageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    activeColor: Color
) {
    Row(
        modifier = Modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { iteration ->
            val isSelected = pagerState.currentPage == iteration
            
            // Microsoft-style fluid width change
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorWidth"
            )
            
            val color by animateColorAsState(
                targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                label = "indicatorColor"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
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
    
    // Parallax & Opacity calculations
    val imageOffset = pageOffset * 200f
    val titleOffset = pageOffset * 100f
    val descOffset = pageOffset * 50f
    
    val alpha = lerp(
        start = 0.0f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (pageIndex == pagerState.currentPage) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating Assembled Graphic
        AssembledGraphic(
            pageIndex = pageIndex,
            isVisible = pageIndex == pagerState.currentPage,
            color = pageData.color,
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    translationX = imageOffset // Parallax
                    rotationZ = pageOffset * -5f
                }
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Title
        Text(
            text = pageData.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer {
                translationX = titleOffset
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = pageData.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
            modifier = Modifier.graphicsLayer {
                translationX = descOffset
            }
        )
    }
}

@Composable
fun AssembledGraphic(
    pageIndex: Int,
    isVisible: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (pageIndex) {
            0 -> DashboardAssembly(isVisible, color)
            1 -> QRAssembly(isVisible, color)
            2 -> ApprovalAssembly(isVisible, color)
            3 -> HistoryAssembly(isVisible, color)
        }
    }
}

@Composable
fun DashboardAssembly(isVisible: Boolean, color: Color) {
    var internalState by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Reset
            internalState = false
            delay(100)
            while (isActive) {
                internalState = true // Assemble
                delay(3000) // Hold
                internalState = false // Disassemble
                delay(1000) // Wait
            }
        } else {
            internalState = false
        }
    }

    val transition = updateTransition(targetState = internalState, label = "Dashboard")
    
    val spread by transition.animateDp(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = 0.6f, stiffness = 50f) 
            else spring(dampingRatio = 0.6f, stiffness = 50f)
        },
        label = "spread"
    ) { visible -> if (visible) 4.dp else 60.dp }
    
    val scale by transition.animateFloat(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = 0.6f, stiffness = 50f)
            else spring(dampingRatio = 0.6f, stiffness = 50f)
        },
        label = "scale"
    ) { visible -> if (visible) 1f else 0f }

    val size = 40.dp
    
    Column(
        verticalArrangement = Arrangement.spacedBy(spread),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spread)) {
            Box(Modifier.size(size).background(color, RoundedCornerShape(8.dp)))
            Box(Modifier.size(size).background(color.copy(alpha = 0.8f), RoundedCornerShape(8.dp)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spread)) {
            Box(Modifier.size(size).background(color.copy(alpha = 0.6f), RoundedCornerShape(8.dp)))
            Box(Modifier.size(size).background(color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
fun QRAssembly(isVisible: Boolean, color: Color) {
    var internalState by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            internalState = false
            delay(100)
            while (isActive) {
                internalState = true // Focus
                delay(4000) // Scanning time
                internalState = false // Expand
                delay(1000)
            }
        } else {
            internalState = false
        }
    }

    val transition = updateTransition(targetState = internalState, label = "QR")
    val expansion by transition.animateFloat(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = 0.7f, stiffness = 40f)
            else spring(dampingRatio = 0.7f, stiffness = 40f)
        },
        label = "expansion"
    ) { visible -> if (visible) 0f else 80f }
    
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    
    val density = LocalDensity.current

    Canvas(Modifier.size(100.dp)) {
        val stroke = 4.dp.toPx()
        val len = 25.dp.toPx()
        val gapPx = expansion * density.density
        
        val w = size.width
        val h = size.height

        // TL
        drawPath(
            path = Path().apply {
                moveTo(0f - gapPx, len - gapPx)
                lineTo(0f - gapPx, 0f - gapPx)
                lineTo(len - gapPx, 0f - gapPx)
            },
            color = color,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // TR
        drawPath(
            path = Path().apply {
                moveTo(w - len + gapPx, 0f - gapPx)
                lineTo(w + gapPx, 0f - gapPx)
                lineTo(w + gapPx, len - gapPx)
            },
            color = color,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // BL
        drawPath(
            path = Path().apply {
                moveTo(0f - gapPx, h - len + gapPx)
                lineTo(0f - gapPx, h + gapPx)
                lineTo(len - gapPx, h + gapPx)
            },
            color = color,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // BR
        drawPath(
            path = Path().apply {
                moveTo(w - len + gapPx, h + gapPx)
                lineTo(w + gapPx, h + gapPx)
                lineTo(w + gapPx, h - len + gapPx)
            },
            color = color,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        
        // Center Code
        val centerSize = 40.dp.toPx()
        // Fade in code when focused
        val codeAlpha = (1f - (expansion / 80f)).coerceIn(0f, 1f)
        
        if (codeAlpha > 0) {
            drawRoundRect(
                color = color.copy(alpha = 0.2f * codeAlpha),
                topLeft = Offset((w - centerSize)/2, (h - centerSize)/2),
                size = Size(centerSize, centerSize),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = color.copy(alpha = codeAlpha),
                topLeft = Offset((w - centerSize/2)/2, (h - centerSize/2)/2),
                size = Size(centerSize/2, centerSize/2),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Scan Line only when focused (internalState is true)
            if (internalState && expansion < 10f) {
                val lineY = h * scanLineY
                drawLine(
                    color = Color.Red.copy(alpha = 0.8f),
                    start = Offset(10.dp.toPx(), lineY),
                    end = Offset(w - 10.dp.toPx(), lineY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ApprovalAssembly(isVisible: Boolean, color: Color) {
    var internalState by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            internalState = false
            delay(100)
            while (isActive) {
                internalState = true // Show paper & stamp
                delay(3500) 
                internalState = false // Hide
                delay(1000)
            }
        } else {
            internalState = false
        }
    }

    val transition = updateTransition(targetState = internalState, label = "Approval")
    
    val paperY by transition.animateDp(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = 0.7f, stiffness = 40f)
            else tween(500)
        },
        label = "paperY"
    ) { if (it) 0.dp else 150.dp }
    
    val stampScale by transition.animateFloat(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = 0.5f, stiffness = 100f) // Bouncy stamp
            else tween(200)
        },
        label = "stamp"
    ) { if (it) 1f else 0f }

    // Delay stamp appearance
    val stampVisible = transition.currentState || transition.targetState
    val currentStampScale = if (transition.targetState && !transition.currentState) {
         // Entering: wait for paper
         if (paperY < 20.dp) stampScale else 0f
    } else {
         stampScale
    }

    Box(contentAlignment = Alignment.Center) {
        // Paper
        Box(
            Modifier
                .offset(y = paperY)
                .size(70.dp, 90.dp)
                .background(Color.White.copy(0.9f), RoundedCornerShape(4.dp))
                .border(1.dp, color.copy(alpha=0.3f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                 Box(Modifier.height(6.dp).fillMaxWidth().background(color.copy(0.2f), RoundedCornerShape(2.dp)))
                 Box(Modifier.height(6.dp).fillMaxWidth().background(color.copy(0.1f), RoundedCornerShape(2.dp)))
                 Box(Modifier.height(6.dp).fillMaxWidth(0.7f).background(color.copy(0.1f), RoundedCornerShape(2.dp)))
                 Box(Modifier.height(6.dp).fillMaxWidth(0.9f).background(color.copy(0.1f), RoundedCornerShape(2.dp)))
             }
        }
        
        // Stamp
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .scale(currentStampScale)
                .offset(x = 10.dp, y = 10.dp),
            tint = color
        )
    }
}

@Composable
fun HistoryAssembly(isVisible: Boolean, color: Color) {
    var internalState by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            internalState = false
            delay(100)
            while (isActive) {
                internalState = true // Grow
                delay(4000) 
                internalState = false // Reset
                delay(800)
            }
        } else {
            internalState = false
        }
    }

    val transition = updateTransition(targetState = internalState, label = "History")
    
    val p1 by transition.animateFloat(
        transitionSpec = { 
            if (targetState) tween(600, easing = LinearOutSlowInEasing)
            else tween(300)
        },
        label = "p1"
    ) { if (it) 1f else 0f }
    
    val p2 by transition.animateFloat(
        transitionSpec = { 
            if (targetState) tween(600, delayMillis = 400, easing = LinearOutSlowInEasing)
            else tween(300)
        },
        label = "p2"
    ) { if (it) 1f else 0f }
    
    val p3 by transition.animateFloat(
        transitionSpec = { 
            if (targetState) tween(600, delayMillis = 800, easing = LinearOutSlowInEasing)
            else tween(300)
        },
        label = "p3"
    ) { if (it) 1f else 0f }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
         // Node 1
         Box(Modifier.scale(p1).size(16.dp).background(color, CircleShape))
         // Line 1
         Box(Modifier.graphicsLayer{ scaleX = p1; transformOrigin = TransformOrigin(0f, 0.5f) }.size(20.dp, 4.dp).background(color.copy(0.5f)))
         // Node 2
         Box(Modifier.scale(p2).size(16.dp).background(color, CircleShape))
         // Line 2
         Box(Modifier.graphicsLayer{ scaleX = p2; transformOrigin = TransformOrigin(0f, 0.5f) }.size(20.dp, 4.dp).background(color.copy(0.5f)))
         // Node 3
         Box(Modifier.scale(p3).size(16.dp).background(color, CircleShape))
    }
}

