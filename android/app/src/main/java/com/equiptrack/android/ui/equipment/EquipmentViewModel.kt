package com.equiptrack.android.ui.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.DepartmentRepository
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.data.repository.EquipmentRepository
import com.equiptrack.android.data.repository.BorrowRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class EquipmentViewModel @Inject constructor(
    private val equipmentRepository: EquipmentRepository,
    private val borrowRepository: BorrowRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EquipmentUiState())
    val uiState: StateFlow<EquipmentUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterDepartmentId = MutableStateFlow<String?>(null)
    val filterDepartmentId: StateFlow<String?> = _filterDepartmentId.asStateFlow()
    
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()

    val departments: StateFlow<List<Department>> = departmentRepository.getAllDepartments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Get items based on user role and search query
    val filteredItems: StateFlow<List<EquipmentItem>> = combine(
        _searchQuery.debounce(250),
        _selectedCategoryId,
        _filterDepartmentId,
        if (currentUser?.role == UserRole.SUPER_ADMIN) {
            equipmentRepository.getAllItems()
        } else {
            equipmentRepository.getItemsByDepartment(currentUser?.departmentId ?: "")
        }
    ) { query, categoryId, filterDeptId, items ->
        var filteredItems = items

        // 超级管理员按部门筛选
        if (currentUser?.role == UserRole.SUPER_ADMIN && filterDeptId != null) {
            filteredItems = filteredItems.filter { it.departmentId == filterDeptId }
        }
        
        // 按分类筛选
        if (categoryId != null) {
            filteredItems = filteredItems.filter { it.categoryId == categoryId }
        }
        
        // 按搜索关键词筛选
        if (query.isNotBlank()) {
            filteredItems = filteredItems.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            }
        }
        
        filteredItems
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val serverUrl: StateFlow<String> = settingsRepository.serverUrlFlow
        .map { url ->
             if (url.isNullOrEmpty()) "http://10.0.2.2:3000/" else url
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://10.0.2.2:3000/")

    val categories: StateFlow<List<Category>> = equipmentRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        syncData()
        // 监听设置变化（本地调试/服务器地址），自动触发设备数据重同步
        viewModelScope.launch {
            settingsRepository.localDebugFlow
                .combine(settingsRepository.serverUrlFlow) { local, url -> Pair(local, url) }
                .distinctUntilChanged()
                .collect {
                    syncData()
                }
        }
        
        // 初始加载部门数据
        if (currentUser?.role == UserRole.SUPER_ADMIN) {
            syncDepartments()
        }
    }

    fun filterByDepartment(departmentId: String?) {
        _filterDepartmentId.value = departmentId
        syncData()
    }

    private fun syncDepartments() {
        viewModelScope.launch {
            departmentRepository.syncDepartments().collect {
                // handle result if needed
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }
    
    fun refreshData() {
        _isRefreshing.value = true
        syncData()
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                if (!_isRefreshing.value) {
                    _uiState.update { it.copy(isLoading = true) }
                }
                
                // Get current user to ensure we have the latest context
                val user = authRepository.getCurrentUser()
                
                // Use coroutineScope to run syncs in parallel
                kotlinx.coroutines.coroutineScope {
                    // Sync categories
                    launch {
                        equipmentRepository.syncCategories().collect { result ->
                            if (result is NetworkResult.Error) {
                                _uiState.update { 
                                    it.copy(errorMessage = "同步类别失败: ${result.message}") 
                                }
                            }
                        }
                    }
                    
                    // Sync items
                    launch {
                        if (user != null) {
                            val departmentId = if (user.role == UserRole.SUPER_ADMIN) _filterDepartmentId.value else user.departmentId
                            equipmentRepository.syncItems(user.role, departmentId).collect { result ->
                                if (result is NetworkResult.Error) {
                                    _uiState.update { 
                                        it.copy(errorMessage = "同步物资失败: ${result.message}") 
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Both finished successfully (or with handled errors)
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = null) 
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "同步数据出错: ${e.message}"
                    ) 
                }
            } finally {
                _isRefreshing.value = false
                if (_uiState.value.isLoading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    fun createItem(item: EquipmentItem) {
        viewModelScope.launch {
            val newItem = item.copy(id = UUID.randomUUID().toString())
            equipmentRepository.createItem(newItem).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showAddDialog = false,
                            successMessage = "物资添加成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "添加物资失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun createCategory(category: Category) {
        viewModelScope.launch {
            equipmentRepository.createCategory(category).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showAddCategoryDialog = false,
                            successMessage = "类别添加成功"
                        )
                        // Refresh categories
                        equipmentRepository.syncCategories().collect()
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "添加类别失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            equipmentRepository.deleteCategory(categoryId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "类别删除成功"
                        )
                        // Refresh categories
                        equipmentRepository.syncCategories().collect()
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "删除类别失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    private suspend fun uploadImage(context: Context, uri: Uri, type: String = "item"): String? {
        return try {
            val file = getFileFromUri(context, uri)
            if (file != null) {
                // Check file size
                val fileSize = file.length()
                val maxFileSize = 10 * 1024 * 1024 // 10MB
                val compressThreshold = 1 * 1024 * 1024 // 1MB
                
                if (fileSize > maxFileSize) {
                    // Fail immediately if > 10MB and we don't have compression logic for extremely large files
                    // Or we could try to compress it heavily. For now, just reject per requirement?
                    // "所选择的单张照片不得大于10MB" -> Reject.
                    // "小于1MB则直接上传，1~10MB的图片则统一压缩至1MB以下" -> Compress.
                    // So if > 10MB, we should probably reject.
                     _uiState.value = _uiState.value.copy(errorMessage = "图片大小超过10MB限制")
                     return null
                }
                
                var fileToUpload = file
                if (fileSize > compressThreshold) {
                    // Compress to < 1MB
                    val compressedFile = compressImage(context, file)
                    if (compressedFile != null) {
                         fileToUpload = compressedFile
                    } else {
                         _uiState.value = _uiState.value.copy(errorMessage = "图片压缩失败")
                         return null
                    }
                }
                
                val result = equipmentRepository.uploadImage(fileToUpload, type)
                if (result is NetworkResult.Success) {
                    result.data
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "图片上传失败: ${result.message}")
                    null
                }
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "无法读取图片文件")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(errorMessage = "图片处理出错: ${e.message}")
            null
        }
    }

    private fun compressImage(context: Context, file: File): File? {
        try {
             val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
             var quality = 90
             val stream = java.io.ByteArrayOutputStream()
             bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
             
             while (stream.toByteArray().size > 1 * 1024 * 1024 && quality > 10) {
                 stream.reset()
                 quality -= 10
                 bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
             }
             
             val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
             val outputStream = FileOutputStream(compressedFile)
             outputStream.write(stream.toByteArray())
             outputStream.close()
             return compressedFile
        } catch (e: Exception) {
             e.printStackTrace()
             return null
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    fun createItem(context: Context, item: EquipmentItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var itemToSave = item
            val imageUri = item.image
            
            if (!imageUri.isNullOrEmpty() && (imageUri.startsWith("content://") || imageUri.startsWith("file://"))) {
                 val uploadedUrl = uploadImage(context, Uri.parse(imageUri), "item")
                 if (uploadedUrl != null) {
                     itemToSave = item.copy(image = uploadedUrl)
                 } else {
                     // Error message already set in uploadImage
                     _uiState.value = _uiState.value.copy(isLoading = false)
                     return@launch
                 }
            }

            equipmentRepository.createItem(itemToSave).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showAddDialog = false,
                            successMessage = "物资添加成功",
                            isLoading = false
                        )
                        refreshData()
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "添加物资失败: ${result.message}",
                            isLoading = false
                        )
                    }
                    is NetworkResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }
    
    fun updateItem(context: Context, item: EquipmentItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var itemToSave = item
            val imageUri = item.image
            
            if (!imageUri.isNullOrEmpty() && (imageUri.startsWith("content://") || imageUri.startsWith("file://"))) {
                 val uploadedUrl = uploadImage(context, Uri.parse(imageUri), "item")
                 if (uploadedUrl != null) {
                     itemToSave = item.copy(image = uploadedUrl)
                 } else {
                     // Error message already set in uploadImage
                     _uiState.value = _uiState.value.copy(isLoading = false)
                     return@launch
                 }
            }

            equipmentRepository.updateItem(itemToSave).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showEditDialog = false,
                            selectedItem = null,
                            successMessage = "物资更新成功",
                            isLoading = false
                        )
                        refreshData()
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "更新物资失败: ${result.message}",
                            isLoading = false
                        )
                    }
                    is NetworkResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }
    
    fun deleteItem(item: EquipmentItem) {
        viewModelScope.launch {
            equipmentRepository.deleteItem(item.id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showDeleteDialog = false,
                            selectedItem = null,
                            successMessage = "物资删除成功"
                        )
                        refreshData()
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "删除物资失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }
    
    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }
    
    fun showAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(showAddCategoryDialog = true)
    }
    
    fun hideAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(showAddCategoryDialog = false)
    }
    
    fun showEditDialog(item: EquipmentItem) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedItem = item
        )
    }
    
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedItem = null
        )
    }
    
    fun showDeleteDialog(item: EquipmentItem) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            selectedItem = item
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedItem = null
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun borrowItem(itemId: String, borrowRequest: BorrowRequest) {
        viewModelScope.launch {
            borrowRepository.borrowItem(itemId, borrowRequest).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showBorrowDialog = false,
                            selectedItem = null,
                            successMessage = "物资借用成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "借用失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun showBorrowDialog(item: EquipmentItem) {
        _uiState.value = _uiState.value.copy(
            showBorrowDialog = true,
            selectedItem = item
        )
    }
    
    fun hideBorrowDialog() {
        _uiState.value = _uiState.value.copy(
            showBorrowDialog = false,
            selectedItem = null
        )
    }
    
    fun canManageItems(): Boolean {
        return PermissionChecker.hasPermission(currentUser, PermissionType.MANAGE_EQUIPMENT_ITEMS)
    }
    
    fun getCurrentUser(): com.equiptrack.android.data.model.User? {
        return currentUser
    }
}

data class EquipmentUiState(
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showBorrowDialog: Boolean = false,
    val showReturnDialog: Boolean = false,
    val selectedItem: EquipmentItem? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)