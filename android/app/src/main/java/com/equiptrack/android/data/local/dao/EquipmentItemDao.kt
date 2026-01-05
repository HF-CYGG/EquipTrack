package com.equiptrack.android.data.local.dao

import androidx.room.*
import com.equiptrack.android.data.model.EquipmentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentItemDao {
    
    @Query("SELECT * FROM equipment_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<EquipmentItem>>
    
    @Query("SELECT * FROM equipment_items WHERE departmentId = :departmentId ORDER BY name ASC")
    fun getItemsByDepartment(departmentId: String): Flow<List<EquipmentItem>>
    
    @Query("SELECT * FROM equipment_items WHERE availableQuantity > 0 ORDER BY name ASC")
    fun getAvailableItems(): Flow<List<EquipmentItem>>
    
    @Query("SELECT * FROM equipment_items WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getItemsByCategory(categoryId: String): Flow<List<EquipmentItem>>
    
    @Query("SELECT * FROM equipment_items WHERE id = :id")
    suspend fun getItemById(id: String): EquipmentItem?
    
    @Query("SELECT * FROM equipment_items WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchItems(query: String): Flow<List<EquipmentItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: EquipmentItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<EquipmentItem>)
    
    @Update
    suspend fun updateItem(item: EquipmentItem)
    
    @Delete
    suspend fun deleteItem(item: EquipmentItem)
    
    @Query("DELETE FROM equipment_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
    
    @Query("DELETE FROM equipment_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM equipment_items WHERE departmentId = :departmentId")
    suspend fun deleteItemsByDepartment(departmentId: String)
    
    @Transaction
    suspend fun replaceItems(items: List<EquipmentItem>, departmentId: String?) {
        if (departmentId != null) {
            deleteItemsByDepartment(departmentId)
        } else {
            deleteAllItems()
        }
        insertItems(items)
    }
    
    @Query("UPDATE equipment_items SET availableQuantity = availableQuantity - 1 WHERE id = :itemId AND availableQuantity > 0")
    suspend fun decreaseAvailableQuantity(itemId: String): Int
    
    @Query("UPDATE equipment_items SET availableQuantity = availableQuantity + 1 WHERE id = :itemId")
    suspend fun increaseAvailableQuantity(itemId: String): Int
}