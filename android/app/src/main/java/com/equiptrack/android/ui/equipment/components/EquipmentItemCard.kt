package com.equiptrack.android.ui.equipment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import com.equiptrack.android.data.model.Category
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.EquipmentStatus
import com.equiptrack.android.ui.theme.*
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.utils.UrlUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentItemCard(
    item: EquipmentItem,
    categories: List<Category>,
    canManage: Boolean,
    serverUrl: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBorrow: () -> Unit
) {
    val category = categories.find { it.id == item.categoryId }
    val isAvailable = item.availableQuantity > 0
    
    val imageUrl = remember(item.image, serverUrl) {
        UrlUtils.resolveImageUrl(serverUrl, item.image)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Reduced elevation
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp) // Reduced padding from 20.dp
        ) {
            // Header with image and basic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing
                verticalAlignment = Alignment.Top
            ) {
                // Item image with enhanced styling
                if (!item.image.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.size(80.dp), // Reduced size from 100.dp
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                            placeholder = rememberVectorPainter(Icons.Default.Image)
                        )
                    }
                } else {
                    // Placeholder for items without image
                    Card(
                        modifier = Modifier.size(80.dp), // Reduced size from 100.dp
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp), // Reduced icon size
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // Item info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium, // Reduced typography
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall, // Reduced typography
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Category chip with enhanced styling
                    if (category != null) {
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelSmall, // Reduced typography
                                    fontWeight = FontWeight.Medium
                                ) 
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp) // Reduced size
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            try {
                                                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(category.color))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                            },
                            modifier = Modifier.height(28.dp), // Reduced height
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing
            
            // Enhanced quantity and status section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), // Reduced padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "总数量",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified // Reset if needed
                                )
                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium, // Reduced typography
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
                                    style = MaterialTheme.typography.bodyMedium, // Reduced typography
                                    color = if (isAvailable) Available else Error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Enhanced status badge
                    Surface(
                        color = when {
                            item.availableQuantity == 0 -> Error
                            item.availableQuantity < item.quantity -> Warning
                            else -> Available
                        },
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp // Reduced elevation
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), // Reduced padding
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
                                modifier = Modifier.size(12.dp), // Reduced size
                                tint = White
                            )
                            Text(
                                text = when {
                                    item.availableQuantity == 0 -> "已借完"
                                    item.availableQuantity < item.quantity -> "部分借出"
                                    else -> "可借用"
                                },
                                style = MaterialTheme.typography.labelSmall, // Reduced typography
                                color = White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing
            
            // Enhanced action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
            ) {
                // Borrow button (Always visible if available)
                if (isAvailable) {
                    Button(
                        onClick = onBorrow,
                        modifier = Modifier.weight(1f).height(40.dp), // Reduced height
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp) // Reduced size
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "借用",
                            style = MaterialTheme.typography.labelMedium, // Reduced typography
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f).height(40.dp), // Reduced height
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "暂不可借",
                            style = MaterialTheme.typography.labelMedium // Reduced typography
                        )
                    }
                }

                // Management buttons (Only for admins)
                if (canManage) {
                     AnimatedOutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.height(40.dp), // Reduced height
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(16.dp) // Reduced size
                        )
                    }
                    
                    AnimatedOutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.height(40.dp), // Reduced height
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp) // Reduced size
                        )
                    }
                }
            }
        }
    }
}