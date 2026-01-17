package com.equiptrack.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.equiptrack.android.data.model.AppVersion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    version: AppVersion,
    isUpdate: Boolean = true,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    // Determine if we are in dark mode based on the current color scheme's surface luminance
    // This respects the app-level theme override (e.g., user forces Dark Mode)
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Dialog(
        onDismissRequest = { if (!version.forceUpdate || !isUpdate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !version.forceUpdate || !isUpdate,
            dismissOnClickOutside = !version.forceUpdate || !isUpdate,
            usePlatformDefaultWidth = false // Allow custom width
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f) // Optimized width for mobile
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Graphic Header
                // Adjust gradient for dark mode to be less harsh or more integrated
                val gradientColors = if (isDarkTheme) {
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                } else {
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(brush = Brush.verticalGradient(colors = gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    // Decorative Circle
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .offset(y = (-40).dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.05f else 0.1f),
                                CircleShape
                            )
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isUpdate) Icons.Default.Refresh else Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isUpdate) "发现新版本" else "版本详情",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface // Use onSurface for better contrast in both modes
                        )
                        Text(
                            text = "v${version.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. Content Body
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    // Meta Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UpdateTag(type = version.updateType)
                        
                        Text(
                            text = version.releaseDate.take(10),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Changelog Title
                    Text(
                        text = "更新内容",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Content
                    Box(
                        modifier = Modifier
                            .heightIn(max = 240.dp) // Limit height
                            .fillMaxWidth()
                            .background(
                                color = if (isDarkTheme) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.verticalScroll(scrollState)
                        ) {
                            Text(
                                text = version.updateContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }

                // 3. Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isUpdate) {
                        if (!version.forceUpdate) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Text("暂不更新")
                            }
                        }
                        
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (version.updateType == "urgent") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("立即更新")
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("我知道了")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateTag(type: String) {
    val (containerColor, contentColor, label) = when (type.lowercase()) {
        "urgent" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "紧急更新"
        )
        "major" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "重大更新"
        )
        "feature" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "功能更新"
        )
        "patch" -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "补丁修复"
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "常规更新"
        )
    }

    val isUrgent = type.lowercase() == "urgent"
    
    // Breathing animation for urgent updates
    val alpha = if (isUrgent) {
        val infiniteTransition = rememberInfiniteTransition(label = "UrgentAlpha")
        val a by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Alpha"
        )
        a
    } else {
        1f
    }
    
    Surface(
        color = containerColor.copy(alpha = alpha),
        shape = CircleShape,
        modifier = Modifier.height(32.dp),
        contentColor = contentColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (isUrgent) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(contentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
