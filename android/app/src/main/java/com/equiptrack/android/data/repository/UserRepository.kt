package com.equiptrack.android.data.repository

import com.equiptrack.android.data.local.dao.UserDao
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.utils.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val userDao: UserDao
) {
    
    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }
    
    fun getUsersByDepartment(departmentId: String): Flow<List<User>> {
        return userDao.getUsersByDepartment(departmentId)
    }
    
    fun getUsersByRole(role: UserRole): Flow<List<User>> {
        return userDao.getUsersByRole(role)
    }
    
    fun getUsersByStatus(status: UserStatus): Flow<List<User>> {
        return userDao.getUsersByStatus(status)
    }
    
    suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)
    }
    
    suspend fun syncUsers(userRole: UserRole, departmentId: String? = null): Flow<NetworkResult<List<User>>> = flow {
        emit(NetworkResult.Loading())
        
        val result = safeApiCall {
            apiService.getUsers(
                userRole = userRole.displayName,
                departmentId = departmentId
            )
        }
        
        when (result) {
            is NetworkResult.Success -> {
                val users = result.data ?: emptyList()
                userDao.insertUsers(users)
                emit(NetworkResult.Success(users))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "同步失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun createUser(user: User): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        
        val newUser = user.copy(
            id = UUID.randomUUID().toString(),
            status = UserStatus.NORMAL
        )
        
        try {
            // Try to create on server first
            val result = safeApiCall {
                apiService.createUser(newUser)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val createdUser = result.data!!
                    userDao.insertUser(createdUser)
                    emit(NetworkResult.Success(createdUser))
                }
                is NetworkResult.Error -> {
                    // Fallback to local creation
                    userDao.insertUser(newUser)
                    emit(NetworkResult.Success(newUser))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local creation
            userDao.insertUser(newUser)
            emit(NetworkResult.Success(newUser))
        }
    }
    
    suspend fun updateUser(user: User): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to update on server first
            val result = safeApiCall {
                apiService.updateUser(user.id, user)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val updatedUser = result.data!!
                    userDao.updateUser(updatedUser)
                    emit(NetworkResult.Success(updatedUser))
                }
                is NetworkResult.Error -> {
                    // Fallback to local update
                    userDao.updateUser(user)
                    emit(NetworkResult.Success(user))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local update
            userDao.updateUser(user)
            emit(NetworkResult.Success(user))
        }
    }
    
    suspend fun updateUserStatus(userId: String, status: UserStatus): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(status = status)
                updateUser(updatedUser).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            emit(NetworkResult.Success("用户状态更新成功"))
                        }
                        is NetworkResult.Error -> {
                            emit(NetworkResult.Error(result.message ?: "状态更新失败"))
                        }
                        is NetworkResult.Loading -> {
                            emit(NetworkResult.Loading())
                        }
                    }
                }
            } else {
                emit(NetworkResult.Error("用户不存在"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("状态更新失败: ${e.message}"))
        }
    }
    
    suspend fun updateUserPassword(userId: String, newPassword: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(password = newPassword)
                updateUser(updatedUser).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            emit(NetworkResult.Success("密码更新成功"))
                        }
                        is NetworkResult.Error -> {
                            emit(NetworkResult.Error(result.message ?: "密码更新失败"))
                        }
                        is NetworkResult.Loading -> {
                            emit(NetworkResult.Loading())
                        }
                    }
                }
            } else {
                emit(NetworkResult.Error("用户不存在"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("密码更新失败: ${e.message}"))
        }
    }
    
    suspend fun isContactExists(contact: String, excludeId: String? = null): Boolean {
        val user = userDao.getUserByContact(contact)
        return user != null && user.id != excludeId
    }
    
    suspend fun generateInvitationCode(): String {
        // Generate a unique invitation code
        var code: String
        do {
            code = UUID.randomUUID().toString().take(8).uppercase()
        } while (userDao.getUserByInvitationCode(code) != null)
        return code
    }

    suspend fun deleteUser(userId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to delete on server first
            val result = safeApiCall {
                apiService.deleteUser(userId)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    userDao.deleteUserById(userId)
                    emit(NetworkResult.Success("用户删除成功"))
                }
                is NetworkResult.Error -> {
                    // Fallback to local delete if network fails but we want to allow offline delete? 
                    // For consistency, maybe we should only delete locally if server delete succeeds or if it's not synced?
                    // For now, let's assume we want to delete locally even if server fails, but warn user?
                    // Actually, typical pattern is: if server fails, operation fails.
                    // However, for better UX in offline-first apps, we might queue it. 
                    // Given the current implementation style, I'll stick to server-first, then local.
                    // If server fails, we don't delete locally to avoid inconsistency.
                    emit(NetworkResult.Error(result.message ?: "删除失败"))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("删除失败: ${e.message}"))
        }
    }
}