package com.equiptrack.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equiptrack.android.data.local.LocalDebugSeeder
import com.equiptrack.android.data.session.SessionManager
import com.equiptrack.android.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import java.net.HttpURLConnection
import java.net.URL

import com.equiptrack.android.utils.AutoStartPermissionHelper
import android.content.Context
import android.content.Intent

import com.equiptrack.android.services.NotificationPollingService

import com.equiptrack.android.data.repository.AuthRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localDebugSeeder: LocalDebugSeeder,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    fun getServerUrl(): String = settingsRepository.getServerUrl() ?: ""
    fun isLocalDebug(): Boolean = settingsRepository.isLocalDebug()
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
                settingsRepository.setLocalDebug(true)
                localDebugSeeder.seedIfLocalDebug()
            } else {
                localDebugSeeder.clearAllData()
                settingsRepository.setLocalDebug(false)
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
}
