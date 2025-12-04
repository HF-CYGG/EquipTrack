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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val TAG = "AuthRepository"
    }
    
    suspend fun login(contact: String, password: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        // In local debug mode, prefer local authentication first for speed
        if (settingsRepository.isLocalDebug()) {
            val localUserFast = userDao.authenticateUser(contact, password)
            if (localUserFast != null) {
                saveUserSession(localUserFast)
                emit(NetworkResult.Success(localUserFast))
                return@flow
            }
        }
        try {
            // Try remote login first
            val result = safeApiCall {
                apiService.login(LoginRequest(contact, password))
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val user = result.data?.user
                    val token = result.data?.token
                    Log.d(TAG, "Login success. User: ${user?.name}, Token present: ${token != null}")
                    if (user != null) {
                        // Save user to local database
                        userDao.insertUser(user)
                        // Save login state
                        saveUserSession(user, token)
                        emit(NetworkResult.Success(user))
                    } else {
                        emit(NetworkResult.Error("登录失败：用户信息无效"))
                    }
                }
                is NetworkResult.Error -> {
                    // Try local authentication as fallback
                    val localUser = userDao.authenticateUser(contact, password)
                    if (localUser != null) {
                        saveUserSession(localUser)
                        emit(NetworkResult.Success(localUser))
                    } else {
                        emit(NetworkResult.Error(result.message ?: "登录失败"))
                    }
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local authentication
            val localUser = userDao.authenticateUser(contact, password)
            if (localUser != null) {
                saveUserSession(localUser)
                emit(NetworkResult.Success(localUser))
            } else {
                emit(NetworkResult.Error("登录失败：${e.message}"))
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
    
    fun logout() {
        sharedPreferences.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
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
        
        val userRole = UserRole.values().find { it.displayName == userRoleString } ?: UserRole.NORMAL_USER
        
        return User(
            id = userId,
            name = userName,
            contact = userContact,
            departmentId = userDepartmentId,
            departmentName = userDepartmentName,
            role = userRole,
            status = UserStatus.NORMAL,
            invitationCode = userInvitationCode
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
            putBoolean(KEY_IS_LOGGED_IN, true)
            if (token != null) {
                putString(KEY_AUTH_TOKEN, token)
            }
            apply()
        }
    }
}