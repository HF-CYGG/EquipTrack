package com.equiptrack.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    Dialog(
        onDismissRequest = { if (!version.forceUpdate || !isUpdate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !version.forceUpdate || !isUpdate,
            dismissOnClickOutside = !version.forceUpdate || !isUpdate
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Text(
                    text = if (isUpdate) "发现新版本" else "版本信息",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UpdateTag(type = version.updateType)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "v${version.versionName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "发布于 ${version.releaseDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Text(
                    text = if (isUpdate) "更新内容：" else "当前版本特性：",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = version.updateContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isUpdate) {
                        if (!version.forceUpdate) {
                            TextButton(onClick = onDismiss) {
                                Text("稍后再说")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("下载更新")
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("确定")
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
            MaterialTheme.colorScheme.surfaceVariant, // Outline style simulation
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
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(containerColor, CircleShape) // Pill shape
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (isUrgent) {
            val infiniteTransition = rememberInfiniteTransition(label = "UrgentIndicator")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Scale"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(contentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}
