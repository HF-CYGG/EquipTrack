package com.equiptrack.android.data.repository

import com.equiptrack.android.data.local.dao.DepartmentDao
import com.equiptrack.android.data.model.Department
import com.equiptrack.android.data.model.DepartmentStructureUpdate
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.utils.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DepartmentRepository @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val departmentDao: DepartmentDao,
    private val settingsRepository: SettingsRepository
) {
    
    fun getAllDepartments(): Flow<List<Department>> {
        return departmentDao.getAllDepartments()
    }
    
    suspend fun getDepartmentById(id: String): Department? {
        return departmentDao.getDepartmentById(id)
    }
    
    suspend fun syncDepartments(): Flow<NetworkResult<List<Department>>> = flow {
        emit(NetworkResult.Loading())
        if (settingsRepository.isLocalDebug()) {
            val local = departmentDao.getAllDepartments().first()
            emit(NetworkResult.Success(local))
            return@flow
        }
        val result = safeApiCall { apiService.getDepartments() }
        when (result) {
            is NetworkResult.Success -> {
                val departments = result.data ?: emptyList()
                departmentDao.replaceDepartments(departments)
                emit(NetworkResult.Success(departments))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "同步失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun createDepartment(name: String, requiresApproval: Boolean = true, parentId: String? = null): Flow<NetworkResult<Department>> = flow {
        emit(NetworkResult.Loading())
        
        // Find max order in the same parent level to append to end
        val currentDepts = departmentDao.getAllDepartments().first()
        val siblings = currentDepts.filter { it.parentId == parentId }
        val maxOrder = siblings.maxOfOrNull { it.order } ?: 0
        val newOrder = maxOrder + 1

        val department = Department(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            requiresApproval = requiresApproval,
            parentId = parentId,
            order = newOrder
        )
        
        try {
            // Try to create on server first
            val result = safeApiCall {
                apiService.createDepartment(department)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val createdDepartment = result.data!!
                    departmentDao.insertDepartment(createdDepartment)
                    emit(NetworkResult.Success(createdDepartment))
                }
                is NetworkResult.Error -> {
                    // Fallback to local creation
                    departmentDao.insertDepartment(department)
                    emit(NetworkResult.Success(department))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local creation
            departmentDao.insertDepartment(department)
            emit(NetworkResult.Success(department))
        }
    }
    
    suspend fun updateDepartment(department: Department): Flow<NetworkResult<Department>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to update on server first
            val result = safeApiCall {
                apiService.updateDepartment(department.id, department)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val updatedDepartment = result.data!!
                    departmentDao.updateDepartment(updatedDepartment)
                    emit(NetworkResult.Success(updatedDepartment))
                }
                is NetworkResult.Error -> {
                    // Fallback to local update
                    departmentDao.updateDepartment(department)
                    emit(NetworkResult.Success(department))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local update
            departmentDao.updateDepartment(department)
            emit(NetworkResult.Success(department))
        }
    }
    
    suspend fun updateDepartmentStructure(updates: List<DepartmentStructureUpdate>): Flow<NetworkResult<List<Department>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val result = safeApiCall {
                apiService.updateDepartmentStructure(updates)
            }
            when (result) {
                is NetworkResult.Success -> {
                    val updatedDepartments = result.data ?: emptyList()
                    if (updatedDepartments.isNotEmpty()) {
                        departmentDao.replaceDepartments(updatedDepartments)
                    }
                    emit(NetworkResult.Success(updatedDepartments))
                }
                is NetworkResult.Error -> {
                    // Fallback to local update if network fails or in local mode?
                    // For now, just emit error, or we could update local DAO directly if we trust the logic.
                    // Let's update local DAO optimistically or if offline mode.
                    if (settingsRepository.isLocalDebug()) {
                         // Local mode logic: update DB manually
                         val allDepts = departmentDao.getAllDepartments().first().toMutableList()
                         updates.forEach { update ->
                             val idx = allDepts.indexOfFirst { it.id == update.id }
                             if (idx != -1) {
                                 val existing = allDepts[idx]
                                 allDepts[idx] = existing.copy(parentId = update.parentId, order = update.order)
                             }
                         }
                         departmentDao.replaceDepartments(allDepts)
                         emit(NetworkResult.Success(allDepts))
                    } else {
                        emit(NetworkResult.Error(result.message ?: "Update structure failed"))
                    }
                }
                is NetworkResult.Loading -> emit(NetworkResult.Loading())
            }
        } catch (e: Exception) {
             emit(NetworkResult.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun deleteDepartment(departmentId: String): Flow<NetworkResult<String>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            // Try to delete on server first
            val result = safeApiCall {
                apiService.deleteDepartment(departmentId)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    departmentDao.deleteDepartmentById(departmentId)
                    emit(NetworkResult.Success("删除成功"))
                }
                is NetworkResult.Error -> {
                    // Fallback to local deletion
                    departmentDao.deleteDepartmentById(departmentId)
                    emit(NetworkResult.Success("删除成功"))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local deletion
            departmentDao.deleteDepartmentById(departmentId)
            emit(NetworkResult.Success("删除成功"))
        }
    }
    
    suspend fun isDepartmentNameExists(name: String, excludeId: String? = null): Boolean {
        return try {
            val trimmedName = name.trim()
            val departmentList = departmentDao.getAllDepartments().first()
            departmentList.any { dept ->
                dept.name.equals(trimmedName, ignoreCase = true) && dept.id != excludeId
            }
        } catch (e: Exception) {
            false
        }
    }
}
