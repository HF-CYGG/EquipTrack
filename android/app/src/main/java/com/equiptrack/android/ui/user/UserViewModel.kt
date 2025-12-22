package com.equiptrack.android.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.data.repository.DepartmentRepository
import com.equiptrack.android.data.repository.UserRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filterRole = MutableStateFlow<UserRole?>(null)
    val filterRole: StateFlow<UserRole?> = _filterRole.asStateFlow()
    
    private val _filterStatus = MutableStateFlow<UserStatus?>(null)
    val filterStatus: StateFlow<UserStatus?> = _filterStatus.asStateFlow()
    
    private val _filterDepartmentId = MutableStateFlow<String?>(null)
    val filterDepartmentId: StateFlow<String?> = _filterDepartmentId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()
    
    // Get users based on current user role and filters
    val filteredUsers: StateFlow<List<User>> = combine(
        _searchQuery,
        _filterRole,
        _filterStatus,
        _filterDepartmentId,
        if (currentUser?.role == UserRole.SUPER_ADMIN) {
            userRepository.getAllUsers()
        } else {
            userRepository.getUsersByDepartment(currentUser?.departmentId ?: "")
        }
    ) { query, roleFilter, statusFilter, deptId, users ->
        var result = users
        
        // Apply search filter
        if (query.isNotBlank()) {
            result = result.filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                user.contact.contains(query, ignoreCase = true) ||
                user.departmentName?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Apply role filter
        if (roleFilter != null) {
            result = result.filter { it.role == roleFilter }
        }
        
        // Apply status filter
        if (statusFilter != null) {
            result = result.filter { it.status == statusFilter }
        }

        // Apply department filter
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
    
    init {
        syncUsers()
        syncDepartments()

        viewModelScope.launch {
            settingsRepository.localDebugFlow
                .combine(settingsRepository.serverUrlFlow) { local, url -> Pair(local, url) }
                .distinctUntilChanged()
                .collect {
                    syncUsers()
                    syncDepartments()
                }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun filterByRole(role: UserRole?) {
        _filterRole.value = role
    }
    
    fun filterByStatus(status: UserStatus?) {
        _filterStatus.value = status
    }
    
    fun filterByDepartment(departmentId: String?) {
        _filterDepartmentId.value = departmentId
        syncUsers()
    }

    fun refreshUsers() {
        _isRefreshing.value = true
        syncUsers()
    }

    fun syncUsers() {
        viewModelScope.launch {
            try {
                if (!_isRefreshing.value) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                val user = authRepository.getCurrentUser()
                if (user != null) {
                    val departmentId = if (user.role == UserRole.SUPER_ADMIN) _filterDepartmentId.value else user.departmentId
                    userRepository.syncUsers(user.role, departmentId).collect { result ->
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
                                    errorMessage = "同步用户失败: ${result.message}"
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
                    errorMessage = "同步用户发生错误: ${e.message}"
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
            departmentRepository.syncDepartments().collect { /* Handle if needed */ }
        }
    }
    
    fun createUser(user: User) {
        viewModelScope.launch {
            // Check if contact already exists
            val contactExists = userRepository.isContactExists(user.contact)
            if (contactExists) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "该联系方式已被注册"
                )
                return@launch
            }
            
            // Generate invitation code if user can invite others and no code is provided
            val userWithCode = if (user.role in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.ADVANCED_USER)) {
                if (user.invitationCode.isNullOrBlank()) {
                    val invitationCode = userRepository.generateInvitationCode()
                    user.copy(invitationCode = invitationCode)
                } else {
                    user
                }
            } else {
                user
            }
            
            userRepository.createUser(userWithCode).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showAddDialog = false,
                            successMessage = "用户创建成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "创建用户失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun updateUser(user: User) {
        viewModelScope.launch {
            // Check if contact already exists (excluding current user)
            val contactExists = userRepository.isContactExists(user.contact, user.id)
            if (contactExists) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "该联系方式已被其他用户使用"
                )
                return@launch
            }
            
            userRepository.updateUser(user).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showEditDialog = false,
                            selectedUser = null,
                            successMessage = "用户更新成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "更新用户失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun updateUserStatus(userId: String, status: UserStatus) {
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, status).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = result.data ?: "状态更新成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message ?: "状态更新失败"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun resetUserPassword(userId: String, newPassword: String) {
        viewModelScope.launch {
            userRepository.updateUserPassword(userId, newPassword).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showPasswordDialog = false,
                            selectedUser = null,
                            successMessage = result.data ?: "密码重置成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message ?: "密码重置失败"
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
    
    fun showEditDialog(user: User) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedUser = user
        )
    }
    
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedUser = null
        )
    }
    
    fun showPasswordDialog(user: User) {
        _uiState.value = _uiState.value.copy(
            showPasswordDialog = true,
            selectedUser = user
        )
    }
    
    fun hidePasswordDialog() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialog = false,
            selectedUser = null
        )
    }

    fun showDeleteDialog(user: User) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            selectedUser = user
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedUser = null
        )
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            userRepository.deleteUser(userId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showDeleteDialog = false,
                            selectedUser = null,
                            successMessage = result.data ?: "用户删除成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message ?: "删除用户失败"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun canManageUsers(): Boolean {
        // 部门范围内的用户管理权限（管理员/超管在同部门下具备）
        return PermissionChecker.hasPermission(currentUser, PermissionType.VIEW_USER_MANAGEMENT)
    }
    
    fun canManageAllUsers(): Boolean {
        // 仅超管具备全局用户管理权限
        return PermissionChecker.hasPermission(currentUser, PermissionType.MANAGE_ALL_DEPARTMENTS)
    }
    
    fun getCurrentUser(): User? {
        return currentUser
    }
}

data class UserUiState(
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val selectedUser: User? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
