package com.equiptrack.android.data.local.dao

import androidx.room.*
import com.equiptrack.android.data.model.Department
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {
    
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<Department>>
    
    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getDepartmentById(id: String): Department?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartment(department: Department)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<Department>)
    
    @Update
    suspend fun updateDepartment(department: Department)
    
    @Delete
    suspend fun deleteDepartment(department: Department)
    
    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteDepartmentById(id: String)
    
    @Query("DELETE FROM departments")
    suspend fun deleteAllDepartments()

    @Transaction
    suspend fun replaceDepartments(departments: List<Department>) {
        deleteAllDepartments()
        insertDepartments(departments)
    }
}