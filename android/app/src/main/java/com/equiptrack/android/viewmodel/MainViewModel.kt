package com.equiptrack.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.BuildConfig
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.utils.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: EquipTrackApiService,
    private val updateManager: UpdateManager
) : ViewModel() {

    val updateStatus = updateManager.updateStatus

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                updateManager.setChecking()
                val response = apiService.getAppVersion()
                if (response.isSuccessful && response.body() != null) {
                    val remoteVersion = response.body()!!
                    updateManager.checkForUpdate(remoteVersion, BuildConfig.VERSION_CODE)
                } else {
                    updateManager.setError("检查更新失败: ${response.message()}")
                }
            } catch (e: Exception) {
                updateManager.setError("检查更新出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun startDownload(url: String) {
        // Redirect to browser instead of downloading directly if it is a webpage
        if (url.startsWith("http") && !url.endsWith(".apk")) {
            updateManager.openBrowser(url)
        } else {
            updateManager.startDownload(url)
        }
    }
    
    fun dismissUpdate() {
        updateManager.resetStatus()
    }
}
