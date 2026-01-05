package com.equiptrack.android.data.local.dao

import androidx.room.*
import com.equiptrack.android.data.model.BorrowHistoryEntry
import com.equiptrack.android.data.model.BorrowStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BorrowHistoryDao {
    
    @Query("SELECT * FROM borrow_history ORDER BY borrowDate DESC")
    fun getAllHistory(): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE departmentId = :departmentId ORDER BY borrowDate DESC")
    fun getHistoryByDepartment(departmentId: String): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE itemId = :itemId ORDER BY borrowDate DESC")
    fun getHistoryByItem(itemId: String): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE status = :status ORDER BY borrowDate DESC")
    fun getHistoryByStatus(status: BorrowStatus): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE borrowerContact = :contact ORDER BY borrowDate DESC")
    fun getHistoryByBorrower(contact: String): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE id = :id")
    suspend fun getHistoryById(id: String): BorrowHistoryEntry?
    
    @Query("SELECT * FROM borrow_history WHERE status IN ('借用中', '逾期未归还') ORDER BY borrowDate DESC")
    fun getActiveBorrows(): Flow<List<BorrowHistoryEntry>>
    
    @Query("SELECT * FROM borrow_history WHERE status IN ('借用中', '逾期未归还') AND expectedReturnDate < :currentDate")
    suspend fun getOverdueItems(currentDate: Date): List<BorrowHistoryEntry>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: BorrowHistoryEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<BorrowHistoryEntry>)
    
    @Update
    suspend fun updateHistory(history: BorrowHistoryEntry)
    
    @Delete
    suspend fun deleteHistory(history: BorrowHistoryEntry)
    
    @Query("DELETE FROM borrow_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)
    
    @Query("DELETE FROM borrow_history")
    suspend fun deleteAllHistory()

    @Query("DELETE FROM borrow_history WHERE departmentId = :departmentId")
    suspend fun deleteHistoryByDepartment(departmentId: String)

    @Transaction
    suspend fun replaceHistory(histories: List<BorrowHistoryEntry>, departmentId: String?) {
        if (departmentId != null) {
            deleteHistoryByDepartment(departmentId)
        } else {
            deleteAllHistory()
        }
        if (histories.isNotEmpty()) {
            insertHistories(histories)
        }
    }
    
    @Query("UPDATE borrow_history SET status = :status, returnDate = :returnDate WHERE id = :historyId")
    suspend fun updateReturnStatus(historyId: String, status: BorrowStatus, returnDate: Date)
    
    @Query("UPDATE borrow_history SET status = :newStatus WHERE status = :oldStatus AND expectedReturnDate < :currentDate")
    suspend fun updateOverdueStatus(oldStatus: BorrowStatus, newStatus: BorrowStatus, currentDate: Date)
}