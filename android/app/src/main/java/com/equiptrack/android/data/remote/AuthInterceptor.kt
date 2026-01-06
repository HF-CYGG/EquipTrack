package com.equiptrack.android.data.remote

import android.content.SharedPreferences
import android.util.Log
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
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip adding token for login/signup endpoints if needed, 
        // but usually adding it doesn't hurt unless server rejects it.
        // For now, we add it to all requests if available.
        
        val token = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        
        Log.d(TAG, "Intercepting request: ${originalRequest.url}")
        if (token != null) {
            Log.d(TAG, "Token found, adding to header. Token prefix: ${token.take(10)}...")
        } else {
            Log.w(TAG, "No token found in SharedPreferences")
        }

        if (token == null) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
            
        Log.d(TAG, "Request headers: ${newRequest.headers}")

        val response = chain.proceed(newRequest)
        if (response.code == 401 || response.code == 431) {
            Log.w(TAG, "Received ${response.code}. Triggering session expiry.")
            sessionManager.onSessionExpired()
        }
        return response
    }
}
