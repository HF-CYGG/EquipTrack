package com.equiptrack.android.data.remote

import android.content.SharedPreferences
import com.equiptrack.android.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

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

        if (token == null) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(newRequest)
        if (response.code == 401 || response.code == 431) {
            sessionManager.onSessionExpired()
        }
        return response
    }
}
