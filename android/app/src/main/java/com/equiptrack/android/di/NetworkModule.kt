package com.equiptrack.android.di

import android.content.Context
import android.content.SharedPreferences
import com.equiptrack.android.data.remote.AuthInterceptor
import com.equiptrack.android.data.remote.BaseUrlInterceptor
import com.equiptrack.android.data.remote.FileLoggingInterceptor
import com.equiptrack.android.data.remote.api.EquipTrackApiService
import com.equiptrack.android.data.settings.SettingsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/" // Default to emulator localhost
    // Users should configure the actual server IP in the app settings for LAN/Production usage
    
    private fun normalizeBaseUrl(raw: String?): String {
        val default = DEFAULT_BASE_URL
        val input = raw?.trim()?.takeIf { it.isNotEmpty() } ?: default
        var url = input
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        // Ensure default port 3000 if none specified
        url = ensureDefaultPort(url, 3000)
        if (!url.endsWith('/')) url = "$url/"
        return url
    }

    private fun ensureDefaultPort(url: String, defaultPort: Int): String {
        return try {
            val uri = URI(url)
            val port = if (uri.port == -1) defaultPort else uri.port
            val path = when {
                uri.path.isNullOrEmpty() -> "/"
                uri.path.endsWith("/") -> uri.path
                else -> uri.path + "/"
            }
            URI(
                uri.scheme,
                uri.userInfo,
                uri.host,
                port,
                path,
                uri.query,
                uri.fragment
            ).toString()
        } catch (e: Exception) {
            val base = if (url.endsWith('/')) url.dropLast(1) else url
            if (":" !in base.substringAfter("//").substringBefore("/")) "$base:$defaultPort/" else "$base/"
        }
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("equiptrack_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        fileLoggingInterceptor: FileLoggingInterceptor,
        settingsRepository: SettingsRepository
    ): OkHttpClient {
        // Adjust logging level via settings, fallback by local debug
        loggingInterceptor.level = settingsRepository.getHttpLogLevel()

        val builder = OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor) // Add BaseUrlInterceptor first to rewrite URL
            .addInterceptor(authInterceptor)
            .addInterceptor(fileLoggingInterceptor) // Add File Logger
            .addInterceptor(loggingInterceptor)

        if (settingsRepository.isLocalDebug()) {
            builder
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
        } else {
            builder
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
        }

        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        settingsRepository: SettingsRepository
    ): Retrofit {
        val baseUrl = normalizeBaseUrl(settingsRepository.getServerUrl())
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideEquipTrackApiService(retrofit: Retrofit): EquipTrackApiService {
        return retrofit.create(EquipTrackApiService::class.java)
    }
}