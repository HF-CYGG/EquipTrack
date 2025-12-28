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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localDebugSeeder: LocalDebugSeeder,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val sessionManager: SessionManager
) : ViewModel() {
    fun getServerUrl(): String = settingsRepository.getServerUrl() ?: ""
    fun isLocalDebug(): Boolean = settingsRepository.isLocalDebug()
    fun isSetupCompleted(): Boolean = settingsRepository.isSetupCompleted()
    fun saveServerUrl(url: String) {
        viewModelScope.launch {
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
