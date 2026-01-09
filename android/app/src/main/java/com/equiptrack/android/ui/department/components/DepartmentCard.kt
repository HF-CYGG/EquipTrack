package com.equiptrack.android.ui.department.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.ui.theme.Error
import com.equiptrack.android.ui.components.AnimatedIconButton

@Composable
fun DepartmentCard(
    department: Department,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { base -> if (onClick != null) base.clickable { onClick() } else base },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = department.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "部门ID: ${department.id.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (canManage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedIconButton(
                            onClick = onEdit
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑部门",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        AnimatedIconButton(
                            onClick = onDelete
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除部门",
                                tint = Error
                            )
                        }
                    }
                }
            }
        }
    }
}
