package com.equiptrack.android.ui.department.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.AnimatedIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailsView(
    selectedDepartmentId: String?,
    departments: List<Department>,
    users: List<User>,
    items: List<EquipmentItem>,
    canManage: Boolean,
    onSelectDepartment: (String) -> Unit,
    onUpdateUserRole: (String, UserRole) -> Unit
) {
    val selectedDepartment = departments.find { it.id == selectedDepartmentId }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp), // Space for FAB
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 部门选择器 (Department Selector) - Enhanced
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = selectedDepartment?.name ?: "请选择部门",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDepartment != null) {
                            Text(
                                text = "ID: ${selectedDepartment.id.take(8)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
            }
            
            // Dropdown logic moved to a separate box to handle positioning correctly if needed, 
            // but standard DropdownMenu works. We need a hidden anchor if we want it to appear attached.
            // Actually, let's use a ExposedDropdownMenuBox style for cleaner look if we were editing,
            // but for just selection, a dialog or bottom sheet might be better.
            // For now, sticking to simple dropdown attached to the card.
            Box(modifier = Modifier.fillMaxWidth()) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept.name) },
                            onClick = {
                                onSelectDepartment(dept.id)
                                expanded = false
                            },
                            leadingIcon = {
                                if (dept.id == selectedDepartmentId) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (selectedDepartment != null) {
            // 2. 概览卡片 (Overview Stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "成员总数",
                    value = users.size.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "物资总数",
                    value = items.size.toString(),
                    icon = Icons.Default.Inventory2,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. 成员管理 (Member Management)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "成员列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AnimatedTextButton(onClick = { /* TODO: View All or Add Member */ }) {
                            Text("查看全部")
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (users.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无成员", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        users.take(5).forEach { user ->
                            UserItem(
                                user = user,
                                canManage = canManage && canManageDepartment(selectedDepartmentId), // Simplified check
                                onUpdateRole = { role -> onUpdateUserRole(user.id, role) }
                            )
                        }
                    }
                }
            }
            
             // 4. 组织架构 (Structure)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "组织架构",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    HierarchicalDepartmentTree(
                        departments = departments,
                        selectedDepartmentId = selectedDepartmentId,
                        onSelect = onSelectDepartment
                    )
                }
            }

        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "请先选择一个部门以查看详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape)
                    .padding(6.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UserItem(
    user: User,
    canManage: Boolean,
    onUpdateRole: (UserRole) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = user.role.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (canManage) {
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                AnimatedIconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    val isAdmin = user.role == UserRole.ADMIN
                    DropdownMenuItem(
                        text = { Text(if (isAdmin) "取消管理员" else "设为管理员") },
                        onClick = {
                            onUpdateRole(if (isAdmin) UserRole.NORMAL_USER else UserRole.ADMIN)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

// Helper placeholder since we don't have direct access to ViewModel logic here easily without passing it in
private fun canManageDepartment(departmentId: String?): Boolean {
    return true // Logic should be passed from parent
}