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

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var shape: ConfettiShape = if (Random.nextBoolean()) ConfettiShape.Rect else ConfettiShape.Circle
)

enum class ConfettiShape {
    Rect, Circle
}

@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (!visible) return

    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    // Use a standard ArrayList instead of mutableStateListOf to avoid state observation overhead in tight loop
    val particles = remember { ArrayList<ConfettiParticle>(100) }
    var lastFrameTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    // State to trigger recomposition for drawing
    var frameTick by remember { mutableStateOf(0L) }

    // Initialize particles when visible becomes true
    LaunchedEffect(visible, size) {
        if (visible && size.width > 0 && size.height > 0) {
            // Reset and spawn particles
            particles.clear()
            repeat(100) { // Number of particles
                particles.add(
                    createParticle(
                        width = size.width.toFloat(),
                        height = size.height.toFloat()
                    )
                )
            }
            isRunning = true
            lastFrameTime = 0L
        }
    }

    // Animation Loop
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive && particles.isNotEmpty()) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTime == 0L) {
                        lastFrameTime = frameTimeNanos
                        return@withFrameNanos
                    }

                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f // Delta time in seconds
                    lastFrameTime = frameTimeNanos

                    // Update particles (Iterate backwards to safely remove)
                    val iterator = particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        
                        // Physics
                        p.x += p.vx * dt * 60 // Scale for 60fps baseline
                        p.y += p.vy * dt * 60
                        p.vy += 0.5f * dt * 60 // Gravity
                        p.rotation += p.rotationSpeed * dt * 60
                        
                        // Air resistance / Drag
                        p.vx *= 0.99f
                        p.vy *= 0.99f

                        // Remove if out of bounds
                        if (p.y > size.height + 50) {
                            iterator.remove()
                        }
                    }
                    
                    // Force redraw by updating state
                    frameTick = frameTimeNanos

                    if (particles.isEmpty()) {
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
            // Read frameTick to subscribe to updates
            val tick = frameTick
            if (isRunning && tick > 0) {
                // Batch draw calls if possible or just iterate fast
                // Using standard loop instead of forEach for slight perf gain on ArrayList
                for (i in 0 until particles.size) {
                    val p = particles[i]
                    withTransform({
                        rotate(degrees = p.rotation, pivot = Offset(p.x, p.y))
                    }) {
                        when (p.shape) {
                            ConfettiShape.Rect -> {
                                drawRect(
                                    color = p.color,
                                    topLeft = Offset(p.x - p.size / 2, p.y - p.size / 2),
                                    size = Size(p.size, p.size * 0.6f) // Slightly rectangular
                                )
                            }
                            ConfettiShape.Circle -> {
                                drawCircle(
                                    color = p.color,
                                    center = Offset(p.x, p.y),
                                    radius = p.size / 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createParticle(width: Float, height: Float): ConfettiParticle {
    val colors = listOf(
        Color(0xFFFFC107), // Amber
        Color(0xFF2196F3), // Blue
        Color(0xFFF44336), // Red
        Color(0xFF4CAF50), // Green
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0)  // Purple
    )
    
    // Start from center, slightly above
    val startX = width / 2
    val startY = height / 2 - 100f
    
    // Random velocity (explosion effect)
    val angle = Random.nextDouble(0.0, Math.PI * 2)
    val speed = Random.nextDouble(10.0, 30.0)
    
    return ConfettiParticle(
        x = startX,
        y = startY,
        vx = (cos(angle) * speed).toFloat(),
        vy = (sin(angle) * speed - 15).toFloat(), // Initial upward boost
        color = colors.random(),
        size = Random.nextDouble(15.0, 25.0).toFloat(),
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = Random.nextFloat() * 10f - 5f
    )
}
