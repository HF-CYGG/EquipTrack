package com.equiptrack.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    private val _loginResult = MutableSharedFlow<NetworkResult<User>>()
    val loginResult: SharedFlow<NetworkResult<User>> = _loginResult.asSharedFlow()

    private var loginJob: Job? = null
    
    fun updateContact(contact: String) {
        _uiState.value = _uiState.value.copy(contact = contact)
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    fun login() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        
        if (currentState.contact.isBlank()) {
            _uiState.value = currentState.copy(contactError = "请输入联系方式")
            return
        }
        
        if (currentState.password.isBlank()) {
            _uiState.value = currentState.copy(passwordError = "请输入密码")
            return
        }

        // Check server configuration
        val isLocalDebug = settingsRepository.isLocalDebug()
        val serverUrl = settingsRepository.getServerUrl()
        
        if (!isLocalDebug && serverUrl.isNullOrBlank()) {
            _uiState.value = currentState.copy(
                showServerConfigPrompt = true,
                errorMessage = "请先配置服务器地址"
            )
            return
        }
        
        // Clear errors
        _uiState.value = currentState.copy(
            contactError = null,
            passwordError = null,
            errorMessage = null,
            showServerConfigPrompt = false,
            isLoading = true
        )
        
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            authRepository.login(currentState.contact.trim(), currentState.password.trim())
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                            _loginResult.emit(result)
                        }
                        is NetworkResult.Error -> {
                            var msg = result.message ?: "登录失败"
                            
                            // 修正 Login 接口返回 401 时的误导性提示
                            if (msg == "登录已过期，请重新登录") {
                                msg = "账号或密码错误"
                            }
                            
                            val isConnectionError = msg.contains("无法连接") || 
                                                  msg.contains("连接超时") || 
                                                  msg.contains("无法解析")
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false, 
                                errorMessage = msg,
                                showServerConfigPrompt = isConnectionError
                            )
                        }
                        is NetworkResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                    }
                }
        }
    }
    
    fun dismissServerConfigPrompt() {
        _uiState.value = _uiState.value.copy(showServerConfigPrompt = false)
    }
    
    fun clearErrors() {
        _uiState.value = _uiState.value.copy(
            contactError = null,
            passwordError = null
        )
    }
}

data class LoginUiState(
    val contact: String = "",
    val password: String = "",
    val contactError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val showServerConfigPrompt: Boolean = false,
    val isLoading: Boolean = false
)
