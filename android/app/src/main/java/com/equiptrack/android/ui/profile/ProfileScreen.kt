package com.equiptrack.android.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.equiptrack.android.BuildConfig
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.utils.UpdateStatus
import com.equiptrack.android.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper

enum class ProfileStyle {
    Default,
    Immersive
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSystemInfo: () -> Unit
) {
    val authRepository: AuthRepository = hiltViewModel<NavigationViewModel>().authRepository
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val currentUser by profileViewModel.currentUser.collectAsState()
    val themeOverrides by mainViewModel.themeOverrides.collectAsState()
    val context = LocalContext.current
    
    // 自动适配样式：如果有自定义背景，则使用 Immersive 风格
    val hasCustomBackground = !themeOverrides.backgroundUri.isNullOrEmpty()
    val profileStyle = if (hasCustomBackground) ProfileStyle.Immersive else ProfileStyle.Default
    
    val avatarMessage by profileViewModel.avatarUpdateMessage.collectAsState(initial = null)
    val passwordMessage by profileViewModel.passwordUpdateMessage.collectAsState(initial = null)
    val refreshMessage by profileViewModel.refreshMessage.collectAsState(initial = null)
    val isRefreshing by profileViewModel.isRefreshing.collectAsState()
    val updateStatus by mainViewModel.updateStatus.collectAsState()
    val toastState = rememberToastState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var versionInfo by remember { mutableStateOf<com.equiptrack.android.data.model.AppVersion?>(null) }

    // Handle update status changes
    LaunchedEffect(updateStatus) {
        if (isCheckingUpdate) {
            when (updateStatus) {
                is UpdateStatus.NoUpdate -> {
                    val remoteVersion = (updateStatus as UpdateStatus.NoUpdate).version
                    if (remoteVersion.versionCode < BuildConfig.VERSION_CODE) {
                        versionInfo = com.equiptrack.android.data.model.AppVersion(
                            versionCode = BuildConfig.VERSION_CODE,
                            versionName = BuildConfig.VERSION_NAME,
                            updateContent = "当前已是最新版本",
                            downloadUrl = "",
                            forceUpdate = false,
                            releaseDate = ""
                        )
                    } else {
                        versionInfo = remoteVersion
                    }
                    showVersionDialog = true 
                    toastState.showSuccess("已是最新版本")
                    isCheckingUpdate = false
                }
                is UpdateStatus.Available -> {
                    isCheckingUpdate = false
                }
                is UpdateStatus.Error -> {
                    toastState.showError("检查更新失败")
                    isCheckingUpdate = false
                }
                else -> {}
            }
        }
    }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { profileViewModel.refreshProfile(true) }
    )
    
    // Auto refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                profileViewModel.refreshProfile(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Handle messages
    LaunchedEffect(passwordMessage) {
        passwordMessage?.let { 
            toastState.showSuccess(it)
            profileViewModel.clearPasswordMessage()
        }
    }
    LaunchedEffect(avatarMessage) {
        avatarMessage?.let { 
            toastState.showSuccess(it)
            profileViewModel.clearAvatarMessage()
        }
    }
    LaunchedEffect(refreshMessage) {
        refreshMessage?.let { 
            toastState.showSuccess(it)
            profileViewModel.clearRefreshMessage()
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { profileViewModel.updateAvatar(context, it) }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // 自定义背景渲染
        if (hasCustomBackground && !themeOverrides.backgroundUri.isNullOrEmpty()) {
            AsyncImage(
                model = themeOverrides.backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 添加轻微遮罩以确保文字可读性，但不要太重
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. 顶部个人信息卡片
            item {
                ProfileHeader(
                    user = currentUser,
                    style = profileStyle,
                    onAvatarClick = { imagePicker.launch("image/*") },
                    onEditClick = { showEditDialog = true }
                )
            }

            // 2. 详细信息
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoCard(
                        icon = Icons.Default.Email,
                        label = "联系方式",
                        value = currentUser?.contact ?: "未设置",
                        style = profileStyle
                    )
                    
                    InfoCard(
                        icon = Icons.Default.Business,
                        label = "所属部门",
                        value = currentUser?.departmentName ?: "未分配",
                        style = profileStyle
                    )
                    
                    if (currentUser?.role == UserRole.SUPER_ADMIN || currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.ADVANCED_USER) {
                        InfoCard(
                            icon = Icons.Default.VpnKey,
                            label = "个人邀请码",
                            value = currentUser?.invitationCode ?: "无",
                            isCopyable = true,
                            onCopy = { toastState.showSuccess("已复制邀请码") },
                            style = profileStyle
                        )
                    }
                }
            }
            
            // 3. 功能列表
            item {
                Text(
                    text = "系统",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profileStyle == ProfileStyle.Immersive) 
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (profileStyle == ProfileStyle.Immersive) 1.dp else 0.dp)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Update,
                            title = "检查更新",
                            subtitle = "当前版本: ${BuildConfig.VERSION_NAME}",
                            onClick = {
                                if (!isCheckingUpdate) {
                                    toastState.showSuccess("正在检查更新...")
                                    isCheckingUpdate = true
                                    mainViewModel.checkForUpdates()
                                }
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileMenuItem(
                            icon = Icons.Default.Info,
                            title = "系统说明",
                            subtitle = "了解 EquipTrack",
                            onClick = onNavigateToSystemInfo
                        )
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ProfileMenuItem(
                            icon = Icons.Default.Code,
                            title = "开发者",
                            subtitle = "夜喵cats (GitHub)",
                            onClick = { mainViewModel.startDownload("https://github.com/HF-CYGG") }
                        )
                    }
                }
            }

            // 4. 退出登录
            item {
                Spacer(modifier = Modifier.height(32.dp))
                AnimatedButton(
                    onClick = {
                        authRepository.logout()
                        onNavigateToLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录")
                }
            }
        }
        
        MD3PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showVersionDialog && versionInfo != null) {
        com.equiptrack.android.ui.components.UpdateDialog(
            version = versionInfo!!,
            isUpdate = false,
            onUpdate = {},
            onDismiss = { showVersionDialog = false }
        )
    }
    
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
fun ProfileHeader(
    user: com.equiptrack.android.data.model.User?,
    style: ProfileStyle,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    // Animation state for avatar border
    val infiniteTransition = rememberInfiniteTransition(label = "profile_header_animation")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp) // Increased height for better spacing and visuals
    ) {
        // Background Decoration
        if (style == ProfileStyle.Default) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            
            // Decorative circles/shapes
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    center = Offset(x = canvasWidth * 0.85f, y = canvasHeight * 0.15f),
                    radius = 120.dp.toPx()
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    center = Offset(x = canvasWidth * 0.15f, y = canvasHeight * 0.4f),
                    radius = 80.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp), // More top padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Avatar with Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(128.dp) // Larger avatar container
            ) {
                // Animated border ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(avatarScale)
                        .clip(CircleShape)
                        .background(
                            if (style == ProfileStyle.Immersive) 
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                )

                // Actual Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            if (style == ProfileStyle.Immersive) 
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) 
                            else 
                                MaterialTheme.colorScheme.surface
                        )
                        .clickable(onClick = onAvatarClick)
                        .padding(4.dp)
                ) {
                    if (user?.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            tint = if (style == ProfileStyle.Immersive) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Edit Badge (Small icon on bottom right of avatar)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onEditClick)
                        .padding(6.dp)
                ) {
                     Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Text Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val textColor = if (style == ProfileStyle.Immersive) Color.White else MaterialTheme.colorScheme.onSurface
                val textShadow = if (style == ProfileStyle.Immersive) Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                ) else null

                Text(
                    text = user?.name ?: "未登录",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = textShadow,
                        fontWeight = FontWeight.Bold
                    ),
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = if (style == ProfileStyle.Immersive) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50), // Fully rounded pill
                    border = if (style == ProfileStyle.Immersive) 
                        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    else null
                ) {
                    Text(
                        text = user?.role?.displayName ?: "游客",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (style == ProfileStyle.Immersive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    isCopyable: Boolean = false,
    onCopy: () -> Unit = {},
    style: ProfileStyle = ProfileStyle.Default
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (style == ProfileStyle.Immersive) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (style == ProfileStyle.Immersive) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isCopyable) {
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                        onClick = onUpdateAvatar,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true
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
