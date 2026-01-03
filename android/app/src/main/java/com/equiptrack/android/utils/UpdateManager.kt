package com.equiptrack.android.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
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
    object NoUpdate : UpdateStatus()
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

    private var downloadId: Long = -1

    fun setChecking() {
        _updateStatus.value = UpdateStatus.Checking
    }

    fun checkForUpdate(remoteVersion: AppVersion, currentVersionCode: Int) {
        if (remoteVersion.versionCode > currentVersionCode) {
            _updateStatus.value = UpdateStatus.Available(remoteVersion)
        } else {
            _updateStatus.value = UpdateStatus.NoUpdate
        }
    }

    fun startDownload(url: String, fileName: String = "app-release.apk") {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0)

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("EquipTrack Update")
                .setDescription("Downloading new version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            // Register receiver for download complete
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (downloadId == id) {
                        _updateStatus.value = UpdateStatus.Downloaded
                        installApk(fileName)
                        context.unregisterReceiver(this)
                    }
                }
            }
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Download failed")
        }
    }

    private fun installApk(fileName: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!file.exists()) {
                _updateStatus.value = UpdateStatus.Error("File not found")
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
            _updateStatus.value = UpdateStatus.Error("Install failed: ${e.message}")
        }
    }
    
    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
