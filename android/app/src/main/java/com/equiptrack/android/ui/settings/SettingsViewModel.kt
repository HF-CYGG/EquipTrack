package com.equiptrack.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.equiptrack.android.data.log.LogManager
import com.equiptrack.android.data.local.LocalDebugSeeder
import com.equiptrack.android.data.session.SessionManager
import com.equiptrack.android.data.repository.AuthRepository
import com.equiptrack.android.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

import com.equiptrack.android.utils.AutoStartPermissionHelper
import android.content.Context
import android.content.Intent

import com.equiptrack.android.services.NotificationPollingService

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localDebugSeeder: LocalDebugSeeder,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val logManager: LogManager
) : ViewModel() {

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    sealed class NavigationEvent {
        object NavigateToMain : NavigationEvent()
        object NavigateToLogin : NavigationEvent()
    }

    fun getServerUrl(): String = settingsRepository.getServerUrl() ?: ""
    fun isLocalDebug(): Boolean = settingsRepository.isLocalDebug()
    fun isNotificationServiceEnabled(): Boolean = settingsRepository.isNotificationServiceEnabled()
    fun isSetupCompleted(): Boolean = settingsRepository.isSetupCompleted()

    fun checkAutoStartPermission(context: Context): Boolean {
        return AutoStartPermissionHelper.isAutoStartPermissionAvailable(context)
    }

    fun requestAutoStartPermission(context: Context) {
        val intent = AutoStartPermissionHelper.getAutoStartPermissionIntent(context)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    fun togglePollingService(context: Context, enabled: Boolean) {
        // Save preference
        settingsRepository.setNotificationServiceEnabled(enabled)
        
        val intent = Intent(context, NotificationPollingService::class.java)
        if (enabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            intent.action = NotificationPollingService.ACTION_STOP
            context.startService(intent)
        }
    }
    
    fun isPollingServiceRunning(context: Context): Boolean {
        // First check if it SHOULD be running according to settings
        // But the UI might want actual status. 
        // Requirement 1 says "keep switch state". 
        // So we should return the saved setting OR the actual service state?
        // Usually, the switch reflects the setting. 
        // But for "Service", we want to know if it's actually running.
        // Let's rely on the setting for the UI switch initial state if service is not running?
        // No, let's trust the system check, but maybe auto-start if setting is true?
        // For now, just save the setting here.
        
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationPollingService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            val oldUrl = settingsRepository.getServerUrl()
            // 如果旧地址存在且与新地址不同，则清理数据
            if (!oldUrl.isNullOrBlank() && oldUrl != url) {
                // URL changed, invalidate everything including backup
                settingsRepository.clearBackupSession()
                
                // 清理所有本地数据库数据
                localDebugSeeder.clearAllData()
                // 退出登录（清理Auth Token等）
                authRepository.logout()
                // 触发会话过期（这将导航回登录页，并确保后续重新加载数据）
                sessionManager.onSessionExpired()
            }

            settingsRepository.saveServerUrl(url)
            settingsRepository.setSetupCompleted(true)
            // 当启用本地调试后，自动初始化本地示例数据
            if (settingsRepository.isLocalDebug()) {
                localDebugSeeder.seedIfLocalDebug()
            }
        }
    }

    fun setLocalDebug(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Switching TO Local Debug
                // 1. Backup current remote session
                authRepository.backupRemoteSession()
                
                // 2. Clear current session (logout) but keep backup
                authRepository.logout()
                
                // 3. Clear existing data to ensure clean local environment
                localDebugSeeder.clearAllData()
                
                // 4. Enable local debug
                settingsRepository.setLocalDebug(true)
                
                // 5. Seed local data
                localDebugSeeder.seedIfLocalDebug()
                
                // 6. Auto Login as Super Admin (Requirement 4)
                authRepository.login("admin", "admin").collect { result ->
                    if (result is com.equiptrack.android.utils.NetworkResult.Success) {
                        _navigationEvent.send(NavigationEvent.NavigateToMain)
                    }
                }
            } else {
                // Switching FROM Local Debug (TO Remote)
                // 1. Clear local session
                authRepository.logout()
                
                // 2. Clear local data
                localDebugSeeder.clearAllData()
                
                // 3. Disable local debug
                settingsRepository.setLocalDebug(false)
                
                // 4. Restore remote session (Requirement 5)
                val restored = authRepository.restoreRemoteSession()
                
                if (restored) {
                     _navigationEvent.send(NavigationEvent.NavigateToMain)
                } else {
                     _navigationEvent.send(NavigationEvent.NavigateToLogin)
                }
            }
        }
    }
    
    fun getHttpLogLevel(): HttpLoggingInterceptor.Level = settingsRepository.getHttpLogLevel()
    
    fun setHttpLogLevel(level: HttpLoggingInterceptor.Level) {
        settingsRepository.setHttpLogLevel(level)
        httpLoggingInterceptor.level = level
    }

    fun resetLocalSeed() {
        viewModelScope.launch {
            localDebugSeeder.resetLocalSeed()
        }
    }

    fun triggerSessionExpiry() {
        sessionManager.onSessionExpired()
    }

    fun testConnection(serverUrlRaw: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                var url = serverUrlRaw.trim()
                if (url.isBlank()) {
                    withContext(Dispatchers.Main) { onResult(false, "服务器地址不能为空") }
                    return@launch
                }
                
                if (url.contains("127.0.0.1")) url = url.replace("127.0.0.1", "10.0.2.2")
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
                if (!url.endsWith('/')) url = "$url/"
                
                // 根据 API 规范，所有端点以 /api 为前缀；此处选择 /api/departments 进行连通性探测
                val testUrl = URL(url + "api/departments")
                conn = (testUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "EquipTrack-Android")
                    useCaches = false
                    doInput = true
                }
                
                val responseCode = conn.responseCode
                val ok = responseCode in 200..299
                val message = if (ok) {
                    null
                } else {
                    "HTTP $responseCode"
                }
                
                withContext(Dispatchers.Main) { onResult(ok, message) }
            } catch (e: java.net.SocketTimeoutException) {
                withContext(Dispatchers.Main) { onResult(false, "连接超时，请检查网络或服务器地址") }
            } catch (e: java.net.UnknownHostException) {
                withContext(Dispatchers.Main) { onResult(false, "无法解析服务器地址") }
            } catch (e: java.net.ConnectException) {
                withContext(Dispatchers.Main) { onResult(false, "无法连接到服务器") }
            } catch (e: java.security.cert.CertificateException) {
                withContext(Dispatchers.Main) { onResult(false, "SSL证书验证失败") }
            } catch (e: javax.net.ssl.SSLException) {
                withContext(Dispatchers.Main) { onResult(false, "SSL连接错误") }
            } catch (e: java.net.MalformedURLException) {
                withContext(Dispatchers.Main) { onResult(false, "服务器地址格式错误") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "连接失败: ${e.message}") }
            } finally {
                try {
                    conn?.disconnect()
                } catch (e: Exception) {
                    // 忽略关闭连接时的异常
                }
            }
        }
    }

    fun shareLogs(context: Context) {
        val logFile = logManager.getLatestLogFile()
        if (logFile == null || !logFile.exists()) {
            return
        }
        
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "分享错误日志")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app, show a toast or something
        }
    }
}
