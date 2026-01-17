package com.equiptrack.android.ui.approval

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.equiptrack.android.data.model.RegistrationRequest
import com.equiptrack.android.data.model.BorrowRequestEntry
import com.equiptrack.android.ui.approval.components.RegistrationRequestCard
import com.equiptrack.android.ui.approval.components.BorrowRequestCard
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.ToastType
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.MD3PullRefreshIndicator
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.AnimatedFloatingActionButton
import com.equiptrack.android.ui.components.ApprovalListSkeleton
import com.equiptrack.android.ui.components.AnimatedListItem
import com.equiptrack.android.ui.navigation.NavigationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ApprovalScreen(
    viewModel: ApprovalViewModel = hiltViewModel()
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.syncRequests(isRefresh = true) }
    )
    
    // 页面进入和返回时自动同步数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncRequests()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
    val filteredRequests by viewModel.filteredRequests.collectAsStateWithLifecycle()
    val settingsRepository = navVm.settingsRepository
    val themeOverrides by settingsRepository.themeOverridesFlow.collectAsStateWithLifecycle()
    val lowPerformanceMode = themeOverrides.lowPerformanceMode ?: settingsRepository.isLowPerformanceMode()
    val listAnimationType = themeOverrides.listAnimationType ?: settingsRepository.getListAnimationType()
    val listState = rememberLazyListState()
    
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 更新搜索查询
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 右下角浮动操作按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            // 主内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showSearch) 80.dp else 0.dp)
            ) {
                // 顶部操作行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 移除标题，仅保留顶部路径显示
                }

                // 访问级别信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = viewModel.getAccessLevelDescription(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "您可以查看和处理注册申请",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // 搜索栏（条件显示）
                AnimatedVisibility(
                    visible = showSearch,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索申请人姓名或联系方式") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                AnimatedIconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true
                    )
                }

                // 申请列表：仅在没有数据且正在加载时显示骨架屏，其余情况优先显示缓存数据
                if (uiState.isLoading && filteredRequests.isEmpty()) {
                    ApprovalListSkeleton()
                } else {
                    val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        if (filteredRequests.isEmpty()) {
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
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "未找到匹配的申请" else "暂无待审批申请",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AnimatedTextButton(onClick = { searchQuery = "" }) {
                                                Text("查看全部申请")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredRequests,
                                key = { _, request -> request.id },
                                contentType = { _, _ -> "registration_request" }
                            ) { index, request ->
                                AnimatedListItem(
                                    enabled = enableAnimations,
                                    listAnimationType = listAnimationType,
                                    index = index
                                ) {
                                    RegistrationRequestCard(
                                        request = request,
                                        canApprove = viewModel.canApproveRequests(),
                                        onApprove = { viewModel.showApproveDialog(request) },
                                        onReject = { viewModel.showRejectDialog(request) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (showSearch) "关闭搜索" else "搜索"
                    )
                }
            }
            
            MD3PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    
    // Approve dialog
    if (uiState.showApproveDialog && uiState.selectedRequest != null) {
        Dialog(onDismissRequest = { viewModel.hideApproveDialog() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "批准申请",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "确定要批准 \"${uiState.selectedRequest!!.name}\" 的注册申请吗？\n\n批准后将创建新用户账号，默认角色为普通用户。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = { viewModel.hideApproveDialog() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isActionLoading
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                viewModel.approveRequest(uiState.selectedRequest!!.id)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isActionLoading
                        ) {
                            if (uiState.isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("批准")
                        }
                    }
                }
            }
        }
    }
    
    // Reject dialog
    if (uiState.showRejectDialog && uiState.selectedRequest != null) {
        Dialog(onDismissRequest = { viewModel.hideRejectDialog() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "拒绝申请",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "确定要拒绝 \"${uiState.selectedRequest!!.name}\" 的注册申请吗？\n\n此操作不可撤销，申请记录将被删除。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = { viewModel.hideRejectDialog() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isActionLoading
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                viewModel.rejectRequest(uiState.selectedRequest!!.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isActionLoading
                        ) {
                             if (uiState.isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("拒绝")
                        }
                    }
                }
            }
        }
    }
    
    // Error/Success messages
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or handle error
            viewModel.clearMessages()
        }
    }
    
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or handle success
            viewModel.clearMessages()
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BorrowApprovalScreen(
    viewModel: BorrowApprovalViewModel = hiltViewModel()
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(Unit) {
        viewModel.fetchRequests()
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
    val filteredRequests by viewModel.filteredRequests.collectAsStateWithLifecycle()
    val historyRequests by viewModel.historyFilteredRequests.collectAsStateWithLifecycle()
    val settingsRepository = navVm.settingsRepository
    val themeOverrides by settingsRepository.themeOverridesFlow.collectAsStateWithLifecycle()
    val lowPerformanceMode = themeOverrides.lowPerformanceMode ?: settingsRepository.isLowPerformanceMode()
    val listAnimationType = themeOverrides.listAnimationType ?: settingsRepository.getListAnimationType()
    val listState = rememberLazyListState()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showSearch) 80.dp else 0.dp)
            ) {
                // Header Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = viewModel.getAccessLevelDescription(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "您可以查看和处理借用申请",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Custom Tab Row
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    TabRow(
                        selectedTabIndex = if (selectedTab == BorrowApprovalTab.PENDING) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            if (selectedTab == BorrowApprovalTab.PENDING) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
                                    height = 0.dp,
                                    color = Color.Transparent
                                )
                            } else {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[1]),
                                    height = 0.dp,
                                    color = Color.Transparent
                                )
                            }
                        },
                        divider = {}
                    ) {
                        val tabs = listOf("待审批", "审批历史")
                        tabs.forEachIndexed { index, title ->
                            val selected = (index == 0 && selectedTab == BorrowApprovalTab.PENDING) ||
                                         (index == 1 && selectedTab == BorrowApprovalTab.HISTORY)
                            
                            val targetColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Tab(
                                selected = selected,
                                onClick = {
                                    val newTab = if (index == 0) BorrowApprovalTab.PENDING else BorrowApprovalTab.HISTORY
                                    if (selectedTab != newTab) {
                                        viewModel.selectTab(newTab)
                                        if (newTab == BorrowApprovalTab.PENDING) {
                                            if (filteredRequests.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                                                viewModel.fetchRequests()
                                            }
                                        } else {
                                            if (historyRequests.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                                                viewModel.fetchHistory()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(targetColor)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSearch,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索借用人姓名或联系方式") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                if (uiState.isLoading) {
                    ApprovalListSkeleton()
                } else {
                    val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp), // Add padding for FAB
                        state = listState
                    ) {
                        val currentList = if (selectedTab == BorrowApprovalTab.PENDING) filteredRequests else historyRequests
                        if (currentList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(80.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (selectedTab == BorrowApprovalTab.PENDING) Icons.Default.AssignmentTurnedIn else Icons.Default.History,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (selectedTab == BorrowApprovalTab.PENDING) {
                                                    if (searchQuery.isNotEmpty()) "未找到匹配的申请" else "暂无待审批申请"
                                                } else {
                                                    if (searchQuery.isNotEmpty()) "未找到匹配的记录" else "暂无审批历史"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (selectedTab == BorrowApprovalTab.PENDING) "所有申请都已处理完毕" else "审批记录将显示在这里",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }

                                        if (searchQuery.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = { searchQuery = "" },
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("清除搜索")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = currentList,
                                key = { _, request -> request.id },
                                contentType = { _, _ -> "borrow_request" }
                            ) { index, request ->
                                AnimatedListItem(
                                    enabled = enableAnimations,
                                    listAnimationType = listAnimationType,
                                    index = index
                                ) {
                                    BorrowRequestCard(
                                        request = request,
                                        canApprove = selectedTab == BorrowApprovalTab.PENDING && viewModel.canApproveRequests(),
                                        onApprove = { viewModel.showApproveDialog(request) },
                                        onReject = { viewModel.showRejectDialog(request) },
                                        serverUrl = serverUrl
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showSearch = !showSearch },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (showSearch) "关闭搜索" else "搜索"
                    )
                }
            }
            
            MD3PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    
    if (uiState.showApproveDialog && uiState.selectedRequest != null) {
        Dialog(onDismissRequest = { viewModel.hideApproveDialog() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }
                        Text(
                            text = "通过借用申请",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "确定要通过该借用申请吗？通过后将为借用人办理借用并扣减库存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.remarkInput,
                        onValueChange = { viewModel.updateRemarkInput(it) },
                        label = { Text("审批备注（可选）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = { viewModel.hideApproveDialog() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            enabled = !uiState.isActionLoading
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                viewModel.approveRequest(uiState.selectedRequest!!.id)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            enabled = !uiState.isActionLoading
                        ) {
                            if (uiState.isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("通过")
                        }
                    }
                }
            }
        }
    }
    
    if (uiState.showRejectDialog && uiState.selectedRequest != null) {
        Dialog(onDismissRequest = { viewModel.hideRejectDialog() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }
                        Text(
                            text = "驳回借用申请",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "确定要驳回该借用申请吗？建议填写驳回原因，便于借用人知晓。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.remarkInput,
                        onValueChange = { viewModel.updateRemarkInput(it) },
                        label = { Text("驳回原因（可选）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = { viewModel.hideRejectDialog() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            enabled = !uiState.isActionLoading
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                viewModel.rejectRequest(uiState.selectedRequest!!.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            enabled = !uiState.isActionLoading
                        ) {
                             if (uiState.isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("驳回")
                        }
                    }
                }
            }
        }
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
