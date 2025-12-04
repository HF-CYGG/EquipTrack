package com.equiptrack.android.ui.user

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun UsersScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredUsers by viewModel.filteredUsers.collectAsStateWithLifecycle()
    val roleFilter by viewModel.filterRole.collectAsStateWithLifecycle()
    val statusFilter by viewModel.filterStatus.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val filterDepartmentId by viewModel.filterDepartmentId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val canManage = viewModel.canManageUsers()
    var fabExpanded by remember { mutableStateOf(false) }
    val toastState = rememberToastState()

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
                    .padding(16.dp)
                    .padding(bottom = if (showSearch) 80.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 搜索和筛选区域（条件显示）
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

                // 常显的快速筛选区（精简版）
                // Remove this entire block to simplify UI as per user request "简洁美观大气"
                // We will rely on the expandable search/filter area or just keep it hidden until requested.
                // Actually, let's just keep the search/filter functionality but make it cleaner.
                // The user asked for "简洁" (concise) so maybe just one row of filters is enough or just the search bar.
                // Let's remove the duplicate "quick row" to reduce clutter.
                
                // Users list
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        items(filteredUsers) { user ->
                            UserCard(
                                user = user,
                                canManage = canManage,
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

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

        // 右下角浮动操作按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // 搜索按钮
                AnimatedFloatingActionButton(
                    onClick = { showSearch = !showSearch },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                ) {
                    // Use Text instead of Icon for a cleaner look if desired, or simple icons without text
                    // User said "不需要额外的图标" (no extra icons), but FABs usually need something.
                    // Let's keep simple icons for FABs but remove decorative ones elsewhere.
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (showSearch) "关闭搜索" else "搜索"
                    )
                }

                // 添加用户按钮
                if (canManage) {
                    AnimatedFloatingActionButton(
                        onClick = { viewModel.showAddDialog() },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加用户"
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showAddDialog) {
        AddEditUserDialog(
            user = null,
            departments = viewModel.departments.collectAsStateWithLifecycle().value,
            isSuperAdmin = viewModel.canManageAllUsers(),
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { newUser -> viewModel.createUser(newUser) }
        )
    }

    if (uiState.showEditDialog && uiState.selectedUser != null) {
        AddEditUserDialog(
            user = uiState.selectedUser,
            departments = viewModel.departments.collectAsStateWithLifecycle().value,
            isSuperAdmin = viewModel.canManageAllUsers(),
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
    canManage: Boolean,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isBanned = user.status == UserStatus.BANNED
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
        colors = CardDefaults.cardColors(
            containerColor = if (isBanned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isBanned) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    // Avatar initials
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    }
                }
                
                Text(
                    text = user.role.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (canManage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedTextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text("编辑")
                    }
                    AnimatedTextButton(onClick = onResetPassword, modifier = Modifier.weight(1f)) {
                        Text("重置密码")
                    }
                    AnimatedTextButton(onClick = onToggleStatus, modifier = Modifier.weight(1f)) {
                        Text(if (isBanned) "解封" else "封禁", color = if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    AnimatedTextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
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
    departments: List<com.equiptrack.android.data.model.Department>,
    isSuperAdmin: Boolean = false,
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
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (user == null) "添加用户" else "编辑用户",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("联系方式") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        // Department selection
                        var deptExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                            OutlinedTextField(
                                readOnly = true,
                                value = departments.find { it.id == deptId }?.name ?: "",
                                onValueChange = {},
                                label = { Text("所属部门") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                                departments.forEach { d ->
                                    DropdownMenuItem(text = { Text(d.name) }, onClick = { deptId = d.id; deptExpanded = false })
                                }
                            }
                        }

                        // Role selection
                        var roleExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                            OutlinedTextField(
                                readOnly = true,
                                value = role.displayName,
                                onValueChange = {},
                                label = { Text("角色") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                UserRole.values().forEach { r ->
                                    DropdownMenuItem(text = { Text(r.displayName) }, onClick = { role = r; roleExpanded = false })
                                }
                            }
                        }

                        // Status selection
                        var statusExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                            OutlinedTextField(
                                readOnly = true,
                                value = status.displayName,
                                onValueChange = {},
                                label = { Text("状态") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                UserStatus.values().forEach { s ->
                                    DropdownMenuItem(text = { Text(s.displayName) }, onClick = { status = s; statusExpanded = false })
                                }
                            }
                        }

                        if (isSuperAdmin) {
                            OutlinedTextField(
                                value = invitationCode,
                                onValueChange = { invitationCode = it },
                                label = { Text("邀请码") },
                                placeholder = { Text("仅超级管理员可编辑") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (user == null) {
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("初始密码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AnimatedOutlinedButton(onClick = onDismiss) { Text("取消") }
                        Spacer(Modifier.width(8.dp))
                        AnimatedButton(onClick = {
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
                        }) { Text(if (user == null) "创建" else "更新") }
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