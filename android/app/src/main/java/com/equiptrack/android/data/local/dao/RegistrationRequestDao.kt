package com.equiptrack.android.data.local.dao

import androidx.room.*
import com.equiptrack.android.data.model.RegistrationRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationRequestDao {
    
    @Query("SELECT * FROM registration_requests ORDER BY requestDate DESC")
    fun getAllRequests(): Flow<List<RegistrationRequest>>
    
    @Query("SELECT * FROM registration_requests WHERE departmentId = :departmentId ORDER BY requestDate DESC")
    fun getRequestsByDepartment(departmentId: String): Flow<List<RegistrationRequest>>
    
    @Query("SELECT * FROM registration_requests WHERE invitedBy = :userId ORDER BY requestDate DESC")
    fun getRequestsByInviter(userId: String): Flow<List<RegistrationRequest>>
    
    @Query("SELECT * FROM registration_requests WHERE status = :status ORDER BY requestDate DESC")
    fun getRequestsByStatus(status: String): Flow<List<RegistrationRequest>>
    
    @Query("SELECT * FROM registration_requests WHERE id = :id")
    suspend fun getRequestById(id: String): RegistrationRequest?
    
    @Query("SELECT * FROM registration_requests WHERE contact = :contact")
    suspend fun getRequestByContact(contact: String): RegistrationRequest?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RegistrationRequest)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<RegistrationRequest>)
    
    @Update
    suspend fun updateRequest(request: RegistrationRequest)
    
    @Delete
    suspend fun deleteRequest(request: RegistrationRequest)
    
    @Query("DELETE FROM registration_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)
    
    @Query("DELETE FROM registration_requests")
    suspend fun deleteAllRequests()
}