package com.equiptrack.android.ui.department.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.Department

@Composable
fun OrganizationTree(
    departments: List<Department>,
    selectedDepartmentId: String?,
    onSelect: (String) -> Unit,
    onEdit: ((Department) -> Unit)? = null,
    onDelete: ((Department) -> Unit)? = null
) {
    val childMap = remember(departments) { departments.groupBy { it.parentId } }
    val rootDepartments = remember(departments) { departments.filter { it.parentId == null } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (rootDepartments.isEmpty() && departments.isNotEmpty()) {
             departments.forEach { dept ->
                DepartmentTreeNode(
                    department = dept,
                    childMap = emptyMap(),
                    selectedDepartmentId = selectedDepartmentId,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    level = 0
                )
            }
        } else {
             rootDepartments.forEach { root ->
                DepartmentTreeNode(
                    department = root,
                    childMap = childMap,
                    selectedDepartmentId = selectedDepartmentId,
                    onSelect = onSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    level = 0
                )
            }
        }
    }
}

@Composable
fun DepartmentTreeNode(
    department: Department,
    childMap: Map<String?, List<Department>>,
    selectedDepartmentId: String?,
    onSelect: (String) -> Unit,
    onEdit: ((Department) -> Unit)?,
    onDelete: ((Department) -> Unit)?,
    level: Int
) {
    val isSelected = department.id == selectedDepartmentId
    val children = childMap[department.id] ?: emptyList()
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(department.id) }
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(start = (level * 24).dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
        ) {
            if (children.isNotEmpty()) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                 modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = department.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (onEdit != null || onDelete != null) {
                Row {
                    if (onEdit != null) {
                        IconButton(onClick = { onEdit(department) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(department) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        AnimatedVisibility(
            visible = expanded && children.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                children.forEach { child ->
                    DepartmentTreeNode(
                        department = child,
                        childMap = childMap,
                        selectedDepartmentId = selectedDepartmentId,
                        onSelect = onSelect,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        level = level + 1
                    )
                }
            }
        }
    }
}
