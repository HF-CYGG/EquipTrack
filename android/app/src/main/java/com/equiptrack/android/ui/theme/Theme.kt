package com.equiptrack.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.equiptrack.android.data.settings.ThemeOverrides

private val DarkColorScheme = darkColorScheme(
    primary = Teal,
    secondary = MediumTeal,
    tertiary = BrightGreen,
    background = DarkGray,
    surface = Black,
    onPrimary = White,
    onSecondary = White,
    onTertiary = Black,
    onBackground = White,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    secondary = MediumTeal,
    tertiary = BrightGreen,
    background = LightTeal,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = Black,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

val LocalHapticFeedbackEnabled = compositionLocalOf { true }
val LocalHapticStrength = compositionLocalOf { 1.0f }

@Composable
fun EquipTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    overrides: ThemeOverrides? = null,
    content: @Composable () -> Unit
) {
    val themeMode = overrides?.themeMode ?: "System"
    val isDark = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> darkTheme
    }

    val resolvedDynamicColor = overrides?.dynamicColorEnabled ?: dynamicColor

    var baseScheme = when {
        resolvedDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    if (isDark && overrides?.darkModeStrategy == "TrueBlack") {
        baseScheme = baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    }
    val colorScheme = overrides?.primaryColorHex?.let { hex ->
        val parsed = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
        if (parsed != null) baseScheme.copy(primary = parsed) else baseScheme
    } ?: baseScheme

    // Apply accent override to secondary if provided
    val finalScheme = overrides?.accentColorHex?.let { hex ->
        val parsed = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
        if (parsed != null) colorScheme.copy(secondary = parsed) else colorScheme
    } ?: colorScheme

    // If background URI is present, make the background color transparent to show the image
    val schemeWithBackground = if (!overrides?.backgroundUri.isNullOrEmpty()) {
        finalScheme.copy(background = Color.Transparent)
    } else {
        finalScheme
    }

    val schemeWithCardOpacity = overrides?.cardOpacity?.let { opacity ->
        if (opacity in 0f..1f) {
            schemeWithBackground.copy(
                surface = schemeWithBackground.surface.copy(alpha = opacity),
                surfaceVariant = schemeWithBackground.surfaceVariant.copy(alpha = opacity)
            )
        } else {
            schemeWithBackground
        }
    } ?: schemeWithBackground

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val hapticEnabled = (overrides?.hapticEnabled ?: true) && (overrides?.lowPerformanceMode != true)
    val hapticStrength = overrides?.hapticIntensity ?: 1.0f

    CompositionLocalProvider(
        LocalHapticFeedbackEnabled provides hapticEnabled,
        LocalHapticStrength provides hapticStrength
    ) {
        MaterialTheme(
            colorScheme = schemeWithCardOpacity,
            typography = Typography,
            content = content
        )
    }
}
