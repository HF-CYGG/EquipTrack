package com.equiptrack.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.BorrowRepository
import com.equiptrack.android.data.repository.DepartmentRepository
import com.equiptrack.android.data.repository.EquipmentRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import com.equiptrack.android.notifications.ApprovalNotificationHelper

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val borrowRepository: BorrowRepository,
    private val equipmentRepository: EquipmentRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val _filterStatus = MutableStateFlow<BorrowStatus?>(null)
    val filterStatus: StateFlow<BorrowStatus?> = _filterStatus.asStateFlow()

    private val _filterDepartmentId = MutableStateFlow<String?>(null)
    val filterDepartmentId: StateFlow<String?> = _filterDepartmentId.asStateFlow()

    private val _borrowRequests = MutableStateFlow<List<BorrowHistoryEntry>>(emptyList())
    // Keep track of previously pending requests to detect status changes
    private var _previousPendingRequests = setOf<String>()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val currentUser = authRepository.getCurrentUser()
    
    // Get history based on user role and filter
    val historyEntries: StateFlow<List<BorrowHistoryEntry>> = combine(
        when (currentUser?.role) {
            UserRole.SUPER_ADMIN -> borrowRepository.getAllHistory()
            UserRole.NORMAL_USER -> borrowRepository.getHistoryByBorrower(currentUser.contact)
            else -> borrowRepository.getHistoryByDepartment(currentUser?.departmentId ?: "")
        },
        _borrowRequests,
        _filterStatus,
        _filterDepartmentId
    ) { entries, requests, status, deptId ->
        // Merge entries and requests
        // Only show PENDING and REJECTED requests.
        // APPROVED requests are ignored here because they should appear in 'entries' as BORROWING/RETURNED.
        val validRequests = requests.filter { 
            it.status == BorrowStatus.PENDING || it.status == BorrowStatus.REJECTED 
        }
        
        var result = entries + validRequests
        // Sort by date descending
        result = result.sortedByDescending { it.borrowDate }

        if (status != null) {
            result = result.filter { it.status == status }
        }
        if (deptId != null && currentUser?.role == UserRole.SUPER_ADMIN) {
            result = result.filter { it.departmentId == deptId }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val departments: StateFlow<List<Department>> = departmentRepository.getAllDepartments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val activeBorrows: StateFlow<List<BorrowHistoryEntry>> = borrowRepository.getActiveBorrows()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val serverUrl: StateFlow<String> = settingsRepository.serverUrlFlow
        .map { url ->
             if (url.isNullOrEmpty()) "http://10.0.2.2:3000/" else url
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "http://10.0.2.2:3000/")
    
    init {
        syncHistory()
        syncDepartments()
        updateOverdueStatus()
        // 监听设置变化（本地调试/服务器地址），自动触发历史数据重同步
        viewModelScope.launch {
            settingsRepository.localDebugFlow
                .combine(settingsRepository.serverUrlFlow) { local, url -> Pair(local, url) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    syncHistory()
                    syncDepartments()
                }
        }
    }
    
    fun refreshHistory() {
        _isRefreshing.value = true
        syncHistory()
        syncDepartments()
    }
    
    fun syncHistory() {
        viewModelScope.launch {
            try {
                if (!_isRefreshing.value) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                val user = authRepository.getCurrentUser()
                if (user != null) {
                    val departmentId = if (user.role == UserRole.SUPER_ADMIN || user.role == UserRole.ADMIN) _filterDepartmentId.value else user.departmentId

                    // 同时同步部门信息，以便显示部门名称
                    if (user.role == UserRole.SUPER_ADMIN || user.role == UserRole.ADMIN) {
                        departmentRepository.syncDepartments().collect()
                    }

                    // 获取我的申请记录 (Pending/Rejected/Approved)
                    launch {
                        borrowRepository.getMyRequests().collect { result ->
                            if (result is NetworkResult.Success) {
                                val requests = result.data ?: emptyList()
                                _borrowRequests.value = requests
                                
                                // Check for status changes (Pending -> Approved)
                                val currentApprovedRequests = requests.filter { it.status == BorrowStatus.APPROVED }
                                for (req in currentApprovedRequests) {
                                    if (_previousPendingRequests.contains(req.id)) {
                                        // Status changed from Pending to Approved
                                        ApprovalNotificationHelper.showBorrowApprovedNotification(context, req.itemName)
                                    }
                                }
                                
                                // Update previous pending requests set
                                _previousPendingRequests = requests
                                    .filter { it.status == BorrowStatus.PENDING }
                                    .map { it.id }
                                    .toSet()
                            }
                        }
                    }

                    borrowRepository.syncHistory(user.role, departmentId).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }

                            is NetworkResult.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "同步历史记录失败: ${result.message}"
                                )
                            }

                            is NetworkResult.Loading -> {
                                if (!_isRefreshing.value) {
                                    _uiState.value = _uiState.value.copy(isLoading = true)
                                }
                            }
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "用户未登录"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "同步历史记录发生错误: ${e.message}"
                )
            } finally {
                _isRefreshing.value = false
                // Ensure loading is false in case of unexpected termination
                if (_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    private fun syncDepartments() {
        viewModelScope.launch {
            departmentRepository.syncDepartments().collect { }
        }
    }
    
    fun updateOverdueStatus() {
        viewModelScope.launch {
            borrowRepository.updateOverdueStatus()
        }
    }
    
    fun filterByStatus(status: BorrowStatus?) {
        _filterStatus.value = status
    }

    fun filterByDepartment(departmentId: String?) {
        _filterDepartmentId.value = departmentId
        syncHistory()
    }
    
    fun returnItem(context: Context, itemId: String, historyEntryId: String, returnRequest: ReturnRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            var requestToSend = returnRequest
            val photoUri = returnRequest.photo
            
            if (photoUri.isNotEmpty() && (photoUri.startsWith("content://") || photoUri.startsWith("file://"))) {
                 val uploadedUrl = uploadImage(context, Uri.parse(photoUri), "history")
                 if (uploadedUrl != null) {
                     requestToSend = returnRequest.copy(photo = uploadedUrl)
                 } else {
                     _uiState.value = _uiState.value.copy(
                         errorMessage = "图片上传失败",
                         isLoading = false
                     )
                     return@launch
                 }
            }

            borrowRepository.returnItem(itemId, historyEntryId, requestToSend).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showReturnDialog = false,
                            selectedHistoryEntry = null,
                            successMessage = "物资归还成功",
                            isLoading = false
                        )
                        syncHistory() // Refresh list to show updated status
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "归还失败: ${result.message}",
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
    
    private suspend fun uploadImage(context: Context, uri: Uri, type: String = "history"): String? {
        return try {
            val file = getFileFromUri(context, uri)
            if (file != null) {
                // Check file size
                val fileSize = file.length()
                val maxFileSize = 10 * 1024 * 1024 // 10MB
                val compressThreshold = 1 * 1024 * 1024 // 1MB
                
                if (fileSize > maxFileSize) {
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
    
    fun showReturnDialog(historyEntry: BorrowHistoryEntry, isForced: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            showReturnDialog = true,
            selectedHistoryEntry = historyEntry,
            isForceReturn = isForced
        )
    }
    
    fun hideReturnDialog() {
        _uiState.value = _uiState.value.copy(
            showReturnDialog = false,
            selectedHistoryEntry = null,
            isForceReturn = false
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun canForceReturn(): Boolean {
        return currentUser?.role in listOf(
            UserRole.SUPER_ADMIN,
            UserRole.ADMIN,
            UserRole.ADVANCED_USER
        )
    }
    
    fun getCurrentUser(): User? {
        return currentUser
    }

    fun getCurrentUserName(): String {
        return currentUser?.name ?: "未知用户"
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val showReturnDialog: Boolean = false,
    val selectedHistoryEntry: BorrowHistoryEntry? = null,
    val isForceReturn: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
