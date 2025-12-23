package com.equiptrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import com.equiptrack.android.data.local.LocalDebugSeeder
import com.equiptrack.android.ui.navigation.EquipTrackNavigation
import com.equiptrack.android.ui.theme.EquipTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import com.equiptrack.android.data.settings.SettingsRepository
import com.equiptrack.android.data.settings.ThemeOverrides
import coil.compose.AsyncImage
import androidx.compose.ui.draw.blur
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var localDebugSeeder: LocalDebugSeeder
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RootApp(
                settingsRepository = settingsRepository,
                localDebugSeeder = localDebugSeeder
            )
        }
    }
}

enum class Stage {
    Shell,
    Full
}

@Composable
fun AppShell() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

object PreloadManager {
    suspend fun preloadCore() {
    }
}

@Composable
fun RootApp(
    settingsRepository: SettingsRepository,
    localDebugSeeder: LocalDebugSeeder
) {
    var stage by remember { mutableStateOf(Stage.Shell) }

    LaunchedEffect(Unit) {
        PreloadManager.preloadCore()
        stage = Stage.Full
    }

    when (stage) {
        Stage.Shell -> {
            EquipTrackTheme {
                AppShell()
            }
        }
        Stage.Full -> {
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
    }
}
