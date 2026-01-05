package com.equiptrack.android.ui.department

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.DepartmentRepository
import com.equiptrack.android.data.repository.UserRepository
import com.equiptrack.android.data.repository.EquipmentRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.UrlUtils
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DepartmentViewModel @Inject constructor(
    private val departmentRepository: DepartmentRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val equipmentRepository: EquipmentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DepartmentUiState())
    val uiState: StateFlow<DepartmentUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()

    // 选中的部门（用于详情视图展示）
    private val _selectedDepartmentId = MutableStateFlow<String?>(null)
    val selectedDepartmentId: StateFlow<String?> = _selectedDepartmentId.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Combine search query with departments flow
    val filteredDepartments: StateFlow<List<Department>> = combine(
        _searchQuery.debounce(250),
        departmentRepository.getAllDepartments()
    ) { query, departments ->
        if (query.isBlank()) {
            departments
        } else {
            departments.filter { department ->
                department.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 部门成员（详情视图）
    val departmentUsers: StateFlow<List<User>> = selectedDepartmentId
        .flatMapLatest { deptId ->
            if (deptId.isNullOrBlank()) flowOf(emptyList()) else userRepository.getUsersByDepartment(deptId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 部门物资（详情视图）
    val departmentItems: StateFlow<List<EquipmentItem>> = selectedDepartmentId
        .flatMapLatest { deptId ->
            if (deptId.isNullOrBlank()) flowOf(emptyList()) else equipmentRepository.getItemsByDepartment(deptId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        syncDepartments()

        // 自动监听设置变化（本地调试/服务器地址），触发重同步
        viewModelScope.launch {
            settingsRepository.localDebugFlow
                .combine(settingsRepository.serverUrlFlow) { local, url -> Pair(local, url) }
                .distinctUntilChanged()
                .collect {
                    syncDepartments()
                }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshDepartments() {
        _isRefreshing.value = true
        UrlUtils.bumpRefreshEpoch()
        syncDepartments()
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun syncDepartments() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                if (!_isRefreshing.value) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                departmentRepository.syncDepartments().collect { result ->
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
                                errorMessage = "同步部门失败: ${result.message}"
                            )
                        }

                        is NetworkResult.Loading -> {
                            if (!_isRefreshing.value) {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "同步部门发生错误: ${e.message}"
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

    fun selectDepartment(deptId: String?) {
        _selectedDepartmentId.value = deptId
    }
    
    fun createDepartment(name: String, requiresApproval: Boolean) {
        viewModelScope.launch {
            // Check if name already exists
            val nameExists = departmentRepository.isDepartmentNameExists(name)
            if (nameExists) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "部门名称已存在"
                )
                return@launch
            }
            
            departmentRepository.createDepartment(name, requiresApproval).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showAddDialog = false,
                            successMessage = "部门创建成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "创建部门失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun updateDepartment(id: String, name: String, requiresApproval: Boolean) {
        viewModelScope.launch {
            // Check if name already exists (excluding current department)
            val nameExists = departmentRepository.isDepartmentNameExists(name, id)
            if (nameExists) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "部门名称已存在"
                )
                return@launch
            }

            // Fetch current department to preserve other fields if any
            val currentDepartment = departmentRepository.getDepartmentById(id)
            if (currentDepartment == null) {
                 _uiState.value = _uiState.value.copy(
                    errorMessage = "部门不存在"
                )
                return@launch
            }

            val updatedDepartment = currentDepartment.copy(name = name, requiresApproval = requiresApproval)
            
            departmentRepository.updateDepartment(updatedDepartment).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showEditDialog = false,
                            selectedDepartment = null,
                            successMessage = "部门更新成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "更新部门失败: ${result.message}"
                        )
                    }
                    is NetworkResult.Loading -> {
                        // Loading state can be handled if needed
                    }
                }
            }
        }
    }
    
    fun deleteDepartment(departmentId: String) {
        viewModelScope.launch {
            departmentRepository.deleteDepartment(departmentId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            showDeleteDialog = false,
                            selectedDepartment = null,
                            successMessage = "部门删除成功"
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "删除部门失败: ${result.message}"
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
    
    fun showEditDialog(department: Department) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedDepartment = department
        )
    }
    
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedDepartment = null
        )
    }
    
    fun showDeleteDialog(department: Department) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            selectedDepartment = department
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedDepartment = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun canManageDepartments(): Boolean {
        return PermissionChecker.hasPermission(currentUser, PermissionType.MANAGE_ALL_DEPARTMENTS)
    }

    fun canManageDepartment(deptId: String?): Boolean {
        return PermissionChecker.hasPermission(
            currentUser,
            PermissionType.VIEW_DEPARTMENT_MANAGEMENT,
            targetDepartmentId = deptId
        )
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            val user = userRepository.getUserById(userId)
            if (user != null) {
                val updated = user.copy(role = newRole)
                userRepository.updateUser(updated).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(successMessage = "角色更新成功")
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(errorMessage = "角色更新失败: ${result.message}")
                        }
                        is NetworkResult.Loading -> { /* ignore */ }
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "未找到用户")
            }
        }
    }
}

data class DepartmentUiState(
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val selectedDepartment: Department? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
