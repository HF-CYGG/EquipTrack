package com.equiptrack.android.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Helper utility to open "Auto Start" or "Background Management" settings 
 * for various Chinese manufacturers (Xiaomi, Vivo, Oppo, etc.)
 */
object AutoStartPermissionHelper {

    fun isAutoStartPermissionAvailable(context: Context): Boolean {
        // Simple heuristic: check if we are on a known manufacturer
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || 
               manufacturer.contains("oppo") || 
               manufacturer.contains("vivo") || 
               manufacturer.contains("huawei") || 
               manufacturer.contains("honor") ||
               manufacturer.contains("meizu") ||
               manufacturer.contains("letv") ||
               manufacturer.contains("samsung")
    }

    fun getAutoStartPermissionIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()

        try {
            when {
                manufacturer.contains("xiaomi") -> {
                    intent.component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                manufacturer.contains("oppo") -> {
                    intent.component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
                manufacturer.contains("vivo") -> {
                    intent.component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    intent.component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
                manufacturer.contains("meizu") -> {
                    intent.component = ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.security.SHOW_APPSEC"
                    )
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.putExtra("packageName", context.packageName)
                }
                manufacturer.contains("samsung") -> {
                    intent.component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
                else -> {
                    return null
                }
            }
            
            // Verify if the intent can be resolved
            val list = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            return if (list.isNotEmpty()) intent else null

        } catch (e: Exception) {
            Log.e("AutoStartHelper", "Failed to create auto start intent", e)
            return null
        }
    }
}
