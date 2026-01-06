package com.equiptrack.android.ui.user

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus

import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.ToastType
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.components.UserListSkeleton
import com.equiptrack.android.ui.components.AnimatedFloatingActionButton
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.DepartmentFilter
import com.equiptrack.android.ui.components.AnimatedListItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.utils.getDepartmentPath

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

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshUsers() }
    )

    // Auto-refresh when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refreshUsers()
    }

    // Handle error and success messages
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
    
    // Show toast after pull-to-refresh completes successfully
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing, uiState.errorMessage) {
        if (wasRefreshing && !isRefreshing && uiState.errorMessage == null) {
            toastState.showSuccess("刷新成功")
        }
        wasRefreshing = isRefreshing
    }

    var showSearch by remember { mutableStateOf(false) }
    
    Scaffold() { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "用户管理",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共 ${filteredUsers.size} 人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                imageVector = if (showSearch) Icons.Default.FilterAltOff else Icons.Default.FilterList,
                                contentDescription = if (showSearch) "收起筛选" else "筛选与搜索"
                            )
                        }
                        IconButton(onClick = { viewModel.refreshUsers() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                        if (canManage) {
                            IconButton(onClick = { viewModel.showAddDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加用户"
                                )
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showSearch,
                    enter = androidx.compose.animation.slideInVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Department Filter for Super Admin
                        if (viewModel.getCurrentUser()?.role == com.equiptrack.android.data.model.UserRole.SUPER_ADMIN) {
                            DepartmentFilter(
                                departments = departments,
                                selectedDepartmentId = filterDepartmentId,
                                onDepartmentSelected = { viewModel.filterByDepartment(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Search & filters
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            label = { Text("搜索用户") },
                            placeholder = { Text("输入姓名、联系方式或部门") },
                            // Remove icons
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Role filter
                            FilterChip(
                                selected = roleFilter == null,
                                onClick = { viewModel.filterByRole(null) },
                                label = { Text("全部角色") },
                                leadingIcon = null
                            )
                            UserRole.values().forEach { role ->
                                FilterChip(
                                    selected = roleFilter == role,
                                    onClick = { viewModel.filterByRole(role) },
                                    label = { Text(role.displayName) },
                                    leadingIcon = null
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Status filter
                            FilterChip(
                                selected = statusFilter == null,
                                onClick = { viewModel.filterByStatus(null) },
                                label = { Text("全部状态") },
                                leadingIcon = null
                            )
                            UserStatus.values().forEach { status ->
                                FilterChip(
                                    selected = statusFilter == status,
                                    onClick = { viewModel.filterByStatus(status) },
                                    label = { Text(status.displayName) },
                                    leadingIcon = null
                                )
                            }
                        }
                    }
                }

                val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "未找到匹配的用户" else "暂无用户",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Text("查看全部用户")
                                        }
                                    }
                                }
                            }
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
                                // Strict permission check: Can only manage users with strictly lower rank (higher ordinal)
                                val canManageUser = canManage && ((currentUser?.role?.ordinal ?: Int.MAX_VALUE) < user.role.ordinal)
                                
                                UserCard(
                                    user = user,
                                    departments = departments,
                                    canManage = canManageUser,
                                    onEdit = { viewModel.showEditDialog(user) },
                                    onResetPassword = { viewModel.showPasswordDialog(user) },
                                    onToggleStatus = {
                                        val newStatus = if (user.status == UserStatus.NORMAL) UserStatus.BANNED else UserStatus.NORMAL
                                        viewModel.updateUserStatus(user.id, newStatus)
                                    },
                                    onDelete = { viewModel.showDeleteDialog(user) }
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
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
fun UserCard(
    user: User,
    departments: List<Department>,
    canManage: Boolean,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isBanned = user.status == UserStatus.BANNED
    val roleColor = when (user.role) {
        UserRole.SUPER_ADMIN -> MaterialTheme.colorScheme.tertiary
        UserRole.ADMIN -> MaterialTheme.colorScheme.primary
        UserRole.ADVANCED_USER -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBanned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isBanned) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape, 
                        color = if (isBanned) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, 
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isBanned) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isBanned) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "已封禁",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (!user.departmentId.isNullOrBlank()) {
                            Text(
                                text = getDepartmentPath(user.departmentId, departments),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!user.contact.isNullOrBlank()) {
                            Text(
                                text = user.contact,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    color = roleColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = user.role.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (canManage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedTextButton(onClick = onEdit) { Text("编辑") }
                    AnimatedTextButton(onClick = onResetPassword) { Text("重置密码") }
                    AnimatedTextButton(onClick = onToggleStatus) {
                        Text(
                            if (isBanned) "解封" else "封禁",
                            color = if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    AnimatedTextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除用户") },
        text = { Text("确定要删除用户 \"${user.name}\" 吗？此操作不可撤销。") },
        confirmButton = {
            AnimatedTextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            AnimatedTextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    user: User?,
    currentUser: User?,
    departments: List<com.equiptrack.android.data.model.Department>,
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

    // Calculate permissions
    val isSuperAdmin = currentUser?.role == UserRole.SUPER_ADMIN
    val canManageStatus = currentUser?.role != null && currentUser.role.ordinal <= UserRole.ADMIN.ordinal
    val availableRoles = remember(currentUser) {
        UserRole.values().filter { targetRole ->
            // User can only assign roles strictly lower or equal to their own (higher ordinal = lower permission)
            (currentUser?.role?.ordinal ?: Int.MAX_VALUE) <= targetRole.ordinal
        }
    }

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
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (user == null) "添加用户" else "编辑用户",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "基础信息",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("姓名") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = contact,
                                    onValueChange = { contact = it },
                                    label = { Text("联系方式") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            var deptExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = getDepartmentPath(deptId, departments),
                                    onValueChange = {},
                                    label = { Text("所属部门") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                                    departments.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(getDepartmentPath(d.id, departments)) },
                                            onClick = { deptId = d.id; deptExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "角色与状态",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "角色",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                availableRoles.forEach { r ->
                                    FilterChip(
                                        selected = role == r,
                                        onClick = { role = r },
                                        label = { Text(r.displayName) }
                                    )
                                }
                            }

                            Text(
                                text = "状态",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserStatus.values().forEach { s ->
                                    FilterChip(
                                        selected = status == s,
                                        onClick = { if (canManageStatus) status = s },
                                        label = { Text(s.displayName) },
                                        enabled = canManageStatus
                                    )
                                }
                            }
                        }

                        if (isSuperAdmin) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "高级选项",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = invitationCode,
                                    onValueChange = { invitationCode = it },
                                    label = { Text("邀请码") },
                                    placeholder = { Text("仅超级管理员可编辑") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (user == null) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("初始密码") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        AnimatedButton(
                            onClick = {
                                val newUser = User(
                                    id = user?.id ?: java.util.UUID.randomUUID().toString(),
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (user == null) "创建" else "更新")
                        }
                    }
                }
            }
        }
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
                    .fillMaxWidth(0.8f)
                    .padding(16.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "重置密码",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("为用户 ${user.name} 重置密码")
                        OutlinedTextField(
                            value = newPwd, 
                            onValueChange = { newPwd = it }, 
                            label = { Text("新密码") }, 
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AnimatedOutlinedButton(onClick = onDismiss) { Text("取消") }
                        Spacer(Modifier.width(8.dp))
                        AnimatedButton(onClick = { onConfirm(newPwd.trim()) }) { Text("确定") }
                    }
                }
            }
        }
    }
}
