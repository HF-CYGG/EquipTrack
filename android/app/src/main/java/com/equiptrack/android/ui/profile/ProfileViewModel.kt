package com.equiptrack.android.ui.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import android.graphics.*
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun getCurrentUser(): User? = authRepository.getCurrentUser()
    
    fun refreshProfile() {
        val user = getCurrentUser() ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                authRepository.refreshUserProfile(user.id).collect { result ->
                    // Logic handled inside collect, but we need to ensure refreshing is turned off
                    if (result !is com.equiptrack.android.utils.NetworkResult.Loading) {
                        // Although it sets false here, we'll rely on finally block for safety
                    }
                }
            } catch (e: Exception) {
                // Log error or show message if needed, but for profile refresh we might just silent fail or rely on toast
            } finally {
                _isRefreshing.value = false
            }
        }
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
                val currentUser = authRepository.getCurrentUser()
                if (currentUser?.password != oldPassword) {
                    _passwordUpdateMessage.value = "当前密码错误"
                    return@launch
                }
                
                userRepository.updateUserPassword(userId, newPassword).collect { result ->
                    _passwordUpdateMessage.value = when (result) {
                        is com.equiptrack.android.utils.NetworkResult.Success -> "密码更新成功"
                        is com.equiptrack.android.utils.NetworkResult.Error -> "密码更新失败: ${result.message}"
                        is com.equiptrack.android.utils.NetworkResult.Loading -> null
                    }
                }
            } catch (e: Exception) {
                _passwordUpdateMessage.value = "密码更新失败: ${e.message}"
            }
        }
    }

    fun updateAvatar(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 读取原图
                val inputStream = contentResolver.openInputStream(imageUri) ?: throw IllegalArgumentException("无法读取图片")
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                // 裁剪为正方形
                val size = minOf(original.width, original.height)
                val x = (original.width - size) / 2
                val y = (original.height - size) / 2
                val square = Bitmap.createBitmap(original, x, y, size, size)

                // 生成圆形头像
                val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                paint.shader = shader
                val radius = size / 2f
                canvas.drawCircle(radius, radius, radius, paint)

                // 压缩到 <= 3MB（逐步调整质量）
                var quality = 95
                var data: ByteArray
                do {
                    val bos = ByteArrayOutputStream()
                    output.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                    data = bos.toByteArray()
                    bos.close()
                    quality -= 5
                } while (data.size > 3 * 1024 * 1024 && quality > 40)

                val base64 = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
                val dataUri = "data:image/jpeg;base64,$base64"

                val current = authRepository.getCurrentUser() ?: throw IllegalStateException("未登录用户")
                val updated = current.copy(avatarUrl = dataUri)

                userRepository.updateUser(updated).collect { result ->
                    // 简化处理：仅提示成功或失败
                    _avatarUpdateMessage.value = when (result) {
                        is com.equiptrack.android.utils.NetworkResult.Success -> "头像已更新"
                        is com.equiptrack.android.utils.NetworkResult.Error -> "头像更新失败: ${result.message}"
                        is com.equiptrack.android.utils.NetworkResult.Loading -> null
                    }
                }
            } catch (e: Exception) {
                _avatarUpdateMessage.value = "处理图片失败: ${e.message}"
            }
        }
    }
}