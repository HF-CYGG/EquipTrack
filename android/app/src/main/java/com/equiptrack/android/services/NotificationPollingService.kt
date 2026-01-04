package com.equiptrack.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.equiptrack.android.MainActivity
import com.equiptrack.android.R
import com.equiptrack.android.data.model.BorrowStatus
import com.equiptrack.android.data.model.UserRole
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.repository.BorrowRepository
import com.equiptrack.android.notifications.ApprovalNotificationHelper
import com.equiptrack.android.utils.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationPollingService : Service() {

    @Inject
    lateinit var borrowRepository: BorrowRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Polling interval: 60 seconds
    private val POLLING_INTERVAL = 60 * 1000L

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun startForegroundService() {
        val channelId = "polling_service_channel"
        val channelName = "后台连接服务"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW // 提高到 LOW，确保显示图标
            ).apply {
                description = "保持后台连接以接收消息"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("EquipTrack 正在运行")
            .setContentText("实时通知服务已开启") // 添加文字说明
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 提高到 LOW
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startPolling() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    checkMessages()
                } catch (e: Exception) {
                    Log.e("PollingService", "Error polling messages", e)
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private suspend fun checkMessages() {
        val user = authRepository.getCurrentUser() ?: return
        val prefs = getSharedPreferences("polling_prefs", Context.MODE_PRIVATE)
        
        // 1. Check for approved requests (for applicant)
        borrowRepository.getMyRequests().collect { result ->
            if (result is NetworkResult.Success) {
                val requests = result.data ?: emptyList()
                val approved = requests.filter { it.status == BorrowStatus.APPROVED }
                
                val notifiedSet = prefs.getStringSet("notified_approved_requests", emptySet()) ?: emptySet()
                val newNotifiedSet = notifiedSet.toMutableSet()
                
                for (req in approved) {
                    if (!notifiedSet.contains(req.id)) {
                        ApprovalNotificationHelper.showBorrowApprovedNotification(this, req.itemName)
                        newNotifiedSet.add(req.id)
                    }
                }
                
                prefs.edit().putStringSet("notified_approved_requests", newNotifiedSet).apply()
            }
        }

        // 2. Check for pending approvals (for admins)
        if (user.role == UserRole.SUPER_ADMIN || user.role == UserRole.ADMIN || user.role == UserRole.ADVANCED_USER) {
            borrowRepository.fetchBorrowRequestsForReview(status = "pending").collect { result ->
                if (result is NetworkResult.Success) {
                    val pending = result.data ?: emptyList()
                    
                    val notifiedPendingSet = prefs.getStringSet("notified_pending_requests", emptySet()) ?: emptySet()
                    val newNotifiedPendingSet = notifiedPendingSet.toMutableSet()
                    var hasNew = false
                    
                    for (req in pending) {
                        if (!notifiedPendingSet.contains(req.id)) {
                            hasNew = true
                            newNotifiedPendingSet.add(req.id)
                        }
                    }
                    
                    if (hasNew) {
                        ApprovalNotificationHelper.showBorrowApprovalNotification(this)
                    }
                    
                    prefs.edit().putStringSet("notified_pending_requests", newNotifiedPendingSet).apply()
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "STOP_SERVICE"
    }
}
