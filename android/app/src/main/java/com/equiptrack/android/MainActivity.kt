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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var localDebugSeeder: LocalDebugSeeder
    @Inject lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RootApp(
                settingsRepository = settingsRepository,
                localDebugSeeder = localDebugSeeder
            )
        }
    }
}

@Composable
fun RootApp(
    settingsRepository: SettingsRepository,
    localDebugSeeder: LocalDebugSeeder
) {
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
        LaunchedEffect(Unit) {
            localDebugSeeder.seedIfLocalDebug()
        }

        var showBackground by remember(overrides.backgroundUri) { mutableStateOf(false) }
        LaunchedEffect(overrides.backgroundUri) {
            showBackground = false
            if (overrides.backgroundUri != null) {
                withFrameNanos { }
                showBackground = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showBackground) {
                val uri = overrides.backgroundUri
                uri?.let {
                    var modifier = Modifier.fillMaxSize()
                    if ((overrides.backgroundBlurRadius ?: 0) > 0) {
                        modifier = modifier.blur((overrides.backgroundBlurRadius ?: 0).dp)
                    }
                    
                    AsyncImage(
                        model = it,
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

                if (uri != null) {
                    val dim = (overrides.backgroundDimAlpha ?: 0.25f).coerceIn(0f, 1f)
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dim))
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (showBackground && overrides.backgroundUri != null) Color.Transparent else MaterialTheme.colorScheme.background
            ) {
                EquipTrackNavigation()
            }
        }
    }
}
