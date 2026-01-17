package com.equiptrack.android.ui.department

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.equiptrack.android.ui.components.EmptyStateCard
import com.equiptrack.android.ui.department.components.AddEditDepartmentDialog
import com.equiptrack.android.ui.department.components.DeleteDepartmentDialog
import com.equiptrack.android.ui.department.components.DepartmentCard
import com.equiptrack.android.ui.department.components.DepartmentDetailsView
import com.equiptrack.android.ui.department.components.OrganizationTree
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.components.MD3PullRefreshIndicator
import com.equiptrack.android.ui.components.AnimatedListItem
import com.equiptrack.android.ui.components.EmptyStateCard
import com.equiptrack.android.ui.components.AnimatedIconButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun DepartmentScreen(
    viewModel: DepartmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredDepartments by viewModel.filteredDepartments.collectAsStateWithLifecycle()
    val allDepartments by viewModel.allDepartments.collectAsStateWithLifecycle()
    val selectedDeptId by viewModel.selectedDepartmentId.collectAsStateWithLifecycle()
    val departmentUsers by viewModel.departmentUsers.collectAsStateWithLifecycle()
    val departmentItems by viewModel.departmentItems.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    var showSearch by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: 全局视图, 1: 部门详情视图
    val toastState = rememberToastState()
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshDepartments() }
    )

    // Auto-refresh when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refreshDepartments(isUserRefresh = false)
    }
    
    // Show toast for messages
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

    Scaffold(
        floatingActionButton = {
            if (viewModel.canManageDepartments()) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加部门")
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
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "部门管理",
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
                                    text = "共 ${allDepartments.size} 个部门",
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
                                    imageVector = if (showSearch) Icons.Default.FilterListOff else Icons.Default.Search,
                                    contentDescription = "搜索"
                                )
                            }
                        }
                    }

                    // Search Bar
                    AnimatedVisibility(
                        visible = showSearch,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            placeholder = { Text("搜索部门名称...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        text = { Text("全局视图") }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        text = { Text("部门详情") }
                    )
                }

                // Content
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "TabTransition",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    if (targetTab == 0) {
                        // Global View
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (filteredDepartments.isEmpty() && !uiState.isLoading) {
                                item {
                                    EmptyStateCard(
                                        message = if (searchQuery.isNotEmpty()) "未找到匹配的部门" else "暂无部门",
                                        onRetry = if (searchQuery.isNotEmpty()) { { viewModel.updateSearchQuery("") } } else null
                                    )
                                }
                            } else {
                                if (searchQuery.isEmpty()) {
                                    // Tree View when not searching
                                    item {
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.elevatedCardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.AccountTree,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "组织架构树",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                
                                                OrganizationTree(
                                                    departments = filteredDepartments,
                                                    selectedDepartmentId = null,
                                                    onSelect = { deptId ->
                                                        viewModel.selectDepartment(deptId)
                                                        currentTab = 1
                                                    },
                                                    onEdit = { dept -> viewModel.showEditDialog(dept) },
                                                    onDelete = { dept -> viewModel.showDeleteDialog(dept) },
                                                    onUpdateStructure = { updates -> viewModel.updateDepartmentStructure(updates) },
                                                    canManage = viewModel.canManageDepartments()
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // List View when searching
                                    itemsIndexed(
                                        items = filteredDepartments,
                                        key = { _, dept -> dept.id }
                                    ) { index, department ->
                                        AnimatedListItem(
                                            enabled = true,
                                            listAnimationType = "Slide",
                                            index = index
                                        ) {
                                            DepartmentCard(
                                                department = department,
                                                canManage = viewModel.canManageDepartments(),
                                                onEdit = { viewModel.showEditDialog(department) },
                                                onDelete = { viewModel.showDeleteDialog(department) },
                                                onClick = {
                                                    viewModel.selectDepartment(department.id)
                                                    currentTab = 1
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Detail View
                        DepartmentDetailsView(
                            selectedDepartmentId = selectedDeptId,
                            departments = filteredDepartments,
                            users = departmentUsers,
                            items = departmentItems,
                            canManage = viewModel.canManageDepartments(),
                            onSelectDepartment = { viewModel.selectDepartment(it) },
                            onUpdateUserRole = { userId, role ->
                                viewModel.updateUserRole(userId, role)
                            }
                        )
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
        AddEditDepartmentDialog(
            department = null,
            availableDepartments = allDepartments,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, requiresApproval, parentId ->
                viewModel.createDepartment(name, requiresApproval, parentId)
            }
        )
    }
    
    if (uiState.showEditDialog && uiState.selectedDepartment != null) {
        AddEditDepartmentDialog(
            department = uiState.selectedDepartment,
            availableDepartments = allDepartments,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, requiresApproval, parentId ->
                viewModel.updateDepartment(uiState.selectedDepartment!!.id, name, requiresApproval, parentId)
            }
        )
    }
    
    if (uiState.showDeleteDialog && uiState.selectedDepartment != null) {
        DeleteDepartmentDialog(
            departmentName = uiState.selectedDepartment!!.name,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = {
                viewModel.deleteDepartment(uiState.selectedDepartment!!.id)
            }
        )
    }
    
    // Toast Overlay
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
