package com.equiptrack.android.ui.equipment

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.data.model.Category
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.User
import com.equiptrack.android.ui.equipment.components.AddEditItemDialog
import com.equiptrack.android.ui.equipment.components.AddEditCategoryDialog
import com.equiptrack.android.ui.equipment.components.EquipmentItemCard
import com.equiptrack.android.ui.equipment.components.DeleteConfirmDialog
import com.equiptrack.android.ui.equipment.components.BorrowItemDialog
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.ToastType
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.components.ConfettiOverlay
import com.equiptrack.android.ui.components.AnimatedFloatingActionButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedSmallFloatingActionButton
import com.equiptrack.android.ui.components.EquipmentListSkeleton
import com.equiptrack.android.ui.components.AnimatedListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    departments: List<Department>,
    selectedDepartmentId: String?,
    onDepartmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var deptMenuExpanded by remember { mutableStateOf(false) }
    val currentDeptName = remember(departments, selectedDepartmentId) {
        departments.find { it.id == selectedDepartmentId }?.name ?: "所有部门"
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { deptMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "部门：$currentDeptName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = deptMenuExpanded,
                    onDismissRequest = { deptMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("所有部门") },
                        onClick = {
                            onDepartmentSelected(null)
                            deptMenuExpanded = false
                        },
                        leadingIcon = {
                            if (selectedDepartmentId == null) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    Divider()
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept.name) },
                            onClick = {
                                onDepartmentSelected(dept.id)
                                deptMenuExpanded = false
                            },
                            leadingIcon = {
                                if (dept.id == selectedDepartmentId) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text("全部") },
                leadingIcon = if (selectedCategoryId == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        
        items(categories) { category ->
            val isSelected = selectedCategoryId == category.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    try {
                                        Color(android.graphics.Color.parseColor(category.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun EquipmentScreen(
    navController: NavController,
    viewModel: EquipmentViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val context = LocalContext.current
    
    // 监听成功消息
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val filterDepartmentId by viewModel.filterDepartmentId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    val settingsRepository = navVm.settingsRepository
    val themeOverrides by settingsRepository.themeOverridesFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val canManageItems = viewModel.canManageItems()
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }
    
    val isCompactList by remember { mutableStateOf(settingsRepository.isEquipmentListCompact()) }
    val listAnimationType = themeOverrides.listAnimationType ?: settingsRepository.getListAnimationType()
    val cornerRadius = themeOverrides.cornerRadius ?: settingsRepository.getCornerRadius()
    val equipmentImageRatio = themeOverrides.equipmentImageRatio ?: settingsRepository.getEquipmentImageRatio()
    val cardMaterial = themeOverrides.cardMaterial ?: settingsRepository.getCardMaterial()
    val tagStyle = themeOverrides.tagStyle ?: settingsRepository.getTagStyle()
    val confettiEnabled = themeOverrides.confettiEnabled ?: settingsRepository.isConfettiEnabled()
    val lowPerformanceMode = themeOverrides.lowPerformanceMode ?: settingsRepository.isLowPerformanceMode()
    var showConfetti by remember { mutableStateOf(false) }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() }
    )

    // Show toast for messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            // Only show toast in main screen if dialog is NOT showing
            if (!uiState.showAddDialog && !uiState.showEditDialog) {
                if (serverUrl.contains("10.0.2.2")) {
                    toastState.showError("$message\n提示：真机调试请在设置中配置局域网IP")
                } else {
                    toastState.showError(message)
                }
            }
            // Delay slightly to allow dialog to receive the error state before clearing
            kotlinx.coroutines.delay(50)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            toastState.showSuccess(message)
            if (confettiEnabled && !lowPerformanceMode && message.contains("借用")) {
                showConfetti = true
            }
            viewModel.clearMessages()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pullRefresh(pullRefreshState)
    ) {
        // 主内容区域
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 搜索栏 - 带滑动展开动画
            AnimatedVisibility(
                visible = showSearch,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = 100
                    )
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutLinearInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            label = { 
                                Text(
                                    "搜索物资",
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            placeholder = { 
                                Text(
                                    "输入物资名称或描述",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            trailingIcon = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (searchQuery.isNotEmpty()) {
                                        AnimatedIconButton(
                                            onClick = { viewModel.updateSearchQuery("") }
                                        ) {
                                            Icon(
                                                Icons.Default.Clear, 
                                                contentDescription = "清除",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    AnimatedIconButton(
                                        onClick = { showSearch = false }
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "关闭搜索",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // 分类选择栏
            CategoryChipsRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { viewModel.selectCategory(it) },
                departments = departments,
                selectedDepartmentId = filterDepartmentId,
                onDepartmentSelected = { viewModel.filterByDepartment(it) }
            )
            
            if (uiState.isLoading && filteredItems.isEmpty()) {
                EquipmentListSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    itemCount = 6
                )
            } else {
                val enableAnimations = !lowPerformanceMode && listAnimationType != "None"
                val enableItemAnimations = enableAnimations
                val categoriesMap = remember(categories) { categories.associateBy { it.id } }
                val categoryColorMap = remember(categories) {
                    categories.associate { category ->
                        category.id to runCatching {
                            Color(android.graphics.Color.parseColor(category.color))
                        }.getOrNull()
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredItems.isEmpty() && !uiState.isLoading) {
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
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = when {
                                            searchQuery.isNotEmpty() -> "没有找到匹配的物资"
                                            selectedCategoryId != null -> "该分类下暂无物资"
                                            else -> "暂无物资"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isEmpty() && selectedCategoryId == null && canManageItems) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "点击右上角的 + 按钮添加物资",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredItems,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "equipment_item" }
                        ) { index, item ->
                            AnimatedListItem(
                                enabled = enableItemAnimations,
                                listAnimationType = listAnimationType,
                                index = index,
                                lazyListState = listState
                            ) {
                                EquipmentItemCard(
                                    item = item,
                                    category = categoriesMap[item.categoryId],
                                    categoryColor = categoryColorMap[item.categoryId],
                                    canManage = canManageItems,
                                    serverUrl = serverUrl,
                                    onEdit = { viewModel.showEditDialog(item) },
                                    onDelete = { viewModel.showDeleteDialog(item) },
                                    onBorrow = { viewModel.showBorrowDialog(item) },
                                    onPreviewImage = { url, title ->
                                        previewImageUrl = url
                                        previewImageTitle = title
                                    },
                                    compact = isCompactList,
                                    cornerRadius = cornerRadius,
                                    equipmentImageRatio = equipmentImageRatio,
                                    cardMaterial = cardMaterial,
                                    tagStyle = tagStyle
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 右下角统一浮动菜单（添加 / 刷新 / 搜索）
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (fabExpanded) {
                AnimatedSmallFloatingActionButton(
                    onClick = { 
                        showSearch = !showSearch
                        fabExpanded = false 
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                
                if (canManageItems) {
                    AnimatedSmallFloatingActionButton(
                        onClick = { viewModel.showAddDialog(); fabExpanded = false },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }
            
            AnimatedFloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = if (fabExpanded) "关闭菜单" else "更多操作",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    
    if (!previewImageUrl.isNullOrEmpty()) {
        Dialog(
            onDismissRequest = {
                previewImageUrl = null
                previewImageTitle = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable {
                        previewImageUrl = null
                        previewImageTitle = null
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = previewImageTitle,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.8f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    error = rememberVectorPainter(Icons.Default.BrokenImage),
                    placeholder = rememberVectorPainter(Icons.Default.Image)
                )
            }
        }
    }
    
    // Dialogs
    if (uiState.showAddDialog) {
        AddEditItemDialog(
            item = null,
            categories = categories,
            departmentId = viewModel.getCurrentUser()?.departmentId ?: "",
            serverUrl = serverUrl,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { item ->
                viewModel.createItem(context, item)
            },
            onAddCategory = { category ->
                viewModel.createCategory(category)
            },
            onDeleteCategory = { categoryId ->
                viewModel.deleteCategory(categoryId)
            }
        )
    }
    
    // Add Category Dialog
    if (uiState.showAddCategoryDialog) {
        AddEditCategoryDialog(
            onDismiss = { viewModel.hideAddCategoryDialog() },
            onConfirm = { category ->
                viewModel.createCategory(category)
            }
        )
    }
    
    if (uiState.showEditDialog && uiState.selectedItem != null) {
        AddEditItemDialog(
            item = uiState.selectedItem,
            categories = categories,
            departmentId = viewModel.getCurrentUser()?.departmentId ?: "",
            serverUrl = serverUrl,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { item ->
                viewModel.updateItem(context, item)
            },
            onAddCategory = { category ->
                viewModel.createCategory(category)
            },
            onDeleteCategory = { categoryId ->
                viewModel.deleteCategory(categoryId)
            }
        )
    }
    
    if (uiState.showDeleteDialog && uiState.selectedItem != null) {
        DeleteConfirmDialog(
            itemName = uiState.selectedItem!!.name,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = {
                viewModel.deleteItem(uiState.selectedItem!!)
            }
        )
    }
    
    if (uiState.showBorrowDialog && uiState.selectedItem != null) {
        BorrowItemDialog(
            item = uiState.selectedItem!!,
            serverUrl = serverUrl,
            onDismiss = { viewModel.hideBorrowDialog() },
            onConfirm = { borrowRequest ->
                viewModel.borrowItem(uiState.selectedItem!!.id, borrowRequest)
            },
            currentUser = viewModel.getCurrentUser()
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
