package com.equiptrack.android.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.repository.ApprovalRepository
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.BorrowRepository
import com.equiptrack.android.permission.PermissionChecker
import com.equiptrack.android.permission.PermissionType
import com.equiptrack.android.utils.NetworkResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.flow.collect

@HiltWorker
class ApprovalCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val approvalRepository: ApprovalRepository,
    private val borrowRepository: BorrowRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "approval_check_worker"
    }

    override suspend fun doWork(): Result {
        val user = authRepository.getCurrentUser() ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences("approval_worker_prefs", Context.MODE_PRIVATE)

        var hasNewBorrow = false
        var hasNewRegistration = false

        if (PermissionChecker.hasPermission(user, PermissionType.VIEW_BORROW_APPROVALS)) {
            val borrowCount = getPendingBorrowRequestsCount()
            val lastBorrowCount = prefs.getInt("last_borrow_count", -1)
            if (lastBorrowCount >= 0 && borrowCount > lastBorrowCount) {
                hasNewBorrow = true
            }
            prefs.edit().putInt("last_borrow_count", borrowCount).apply()
        } else {
            prefs.edit().remove("last_borrow_count").apply()
        }

        if (PermissionChecker.hasPermission(user, PermissionType.VIEW_REGISTRATION_APPROVALS)) {
            val registrationCount = getRegistrationRequestsCount(
                userId = user.id,
                userRole = user.role,
                departmentId = user.departmentId
            )
            val lastRegistrationCount = prefs.getInt("last_registration_count", -1)
            if (lastRegistrationCount >= 0 && registrationCount > lastRegistrationCount) {
                hasNewRegistration = true
            }
            prefs.edit().putInt("last_registration_count", registrationCount).apply()
        } else {
            prefs.edit().remove("last_registration_count").apply()
        }

        if (hasNewBorrow) {
            ApprovalNotificationHelper.showBorrowApprovalNotification(applicationContext)
        }
        if (hasNewRegistration) {
            ApprovalNotificationHelper.showRegistrationApprovalNotification(applicationContext)
        }

        // Check for approved requests for the current user (borrower)
        checkApprovedRequests(user.id, prefs)

        return Result.success()
    }

    private suspend fun checkApprovedRequests(userId: String, prefs: android.content.SharedPreferences) {
        // Fetch my requests
        borrowRepository.getMyRequests().collect { result ->
            if (result is NetworkResult.Success) {
                val requests = result.data ?: emptyList()
                // Filter approved requests
                val approved = requests.filter { it.status == com.equiptrack.android.data.model.BorrowStatus.APPROVED }
                
                // Get previously notified approved IDs
                val notifiedSet = prefs.getStringSet("notified_approved_requests", emptySet()) ?: emptySet()
                val newNotifiedSet = notifiedSet.toMutableSet()
                
                for (req in approved) {
                    if (!notifiedSet.contains(req.id)) {
                        // New approval found!
                        ApprovalNotificationHelper.showBorrowApprovedNotification(applicationContext, req.itemName)
                        newNotifiedSet.add(req.id)
                    }
                }
                
                // Save updated set
                prefs.edit().putStringSet("notified_approved_requests", newNotifiedSet).apply()
            }
        }
    }

    private suspend fun getPendingBorrowRequestsCount(): Int {
        var count = 0
        borrowRepository.fetchBorrowRequestsForReview(status = "pending").collect { result ->
            if (result is NetworkResult.Success) {
                count = result.data?.size ?: 0
            }
        }
        return count
    }

    private suspend fun getRegistrationRequestsCount(
        userId: String,
        userRole: UserRole,
        departmentId: String
    ): Int {
        var count = 0
        approvalRepository.syncRequests(
            userId = userId,
            userRole = userRole,
            departmentId = departmentId
        ).collect { result ->
            if (result is NetworkResult.Success) {
                count = result.data?.size ?: 0
            }
        }
        return count
    }
}
