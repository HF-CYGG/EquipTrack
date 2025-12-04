package com.equiptrack.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.SignupRequest
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.DepartmentRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val departmentRepository: DepartmentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()
    
    val departments = departmentRepository.getAllDepartments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _signupResult = MutableSharedFlow<NetworkResult<String>>()
    val signupResult: SharedFlow<NetworkResult<String>> = _signupResult.asSharedFlow()
    
    init {
        refreshDepartments()
    }

    private fun refreshDepartments() {
        viewModelScope.launch {
            departmentRepository.syncDepartments().collect()
        }
    }
    
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }
    
    fun updateContact(contact: String) {
        _uiState.value = _uiState.value.copy(contact = contact)
    }
    
    fun updateDepartmentName(departmentName: String) {
        _uiState.value = _uiState.value.copy(departmentName = departmentName)
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }
    
    fun updateInvitationCode(invitationCode: String) {
        _uiState.value = _uiState.value.copy(invitationCode = invitationCode)
    }
    
    fun signup() {
        val currentState = _uiState.value
        
        // Validate form
        val errors = mutableMapOf<String, String>()
        
        if (currentState.name.isBlank()) {
            errors["name"] = "请输入姓名"
        }
        
        if (currentState.contact.isBlank()) {
            errors["contact"] = "请输入联系方式"
        }
        
        if (currentState.departmentName.isBlank()) {
            errors["departmentName"] = "请输入部门名称"
        }
        
        if (currentState.password.isBlank()) {
            errors["password"] = "请输入密码"
        } else if (currentState.password.length < 6) {
            errors["password"] = "密码长度至少6位"
        }
        
        if (currentState.confirmPassword.isBlank()) {
            errors["confirmPassword"] = "请确认密码"
        } else if (currentState.password != currentState.confirmPassword) {
            errors["confirmPassword"] = "两次输入的密码不一致"
        }
        
        if (currentState.invitationCode.isBlank()) {
            errors["invitationCode"] = "请输入邀请码"
        }
        
        if (errors.isNotEmpty()) {
            _uiState.value = currentState.copy(
                nameError = errors["name"],
                contactError = errors["contact"],
                departmentNameError = errors["departmentName"],
                passwordError = errors["password"],
                confirmPasswordError = errors["confirmPassword"],
                invitationCodeError = errors["invitationCode"]
            )
            return
        }
        
        // Clear errors and set loading
        _uiState.value = currentState.copy(
            nameError = null,
            contactError = null,
            departmentNameError = null,
            passwordError = null,
            confirmPasswordError = null,
            invitationCodeError = null,
            isLoading = true
        )
        
        val request = SignupRequest(
            name = currentState.name,
            contact = currentState.contact,
            departmentName = currentState.departmentName,
            password = currentState.password,
            invitationCode = currentState.invitationCode
        )
        
        viewModelScope.launch {
            authRepository.signup(request)
                .collect { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _signupResult.emit(result)
                }
        }
    }
    
    fun clearErrors() {
        _uiState.value = _uiState.value.copy(
            nameError = null,
            contactError = null,
            departmentNameError = null,
            passwordError = null,
            confirmPasswordError = null,
            invitationCodeError = null
        )
    }
}

data class SignupUiState(
    val name: String = "",
    val contact: String = "",
    val departmentName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val invitationCode: String = "",
    val nameError: String? = null,
    val contactError: String? = null,
    val departmentNameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val invitationCodeError: String? = null,
    val isLoading: Boolean = false
)