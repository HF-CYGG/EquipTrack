package com.equiptrack.android.data.remote

import com.equiptrack.android.data.settings.SettingsRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository
) : Interceptor {

    // Helper to normalize URL similar to NetworkModule logic
    private fun normalizeBaseUrl(raw: String?): String {
        // Default fallback if nothing is set
        val default = "http://10.0.2.2:3000/"
        val input = raw?.trim()?.takeIf { it.isNotEmpty() } ?: default
        
        // Simple normalization
        var url = input.replace("127.0.0.1", "10.0.2.2")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        
        // Ensure trailing slash for Retrofit/OkHttp compatibility
        if (!url.endsWith('/')) url = "$url/"
        return url
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 1. Get the current configured URL from settings
        val currentUrlStr = normalizeBaseUrl(settingsRepository.getServerUrl())
        val newBaseUrl = currentUrlStr.toHttpUrlOrNull()

        // If the new URL is invalid, proceed with the original request
        if (newBaseUrl == null) {
            return chain.proceed(originalRequest)
        }

        // 2. Reconstruct the request URL with the new Scheme, Host, and Port
        val newUrl = originalRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        // 3. Create a new request with the modified URL
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
