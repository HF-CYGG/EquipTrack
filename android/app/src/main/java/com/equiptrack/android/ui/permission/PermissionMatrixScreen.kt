package com.equiptrack.android.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.permission.RolePermissionsMatrix

@Composable
fun PermissionMatrixScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 概览信息卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "权限系统说明（只读）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "遵循最小权限原则。各角色权限范围如下：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // 角色说明
        RoleSummary()

        // 权限矩阵
        PermissionMatrix()
    }
}

@Composable
private fun RoleSummary() {
    val summaries = listOf(
        UserRole.SUPER_ADMIN to "跨部门管理、系统全局配置、管理所有用户和物资",
        UserRole.ADMIN to "管理本部门用户与物资、审批注册申请、维护部门基础数据",
        UserRole.ADVANCED_USER to "编辑物资、强制归还、管理普通用户账户",
        UserRole.NORMAL_USER to "浏览信息、参与借还操作"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(summaries) { (role, desc) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = role.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PermissionMatrix() {
    val permissions = PermissionType.values().toList()
    val roles = listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.ADVANCED_USER, UserRole.NORMAL_USER)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 表头
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "权限", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1.5f))
            roles.forEach { role ->
                Text(text = role.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
        }
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                permissions.forEach { perm ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = perm.toReadable(), modifier = Modifier.weight(1.5f))
                        roles.forEach { role ->
                            val granted = RolePermissionsMatrix.roleToPermissions[role]?.contains(perm) == true
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (granted) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun PermissionType.toReadable(): String = when (this) {
    PermissionType.MANAGE_ALL_DEPARTMENTS -> "管理所有部门"
    PermissionType.VIEW_REGISTRATION_APPROVALS -> "查看注册审批"
    PermissionType.VIEW_USER_MANAGEMENT -> "查看用户管理"
    PermissionType.VIEW_DEPARTMENT_MANAGEMENT -> "查看部门管理"
    PermissionType.MANAGE_EQUIPMENT_ITEMS -> "管理物资信息"
    PermissionType.VIEW_DEPARTMENT_HISTORY -> "查看部门借用记录"
    PermissionType.BORROW_ITEMS -> "借用物资"
    PermissionType.VIEW_OWN_HISTORY -> "查看个人借用记录"
}