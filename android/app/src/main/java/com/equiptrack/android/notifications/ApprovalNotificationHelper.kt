package com.equiptrack.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.equiptrack.android.MainActivity
import com.equiptrack.android.R
import android.app.PendingIntent

/**
 * 审批通知工具类
 *
 * 负责发送本地通知提醒，包括：
 * - 新的借用审批申请（面向管理员）
 * - 新的注册审批申请（面向管理员）
 * - 借用申请通过通知（面向申请人）
 *
 * 安全措施：
 * - Android 13+ (TIRAMISU) 需要 POST_NOTIFICATIONS 运行时权限，发送前主动检查
 * - 所有 notify() 调用均包裹 try-catch 以防止 SecurityException 崩溃
 */
object ApprovalNotificationHelper {

    const val CHANNEL_ID = "approval_updates"
    private const val CHANNEL_NAME = "审批提醒"
    private const val CHANNEL_DESCRIPTION = "新的借用审批或注册审批提醒"

    private const val NOTIFICATION_ID_BORROW = 1001
    private const val NOTIFICATION_ID_REGISTRATION = 1002

    /**
     * 检查是否拥有通知发送权限
     * Android 13 以下默认允许；Android 13+ 需要运行时权限 POST_NOTIFICATIONS
     */
    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel for Approval Reminders (Admins)
            val adminChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // High importance for notifications
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(adminChannel)
            
            // Ensure compatibility with old channel creation if needed (idempotent)
        }
    }

    /** 发送"新借用审批"通知（面向管理员） */
    fun showBorrowApprovalNotification(context: Context) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("审批提醒")
            .setContentText("有新的借用审批申请")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BORROW, builder.build())
            } catch (_: SecurityException) {
            }
        }
    }

    /** 发送"新注册审批"通知（面向管理员） */
    fun showRegistrationApprovalNotification(context: Context) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("审批提醒")
            .setContentText("有新的注册审批申请")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_REGISTRATION, builder.build())
            } catch (e: SecurityException) {
                // Ignore if permission not granted
            }
        }
    }

    /** 发送"借用申请已通过"通知（面向申请人），使用时间戳作为通知 ID 以支持多条并存 */
    fun showBorrowApprovedNotification(context: Context, itemName: String) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 2, intent, flags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("申请通过")
            .setContentText("您申请借用的 $itemName 已通过审批")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }
}
