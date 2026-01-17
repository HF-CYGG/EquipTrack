package com.equiptrack.android.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.UserRepository
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _avatarUpdateMessage = MutableStateFlow<String?>(null)
    val avatarUpdateMessage: StateFlow<String?> = _avatarUpdateMessage.asStateFlow()
    
    private val _passwordUpdateMessage = MutableStateFlow<String?>(null)
    val passwordUpdateMessage: StateFlow<String?> = _passwordUpdateMessage.asStateFlow()

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentUser = MutableStateFlow(authRepository.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun updateAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 1. Save to local storage
                val file = saveAvatarLocally(context, uri)
                if (file == null) {
                    _avatarUpdateMessage.value = "无法保存头像到本地"
                    _isRefreshing.value = false
                    return@launch
                }

                // 2. Update local user avatar (Local only)
                val localAvatarUrl = android.net.Uri.fromFile(file).toString()
                authRepository.updateLocalUserAvatar(localAvatarUrl)
                _currentUser.value = authRepository.getCurrentUser()
                _avatarUpdateMessage.value = "头像已更新（仅本地生效）"

                /* 预留接口：上传到服务器
                // 3. Upload to server
                userRepository.uploadAvatar(file).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val avatarUrl = result.data
                            if (avatarUrl != null) {
                                // 3. Update user profile with new URL
                                val currentUser = getCurrentUser()
                                if (currentUser != null) {
                                    val updatedUser = currentUser.copy(avatarUrl = avatarUrl)
                                    userRepository.updateUser(updatedUser).collect { updateResult ->
                                        if (updateResult is NetworkResult.Success) {
                                            _avatarUpdateMessage.value = "头像更新成功"
                                            refreshProfile()
                                        } else if (updateResult is NetworkResult.Error) {
                                            _avatarUpdateMessage.value = "头像更新失败: ${updateResult.message}"
                                        }
                                        // Handle Loading if needed, or ignore
                                    }
                                }
                            }
                        }
                        is NetworkResult.Error -> {
                            _avatarUpdateMessage.value = "头像上传失败: ${result.message}"
                        }
                        is NetworkResult.Loading -> {
                            // Loading state
                        }
                    }
                }
                */
            } catch (e: Exception) {
                _avatarUpdateMessage.value = "头像更新异常: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun saveAvatarLocally(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) avatarsDir.mkdirs()
            
            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            val file = File(avatarsDir, fileName)
            
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCurrentUser(): User? = authRepository.getCurrentUser()
    
    fun refreshProfile(isUserRefresh: Boolean = false) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                if (isUserRefresh) {
                    _isRefreshing.value = true
                }
                authRepository.refreshUserProfile(user.id).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            if (isUserRefresh) {
                                _refreshMessage.value = "刷新成功"
                            }
                            _currentUser.value = result.data
                        }
                        is NetworkResult.Error -> {
                            // Optionally handle error message
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                // Log error
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    fun clearAvatarMessage() {
        _avatarUpdateMessage.value = null
    }
    
    fun clearPasswordMessage() {
        _passwordUpdateMessage.value = null
    }
    
    fun updatePassword(userId: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                // 验证当前密码
                // 注意：服务器不返回密码，因此本地currentUser.password通常为空。
                // 暂时移除本地旧密码校验，以允许修改密码。
                // 理想情况下应在服务器端验证旧密码。
                /*
                val currentUser = authRepository.getCurrentUser()
                if (currentUser?.password != oldPassword) {
                    _passwordUpdateMessage.value = "当前密码错误"
                    return@launch
                }
                */
                
                userRepository.updateUserPassword(userId, newPassword).collect { result ->
                    _passwordUpdateMessage.value = when (result) {
                        is NetworkResult.Success -> "密码更新成功"
                        is NetworkResult.Error -> "密码更新失败: ${result.message}"
                        is NetworkResult.Loading -> null
                    }
                }
            } catch (e: Exception) {
                _passwordUpdateMessage.value = "密码更新失败: ${e.message}"
            }
        }
    }
}
