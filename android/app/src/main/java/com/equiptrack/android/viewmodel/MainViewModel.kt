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
                }
            } catch (e: Exception) {
                // Silent fail for update check
                e.printStackTrace()
            }
        }
    }
    
    fun startDownload(url: String) {
        updateManager.startDownload(url)
    }
    
    fun dismissUpdate() {
        updateManager.resetStatus()
    }
}
