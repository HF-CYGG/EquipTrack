package com.equiptrack.android.ui.history.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.equiptrack.android.data.model.BorrowHistoryEntry
import com.equiptrack.android.data.model.BorrowStatus
import com.equiptrack.android.ui.theme.*
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.utils.CameraUtils
import com.equiptrack.android.utils.UrlUtils
import java.text.SimpleDateFormat
import java.util.*

import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

@Composable
fun HistoryEntryCard(
    entry: BorrowHistoryEntry,
    canForceReturn: Boolean,
    serverUrl: String,
    onReturn: () -> Unit,
    onForceReturn: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd\nHH:mm", Locale.getDefault())
    val isOverdue = entry.status == BorrowStatus.OVERDUE_NOT_RETURNED
    val overdueDays = if (isOverdue) {
        val now = Date()
        val diff = now.time - entry.expectedReturnDate.time
        (diff / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    } else 0
    val canReturn = entry.status in listOf(BorrowStatus.BORROWING, BorrowStatus.OVERDUE_NOT_RETURNED)
    var showImageDialog by remember { mutableStateOf(false) }
    
    if (showImageDialog && entry.returnPhoto != null) {
        Dialog(
            onDismissRequest = { showImageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)) // Darker background
                    .clickable { showImageDialog = false },
                contentAlignment = Alignment.Center
            ) {
                val photo = entry.returnPhoto
                if (photo.startsWith("data:image")) {
                    val bitmap = remember(photo) { CameraUtils.base64ToBitmap(photo) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "归还凭证",
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(UrlUtils.resolveImageUrl(serverUrl, photo))
                                .crossfade(false)
                                .build(),
                            contentDescription = "归还凭证",
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Fit,
                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                            placeholder = rememberVectorPainter(Icons.Default.Image)
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(UrlUtils.resolveImageUrl(serverUrl, photo))
                            .crossfade(false)
                            .build(),
                        contentDescription = "归还凭证",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Fit,
                        error = rememberVectorPainter(Icons.Default.BrokenImage),
                        placeholder = rememberVectorPainter(Icons.Default.Image)
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with item name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = entry.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (entry.borrowerName != entry.operatorName && entry.operatorName.isNotEmpty() && entry.operatorName != "系统记录") {
                            Column {
                                Text(
                                    text = "借用人: ${entry.borrowerName} (${entry.borrowerContact})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "经办人: ${entry.operatorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "借用人: ${entry.borrowerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status badge
                Surface(
                    color = when (entry.status) {
                        BorrowStatus.RETURNED -> Available.copy(alpha = 0.1f)
                        BorrowStatus.OVERDUE_RETURNED -> Warning.copy(alpha = 0.1f)
                        BorrowStatus.BORROWING -> Info.copy(alpha = 0.1f)
                        BorrowStatus.OVERDUE_NOT_RETURNED -> Error.copy(alpha = 0.1f)
                        BorrowStatus.PENDING -> Warning.copy(alpha = 0.1f)
                        BorrowStatus.APPROVED -> Available.copy(alpha = 0.1f)
                        BorrowStatus.REJECTED -> Error.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, when (entry.status) {
                        BorrowStatus.RETURNED -> Available
                        BorrowStatus.OVERDUE_RETURNED -> Warning
                        BorrowStatus.BORROWING -> Info
                        BorrowStatus.OVERDUE_NOT_RETURNED -> Error
                        BorrowStatus.PENDING -> Warning
                        BorrowStatus.APPROVED -> Available
                        BorrowStatus.REJECTED -> Error
                    })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (entry.status) {
                                BorrowStatus.RETURNED, BorrowStatus.OVERDUE_RETURNED -> Icons.Default.CheckCircle
                                BorrowStatus.BORROWING -> Icons.Default.AccessTime
                                BorrowStatus.OVERDUE_NOT_RETURNED -> Icons.Default.Warning
                                BorrowStatus.PENDING -> Icons.Default.HourglassEmpty
                                BorrowStatus.APPROVED -> Icons.Default.CheckCircle
                                BorrowStatus.REJECTED -> Icons.Default.Cancel
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when (entry.status) {
                                BorrowStatus.RETURNED -> Available
                                BorrowStatus.OVERDUE_RETURNED -> Warning
                                BorrowStatus.BORROWING -> Info
                                BorrowStatus.OVERDUE_NOT_RETURNED -> Error
                                BorrowStatus.PENDING -> Warning
                                BorrowStatus.APPROVED -> Available
                                BorrowStatus.REJECTED -> Error
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (entry.status) {
                                BorrowStatus.RETURNED -> Available
                                BorrowStatus.OVERDUE_RETURNED -> Warning
                                BorrowStatus.BORROWING -> Info
                                BorrowStatus.OVERDUE_NOT_RETURNED -> Error
                                BorrowStatus.PENDING -> Warning
                                BorrowStatus.APPROVED -> Available
                                BorrowStatus.REJECTED -> Error
                            }
                        )
                    }
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Date information Grid
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
                        text = dateFormat.format(entry.borrowDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2 // Adjust line height for better readability
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "预期归还",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(entry.expectedReturnDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) Error else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    )
                }
                
                if (entry.returnDate != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "实际归还",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(entry.returnDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                        )
                    }
                }
            }

            // Overdue warning
            if (isOverdue) {
                Surface(
                    color = Error.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Error
                        )
                        Text(
                            text = "已逾期未归还（$overdueDays 天）",
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Forced return info
            if (entry.forcedReturnBy != null) {
                 Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "由管理员 ${entry.forcedReturnBy} 强制归还",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Return Photo Section (Visible for managers/advanced users if photo exists)
            if (canForceReturn && !entry.returnPhoto.isNullOrEmpty()) {
                 Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showImageDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "查看归还凭证照片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // Action buttons
            if (canReturn) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedButton(
                        onClick = onReturn,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AssignmentReturn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("归还")
                    }
                    
                    if (canForceReturn) {
                        AnimatedOutlinedButton(
                            onClick = onForceReturn,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Warning
                            )
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("强制归还")
                        }
                    }
                }
            }
        }
    }
}
