package com.equiptrack.android.ui.equipment.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.equiptrack.android.data.model.BorrowHistoryEntry
import com.equiptrack.android.data.model.ReturnRequest
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.CameraCapture
import com.equiptrack.android.utils.CameraUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReturnItemDialog(
    historyEntry: BorrowHistoryEntry,
    isForced: Boolean = false,
    adminName: String? = null,
    currentUserRole: com.equiptrack.android.data.model.UserRole? = null,
    onDismiss: () -> Unit,
    onConfirm: (ReturnRequest) -> Unit
) {
    val context = LocalContext.current
    
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val isOverdue = Date().after(historyEntry.expectedReturnDate)

    // Determine if photo is mandatory
    val isPhotoRequired = remember(isForced, currentUserRole) {
        if (!isForced) {
            // Normal return always requires photo
            true 
        } else {
            // Forced return:
            // Admin/Super Admin -> NOT required
            // Advanced User -> REQUIRED
            val role = currentUserRole
            if (role == com.equiptrack.android.data.model.UserRole.SUPER_ADMIN || 
                role == com.equiptrack.android.data.model.UserRole.ADMIN) {
                false
            } else {
                true
            }
        }
    }

    LaunchedEffect(capturedImageUri) {
        capturedImageUri?.let { uri ->
            val base64 = CameraUtils.imageUriToBase64(context, uri)
            photoBase64 = base64
            photoError = null
        }
    }
    
    if (showCamera) {
        Dialog(
            onDismissRequest = { showCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraCapture(
                onImageCaptured = { uri ->
                    capturedImageUri = uri
                    showCamera = false
                },
                onError = { exception ->
                    photoError = "拍照失败: ${exception.message}"
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f) // Reduced height from 0.85f
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(20.dp), // Reduced corner radius from 28.dp
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Reduced elevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp) // Reduced padding from 24.dp
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isForced) "强制归还" else "归还物资",
                            style = MaterialTheme.typography.titleLarge, // Reduced typography
                            fontWeight = FontWeight.Bold,
                            color = if (isForced) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                .size(28.dp) // Reduced size from 32.dp
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Close", 
                                modifier = Modifier.size(16.dp), // Reduced icon size
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(top = 16.dp, bottom = 0.dp), // Reduced padding
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp), // Reduced padding
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
                    ) {
                        // 1. Item Info Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp), // Reduced padding
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced spacing
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(40.dp), // Reduced size
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AssignmentReturn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp) // Reduced icon size
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing
                                Text(
                                    text = historyEntry.itemName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // Info Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "借用人",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = historyEntry.borrowerName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "联系方式",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = historyEntry.borrowerContact,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "借用日期",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormat.format(historyEntry.borrowDate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "预期归还",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormat.format(historyEntry.expectedReturnDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // 2. Alerts
                        if (isOverdue) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp) // Reduced size
                                )
                                Spacer(modifier = Modifier.width(8.dp)) // Reduced spacing
                                Text(
                                    text = "该物资已逾期，请确认完好无损后归还",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        if (isForced && adminName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp) // Reduced size
                                )
                                Spacer(modifier = Modifier.width(8.dp)) // Reduced spacing
                                Text(
                                    text = "由管理员 $adminName 执行强制归还",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 3. Photo Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isPhotoRequired) "归还拍照记录 (必须)" else "归还拍照记录 (可选)",
                                style = MaterialTheme.typography.labelMedium, // Reduced typography
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (capturedImageUri != null && photoBase64 != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp) // Reduced height
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    val bitmap = CameraUtils.base64ToBitmap(photoBase64!!)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        IconButton(
                                            onClick = { capturedImageUri = null; photoBase64 = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else {
                                val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                                val color = MaterialTheme.colorScheme.outlineVariant
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp) // Reduced height
                                        .clip(RoundedCornerShape(12.dp))
                                        .drawBehind {
                                            drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(12.dp.toPx()))
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .clickable { showCamera = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp) // Reduced size
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "点击拍摄归还照片",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            if (photoError != null) {
                                Text(
                                    text = photoError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp), // Reduced height
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                if (isPhotoRequired && photoBase64.isNullOrBlank()) {
                                    photoError = "请拍照记录归还状态"
                                    return@AnimatedButton
                                }
                                
                                val returnRequest = ReturnRequest(
                                    photo = photoBase64 ?: "", // Pass empty string if optional and not taken (backend should handle or not care)
                                    isForced = isForced,
                                    adminName = adminName
                                )
                                onConfirm(returnRequest)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp), // Reduced height
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isForced) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                        ) {
                            Text(if (isForced) "确认强制归还" else "确认归还")
                        }
                    }
                }
            }
        }
    }
}
