package com.equiptrack.android.data.remote

import android.content.SharedPreferences
import com.equiptrack.android.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp 认证拦截器
 *
 * 职责：
 * 1. 为每个 HTTP 请求自动附加 JWT Bearer Token（如果已登录）
 * 2. 监听 401/431 响应码，触发会话过期处理（跳转登录页）
 */
class AuthInterceptor @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val sessionManager: SessionManager
) : Interceptor {

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sharedPreferences.getString(KEY_AUTH_TOKEN, null)

        // 未登录时直接放行，不附加 Authorization 头
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // 附加 Bearer Token 到请求头
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(newRequest)
        // 服务端返回 401（未授权）或 431（Token 无效）时，通知会话管理器触发重新登录
        if (response.code == 401 || response.code == 431) {
            sessionManager.onSessionExpired()
        }
        return response
    }
}
