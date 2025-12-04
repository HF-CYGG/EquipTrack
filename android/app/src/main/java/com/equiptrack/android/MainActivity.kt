package com.equiptrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.equiptrack.android.data.local.LocalDebugSeeder
import javax.inject.Inject
import com.equiptrack.android.ui.navigation.EquipTrackNavigation
import com.equiptrack.android.ui.theme.EquipTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.data.settings.ThemeOverrides
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var localDebugSeeder: LocalDebugSeeder
    @Inject lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Seed local debug users on startup (if local debug is enabled)
        lifecycleScope.launchWhenCreated {
            localDebugSeeder.seedIfLocalDebug()
        }
        
        setContent {
            val overrides: ThemeOverrides by settingsRepository.themeOverridesFlow
                .collectAsState(
                    initial = ThemeOverrides(
                        primaryColorHex = settingsRepository.getPrimaryColorHex(),
                        accentColorHex = settingsRepository.getAccentColorHex(),
                        backgroundUri = settingsRepository.getBackgroundUri(),
                        backgroundDimAlpha = settingsRepository.getBackgroundDimAlpha(),
                        backgroundContentScale = settingsRepository.getBackgroundContentScale(),
                        backgroundBlurRadius = settingsRepository.getBackgroundBlurRadius()
                    )
                )
            EquipTrackTheme(overrides = overrides) {
                Box(modifier = Modifier.fillMaxSize()) {
                    overrides.backgroundUri?.let { uri ->
                        var modifier = Modifier.fillMaxSize()
                        if ((overrides.backgroundBlurRadius ?: 0) > 0) {
                            modifier = modifier.blur((overrides.backgroundBlurRadius ?: 0).dp)
                        }
                        
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = modifier,
                            contentScale = when (overrides.backgroundContentScale) {
                                "Fit" -> ContentScale.Fit
                                "FillBounds" -> ContentScale.FillBounds
                                "Inside" -> ContentScale.Inside
                                else -> ContentScale.Crop
                            },
                        )
                    }
                    // Dim overlay for readability when background is set
                    if (overrides.backgroundUri != null) {
                        val dim = (overrides.backgroundDimAlpha ?: 0.25f).coerceIn(0f, 1f)
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    androidx.compose.ui.Modifier
                                        .background(Color.Black.copy(alpha = dim))
                                )
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (overrides.backgroundUri != null) Color.Transparent else MaterialTheme.colorScheme.background
                    ) {
                        EquipTrackNavigation()
                    }
                }
            }
        }
    }
}