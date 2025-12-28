package com.equiptrack.android.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.permission.RolePermissionsMatrix

// 统一说明卡片：包含标题行（图标+标题）和内容分段
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = titleTint ?: MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleTint ?: MaterialTheme.colorScheme.onSurface
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SystemInfoScreen() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部说明卡片（页面级）
        SectionCard(
            title = "系统说明",
            icon = Icons.Default.Info,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "此页面汇总系统相关说明，包含权限说明、使用指南、常见问题与同步策略。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 权限系统说明区块
        PermissionSystemSection()

        Spacer(modifier = Modifier.height(12.dp))

        // 邀请码注册机制区块
        InviteCodeSection()

        Spacer(modifier = Modifier.height(12.dp))

        // 使用指南区块
        UsageGuideSection()

        Spacer(modifier = Modifier.height(12.dp))

        // 常见问题（FAQ）区块
        FAQSection()

        Spacer(modifier = Modifier.height(12.dp))

        // 数据同步策略区块
        DataSyncSection()
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun InviteCodeSection() {
    SectionCard(title = "邀请码注册机制", icon = Icons.Default.VpnKey) {
        Text("• 限制注册来源，确保成员身份可控。", style = MaterialTheme.typography.bodyMedium)
        Text("• 管理员或核心成员生成并分发邀请码。", style = MaterialTheme.typography.bodyMedium)
        Text("• 注册需填写有效邀请码并提交申请。", style = MaterialTheme.typography.bodyMedium)
        Text("• 申请流转至审批人，通过后自动创建账号。", style = MaterialTheme.typography.bodyMedium)
        Text("• 初次创建赋予基础权限，后续可按需调整。", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionSystemSection() {
    // 概览
    SectionCard(
        title = "权限系统说明（只读）",
        icon = Icons.Default.Info,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleTint = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = "遵循最小权限原则，权限按角色分配。下表展示各角色的权限范围。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 角色说明
    RoleSummary()

    Spacer(modifier = Modifier.height(12.dp))

    // 权限矩阵
    PermissionMatrix()
}

@Composable
private fun UsageGuideSection() {
    SectionCard(title = "使用指南", icon = Icons.Default.Help) {
        Text("1. 登录与注册：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("   使用邀请码注册，审批通过后即可登录。", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(4.dp))
        Text("2. 物资借用：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("   在‘物资管理’中选择物资，点击借用。需选择预计归还日期和精确时间（年-月-日 时:分）。", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(4.dp))
        Text("3. 物资归还：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("   在‘借用历史’中选择记录，点击归还。支持拍照留证，系统将自动记录当前精确归还时间。", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(4.dp))
        Text("4. 审批流程：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("   管理员在‘注册审批’页处理申请；借用申请需由管理员或高级用户审批通过后方可取用物资。", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(4.dp))
        Text("5. 个性化设置：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("   支持自定义主题颜色、背景壁纸。自定义壁纸支持调整磨砂（模糊）程度，顶部栏将自动适配磨砂效果。", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FAQSection() {
    SectionCard(title = "常见问题（FAQ）", icon = Icons.Default.Help) {
        Text("Q：为什么我看不到某些功能入口？", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("A：功能入口受角色权限控制。例如审批入口仅对管理员开放。", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(8.dp))
        Text("Q：如何调整背景图片的模糊程度？", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("A：进入‘设置’ -> ‘主题自定义’，在上传背景图后，通过滑动条调整磨砂程度。", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(8.dp))
        Text("Q：借用记录显示不准确怎么办？", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("A：下拉刷新列表。系统会自动同步最新数据并清除本地陈旧缓存，确保数据一致。", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DataSyncSection() {
    SectionCard(title = "数据同步策略", icon = Icons.Default.Sync) {
        Text("• 实时同步：借用、归还、审批等关键操作实时提交至服务器。", style = MaterialTheme.typography.bodyMedium)
        Text("• 缓存机制：本地缓存仅用于加速显示，刷新时自动全量同步。", style = MaterialTheme.typography.bodyMedium)
        Text("• 冲突处理：以服务器端数据为准，本地数据在同步时会被覆盖，确保多端一致性。", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RoleSummary() {
    val summaries = listOf(
        UserRole.SUPER_ADMIN to "全权管理：跨部门管理、系统全局配置、用户与物资全生命周期管理、全局借用审批。",
        UserRole.ADMIN to "部门管理：管理本部门用户与物资、审批注册申请、审批部门借用申请、维护部门数据。",
        UserRole.ADVANCED_USER to "高级操作：除基础借还外，可编辑物资信息、执行强制归还操作、协助审批借用申请。",
        UserRole.NORMAL_USER to "基础使用：浏览物资信息、提交借用申请（需审批）、归还物资、查看个人记录。"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        summaries.forEach { (role, desc) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
            Text(text = "权限项", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1.4f))
            roles.forEach { role ->
                // 简化表头显示，仅显示首字母或缩写，或者使用Tooltip（此处直接显示中文名，可能需要截断）
                Text(
                    text = role.displayName.take(2), 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                permissions.forEach { perm ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = perm.toReadable(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
                        roles.forEach { role ->
                            val granted = RolePermissionsMatrix.roleToPermissions[role]?.contains(perm) == true
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (granted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle, 
                                        contentDescription = "Granted", 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
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
    PermissionType.MANAGE_ALL_DEPARTMENTS -> "全局部门管理"
    PermissionType.VIEW_REGISTRATION_APPROVALS -> "注册审批查看"
    PermissionType.VIEW_USER_MANAGEMENT -> "用户管理查看"
    PermissionType.VIEW_DEPARTMENT_MANAGEMENT -> "部门管理查看"
    PermissionType.MANAGE_EQUIPMENT_ITEMS -> "物资信息管理"
    PermissionType.VIEW_DEPARTMENT_HISTORY -> "部门记录查看"
    PermissionType.BORROW_ITEMS -> "物资借用权限"
    PermissionType.VIEW_OWN_HISTORY -> "个人记录查看"
}
