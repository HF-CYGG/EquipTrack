package com.equiptrack.android.data.repository

import com.equiptrack.android.data.local.dao.BorrowHistoryDao
import com.equiptrack.android.data.local.dao.EquipmentItemDao
import com.equiptrack.android.data.model.*
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
class BorrowRepository @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val borrowHistoryDao: BorrowHistoryDao,
    private val equipmentItemDao: EquipmentItemDao,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) {
    
    fun getAllHistory(): Flow<List<BorrowHistoryEntry>> {
        return borrowHistoryDao.getAllHistory()
    }
    
    fun getHistoryByDepartment(departmentId: String): Flow<List<BorrowHistoryEntry>> {
        return borrowHistoryDao.getHistoryByDepartment(departmentId)
    }

    fun getHistoryByBorrower(contact: String): Flow<List<BorrowHistoryEntry>> {
        return borrowHistoryDao.getHistoryByBorrower(contact)
    }
    
    fun getActiveBorrows(): Flow<List<BorrowHistoryEntry>> {
        return borrowHistoryDao.getActiveBorrows()
    }
    
    suspend fun borrowItem(
        itemId: String,
        borrowRequest: BorrowRequest
    ): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())
        
        // Check if item is available and has enough quantity
        val item = equipmentItemDao.getItemById(itemId)
        if (item == null || item.availableQuantity < borrowRequest.quantity) {
            if (item == null) {
                emit(NetworkResult.Error("物资不存在"))
            } else {
                emit(NetworkResult.Error("库存不足，当前可用数量: ${item.availableQuantity}"))
            }
            return@flow
        }
        
        if (settingsRepository.isLocalDebug()) {
            val updatedItem = item.copy(
                availableQuantity = item.availableQuantity - borrowRequest.quantity,
                borrowPhoto = borrowRequest.photo
            )
            equipmentItemDao.updateItem(updatedItem)
            val currentUser = authRepository.getCurrentUser()
            val historyEntry = BorrowHistoryEntry(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                itemName = item.name,
                departmentId = item.departmentId,
                borrowerName = borrowRequest.borrower.name,
                borrowerContact = borrowRequest.borrower.phone,
                operatorUserId = currentUser?.id ?: "",
                operatorName = currentUser?.name ?: "",
                operatorContact = currentUser?.contact ?: "",
                borrowDate = Date(),
                expectedReturnDate = borrowRequest.expectedReturnDate,
                status = BorrowStatus.BORROWING,
                photo = borrowRequest.photo
            )
            borrowHistoryDao.insertHistory(historyEntry)
            emit(NetworkResult.Success(Unit))
            return@flow
        }
        try {
            val result = safeApiCall {
                apiService.createBorrowRequest(
                    BorrowRequestCreateRequest(
                        itemId = itemId,
                        borrower = borrowRequest.borrower,
                        expectedReturnDate = borrowRequest.expectedReturnDate,
                        photo = borrowRequest.photo,
                        quantity = borrowRequest.quantity
                    )
                )
            }

            when (result) {
                is NetworkResult.Success -> {
                    emit(NetworkResult.Success(Unit))
                }
                is NetworkResult.Error -> {
                    emit(NetworkResult.Error(result.message ?: "提交借用申请失败"))
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("提交借用申请失败: ${e.message}"))
        }
    }

    suspend fun fetchBorrowRequestsForReview(
        status: String? = null
    ): Flow<NetworkResult<List<BorrowRequestEntry>>> = flow {
        emit(NetworkResult.Loading())
        val result = safeApiCall {
            apiService.getBorrowReviewRequests(status)
        }
        when (result) {
            is NetworkResult.Success -> {
                val requests = result.data ?: emptyList()
                emit(NetworkResult.Success(requests))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "加载借用申请失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }

    suspend fun fetchBorrowReviewHistory(): Flow<NetworkResult<List<BorrowRequestEntry>>> = flow {
        emit(NetworkResult.Loading())
        val approvedResult = safeApiCall {
            apiService.getBorrowReviewRequests("approved")
        }
        val rejectedResult = safeApiCall {
            apiService.getBorrowReviewRequests("rejected")
        }

        val approved = if (approvedResult is NetworkResult.Success) {
            approvedResult.data ?: emptyList()
        } else {
            emptyList()
        }

        val rejected = if (rejectedResult is NetworkResult.Success) {
            rejectedResult.data ?: emptyList()
        } else {
            emptyList()
        }

        if (approvedResult is NetworkResult.Error && rejectedResult is NetworkResult.Error) {
            emit(
                NetworkResult.Error(
                    approvedResult.message ?: rejectedResult.message ?: "加载审批历史失败"
                )
            )
            return@flow
        }

        val combined = (approved + rejected).sortedByDescending { entry ->
            entry.reviewedAt ?: entry.createdAt
        }
        emit(NetworkResult.Success(combined))
    }

    suspend fun approveBorrowRequest(
        requestId: String,
        remark: String?
    ): Flow<NetworkResult<BorrowRequestEntry>> = flow {
        emit(NetworkResult.Loading())
        val result = safeApiCall {
            apiService.approveBorrowRequest(
                requestId,
                BorrowReviewActionRequest(remark = remark)
            )
        }
        when (result) {
            is NetworkResult.Success -> {
                val entry = result.data!!
                emit(NetworkResult.Success(entry))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "通过借用申请失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }

    suspend fun rejectBorrowRequest(
        requestId: String,
        remark: String?
    ): Flow<NetworkResult<BorrowRequestEntry>> = flow {
        emit(NetworkResult.Loading())
        val result = safeApiCall {
            apiService.rejectBorrowRequest(
                requestId,
                BorrowReviewActionRequest(remark = remark)
            )
        }
        when (result) {
            is NetworkResult.Success -> {
                val entry = result.data!!
                emit(NetworkResult.Success(entry))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "驳回借用申请失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun returnItem(
        itemId: String,
        historyEntryId: String,
        returnRequest: ReturnRequest
    ): Flow<NetworkResult<EquipmentItem>> = flow {
        emit(NetworkResult.Loading())
        if (settingsRepository.isLocalDebug()) {
            val item = equipmentItemDao.getItemById(itemId)
            val historyEntry = borrowHistoryDao.getHistoryById(historyEntryId)
            if (item != null && historyEntry != null) {
                val updatedItem = item.copy(
                    availableQuantity = item.availableQuantity + 1,
                    lastReturnPhoto = returnRequest.photo
                )
                equipmentItemDao.updateItem(updatedItem)
                val currentDate = Date()
                val status = if (currentDate.after(historyEntry.expectedReturnDate)) {
                    BorrowStatus.OVERDUE_RETURNED
                } else {
                    BorrowStatus.RETURNED
                }
                val updatedHistory = historyEntry.copy(
                    returnDate = currentDate,
                    status = status,
                    forcedReturnBy = if (returnRequest.isForced) returnRequest.adminName else null
                )
                borrowHistoryDao.updateHistory(updatedHistory)
                emit(NetworkResult.Success(updatedItem))
                return@flow
            } else {
                emit(NetworkResult.Error("归还失败：找不到相关记录"))
                return@flow
            }
        }
        try {
            // Try to return on server first
            val result = safeApiCall {
                apiService.returnItem(itemId, historyEntryId, returnRequest)
            }
            
            when (result) {
                is NetworkResult.Success -> {
                    val updatedItem = result.data!!
                    // Update local database
                    equipmentItemDao.updateItem(updatedItem)
                    
                    // Update history entry
                    val historyEntry = borrowHistoryDao.getHistoryById(historyEntryId)
                    if (historyEntry != null) {
                        val currentDate = Date()
                        val status = if (currentDate.after(historyEntry.expectedReturnDate)) {
                            BorrowStatus.OVERDUE_RETURNED
                        } else {
                            BorrowStatus.RETURNED
                        }
                        
                        val updatedHistory = historyEntry.copy(
                            returnDate = currentDate,
                            status = status,
                            forcedReturnBy = if (returnRequest.isForced) returnRequest.adminName else null
                        )
                        borrowHistoryDao.updateHistory(updatedHistory)
                    }
                    
                    emit(NetworkResult.Success(updatedItem))
                }
                is NetworkResult.Error -> {
                    // Try local return as fallback
                    val item = equipmentItemDao.getItemById(itemId)
                    val historyEntry = borrowHistoryDao.getHistoryById(historyEntryId)
                    
                    if (item != null && historyEntry != null) {
                        val updatedItem = item.copy(
                            availableQuantity = item.availableQuantity + 1,
                            lastReturnPhoto = returnRequest.photo
                        )
                        equipmentItemDao.updateItem(updatedItem)
                        
                        // Update history entry
                        val currentDate = Date()
                        val status = if (currentDate.after(historyEntry.expectedReturnDate)) {
                            BorrowStatus.OVERDUE_RETURNED
                        } else {
                            BorrowStatus.RETURNED
                        }
                        
                        val updatedHistory = historyEntry.copy(
                            returnDate = currentDate,
                            status = status,
                            forcedReturnBy = if (returnRequest.isForced) returnRequest.adminName else null
                        )
                        borrowHistoryDao.updateHistory(updatedHistory)
                        
                        emit(NetworkResult.Success(updatedItem))
                    } else {
                        emit(NetworkResult.Error("归还失败：找不到相关记录"))
                    }
                }
                is NetworkResult.Loading -> {
                    emit(NetworkResult.Loading())
                }
            }
        } catch (e: Exception) {
            // Fallback to local return
            val item = equipmentItemDao.getItemById(itemId)
            val historyEntry = borrowHistoryDao.getHistoryById(historyEntryId)
            
            if (item != null && historyEntry != null) {
                val updatedItem = item.copy(
                    availableQuantity = item.availableQuantity + 1,
                    lastReturnPhoto = returnRequest.photo
                )
                equipmentItemDao.updateItem(updatedItem)
                
                // Update history entry
                val currentDate = Date()
                val status = if (currentDate.after(historyEntry.expectedReturnDate)) {
                    BorrowStatus.OVERDUE_RETURNED
                } else {
                    BorrowStatus.RETURNED
                }
                
                val updatedHistory = historyEntry.copy(
                    returnDate = currentDate,
                    status = status,
                    forcedReturnBy = if (returnRequest.isForced) returnRequest.adminName else null
                )
                borrowHistoryDao.updateHistory(updatedHistory)
                
                emit(NetworkResult.Success(updatedItem))
            } else {
                emit(NetworkResult.Error("归还失败: ${e.message}"))
            }
        }
    }
    
    suspend fun syncHistory(userRole: UserRole, departmentId: String? = null): Flow<NetworkResult<List<BorrowHistoryEntry>>> = flow {
        emit(NetworkResult.Loading())
        if (settingsRepository.isLocalDebug()) {
            val local = if (departmentId != null) borrowHistoryDao.getHistoryByDepartment(departmentId) else borrowHistoryDao.getAllHistory()
            // Collect one snapshot for immediate success
            val snapshot = local.first()
            emit(NetworkResult.Success(snapshot))
            return@flow
        }
        val token = authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            emit(NetworkResult.Error("登录状态缺少令牌，请退出后重新登录"))
            return@flow
        }
        val result = safeApiCall {
            apiService.getBorrowHistory(
                userRole = userRole.displayName,
                departmentId = departmentId
            )
        }
        when (result) {
            is NetworkResult.Success -> {
                val rawHistories = result.data ?: emptyList()
                val sanitizedHistories = rawHistories.map { entry ->
                    // Sanitize data to prevent "Parameter specified as non-null is null" error
                    // casting to String? to handle potential nulls from JSON parsing that bypass Kotlin null-safety
                    entry.copy(
                        id = (entry.id as? String) ?: UUID.randomUUID().toString(),
                        itemId = (entry.itemId as? String) ?: "",
                        itemName = (entry.itemName as? String) ?: "未知物品",
                        departmentId = (entry.departmentId as? String) ?: "",
                        borrowerName = (entry.borrowerName as? String) ?: "未知借用人",
                        borrowerContact = (entry.borrowerContact as? String) ?: "",
                        operatorUserId = (entry.operatorUserId as? String) ?: "",
                        operatorName = (entry.operatorName as? String) ?: "未知操作员",
                        operatorContact = (entry.operatorContact as? String) ?: "",
                        // Ensure status is not null (handle potential Gson unsafe deserialization nulls)
                        status = try { entry.status } catch (e: NullPointerException) { BorrowStatus.BORROWING }
                    )
                }
                // Clear old history before inserting new ones to prevent duplicates if IDs changed or if we want a fresh sync
                // However, wiping all history might be too aggressive if sync fails later.
                // But since we get the FULL history from server for the user/department, we can safely replace.
                // Ideally we should delete only those that are not in the new list, or just rely on REPLACE strategy.
                // If the server returns duplicates (which it shouldn't), REPLACE will handle ID collisions.
                // But if the server returns the SAME logical entry with a DIFFERENT ID (e.g. generated on fly), we get duplicates.
                // Based on items.json, the IDs look stable ("id": "hist_...").
                
                // To be safe and ensure clean state matching server:
                // 1. Get all local IDs
                // 2. Delete local items that are NOT in the new list (optional, for cleanup)
                // 3. Insert/Replace new items
                
                // Simple approach for now: Since `insertHistories` uses REPLACE, existing IDs are updated.
                // If users see "duplicates", it implies different IDs for same content.
                // Let's check if we should clear table for the relevant scope (all or dept).
                // For now, let's try clearing all history before inserting to guarantee 1:1 sync with server.
                if (departmentId != null) {
                     // borrowHistoryDao.deleteHistoryByDepartment(departmentId) // Need to add this DAO method if we go this route
                } else {
                     borrowHistoryDao.deleteAllHistory()
                }
                
                borrowHistoryDao.insertHistories(sanitizedHistories)
                emit(NetworkResult.Success(sanitizedHistories))
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message ?: "同步失败"))
            }
            is NetworkResult.Loading -> {
                emit(NetworkResult.Loading())
            }
        }
    }
    
    suspend fun updateOverdueStatus() {
        val currentDate = Date()
        borrowHistoryDao.updateOverdueStatus(
            oldStatus = BorrowStatus.BORROWING,
            newStatus = BorrowStatus.OVERDUE_NOT_RETURNED,
            currentDate = currentDate
        )
    }
}
