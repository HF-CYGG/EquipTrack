package com.equiptrack.android.data.remote

import com.equiptrack.android.data.log.LogManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class FileLoggingInterceptor @Inject constructor(
    private val logManager: LogManager
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        try {
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                logManager.e("Network", "Request failed: $method $url - Code: ${response.code} Message: ${response.message}")
            }
            
            return response
        } catch (e: Exception) {
            logManager.e("Network", "Request error: $method $url", e)
            
            // Fix for OkHttp crash "IllegalStateException: state: 0"
            // Wrap RuntimeExceptions in IOException so Retrofit handles them as network errors
            if (e is IllegalStateException) {
                throw IOException("Network internal error: ${e.message}", e)
            }
            throw e
        }
    }
}
