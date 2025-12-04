package com.equiptrack.android.data.local.dao

import androidx.room.*
import com.equiptrack.android.data.model.User
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.model.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE departmentId = :departmentId ORDER BY name ASC")
    fun getUsersByDepartment(departmentId: String): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: UserRole): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE status = :status ORDER BY name ASC")
    fun getUsersByStatus(status: UserStatus): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    // Fast local authentication by indexed fields
    @Query("SELECT * FROM users WHERE contact = :contact AND password = :password LIMIT 1")
    suspend fun authenticateUser(contact: String, password: String): User?
    
    @Query("SELECT * FROM users WHERE contact = :contact")
    suspend fun getUserByContact(contact: String): User?
    
    @Query("SELECT * FROM users WHERE invitationCode = :code")
    suspend fun getUserByInvitationCode(code: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
    
    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: UserStatus)
    
    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updateUserPassword(userId: String, newPassword: String)
}