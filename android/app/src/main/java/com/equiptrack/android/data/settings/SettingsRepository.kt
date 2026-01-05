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
    val backgroundBlurRadius: Int? = null,
    val cardOpacity: Float? = null,
    val cardMaterial: String? = null,
    val noiseEnabled: Boolean? = null,
    val cornerRadius: Float? = null,
    val dynamicColorEnabled: Boolean? = null,
    val darkModeStrategy: String? = null,
    val equipmentImageRatio: String? = null,
    val listAnimationType: String? = null,
    val hapticEnabled: Boolean? = null,
    val confettiEnabled: Boolean? = null,
    val tagStyle: String? = null,
    val lowPerformanceMode: Boolean? = null,
    val themeMode: String? = null
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
            backgroundBlurRadius = null,
            cardOpacity = null,
            cardMaterial = null,
            noiseEnabled = null,
            cornerRadius = null,
            dynamicColorEnabled = null,
            darkModeStrategy = null,
            equipmentImageRatio = null,
            listAnimationType = null,
            hapticEnabled = null,
            confettiEnabled = null,
            tagStyle = null,
            lowPerformanceMode = null,
            themeMode = null
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
            KEY_PRIMARY_COLOR,
            KEY_ACCENT_COLOR,
            KEY_BACKGROUND_URI,
            KEY_BACKGROUND_DIM_ALPHA,
            KEY_BACKGROUND_CONTENT_SCALE,
            KEY_BACKGROUND_BLUR_RADIUS,
            KEY_CARD_OPACITY,
            KEY_CARD_MATERIAL,
            KEY_NOISE_ENABLED,
            KEY_CORNER_RADIUS,
            KEY_DYNAMIC_COLOR_ENABLED,
            KEY_DARK_MODE_STRATEGY,
            KEY_EQUIPMENT_IMAGE_RATIO,
            KEY_LIST_ANIMATION_TYPE,
            KEY_HAPTIC_ENABLED,
            KEY_CONFETTI_ENABLED,
            KEY_TAG_STYLE,
            KEY_LOW_PERFORMANCE_MODE,
            KEY_THEME_MODE -> {
                _themeOverridesFlow.value = ThemeOverrides(
                    primaryColorHex = shared.getString(KEY_PRIMARY_COLOR, null),
                    accentColorHex = shared.getString(KEY_ACCENT_COLOR, null),
                    backgroundUri = shared.getString(KEY_BACKGROUND_URI, null),
                    backgroundDimAlpha = runCatching { shared.getFloat(KEY_BACKGROUND_DIM_ALPHA, -1f) }.getOrNull()?.takeIf { it >= 0f },
                    backgroundContentScale = shared.getString(KEY_BACKGROUND_CONTENT_SCALE, null),
                    backgroundBlurRadius = shared.getInt(KEY_BACKGROUND_BLUR_RADIUS, 0),
                    cardOpacity = runCatching { shared.getFloat(KEY_CARD_OPACITY, -1f) }.getOrNull()?.takeIf { it in 0f..1f },
                    cardMaterial = shared.getString(KEY_CARD_MATERIAL, null),
                    noiseEnabled = runCatching { shared.getBoolean(KEY_NOISE_ENABLED, false) }.getOrNull(),
                    cornerRadius = runCatching { shared.getFloat(KEY_CORNER_RADIUS, -1f) }.getOrNull()?.takeIf { it >= 0f },
                    dynamicColorEnabled = runCatching { shared.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true) }.getOrNull(),
                    darkModeStrategy = shared.getString(KEY_DARK_MODE_STRATEGY, null),
                    equipmentImageRatio = shared.getString(KEY_EQUIPMENT_IMAGE_RATIO, null),
                    listAnimationType = shared.getString(KEY_LIST_ANIMATION_TYPE, null),
                    hapticEnabled = runCatching { shared.getBoolean(KEY_HAPTIC_ENABLED, true) }.getOrNull(),
                    confettiEnabled = runCatching { shared.getBoolean(KEY_CONFETTI_ENABLED, false) }.getOrNull(),
                    tagStyle = shared.getString(KEY_TAG_STYLE, null),
                    lowPerformanceMode = runCatching { shared.getBoolean(KEY_LOW_PERFORMANCE_MODE, false) }.getOrNull(),
                    themeMode = shared.getString(KEY_THEME_MODE, null)
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
            backgroundBlurRadius = prefs.getInt(KEY_BACKGROUND_BLUR_RADIUS, 0),
            cardOpacity = runCatching { prefs.getFloat(KEY_CARD_OPACITY, -1f) }.getOrNull()?.takeIf { it in 0f..1f },
            cardMaterial = prefs.getString(KEY_CARD_MATERIAL, null),
            noiseEnabled = runCatching { prefs.getBoolean(KEY_NOISE_ENABLED, false) }.getOrNull(),
            cornerRadius = runCatching { prefs.getFloat(KEY_CORNER_RADIUS, -1f) }.getOrNull()?.takeIf { it >= 0f },
            dynamicColorEnabled = runCatching { prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true) }.getOrNull(),
            darkModeStrategy = prefs.getString(KEY_DARK_MODE_STRATEGY, null),
            equipmentImageRatio = prefs.getString(KEY_EQUIPMENT_IMAGE_RATIO, null),
            listAnimationType = prefs.getString(KEY_LIST_ANIMATION_TYPE, null),
            hapticEnabled = runCatching { prefs.getBoolean(KEY_HAPTIC_ENABLED, true) }.getOrNull(),
            confettiEnabled = runCatching { prefs.getBoolean(KEY_CONFETTI_ENABLED, false) }.getOrNull(),
            tagStyle = prefs.getString(KEY_TAG_STYLE, null),
            lowPerformanceMode = runCatching { prefs.getBoolean(KEY_LOW_PERFORMANCE_MODE, false) }.getOrNull(),
            themeMode = prefs.getString(KEY_THEME_MODE, null)
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
        private const val KEY_CARD_OPACITY = "card_opacity"
        private const val KEY_EQUIPMENT_LIST_COMPACT = "equipment_list_compact"
        private const val KEY_CARD_MATERIAL = "card_material"
        private const val KEY_NOISE_ENABLED = "noise_enabled"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled"
        private const val KEY_DARK_MODE_STRATEGY = "dark_mode_strategy"
        private const val KEY_EQUIPMENT_IMAGE_RATIO = "equipment_image_ratio"
        private const val KEY_LIST_ANIMATION_TYPE = "list_animation_type"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_CONFETTI_ENABLED = "confetti_enabled"
        private const val KEY_TAG_STYLE = "tag_style"
        private const val KEY_LOW_PERFORMANCE_MODE = "low_performance_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFICATION_SERVICE_ENABLED = "notification_service_enabled"
        
        // Session Backup Keys
        private const val KEY_BACKUP_AUTH_TOKEN = "backup_auth_token"
        private const val KEY_BACKUP_USER_ID = "backup_user_id"
        private const val KEY_BACKUP_USER_NAME = "backup_user_name"
        private const val KEY_BACKUP_USER_ROLE = "backup_user_role"
        private const val KEY_BACKUP_USER_CONTACT = "backup_user_contact"
        private const val KEY_BACKUP_USER_DEPT_ID = "backup_user_dept_id"
        private const val KEY_BACKUP_USER_DEPT_NAME = "backup_user_dept_name"
    }

    fun isNotificationServiceEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_SERVICE_ENABLED, false)
    fun setNotificationServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SERVICE_ENABLED, enabled).apply()
    }
    
    // Session Backup Methods
    fun backupSession(
        token: String?,
        userId: String?,
        userName: String?,
        userRole: String?,
        userContact: String?,
        deptId: String?,
        deptName: String?
    ) {
        val editor = prefs.edit()
        editor.putString(KEY_BACKUP_AUTH_TOKEN, token)
        editor.putString(KEY_BACKUP_USER_ID, userId)
        editor.putString(KEY_BACKUP_USER_NAME, userName)
        editor.putString(KEY_BACKUP_USER_ROLE, userRole)
        editor.putString(KEY_BACKUP_USER_CONTACT, userContact)
        editor.putString(KEY_BACKUP_USER_DEPT_ID, deptId)
        editor.putString(KEY_BACKUP_USER_DEPT_NAME, deptName)
        editor.apply()
    }

    fun getBackupSession(): Map<String, String?> {
        return mapOf(
            "token" to prefs.getString(KEY_BACKUP_AUTH_TOKEN, null),
            "userId" to prefs.getString(KEY_BACKUP_USER_ID, null),
            "userName" to prefs.getString(KEY_BACKUP_USER_NAME, null),
            "userRole" to prefs.getString(KEY_BACKUP_USER_ROLE, null),
            "userContact" to prefs.getString(KEY_BACKUP_USER_CONTACT, null),
            "deptId" to prefs.getString(KEY_BACKUP_USER_DEPT_ID, null),
            "deptName" to prefs.getString(KEY_BACKUP_USER_DEPT_NAME, null)
        )
    }

    fun clearBackupSession() {
        val editor = prefs.edit()
        editor.remove(KEY_BACKUP_AUTH_TOKEN)
        editor.remove(KEY_BACKUP_USER_ID)
        editor.remove(KEY_BACKUP_USER_NAME)
        editor.remove(KEY_BACKUP_USER_ROLE)
        editor.remove(KEY_BACKUP_USER_CONTACT)
        editor.remove(KEY_BACKUP_USER_DEPT_ID)
        editor.remove(KEY_BACKUP_USER_DEPT_NAME)
        editor.apply()
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

    fun getCardOpacity(): Float = prefs.getFloat(KEY_CARD_OPACITY, 1f)
    fun setCardOpacity(opacity: Float) {
        val editor = prefs.edit()
        editor.putFloat(KEY_CARD_OPACITY, opacity.coerceIn(0.3f, 1f))
        editor.apply()
    }

    fun isEquipmentListCompact(): Boolean = prefs.getBoolean(KEY_EQUIPMENT_LIST_COMPACT, false)
    fun setEquipmentListCompact(compact: Boolean) {
        prefs.edit().putBoolean(KEY_EQUIPMENT_LIST_COMPACT, compact).apply()
    }

    fun getCardMaterial(): String = prefs.getString(KEY_CARD_MATERIAL, "Solid") ?: "Solid"
    fun setCardMaterial(material: String) {
        prefs.edit().putString(KEY_CARD_MATERIAL, material).apply()
    }

    fun isNoiseEnabled(): Boolean = prefs.getBoolean(KEY_NOISE_ENABLED, false)
    fun setNoiseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOISE_ENABLED, enabled).apply()
    }

    fun getCornerRadius(): Float = prefs.getFloat(KEY_CORNER_RADIUS, 12f)
    fun setCornerRadius(radius: Float) {
        prefs.edit().putFloat(KEY_CORNER_RADIUS, radius.coerceIn(0f, 30f)).apply()
    }

    fun isDynamicColorEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true)
    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled).apply()
    }

    fun getDarkModeStrategy(): String = prefs.getString(KEY_DARK_MODE_STRATEGY, "DarkGrey") ?: "DarkGrey"
    fun setDarkModeStrategy(strategy: String) {
        prefs.edit().putString(KEY_DARK_MODE_STRATEGY, strategy).apply()
    }

    fun getEquipmentImageRatio(): String = prefs.getString(KEY_EQUIPMENT_IMAGE_RATIO, "Square") ?: "Square"
    fun setEquipmentImageRatio(ratio: String) {
        prefs.edit().putString(KEY_EQUIPMENT_IMAGE_RATIO, ratio).apply()
    }

    fun getListAnimationType(): String = prefs.getString(KEY_LIST_ANIMATION_TYPE, "None") ?: "None"
    fun setListAnimationType(type: String) {
        prefs.edit().putString(KEY_LIST_ANIMATION_TYPE, type).apply()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun isConfettiEnabled(): Boolean = prefs.getBoolean(KEY_CONFETTI_ENABLED, false)
    fun setConfettiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFETTI_ENABLED, enabled).apply()
    }

    fun getTagStyle(): String = prefs.getString(KEY_TAG_STYLE, "Solid") ?: "Solid"
    fun setTagStyle(style: String) {
        prefs.edit().putString(KEY_TAG_STYLE, style).apply()
    }

    fun isLowPerformanceMode(): Boolean = prefs.getBoolean(KEY_LOW_PERFORMANCE_MODE, false)
    fun setLowPerformanceMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_PERFORMANCE_MODE, enabled).apply()
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "System") ?: "System"
    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}
