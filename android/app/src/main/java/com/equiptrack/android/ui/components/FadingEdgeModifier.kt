package com.equiptrack.android.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalFadingEdge(
    topFadeHeight: Dp = 0.dp,
    bottomFadeHeight: Dp = 0.dp
) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        if (topFadeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topFadeHeight.toPx()
                ),
                size = Size(size.width, topFadeHeight.toPx()),
                blendMode = BlendMode.DstIn
            )
        }

        if (bottomFadeHeight > 0.dp) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottomFadeHeight.toPx(),
                    endY = size.height
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - bottomFadeHeight.toPx()),
                size = Size(size.width, bottomFadeHeight.toPx()),
                blendMode = BlendMode.DstIn
            )
        }
    }
