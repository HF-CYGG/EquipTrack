package com.equiptrack.android.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.data.model.BorrowStatus
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.ui.equipment.components.ReturnItemDialog
import com.equiptrack.android.ui.history.components.HistoryEntryCard
import com.equiptrack.android.ui.navigation.NavigationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val historyEntries by viewModel.historyEntries.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val filterDepartmentId by viewModel.filterDepartmentId.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    
    val toastState = rememberToastState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    val settingsRepository = navVm.settingsRepository
    val themeOverrides by settingsRepository.themeOverridesFlow.collectAsState()
    
    val confettiEnabled = themeOverrides.confettiEnabled ?: settingsRepository.isConfettiEnabled()
    val lowPerformanceMode = themeOverrides.lowPerformanceMode ?: settingsRepository.isLowPerformanceMode()
    var showConfetti by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshHistory() }
    )

    // Auto refresh on entry
    LaunchedEffect(Unit) {
        viewModel.syncHistory()
        viewModel.updateOverdueStatus()
    }
    
    // Handle error and success messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            toastState.showError(message)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            toastState.showSuccess(message)
            if (confettiEnabled && !lowPerformanceMode && message.contains("归还")) {
                showConfetti = true
            }
            viewModel.clearMessages()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (fabExpanded) {
                    AnimatedSmallFloatingActionButton(onClick = { showFilterDialog = true; fabExpanded = false }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
                AnimatedFloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Department Filter for Super Admin
                    if (viewModel.getCurrentUser()?.role == com.equiptrack.android.data.model.UserRole.SUPER_ADMIN) {
                        DepartmentFilter(
                            departments = departments,
                            selectedDepartmentId = filterDepartmentId,
                            onDepartmentSelected = { viewModel.filterByDepartment(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Loading indicator
                    if (uiState.isLoading && !isRefreshing) {
                        HistoryListSkeleton()
                    } else {
                        val listAnimationType = themeOverrides.listAnimationType ?: navVm.settingsRepository.getListAnimationType()
                        val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            state = listState
                        ) {
                            if (historyEntries.isEmpty()) {
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
                                                Icons.Default.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = if (filterStatus != null) "没有符合条件的记录" else "暂无借用记录",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (filterStatus != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                AnimatedTextButton(
                                                    onClick = { viewModel.filterByStatus(null) }
                                                ) {
                                                    Text("查看全部记录")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = historyEntries,
                                    key = { _, entry -> entry.id },
                                    contentType = { _, _ -> "borrow_history_entry" }
                                ) { index, entry ->
                                    AnimatedListItem(
                                        enabled = enableAnimations,
                                        listAnimationType = listAnimationType,
                                        index = index
                                    ) {
                                        HistoryEntryCard(
                                            entry = entry,
                                            canForceReturn = viewModel.canForceReturn(),
                                            serverUrl = serverUrl,
                                            onReturn = {
                                                viewModel.showReturnDialog(entry, isForced = false)
                                            },
                                            onForceReturn = {
                                                viewModel.showReturnDialog(entry, isForced = true)
                                            }
                                        )
                                    }
                                }
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
        
        // Filter Dialog
        if (showFilterDialog) {
            Dialog(
                onDismissRequest = { showFilterDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showFilterDialog = false }
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
                                text = "筛选借用记录",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { viewModel.filterByStatus(null) },
                                    label = { Text("全部记录") },
                                    leadingIcon = { if (filterStatus == null) Icon(Icons.Default.Check, contentDescription = null) }
                                )
                                BorrowStatus.values().forEach { status ->
                                    AssistChip(
                                        onClick = { viewModel.filterByStatus(status) },
                                        label = { Text(status.displayName) },
                                        leadingIcon = { if (filterStatus == status) Icon(Icons.Default.Check, contentDescription = null) }
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                AnimatedTextButton(onClick = { showFilterDialog = false }) {
                                    Text("完成")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Return dialog
        if (uiState.showReturnDialog && uiState.selectedHistoryEntry != null) {
            val context = LocalContext.current
            ReturnItemDialog(
                historyEntry = uiState.selectedHistoryEntry!!,
                isForced = uiState.isForceReturn,
                adminName = if (uiState.isForceReturn) viewModel.getCurrentUserName() else null,
                currentUserRole = viewModel.getCurrentUser()?.role,
                onDismiss = { viewModel.hideReturnDialog() },
                onConfirm = { returnRequest ->
                    viewModel.returnItem(
                        context = context,
                        itemId = uiState.selectedHistoryEntry!!.itemId,
                        historyEntryId = uiState.selectedHistoryEntry!!.id,
                        returnRequest = returnRequest
                    )
                }
            )
        }
        
        // Toast message overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ToastMessage(
                toastData = toastState.currentToast,
                onDismiss = { toastState.dismiss() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
            ConfettiOverlay(
                visible = showConfetti,
                modifier = Modifier.align(Alignment.Center),
                onFinished = { showConfetti = false }
            )
        }
    }
}
