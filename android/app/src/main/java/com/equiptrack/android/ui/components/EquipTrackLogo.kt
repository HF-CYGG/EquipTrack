package com.equiptrack.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill

import androidx.compose.material3.MaterialTheme

@Composable
fun EquipTrackLogo(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    outerTriangleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    innerTriangleColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        
        // Draw Background Rounded Rectangle
        drawRoundRect(
            color = backgroundColor,
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
        )

        // Draw Outer Triangle (White)
        // Path Data: M54,24 L84,84 L24,84 Z (based on 108x108 viewport)
        // Normalized: Top(0.5, 0.222), BR(0.778, 0.778), BL(0.222, 0.778)
        val outerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.2222f)
            lineTo(w * 0.7778f, h * 0.7778f)
            lineTo(w * 0.2222f, h * 0.7778f)
            close()
        }
        drawPath(
            path = outerPath,
            color = outerTriangleColor,
            style = Fill
        )

        // Draw Inner Triangle (Dark Teal)
        // Path Data: M54,36 L72,78 L36,78 Z (based on 108x108 viewport)
        // Normalized: Top(0.5, 0.333), BR(0.667, 0.722), BL(0.333, 0.722)
        val innerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.3333f)
            lineTo(w * 0.6667f, h * 0.7222f)
            lineTo(w * 0.3333f, h * 0.7222f)
            close()
        }
        drawPath(
            path = innerPath,
            color = innerTriangleColor,
            style = Fill
        )
    }
}
