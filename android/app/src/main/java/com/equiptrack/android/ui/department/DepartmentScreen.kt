package com.equiptrack.android.ui.department

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.ui.department.components.AddEditDepartmentDialog
import com.equiptrack.android.ui.department.components.DepartmentCard
import com.equiptrack.android.ui.department.components.DepartmentDetailsView
import com.equiptrack.android.ui.equipment.components.DeleteConfirmDialog
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.ToastType
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.components.AnimatedFloatingActionButton
import com.equiptrack.android.ui.components.AnimatedSmallFloatingActionButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DepartmentScreen(
    viewModel: DepartmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredDepartments by viewModel.filteredDepartments.collectAsStateWithLifecycle()
    val selectedDeptId by viewModel.selectedDepartmentId.collectAsStateWithLifecycle()
    val departmentUsers by viewModel.departmentUsers.collectAsStateWithLifecycle()
    val departmentItems by viewModel.departmentItems.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // 0: 全局视图, 1: 部门详情视图
    val toastState = rememberToastState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshDepartments() }
    )

    // Auto-refresh when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refreshDepartments()
    }
    
    // Show snackbar for messages
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            ) {
                // 视图切换（全局 / 详情）
                TabRow(selectedTabIndex = currentTab) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }) {
                        Text("全局视图", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }) {
                        Text("部门详情视图", modifier = Modifier.padding(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 顶部操作行已移除，搜索改为右下角弹出菜单控制
                // Search bar（点击图标后展开）
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        label = { Text("搜索部门") },
                        placeholder = { Text("输入部门名称") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                AnimatedIconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                if (currentTab == 0) {
                    // 全局视图：部门列表
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredDepartments.isEmpty() && !uiState.isLoading) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Business,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "未找到匹配的部门" else "暂无部门",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (searchQuery.isEmpty() && viewModel.canManageDepartments()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "点击右上角的 + 按钮添加部门",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredDepartments) { department ->
                                DepartmentCard(
                                    department = department,
                                    canManage = viewModel.canManageDepartments(),
                                    onEdit = { viewModel.showEditDialog(department) },
                                    onDelete = { viewModel.showDeleteDialog(department) }
                                )
                            }
                        }
                    }
                } else {
                    // 部门详情视图
                    DepartmentDetailsView(
                        selectedDepartmentId = selectedDeptId,
                        departments = filteredDepartments,
                        users = departmentUsers,
                        items = departmentItems,
                        canManage = viewModel.canManageDepartments(), // Or logic for specific dept
                        onSelectDepartment = { viewModel.selectDepartment(it) },
                        onUpdateUserRole = { userId, role ->
                            viewModel.updateUserRole(userId, role)
                        }
                    )
                }
            }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 右下角统一浮动菜单（添加 / 刷新）
        // 刷新按钮已移除，保留添加和搜索
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (fabExpanded) {
                    AnimatedSmallFloatingActionButton(onClick = { viewModel.showAddDialog(); fabExpanded = false }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                    // Refresh button removed
                    AnimatedSmallFloatingActionButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
                AnimatedFloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                }
            }
        }
        
        // Dialogs
    }
    
    // Dialogs
    if (uiState.showAddDialog) {
        AddEditDepartmentDialog(
            department = null,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name ->
                viewModel.createDepartment(name)
            }
        )
    }
    
    if (uiState.showEditDialog && uiState.selectedDepartment != null) {
        AddEditDepartmentDialog(
            department = uiState.selectedDepartment,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name ->
                val updatedDepartment = uiState.selectedDepartment!!.copy(name = name)
                viewModel.updateDepartment(updatedDepartment)
            }
        )
    }
    
    if (uiState.showDeleteDialog && uiState.selectedDepartment != null) {
        DeleteConfirmDialog(
            itemName = "部门 \"${uiState.selectedDepartment!!.name}\"",
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = {
                viewModel.deleteDepartment(uiState.selectedDepartment!!.id)
            }
        )
    }
    
    // Error/Success messages - handled by LaunchedEffect above
    
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
private fun OrganizationTree(
    departments: List<com.equiptrack.android.data.model.Department>,
    canManage: Boolean,
    onSelect: (String) -> Unit
) {
    val groups = remember(departments) {
        departments.groupBy { dept ->
            dept.name.firstOrNull()?.uppercaseChar() ?: '#'
        }.toSortedMap()
    }

    groups.forEach { (initial, list) ->
        Text("分组：$initial", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        list.forEach { dept ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(dept.name)
                    Text("ID：${dept.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedTextButton(onClick = { onSelect(dept.id) }) { Text("查看") }
                    if (canManage) {
                        // 未来接入层级与结构维护入口（仅展示按钮骨架）
                        AnimatedOutlinedButton(onClick = { /* TODO: 打开结构维护 */ }) { Text("维护结构") }
                    }
                }
            }
            Divider()
        }
        Spacer(Modifier.height(8.dp))
    }
}