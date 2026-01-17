package com.equiptrack.android.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.equiptrack.android.R
import com.equiptrack.android.data.model.AppVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val version: AppVersion) : UpdateStatus()
    data class NoUpdate(val version: AppVersion) : UpdateStatus()
    data class Downloading(val progress: Int) : UpdateStatus()
    object Downloaded : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    private val prefs by lazy {
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
    }

    fun setChecking() {
        _updateStatus.value = UpdateStatus.Checking
    }
    
    fun setError(message: String) {
        _updateStatus.value = UpdateStatus.Error(message)
    }

    fun checkForUpdate(remoteVersion: AppVersion, currentVersionCode: Int) {
        if (remoteVersion.versionCode > currentVersionCode) {
            _updateStatus.value = UpdateStatus.Available(remoteVersion)
        } else {
            _updateStatus.value = UpdateStatus.NoUpdate(remoteVersion)
        }
    }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("${context.getString(R.string.update_failed_link)}: ${e.message}")
        }
    }

    fun startDownload(url: String, fileName: String = "app-release.apk") {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0)

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.update_title))
                .setDescription(context.getString(R.string.update_downloading_desc))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // Save downloadId to SharedPreferences
            prefs.edit().putLong("download_id", downloadId).apply()

        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: context.getString(R.string.update_download_failed))
        }
    }

    fun onDownloadComplete(id: Long, fileName: String = "app-release.apk") {
        val savedId = prefs.getLong("download_id", -1)
        if (id == savedId) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            checkDownloadStatus(downloadManager, id, fileName)
            // Clear saved ID
            prefs.edit().remove("download_id").apply()
        }
    }

    private fun checkDownloadStatus(downloadManager: DownloadManager, id: Long, fileName: String) {
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex != -1) {
                when (cursor.getInt(statusIndex)) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        _updateStatus.value = UpdateStatus.Downloaded
                        installApk(fileName)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                        _updateStatus.value = UpdateStatus.Error("${context.getString(R.string.update_download_failed)}: $reason")
                    }
                    else -> {
                        // Other statuses (shouldn't happen on COMPLETE broadcast usually)
                    }
                }
            }
        } else {
             _updateStatus.value = UpdateStatus.Error(context.getString(R.string.update_download_info_not_found))
        }
        cursor.close()
    }

    private fun installApk(fileName: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!file.exists()) {
                _updateStatus.value = UpdateStatus.Error(context.getString(R.string.update_file_not_found))
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("${context.getString(R.string.update_install_failed)}: ${e.message}")
        }
    }
    
    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
