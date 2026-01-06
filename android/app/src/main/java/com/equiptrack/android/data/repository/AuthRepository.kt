package com.equiptrack.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.equiptrack.android.data.local.dao.UserDao
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.utils.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("equiptrack_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_CONTACT = "user_contact"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_DEPARTMENT_ID = "user_department_id"
        private const val KEY_USER_DEPARTMENT_NAME = "user_department_name"
        private const val KEY_USER_INVITATION_CODE = "user_invitation_code"
        private const val KEY_USER_AVATAR_URL = "user_avatar_url"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val TAG = "AuthRepository"
    }
    
    suspend fun registerDeviceToken(token: String) {
        try {
            val response = apiService.registerDeviceToken(mapOf("token" to token, "platform" to "android"))
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "FCM Token registered successfully")
            } else {
                Log.e(TAG, "Failed to register FCM token: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token", e)
        }
    }

    fun saveFCMToken(token: String) {
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFCMToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    suspend fun login(contact: String, password: String): Flow<NetworkResult<User>> = flow {
        // Local Debug Mode Logic
        if (settingsRepository.isLocalDebug()) {
            emit(NetworkResult.Loading())
            // Simulate network delay
            kotlinx.coroutines.delay(500)
            
            // Authenticate against local database
            val localUser = userDao.authenticateUser(contact, password)
            if (localUser != null) {
                // In local mode, admin is "free to use" (no extra checks needed beyond seeding)
                // Other users password checked by DAO (seeded as 020414)
                saveUserSession(localUser)
                emit(NetworkResult.Success(localUser))
            } else {
                emit(NetworkResult.Error("用户名或密码错误"))
            }
            return@flow
        }

        emit(NetworkResult.Loading())
        val remoteResult = try {
            safeApiCall { apiService.login(LoginRequest(contact, password)) }
        } catch (e: Exception) {
            NetworkResult.Error("网络错误: ${e.message}")
        }

        when (remoteResult) {
            is NetworkResult.Success -> {
                val user = remoteResult.data?.user
                val token = remoteResult.data?.token
                Log.d(TAG, "Login success. User: ${user?.name}, Token present: ${token != null}")
                if (user != null) {
                    userDao.insertUser(user)
                    saveUserSession(user, token)
                    emit(NetworkResult.Success(user))
                } else {
                    emit(NetworkResult.Error("登录失败：用户信息无效"))
                }
            }
            is NetworkResult.Error -> {
                val message = remoteResult.message ?: "登录失败"
                val canFallbackToLocal = message.contains("无法连接") ||
                    message.contains("连接超时") ||
                    message.contains("无法解析") ||
                    message.startsWith("网络错误")

                if (canFallbackToLocal) {
                    val localUser = userDao.authenticateUser(contact, password)
                    if (localUser != null) {
                        saveUserSession(localUser)
                        emit(NetworkResult.Success(localUser))
                    } else {
                        emit(NetworkResult.Error(message))
                    }
                } else {
                    emit(NetworkResult.Error(message))
                }
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun signup(request: SignupRequest): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        val result = safeApiCall {
            apiService.signup(request)
        }
        
        when (result) {
            is NetworkResult.Success -> {
                emit(NetworkResult.Success(result.data?.message ?: "注册申请已提交"))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "注册申请失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun backupRemoteSession() {
        if (!isLoggedIn()) return
        val user = getCurrentUser() ?: return
        val token = getAuthToken()
        
        settingsRepository.backupSession(
            token = token,
            userId = user.id,
            userName = user.name,
            userRole = user.role.displayName,
            userContact = user.contact,
            deptId = user.departmentId,
            deptName = user.departmentName
        )
    }

    fun restoreRemoteSession(): Boolean {
        val backup = settingsRepository.getBackupSession()
        val token = backup["token"]
        val userId = backup["userId"]
        
        if (token != null && userId != null) {
            sharedPreferences.edit().apply {
                putString(KEY_AUTH_TOKEN, token)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_NAME, backup["userName"])
                putString(KEY_USER_ROLE, backup["userRole"])
                putString(KEY_USER_CONTACT, backup["userContact"])
                putString(KEY_USER_DEPARTMENT_ID, backup["deptId"])
                putString(KEY_USER_DEPARTMENT_NAME, backup["deptName"])
                putBoolean(KEY_IS_LOGGED_IN, true)
                apply()
            }
            settingsRepository.clearBackupSession()
            return true
        }
        return false
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
    
    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        
        val userId = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        val userName = sharedPreferences.getString(KEY_USER_NAME, null) ?: return null
        val userContact = sharedPreferences.getString(KEY_USER_CONTACT, null) ?: return null
        val userRoleString = sharedPreferences.getString(KEY_USER_ROLE, null) ?: return null
        val userDepartmentId = sharedPreferences.getString(KEY_USER_DEPARTMENT_ID, null) ?: return null
        val userDepartmentName = sharedPreferences.getString(KEY_USER_DEPARTMENT_NAME, "") ?: ""
        val userInvitationCode = sharedPreferences.getString(KEY_USER_INVITATION_CODE, null)
        val userAvatarUrl = sharedPreferences.getString(KEY_USER_AVATAR_URL, null)
        
        val userRole = UserRole.values().find { it.displayName == userRoleString } ?: UserRole.NORMAL_USER
        
        return User(
            id = userId,
            name = userName,
            contact = userContact,
            departmentId = userDepartmentId,
            departmentName = userDepartmentName,
            role = userRole,
            status = UserStatus.NORMAL,
            invitationCode = userInvitationCode,
            avatarUrl = userAvatarUrl
        )
    }

    suspend fun refreshUserProfile(userId: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        val result = safeApiCall {
            apiService.getUserById(userId)
        }
        
        when (result) {
            is NetworkResult.Success -> {
                result.data?.let { user ->
                    saveUserSession(user, getAuthToken())
                    emit(NetworkResult.Success(user))
                } ?: emit(NetworkResult.Error("User data is null"))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "Failed to refresh profile"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    private fun saveUserSession(user: User, token: String? = null) {
        Log.d(TAG, "Saving user session. Token length: ${token?.length ?: 0}")
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_CONTACT, user.contact)
            putString(KEY_USER_ROLE, user.role.displayName)
            putString(KEY_USER_DEPARTMENT_ID, user.departmentId)
            putString(KEY_USER_DEPARTMENT_NAME, user.departmentName)
            putString(KEY_USER_INVITATION_CODE, user.invitationCode)
            putString(KEY_USER_AVATAR_URL, user.avatarUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            if (token != null) {
                putString(KEY_AUTH_TOKEN, token)
            }
            apply()
        }
    }

}
