package com.equiptrack.android.data.repository

import com.equiptrack.android.data.local.dao.RegistrationRequestDao
import com.equiptrack.android.data.local.dao.UserDao
import com.equiptrack.android.data.model.*
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.utils.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApprovalRepository @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val registrationRequestDao: RegistrationRequestDao,
    private val userDao: UserDao
) {
    
    fun getAllRequests(): Flow<List<RegistrationRequest>> {
        return registrationRequestDao.getAllRequests()
    }
    
    fun getRequestsByDepartment(departmentId: String): Flow<List<RegistrationRequest>> {
        return registrationRequestDao.getRequestsByDepartment(departmentId)
    }
    
    fun getRequestsByInviter(userId: String): Flow<List<RegistrationRequest>> {
        return registrationRequestDao.getRequestsByInviter(userId)
    }
    
    suspend fun getRequestById(id: String): RegistrationRequest? {
        return registrationRequestDao.getRequestById(id)
    }
    
    suspend fun syncRequests(
        userId: String,
        userRole: UserRole,
        departmentId: String
    ): Flow<NetworkResult<List<RegistrationRequest>>> = flow {
        emit(NetworkResult.Loading())
        
        val result = safeApiCall {
            apiService.getRegistrationRequests(
                userId = userId,
                userRole = userRole.displayName,
                departmentId = departmentId
            )
        }
        
        when (result) {
            is NetworkResult.Success -> {
                val requests = result.data ?: emptyList()
                registrationRequestDao.insertRequests(requests)
                emit(NetworkResult.Success(requests))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "同步失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun approveRequest(requestId: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to approve on server first
            val result = safeApiCall {
                apiService.approveRegistration(requestId)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val newUser = result.data!!
                    // Add user to local database
                    userDao.insertUser(newUser)
                    // Remove request from local database
                    registrationRequestDao.deleteRequestById(requestId)
                    emit(NetworkResult.Success(newUser))
                }
                is NetworkResult.Error -> {
                    // Fallback to local approval
                    val request = registrationRequestDao.getRequestById(requestId)
                    if (request != null) {
                        // Create user from request
                        val newUser = User(
                            id = java.util.UUID.randomUUID().toString(),
                            name = request.name,
                            contact = request.contact,
                            departmentId = request.departmentId ?: "",
                            departmentName = request.departmentName,
                            role = UserRole.NORMAL_USER,
                            status = UserStatus.NORMAL,
                            password = request.password
                        )
                        
                        userDao.insertUser(newUser)
                        registrationRequestDao.deleteRequestById(requestId)
                        emit(NetworkResult.Success(newUser))
                    } else {
                        emit(NetworkResult.Error("申请不存在"))
                    }
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local approval
            val request = registrationRequestDao.getRequestById(requestId)
            if (request != null) {
                // Create user from request
                val newUser = User(
                    id = java.util.UUID.randomUUID().toString(),
                    name = request.name,
                    contact = request.contact,
                    departmentId = request.departmentId ?: "",
                    departmentName = request.departmentName,
                    role = UserRole.NORMAL_USER,
                    status = UserStatus.NORMAL,
                    password = request.password
                )
                
                userDao.insertUser(newUser)
                registrationRequestDao.deleteRequestById(requestId)
                emit(NetworkResult.Success(newUser))
            } else {
                emit(NetworkResult.Error("批准失败: ${e.message}"))
            }
        }
    }
    
    suspend fun rejectRequest(requestId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to reject on server first
            val result = safeApiCall {
                apiService.rejectRegistration(requestId)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    registrationRequestDao.deleteRequestById(requestId)
                    emit(NetworkResult.Success("申请已拒绝"))
                }
                is NetworkResult.Error -> {
                    // Fallback to local rejection
                    registrationRequestDao.deleteRequestById(requestId)
                    emit(NetworkResult.Success("申请已拒绝"))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local rejection
            registrationRequestDao.deleteRequestById(requestId)
            emit(NetworkResult.Success("申请已拒绝"))
        }
    }
    
    suspend fun getRequestsCount(): Int {
        return try {
            val requests = mutableListOf<RegistrationRequest>()
            registrationRequestDao.getAllRequests().collect { list ->
                requests.clear()
                requests.addAll(list)
            }
            requests.size
        } catch (e: Exception) {
            0
        }
    }
}