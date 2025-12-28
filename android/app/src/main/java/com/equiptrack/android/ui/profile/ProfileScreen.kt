package com.equiptrack.android.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.ui.profile.ProfileViewModel
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.ToastType
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedTextButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSystemInfo: () -> Unit
) {
    val authRepository: AuthRepository = hiltViewModel<NavigationViewModel>().authRepository
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val currentUser = authRepository.getCurrentUser()
    val context = LocalContext.current
    val avatarMessage by profileViewModel.avatarUpdateMessage.collectAsState(initial = null)
    val passwordMessage by profileViewModel.passwordUpdateMessage.collectAsState(initial = null)
    val isRefreshing by profileViewModel.isRefreshing.collectAsState()
    val toastState = rememberToastState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { profileViewModel.refreshProfile() }
    )
    
    // Auto refresh on entry
    LaunchedEffect(Unit) {
        profileViewModel.refreshProfile()
    }
    
    // Handle password update messages
    LaunchedEffect(passwordMessage) {
        passwordMessage?.let { message ->
            toastState.showSuccess(message)
            profileViewModel.clearPasswordMessage()
        }
    }
    
    // Handle avatar update messages
    LaunchedEffect(avatarMessage) {
        avatarMessage?.let { message ->
            toastState.showSuccess(message)
            profileViewModel.clearAvatarMessage()
        }
    }
    
    // Show toast after pull-to-refresh completes
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            toastState.showSuccess("刷新成功")
        }
        wasRefreshing = isRefreshing
    }
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { profileViewModel.updateAvatar(context.contentResolver, it) }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 头像：使用 AsyncImage 优化加载
                        if (currentUser?.avatarUrl != null) {
                            AsyncImage(
                                model = currentUser.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = rememberVectorPainter(Icons.Default.Person),
                                placeholder = rememberVectorPainter(Icons.Default.Person)
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = currentUser?.name ?: "未知用户",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentUser?.role?.displayName ?: "未知角色",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        AnimatedOutlinedButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("编辑")
                        }
                    }
                    
                    Divider()
                    
                    // 显示头像更新消息
                    avatarMessage?.let { message ->
                        LaunchedEffect(message) {
                            // 显示消息后清除
                            kotlinx.coroutines.delay(2000)
                            profileViewModel.clearAvatarMessage()
                        }
                        Text(
                            text = message,
                            color = if (message.contains("失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Contact info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentUser?.contact ?: "未知联系方式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Department info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentUser?.departmentName ?: "未知部门",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Invitation Code info (Only for Admins/Super Admins/Advanced Users)
                    if (currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.ADVANCED_USER) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "个人邀请码",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentUser?.invitationCode ?: "无邀请码",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Settings section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    // 设置入口已移除，根据需求仅保留关于内容
                    // 已根据需求移除"注册审批"、"用户管理"和"物资管理"入口
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSystemInfo() }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("关于", style = MaterialTheme.typography.labelLarge)
                        }
                        Text("EquipTrack 现代化智能物资管理系统", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("版本：0.4.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("说明：支持多部门、角色权限、带拍照的借还流程、借用审批与历史审计。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("开发者：夜喵cats（https://github.com/HF-CYGG）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedOutlinedButton(onClick = onNavigateToSystemInfo, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("系统说明")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout button
            AnimatedButton(
                onClick = {
                    authRepository.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    
    // 编辑对话框
    if (showEditDialog) {
        EditProfileDialog(
            onDismiss = { showEditDialog = false },
            onUpdateAvatar = { imagePicker.launch("image/*") },
            onUpdatePassword = { oldPassword, newPassword ->
                currentUser?.let { user ->
                    profileViewModel.updatePassword(user.id, oldPassword, newPassword)
                }
            }
        )
    }
    
    // Toast message overlay
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        ToastMessage(
            toastData = toastState.currentToast,
            onDismiss = { toastState.dismiss() },
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun EditProfileDialog(
    onDismiss: () -> Unit,
    onUpdateAvatar: () -> Unit,
    onUpdatePassword: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswordFields by remember { mutableStateOf(false) }
    
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
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "编辑个人信息",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    // 头像更新按钮
                    AnimatedOutlinedButton(
                        onClick = {
                            onUpdateAvatar()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("更换头像")
                    }
                    
                    // 密码修改按钮
                    AnimatedOutlinedButton(
                        onClick = { showPasswordFields = !showPasswordFields },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showPasswordFields) "取消修改密码" else "修改密码")
                    }
                    
                    // 密码输入字段
                    if (showPasswordFields) {
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("当前密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("新密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("确认新密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AnimatedTextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        
                        if (showPasswordFields) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AnimatedTextButton(
                                onClick = {
                                    if (newPassword == confirmPassword && newPassword.isNotBlank()) {
                                        onUpdatePassword(oldPassword, newPassword)
                                        onDismiss()
                                    }
                                },
                                enabled = newPassword.isNotBlank() && newPassword == confirmPassword
                            ) {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }
}
