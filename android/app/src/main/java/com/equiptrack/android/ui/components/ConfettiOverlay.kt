package com.equiptrack.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Deep optimization using Structure of Arrays (SoA) to avoid object allocation
private const val PARTICLE_COUNT = 100

@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (!visible) return

    var size by remember { mutableStateOf(IntSize.Zero) }
    
    // Structure of Arrays (SoA)
    // Using primitive arrays to avoid boxing and object overhead
    val x = remember { FloatArray(PARTICLE_COUNT) }
    val y = remember { FloatArray(PARTICLE_COUNT) }
    val vx = remember { FloatArray(PARTICLE_COUNT) }
    val vy = remember { FloatArray(PARTICLE_COUNT) }
    val sizes = remember { FloatArray(PARTICLE_COUNT) }
    val rotations = remember { FloatArray(PARTICLE_COUNT) }
    val rotationSpeeds = remember { FloatArray(PARTICLE_COUNT) }
    val colors = remember { IntArray(PARTICLE_COUNT) } // Store ARGB Int
    val shapes = remember { IntArray(PARTICLE_COUNT) } // 0: Rect, 1: Circle
    val active = remember { BooleanArray(PARTICLE_COUNT) }
    
    var lastFrameTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var activeCount by remember { mutableStateOf(0) }
    
    // State to trigger recomposition for drawing
    var frameTick by remember { mutableStateOf(0L) }

    // Colors cache
    val particleColors = remember {
        listOf(
            0xFFFFC107.toInt(), // Amber
            0xFF2196F3.toInt(), // Blue
            0xFFF44336.toInt(), // Red
            0xFF4CAF50.toInt(), // Green
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt()  // Purple
        ).toIntArray()
    }

    LaunchedEffect(visible, size) {
        if (visible && size.width > 0 && size.height > 0) {
            // Initialize particles
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            val startX = width / 2
            val startY = height / 2 - 100f
            
            for (i in 0 until PARTICLE_COUNT) {
                // Random velocity (explosion effect)
                val angle = Random.nextDouble(0.0, Math.PI * 2)
                val speed = Random.nextDouble(10.0, 30.0)
                
                x[i] = startX
                y[i] = startY
                vx[i] = (cos(angle) * speed).toFloat()
                vy[i] = (sin(angle) * speed - 15).toFloat()
                
                sizes[i] = Random.nextDouble(15.0, 25.0).toFloat()
                rotations[i] = Random.nextFloat() * 360f
                rotationSpeeds[i] = Random.nextFloat() * 10f - 5f
                
                colors[i] = particleColors[Random.nextInt(particleColors.size)]
                shapes[i] = if (Random.nextBoolean()) 0 else 1
                active[i] = true
            }
            activeCount = PARTICLE_COUNT
            isRunning = true
            lastFrameTime = 0L
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive && activeCount > 0) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTime == 0L) {
                        lastFrameTime = frameTimeNanos
                        return@withFrameNanos
                    }

                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                    lastFrameTime = frameTimeNanos

                    // Update physics
                    var currentActive = 0
                    val height = size.height + 50f
                    
                    // Optimization: Use a local var for speed scale
                    val speedScale = dt * 60f
                    
                    for (i in 0 until PARTICLE_COUNT) {
                        if (active[i]) {
                            x[i] += vx[i] * speedScale
                            y[i] += vy[i] * speedScale
                            vy[i] += 0.5f * speedScale // Gravity
                            rotations[i] += rotationSpeeds[i] * speedScale
                            
                            vx[i] *= 0.99f
                            vy[i] *= 0.99f
                            
                            if (y[i] > height) {
                                active[i] = false
                            } else {
                                currentActive++
                            }
                        }
                    }
                    
                    activeCount = currentActive
                    frameTick = frameTimeNanos
                    
                    if (activeCount == 0) {
                        isRunning = false
                        onFinished()
                    }
                }
            }
        }
    }

    if (visible || isRunning) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
        ) {
            // Read frameTick to subscribe
            val tick = frameTick
            if (isRunning && tick > 0) {
                for (i in 0 until PARTICLE_COUNT) {
                    if (active[i]) {
                        val color = Color(colors[i])
                        if (shapes[i] == 1) { // Circle
                            drawCircle(
                                color = color,
                                center = Offset(x[i], y[i]),
                                radius = sizes[i] / 2
                            )
                        } else { // Rect
                            withTransform({
                                rotate(degrees = rotations[i], pivot = Offset(x[i], y[i]))
                            }) {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(x[i] - sizes[i] / 2, y[i] - sizes[i] / 2),
                                    size = Size(sizes[i], sizes[i] * 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
