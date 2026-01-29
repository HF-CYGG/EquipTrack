package com.equiptrack.android.ui.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.utils.getDepartmentPath
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun UsersScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredUsers by viewModel.filteredUsers.collectAsStateWithLifecycle()
    val roleFilter by viewModel.filterRole.collectAsStateWithLifecycle()
    val statusFilter by viewModel.filterStatus.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val filterDepartmentId by viewModel.filterDepartmentId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val canManage = viewModel.canManageUsers()
    val toastState = rememberToastState()

    val settingsRepository = navVm.settingsRepository
    val themeOverrides by settingsRepository.themeOverridesFlow.collectAsStateWithLifecycle()
    val lowPerformanceMode = themeOverrides.lowPerformanceMode ?: settingsRepository.isLowPerformanceMode()
    val listAnimationType = themeOverrides.listAnimationType ?: settingsRepository.getListAnimationType()
    val listState = rememberLazyListState()
    val isImmersive = !themeOverrides.backgroundUri.isNullOrEmpty()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshUsers() }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncUsers(isUserRefresh = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            toastState.showError(message)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            toastState.showSuccess(message)
            viewModel.clearMessages()
        }
    }

    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加用户")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isImmersive) Color.Transparent else MaterialTheme.colorScheme.surface)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "用户管理",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "共 ${filteredUsers.size} 位成员",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { showSearch = !showSearch },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (showSearch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (showSearch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (showSearch) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                    contentDescription = "筛选"
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refreshUsers() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                        }
                    }

                    // Search & Filter Section
                        AnimatedVisibility(
                            visible = showSearch,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = viewModel::updateSearchQuery,
                                    placeholder = { Text("搜索姓名、手机号或部门") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                // Filters Row
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (viewModel.getCurrentUser()?.role == UserRole.SUPER_ADMIN) {
                                        DepartmentFilter(
                                            departments = departments,
                                            selectedDepartmentId = filterDepartmentId,
                                            onDepartmentSelected = { viewModel.filterByDepartment(it) }
                                        )
                                    }

                                    FilterChip(
                                        selected = roleFilter == null,
                                        onClick = { viewModel.filterByRole(null) },
                                        label = { Text("所有角色") }
                                    )
                                    UserRole.values().forEach { role ->
                                        FilterChip(
                                            selected = roleFilter == role,
                                            onClick = { viewModel.filterByRole(role) },
                                            label = { Text(role.displayName) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilterChip(
                                        selected = statusFilter == null,
                                        onClick = { viewModel.filterByStatus(null) },
                                        label = { Text("所有状态") }
                                    )
                                    UserStatus.values().forEach { status ->
                                        FilterChip(
                                            selected = statusFilter == status,
                                            onClick = { viewModel.filterByStatus(status) },
                                            label = { Text(status.displayName) }
                                        )
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty() || roleFilter != null || statusFilter != null || (viewModel.getCurrentUser()?.role == UserRole.SUPER_ADMIN && filterDepartmentId != null),
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "已应用筛选",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        AnimatedTextButton(
                                            onClick = {
                                                viewModel.updateSearchQuery("")
                                                viewModel.filterByRole(null)
                                                viewModel.filterByStatus(null)
                                                if (viewModel.getCurrentUser()?.role == UserRole.SUPER_ADMIN) {
                                                    viewModel.filterByDepartment(null)
                                                }
                                            }
                                        ) {
                                            Text("清除筛选")
                                        }
                                    }
                                }
                            }
                        }

                // Users List
                val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                
                if (uiState.isLoading && filteredUsers.isEmpty()) {
                    UserListSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp), // Extra bottom padding for FAB
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredUsers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    message = if (searchQuery.isNotEmpty()) "未找到匹配用户" else "暂无用户数据",
                                    icon = Icons.Outlined.PersonSearch,
                                    onRetry = if (searchQuery.isNotEmpty()) { { viewModel.updateSearchQuery("") } } else null,
                                    retryText = "清除搜索条件"
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = filteredUsers,
                                key = { _, user -> user.id },
                                contentType = { _, _ -> "user" }
                            ) { index, user ->
                                AnimatedListItem(
                                    enabled = enableAnimations,
                                    listAnimationType = listAnimationType,
                                    index = index
                                ) {
                                    val currentUser = viewModel.getCurrentUser()
                                    val canManageUser = canManage && ((currentUser?.role?.ordinal ?: Int.MAX_VALUE) < user.role.ordinal)
                                    
                                    UserCard(
                                        user = user,
                                        departments = departments,
                                        canManage = canManageUser,
                                        isImmersive = isImmersive,
                                        onEdit = { viewModel.showEditDialog(user) },
                                        onResetPassword = { viewModel.showPasswordDialog(user) },
                                        onToggleStatus = {
                                            if (user.status == UserStatus.NORMAL) {
                                                viewModel.showBanDialog(user)
                                            } else {
                                                viewModel.updateUserStatus(user.id, UserStatus.NORMAL)
                                            }
                                        },
                                        onDelete = { viewModel.showDeleteDialog(user) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            MD3PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Dialogs
    if (uiState.showAddDialog) {
        AddEditUserDialog(
            user = null,
            currentUser = viewModel.getCurrentUser(),
            departments = viewModel.departments.collectAsStateWithLifecycle().value,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { newUser -> viewModel.createUser(newUser) }
        )
    }

    if (uiState.showEditDialog && uiState.selectedUser != null) {
        AddEditUserDialog(
            user = uiState.selectedUser,
            currentUser = viewModel.getCurrentUser(),
            departments = viewModel.departments.collectAsStateWithLifecycle().value,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { updatedUser -> viewModel.updateUser(updatedUser) }
        )
    }

    if (uiState.showPasswordDialog && uiState.selectedUser != null) {
        ResetPasswordDialog(
            user = uiState.selectedUser!!,
            onDismiss = { viewModel.hidePasswordDialog() },
            onConfirm = { newPassword -> viewModel.resetUserPassword(uiState.selectedUser!!.id, newPassword) }
        )
    }

    if (uiState.showDeleteDialog && uiState.selectedUser != null) {
        DeleteUserDialog(
            user = uiState.selectedUser!!,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteUser(uiState.selectedUser!!.id) }
        )
    }

    if (uiState.showBanDialog && uiState.selectedUser != null) {
        BanUserDialog(
            user = uiState.selectedUser!!,
            onDismiss = { viewModel.hideBanDialog() },
            onConfirm = { reason -> 
                viewModel.updateUserStatus(uiState.selectedUser!!.id, UserStatus.BANNED, reason)
                viewModel.hideBanDialog()
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
fun UserCard(
    user: User,
    departments: List<Department>,
    canManage: Boolean,
    isImmersive: Boolean = false,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isBanned = user.status == UserStatus.BANNED
    val roleColor = when (user.role) {
        UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.primary
        UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
        UserRole.ADVANCED_USER -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isImmersive) {
                    Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isImmersive) 0.dp else 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isBanned) MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isImmersive) 0.25f else 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = if (isImmersive) 0.85f else 1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Section: Avatar & Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBanned) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isBanned) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isBanned) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "已封禁",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = getDepartmentPath(user.departmentId, departments),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (!user.contact.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.contact,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Role Badge
                Surface(
                    color = roleColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = user.role.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Divider and Actions
            if (canManage) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Status Toggle
                    OutlinedButton(
                        onClick = onToggleStatus,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            if (isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isBanned) "解封账号" else "封禁账号", style = MaterialTheme.typography.labelMedium)
                    }

                    // Right side: Edit & More
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("编辑", style = MaterialTheme.typography.labelMedium)
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多操作", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重置密码") },
                                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onResetPassword()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除用户", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmName by remember { mutableStateOf("") }
    val isMatch = confirmName == user.name

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("删除用户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("确定要删除用户 \"${user.name}\" 吗？此操作不可撤销。")
                Text("请在下方输入用户名以确认删除：", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = confirmName,
                    onValueChange = { confirmName = it },
                    placeholder = { Text(user.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isMatch,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun BanUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(3) }
    
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Block, contentDescription = null) },
        title = { Text("封禁账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("确定要封禁用户 \"${user.name}\" 吗？封禁后该用户将无法登录。")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("封禁原因 (可选)") },
                    placeholder = { Text("请输入封禁原因") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = countdown == 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (countdown > 0) "确认封禁 ($countdown)" else "确认封禁")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    user: User?,
    currentUser: User?,
    departments: List<Department>,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var contact by remember { mutableStateOf(user?.contact ?: "") }
    var deptId by remember { mutableStateOf(user?.departmentId ?: departments.firstOrNull()?.id ?: "") }
    var role by remember { mutableStateOf(user?.role ?: UserRole.NORMAL_USER) }
    var status by remember { mutableStateOf(user?.status ?: UserStatus.NORMAL) }
    var password by remember { mutableStateOf(user?.password ?: "") }
    var invitationCode by remember { mutableStateOf(user?.invitationCode ?: "") }

    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN
    val canManageStatus = currentUser?.role != null && currentUser.role.ordinal <= UserRole.ADMIN.ordinal
    val availableRoles = remember(currentUser) {
        UserRole.values().filter { targetRole ->
            (currentUser?.role?.ordinal ?: Int.MAX_VALUE) <= targetRole.ordinal
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (user == null) "添加新成员" else "编辑成员信息",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Basic Info
                    SectionTitle(icon = Icons.Default.Person, title = "基础信息")
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("姓名") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                    )
                    
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("联系方式") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )
                    
                    var deptExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = getDepartmentPath(deptId, departments),
                            onValueChange = {},
                            label = { Text("所属部门") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false }
                        ) {
                            departments.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(getDepartmentPath(d.id, departments)) },
                                    onClick = { deptId = d.id; deptExpanded = false }
                                )
                            }
                        }
                    }

                    // 2. Role & Status
                    SectionTitle(icon = Icons.Default.Security, title = "权限与状态")
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("用户角色", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            availableRoles.forEach { r ->
                                FilterChip(
                                    selected = role == r,
                                    onClick = { role = r },
                                    label = { Text(r.displayName) },
                                    leadingIcon = if (role == r) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    if (canManageStatus) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("账号状态", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UserStatus.values().forEach { s ->
                                    FilterChip(
                                        selected = status == s,
                                        onClick = { status = s },
                                        label = { Text(s.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (s == UserStatus.BANNED) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = if (s == UserStatus.BANNED) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Advanced / Security
                    if (isSuperAdmin || user == null) {
                        SectionTitle(icon = Icons.Default.AdminPanelSettings, title = "其他设置")
                        
                        if (isSuperAdmin) {
                            OutlinedTextField(
                                value = invitationCode,
                                onValueChange = { invitationCode = it },
                                label = { Text("邀请码 (可选)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                            )
                        }

                        if (user == null) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("初始密码") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) }
                            )
                        }
                    }
                }

                // Footer Actions
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val newUser = User(
                                id = user?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                contact = contact.trim(),
                                departmentId = deptId,
                                departmentName = departments.find { it.id == deptId }?.name,
                                role = role,
                                status = status,
                                password = if (user == null) password.trim() else user.password,
                                invitationCode = if (isSuperAdmin) invitationCode.trim() else user?.invitationCode
                            )
                            onConfirm(newUser)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (user == null) "确认创建" else "保存修改")
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPwd by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LockReset, contentDescription = null) },
        title = { Text("重置密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("正在为用户 ${user.name} 设置新密码")
                OutlinedTextField(
                    value = newPwd, 
                    onValueChange = { newPwd = it }, 
                    label = { Text("输入新密码") }, 
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newPwd.trim()) }) {
                Text("确认重置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
