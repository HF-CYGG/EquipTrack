package com.equiptrack.android.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DownloadCompleteReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateManagerEntryPoint {
        fun getUpdateManager(): UpdateManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context,
                    UpdateManagerEntryPoint::class.java
                )
                entryPoint.getUpdateManager().onDownloadComplete(id)
            }
        }
    }
}
