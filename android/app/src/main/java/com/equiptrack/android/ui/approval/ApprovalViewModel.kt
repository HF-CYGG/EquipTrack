package com.equiptrack.android.ui.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.repository.ApprovalRepository
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApprovalViewModel @Inject constructor(
    private val approvalRepository: ApprovalRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApprovalUiState())
    val uiState: StateFlow<ApprovalUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val currentUser = authRepository.getCurrentUser()
    
    // Get requests based on user role and search query
    val filteredRequests: StateFlow<List<RegistrationRequest>> = combine(
        _searchQuery,
        when (currentUser?.role) {
            UserRole.SUPER_ADMIN -> approvalRepository.getAllRequests()
            UserRole.ADMIN -> approvalRepository.getRequestsByDepartment(currentUser!!.departmentId)
            else -> flowOf(emptyList())
        }
    ) { query, requests ->
        if (query.isBlank()) {
            requests
        } else {
            requests.filter { request ->
                request.name.contains(query, ignoreCase = true) ||
                request.contact.contains(query, ignoreCase = true) ||
                request.departmentName?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        syncRequests()
        // 监听设置变化（本地调试/服务器地址），自动触发审批数据重同步
        viewModelScope.launch {
            settingsRepository.localDebugFlow
                .combine(settingsRepository.serverUrlFlow) { local, url -> Pair(local, url) }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    syncRequests()
                }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun syncRequests(isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    if (isRefresh) {
                        _uiState.value = _uiState.value.copy(isRefreshing = true)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    approvalRepository.syncRequests(
                        userId = user.id,
                        userRole = user.role,
                        departmentId = user.departmentId
                    ).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessage = null
                                )
                            }

                            is NetworkResult.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessage = "同步申请失败: ${result.message}"
                                )
                            }

                            is NetworkResult.Loading -> {
                                // Do nothing here as we set state above
                            }
                        }
                    }
                } else {
                    // User is null, maybe logged out?
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "同步申请发生错误: ${e.message}"
                )
            } finally {
                // Ensure states are reset in case of unexpected termination
                if (_uiState.value.isLoading || _uiState.value.isRefreshing) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }
    
    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            approvalRepository.approveRequest(requestId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showApproveDialog = false,
                            selectedRequest = null,
                            isActionLoading = false,
                            successMessage = "申请已批准，用户创建成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isActionLoading = false,
                            errorMessage = "批准申请失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isActionLoading = true)
                    }
                }
            }
        }
    }
    
    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            approvalRepository.rejectRequest(requestId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showRejectDialog = false,
                            selectedRequest = null,
                            isActionLoading = false,
                            successMessage = result.data ?: "申请已拒绝"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isActionLoading = false,
                            errorMessage = "拒绝申请失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isActionLoading = true)
                    }
                }
            }
        }
    }
    
    fun showApproveDialog(request: RegistrationRequest) {
        _uiState.value = _uiState.value.copy(
            showApproveDialog = true,
            selectedRequest = request
        )
    }
    
    fun hideApproveDialog() {
        _uiState.value = _uiState.value.copy(
            showApproveDialog = false,
            selectedRequest = null
        )
    }
    
    fun showRejectDialog(request: RegistrationRequest) {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = true,
            selectedRequest = request
        )
    }
    
    fun hideRejectDialog() {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = false,
            selectedRequest = null
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun canApproveRequests(): Boolean {
        // 仅超管和部门管理员可审批注册申请（部门范围）
        return PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_REGISTRATION_APPROVALS)
    }

    fun getAccessLevelDescription(): String {
        return when (currentUser?.role) {
            UserRole.SUPER_ADMIN -> "可查看和处理所有部门的注册申请"
            UserRole.ADMIN -> "可查看和处理本部门的注册申请"
            else -> "无权限查看注册申请"
        }
    }
}

data class ApprovalUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isActionLoading: Boolean = false,
    val showApproveDialog: Boolean = false,
    val showRejectDialog: Boolean = false,
    val selectedRequest: RegistrationRequest? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
