package com.equiptrack.android.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.permission.RolePermissionsMatrix
import kotlinx.coroutines.delay

// 可折叠的说明卡片，带动画效果
@Composable
private fun ExpandableSectionCard(
    title: String,
    icon: ImageVector,
    initialExpanded: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ArrowRotation"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = titleTint ?: MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleTint ?: MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    content()
                }
            }
        }
    }
}

@Composable
fun SystemInfoScreen() {
    val scrollState = rememberScrollState()
    // Staggered animation state
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部说明卡片（页面级）
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + expandVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "系统说明中心",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "汇总权限规则、操作指南与常见问题",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 依次展示各个区块，带轻微延迟效果（模拟）
        // 由于Compose重组特性，这里直接按顺序排列，ExpandableSectionCard自带展开动画

        // 权限系统说明区块
        ExpandableSectionCard(
            title = "权限与角色体系",
            icon = Icons.Default.Security,
            initialExpanded = true
        ) {
            PermissionSystemSection()
        }

        // 邀请码注册机制区块
        ExpandableSectionCard(
            title = "注册与准入机制",
            icon = Icons.Default.VpnKey
        ) {
            InviteCodeSection()
        }

        // 使用指南区块
        ExpandableSectionCard(
            title = "核心功能指南",
            icon = Icons.Default.Build
        ) {
            UsageGuideSection()
        }

        // 常见问题（FAQ）区块
        ExpandableSectionCard(
            title = "常见问题解答 (FAQ)",
            icon = Icons.Default.Help
        ) {
            FAQSection()
        }

        // 数据同步策略区块
        ExpandableSectionCard(
            title = "数据同步与安全",
            icon = Icons.Default.Sync
        ) {
            DataSyncSection()
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 底部版本信息占位
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "EquipTrack System v1.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun InviteCodeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow("注册限制", "系统采用严格的邀请码注册制，确保只有受邀的内部成员方可加入。")
        InfoRow("流程概览", "获取邀请码 -> 填写注册信息 -> 管理员/高级用户审批 -> 激活账号。")
        InfoRow("权限初始化", "新注册账号默认为【普通用户】，后续可由管理员根据职责调整职级。")
    }
}

@Composable
private fun PermissionSystemSection() {
    Text(
        text = "本系统遵循“最小权限原则”与“严格层级控制”。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 核心规则高亮
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🛡️ 核心安全规则",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "• 职级压制：用户无法管理（编辑/封禁/删除）同级或更高职级的账号。", style = MaterialTheme.typography.bodySmall)
            Text(text = "• 提权限制：用户无法将他人提升至同级或更高职级。", style = MaterialTheme.typography.bodySmall)
            Text(text = "• 自我管理：用户可更新个人头像/密码，但不可修改自身角色/状态。", style = MaterialTheme.typography.bodySmall)
            Text(text = "• 邀请码：仅超级管理员有权修改用户的邀请码。", style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("角色职能定义", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    RoleSummary()

    Spacer(modifier = Modifier.height(8.dp))
    Text("详细权限矩阵", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    PermissionMatrix()
}

@Composable
private fun UsageGuideSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideItem(
            index = "01",
            title = "借用流程",
            desc = "在“物资”页选择物品 -> 提交申请（需包含照片与预计归还时间）-> 等待审批 -> 审批通过后即可取用。"
        )
        GuideItem(
            index = "02",
            title = "归还流程",
            desc = "在“历史”页找到在借记录 -> 点击归还 -> 拍照留证 -> 确认归还。系统自动记录精确时间。"
        )
        GuideItem(
            index = "03",
            title = "审批管理",
            desc = "管理员/高级用户在“审批”页处理申请。支持批量通过/驳回，并可查看申请人详细信誉记录。"
        )
        GuideItem(
            index = "04",
            title = "个性化",
            desc = "支持自定义Material You主题色、背景壁纸及磨砂效果。设置页可调整服务器连接。"
        )
    }
}

@Composable
private fun GuideItem(index: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = index,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, content: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "• $label：",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FAQSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FAQItem(
            q = "我看不到“审批”或“管理”入口？",
            a = "功能入口受严格的权限控制。如需相关权限，请联系上级管理员申请提升职级。"
        )
        FAQItem(
            q = "修改服务器地址后为何被登出？",
            a = "切换服务器意味着接入新的数据源。为防止数据冲突与脏数据，系统会自动清除本地缓存并要求重新认证。"
        )
        FAQItem(
            q = "如何修改我的密码或头像？",
            a = "点击首页右上角头像进入“个人中心”，点击头像即可更换图片，点击“修改密码”可重置安全凭证。"
        )
        FAQItem(
            q = "借用申请一直未被审批？",
            a = "请尝试联系您所属部门的管理员。系统也会通过FCM推送通知提醒审批人。"
        )
    }
}

@Composable
private fun FAQItem(q: String, a: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Q: $q", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = a, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DataSyncSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow("实时性", "关键操作（借还、审批）实时联网提交，确保数据准确。")
        InfoRow("冲突解决", "以服务器端数据为最终真理（Single Source of Truth），本地缓存仅用于加速展示。")
        InfoRow("安全传输", "全链路HTTPS加密，敏感操作需JWT令牌验证。")
    }
}

@Composable
private fun RoleSummary() {
    val summaries = listOf(
        UserRole.SUPER_ADMIN to "系统主宰：全局配置、跨部门管理、全生命周期管控。",
        UserRole.ADMIN to "部门主管：部门内人员/物资管理、审批权。",
        UserRole.ADVANCED_USER to "核心骨干：协助审批、物资信息维护、强制归还操作。",
        UserRole.NORMAL_USER to "基础成员：物资浏览、借用申请、个人记录查看。"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        summaries.forEach { (role, desc) ->
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = role.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionMatrix() {
    val permissions = PermissionType.values().toList()
    val roles = listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.ADVANCED_USER, UserRole.NORMAL_USER)

    Card(
        colors = CardDefaults.outlinedCardColors(),
        border = null // 移除边框，让它融入背景或仅使用背景色
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1.2f))
                roles.forEach { role ->
                    Text(
                        text = role.displayName.take(1), // 仅显示首字以节省空间
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Rows
            permissions.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.toReadable(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f).padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    roles.forEach { role ->
                        val granted = RolePermissionsMatrix.roleToPermissions[role]?.contains(perm) == true
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (granted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Yes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                // 占位，保持对齐
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun PermissionType.toReadable(): String = when (this) {
    PermissionType.MANAGE_ALL_DEPARTMENTS -> "全局管理"
    PermissionType.VIEW_REGISTRATION_APPROVALS -> "注册审批"
    PermissionType.VIEW_BORROW_APPROVALS -> "借用审批"
    PermissionType.VIEW_USER_MANAGEMENT -> "用户管理"
    PermissionType.VIEW_DEPARTMENT_MANAGEMENT -> "部门管理"
    PermissionType.MANAGE_EQUIPMENT_ITEMS -> "物资维护"
    PermissionType.VIEW_DEPARTMENT_HISTORY -> "部门记录"
    PermissionType.BORROW_ITEMS -> "物资借用"
    PermissionType.VIEW_OWN_HISTORY -> "个人记录"
}
