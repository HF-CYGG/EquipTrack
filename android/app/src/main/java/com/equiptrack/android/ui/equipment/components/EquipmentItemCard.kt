package com.equiptrack.android.ui.equipment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.equiptrack.android.data.model.Category
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.EquipmentStatus
import com.equiptrack.android.ui.theme.*
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.utils.UrlUtils

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun EquipmentItemCard(
    item: EquipmentItem,
    category: Category?,
    categoryColor: Color?,
    canManage: Boolean,
    serverUrl: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBorrow: () -> Unit,
    onPreviewImage: (String, String) -> Unit,
    compact: Boolean = false,
    cornerRadius: Float = 12f,
    equipmentImageRatio: String = "Square",
    cardMaterial: String = "Solid",
    tagStyle: String = "Solid"
) {
    val isAvailable = item.availableQuantity > 0

    val imageThumbnailUrl = remember(item.image, serverUrl) {
        UrlUtils.resolveImageUrl(serverUrl, item.image)
    }
    val imageFullUrl = remember(item.imageFull, item.image, serverUrl) {
        UrlUtils.resolveImageUrl(serverUrl, item.imageFull ?: item.image)
    }

    val cardPadding = if (compact) 8.dp else 12.dp
    val baseImageHeight = if (compact) 64.dp else 80.dp
    val innerSpacing = if (compact) 8.dp else 12.dp
    val sectionSpacing = if (compact) 8.dp else 12.dp

    val imageWidth = when (equipmentImageRatio) {
        "Wide" -> baseImageHeight * (16f / 9f)
        "Tall" -> baseImageHeight * (3f / 4f)
        else -> baseImageHeight
    }
    val imageHeight = when (equipmentImageRatio) {
        "Tall" -> baseImageHeight * (4f / 3f)
        else -> baseImageHeight
    }

    val density = LocalDensity.current
    val imageWidthPx = remember(imageWidth, density) {
        with(density) { imageWidth.roundToPx() }
    }
    val imageHeightPx = remember(imageHeight, density) {
        with(density) { imageHeight.roundToPx() }
    }

    val context = LocalContext.current
    val thumbnailRequest = remember(context, imageThumbnailUrl, imageWidthPx, imageHeightPx) {
        ImageRequest.Builder(context)
            .data(imageThumbnailUrl)
            .size(imageWidthPx, imageHeightPx)
            .allowHardware(true)
            .build()
    }

    val cardShape = RoundedCornerShape(cornerRadius.dp)
    val cardColors = when (cardMaterial) {
        "Glass" -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
        "Outline" -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        )
        else -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    val cardElevation = when (cardMaterial) {
        "Glass" -> CardDefaults.cardElevation(defaultElevation = 4.dp)
        "Outline" -> CardDefaults.cardElevation(defaultElevation = 0.dp)
        else -> CardDefaults.cardElevation(defaultElevation = 2.dp)
    }
    val cardBorder = if (cardMaterial == "Outline") {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    } else null

    val statusColor = when {
        item.availableQuantity == 0 -> Error
        item.availableQuantity < item.quantity -> Warning
        else -> Available
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = cardElevation,
        colors = cardColors,
        shape = cardShape,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(cardPadding)
        ) {
            // Header with image and basic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(innerSpacing),
                verticalAlignment = Alignment.Top
            ) {
                // Item image with enhanced styling
                if (!item.image.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(imageWidth)
                            .height(imageHeight)
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant, cardShape)
                    ) {
                        AsyncImage(
                            model = thumbnailRequest,
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !imageFullUrl.isNullOrEmpty()) {
                                    val url = imageFullUrl
                                    if (!url.isNullOrEmpty()) {
                                        onPreviewImage(url, item.name)
                                    }
                                },
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                            placeholder = rememberVectorPainter(Icons.Default.Image)
                        )
                    }
                } else {
                    // Placeholder for items without image
                    Box(
                        modifier = Modifier
                            .width(imageWidth)
                            .height(imageHeight)
                            .background(MaterialTheme.colorScheme.surfaceVariant, cardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Item info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Category chip
                    if (category != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                             Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        categoryColor ?: MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            // Enhanced quantity and status section (Optimized: Removed nested Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(if (compact) 8.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "总数量",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "可用",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.availableQuantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isAvailable) Available else Error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Enhanced status badge
                Row(
                    modifier = Modifier
                        .background(
                            statusColor,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        when {
                            item.availableQuantity == 0 -> Icons.Default.Block
                            item.availableQuantity < item.quantity -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = White
                    )
                    Text(
                        text = when {
                            item.availableQuantity == 0 -> "已借完"
                            item.availableQuantity < item.quantity -> "部分借出"
                            else -> "可借用"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            // Enhanced action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Borrow button
                if (isAvailable) {
                    AnimatedButton(
                        onClick = onBorrow,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 36.dp else 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "借用",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 36.dp else 40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "暂不可借",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Management buttons
                if (canManage) {
                     OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.height(if (compact) 36.dp else 40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    AnimatedOutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.height(if (compact) 36.dp else 40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
