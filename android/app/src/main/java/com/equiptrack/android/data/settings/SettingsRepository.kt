package com.equiptrack.android.data.settings

import android.content.Context
import android.content.SharedPreferences
import okhttp3.logging.HttpLoggingInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

data class ThemeOverrides(
    val primaryColorHex: String? = null,
    val accentColorHex: String? = null,
    val backgroundUri: String? = null,
    val backgroundDimAlpha: Float? = null,
    val backgroundContentScale: String? = null,
    val backgroundBlurRadius: Int? = null
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("equiptrack_settings", Context.MODE_PRIVATE)

    private val _themeOverridesFlow: MutableStateFlow<ThemeOverrides> = MutableStateFlow(
        ThemeOverrides(
            primaryColorHex = null,
            accentColorHex = null,
            backgroundUri = null,
            backgroundDimAlpha = null,
            backgroundContentScale = null,
            backgroundBlurRadius = null
        )
    )

    // Observable flows for server URL and local debug flag
    private val _serverUrlFlow: MutableStateFlow<String?> = MutableStateFlow(
        prefs.getString(KEY_SERVER_URL, null)
    )
    private val _localDebugFlow: MutableStateFlow<Boolean> = MutableStateFlow(
        prefs.getBoolean(KEY_LOCAL_DEBUG, false)
    )

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        when (key) {
            KEY_PRIMARY_COLOR, KEY_ACCENT_COLOR, KEY_BACKGROUND_URI, KEY_BACKGROUND_DIM_ALPHA, KEY_BACKGROUND_CONTENT_SCALE, KEY_BACKGROUND_BLUR_RADIUS -> {
                _themeOverridesFlow.value = ThemeOverrides(
                    primaryColorHex = shared.getString(KEY_PRIMARY_COLOR, null),
                    accentColorHex = shared.getString(KEY_ACCENT_COLOR, null),
                    backgroundUri = shared.getString(KEY_BACKGROUND_URI, null),
                    backgroundDimAlpha = runCatching { shared.getFloat(KEY_BACKGROUND_DIM_ALPHA, -1f) }.getOrNull()?.takeIf { it >= 0f },
                    backgroundContentScale = shared.getString(KEY_BACKGROUND_CONTENT_SCALE, null),
                    backgroundBlurRadius = shared.getInt(KEY_BACKGROUND_BLUR_RADIUS, 0)
                )
            }
            KEY_SERVER_URL -> {
                _serverUrlFlow.value = shared.getString(KEY_SERVER_URL, null)
            }
            KEY_LOCAL_DEBUG -> {
                _localDebugFlow.value = shared.getBoolean(KEY_LOCAL_DEBUG, false)
            }
        }
    }

    init {
        _themeOverridesFlow.value = ThemeOverrides(
            primaryColorHex = prefs.getString(KEY_PRIMARY_COLOR, null),
            accentColorHex = prefs.getString(KEY_ACCENT_COLOR, null),
            backgroundUri = prefs.getString(KEY_BACKGROUND_URI, null),
            backgroundDimAlpha = runCatching { prefs.getFloat(KEY_BACKGROUND_DIM_ALPHA, -1f) }.getOrNull()?.takeIf { it >= 0f },
            backgroundContentScale = prefs.getString(KEY_BACKGROUND_CONTENT_SCALE, null),
            backgroundBlurRadius = prefs.getInt(KEY_BACKGROUND_BLUR_RADIUS, 0)
        )
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LOCAL_DEBUG = "local_debug"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_HTTP_LOG_LEVEL = "http_log_level"
        private const val KEY_PRIMARY_COLOR = "primary_color_hex"
        private const val KEY_ACCENT_COLOR = "accent_color_hex"
        private const val KEY_BACKGROUND_URI = "background_image_uri"
        private const val KEY_BACKGROUND_DIM_ALPHA = "background_dim_alpha"
        private const val KEY_BACKGROUND_CONTENT_SCALE = "background_content_scale"
        private const val KEY_BACKGROUND_BLUR_RADIUS = "background_blur_radius"
    }

    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)
    val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()
    
    fun saveServerUrl(url: String?) {
        val editor = prefs.edit()
        if (url.isNullOrBlank()) {
            editor.remove(KEY_SERVER_URL)
        } else {
            editor.putString(KEY_SERVER_URL, url)
        }
        editor.apply()
    }

    fun isLocalDebug(): Boolean = prefs.getBoolean(KEY_LOCAL_DEBUG, false)
    val localDebugFlow: StateFlow<Boolean> = _localDebugFlow.asStateFlow()
    
    fun setLocalDebug(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_DEBUG, enabled).apply()
    }

    fun isSetupCompleted(): Boolean = prefs.getBoolean(KEY_SETUP_COMPLETED, false)

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getHttpLogLevel(): HttpLoggingInterceptor.Level {
        val levelStr = prefs.getString(KEY_HTTP_LOG_LEVEL, HttpLoggingInterceptor.Level.BASIC.name)
        val stored = prefs.getString(KEY_HTTP_LOG_LEVEL, null)
        return when (stored) {
            HttpLoggingInterceptor.Level.NONE.name -> HttpLoggingInterceptor.Level.NONE
            HttpLoggingInterceptor.Level.BASIC.name -> HttpLoggingInterceptor.Level.BASIC
            HttpLoggingInterceptor.Level.BODY.name -> HttpLoggingInterceptor.Level.BODY
            else -> if (isLocalDebug()) HttpLoggingInterceptor.Level.NONE else HttpLoggingInterceptor.Level.BODY
        }
    }

    fun setHttpLogLevel(level: HttpLoggingInterceptor.Level) {
        prefs.edit().putString(KEY_HTTP_LOG_LEVEL, level.name).apply()
    }

    // Theme overrides
    val themeOverridesFlow: StateFlow<ThemeOverrides> = _themeOverridesFlow

    fun getPrimaryColorHex(): String? = prefs.getString(KEY_PRIMARY_COLOR, null)
    fun setPrimaryColorHex(hex: String?) {
        val editor = prefs.edit()
        if (hex.isNullOrBlank()) editor.remove(KEY_PRIMARY_COLOR) else editor.putString(KEY_PRIMARY_COLOR, hex)
        editor.apply()
    }

    fun getBackgroundUri(): String? = prefs.getString(KEY_BACKGROUND_URI, null)
    fun setBackgroundUri(uri: String?) {
        val editor = prefs.edit()
        if (uri.isNullOrBlank()) editor.remove(KEY_BACKGROUND_URI) else editor.putString(KEY_BACKGROUND_URI, uri)
        editor.apply()
    }

    fun getAccentColorHex(): String? = prefs.getString(KEY_ACCENT_COLOR, null)
    fun setAccentColorHex(hex: String?) {
        val editor = prefs.edit()
        if (hex.isNullOrBlank()) editor.remove(KEY_ACCENT_COLOR) else editor.putString(KEY_ACCENT_COLOR, hex)
        editor.apply()
    }

    fun getBackgroundDimAlpha(): Float = prefs.getFloat(KEY_BACKGROUND_DIM_ALPHA, 0.25f)
    fun setBackgroundDimAlpha(alpha: Float) {
        val editor = prefs.edit()
        editor.putFloat(KEY_BACKGROUND_DIM_ALPHA, alpha.coerceIn(0f, 1f))
        editor.apply()
    }

    fun getBackgroundContentScale(): String = prefs.getString(KEY_BACKGROUND_CONTENT_SCALE, "Crop") ?: "Crop"
    fun setBackgroundContentScale(scale: String) {
        val editor = prefs.edit()
        editor.putString(KEY_BACKGROUND_CONTENT_SCALE, scale)
        editor.apply()
    }

    fun getBackgroundBlurRadius(): Int = prefs.getInt(KEY_BACKGROUND_BLUR_RADIUS, 0)
    fun setBackgroundBlurRadius(radius: Int) {
        val editor = prefs.edit()
        editor.putInt(KEY_BACKGROUND_BLUR_RADIUS, radius.coerceIn(0, 25))
        editor.apply()
    }
}