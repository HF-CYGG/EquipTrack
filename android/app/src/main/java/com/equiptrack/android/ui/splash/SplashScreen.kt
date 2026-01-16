package com.equiptrack.android.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.navigation.NavigationViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import com.equiptrack.android.ui.components.EquipTrackLogo

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        startAnimation = true
        // Dynamic duration: wait for app readiness + minimal visual time
        // We use a small delay to ensure the user perceives the logo before transition
        delay(800)
        
        val authRepository = viewModel.authRepository
        val settingsRepository = viewModel.settingsRepository
        
        if (authRepository.isLoggedIn()) {
            onNavigateToMain()
        } else if (!settingsRepository.isOnboardingCompleted()) {
            onNavigateToOnboarding()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        FluidSplashBackground()
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Unified App Logo
            EquipTrackLogo(
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "EquipTrack",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun FluidSplashBackground(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "splash_bg_anim")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t"
    )

    Canvas(modifier = modifier.fillMaxSize().blur(100.dp)) {
        val w = size.width
        val h = size.height
        
        // Rotating blobs
        drawCircle(
            color = primary.copy(alpha = 0.4f),
            radius = w * 0.6f,
            center = Offset(w * 0.5f + (w * 0.2f * cos(t * 2 * Math.PI).toFloat()), h * 0.4f + (h * 0.2f * sin(t * 2 * Math.PI).toFloat()))
        )
        
        drawCircle(
            color = secondary.copy(alpha = 0.4f),
            radius = w * 0.5f,
            center = Offset(w * 0.5f - (w * 0.2f * cos(t * 2 * Math.PI).toFloat()), h * 0.6f - (h * 0.2f * sin(t * 2 * Math.PI).toFloat()))
        )
        
        drawCircle(
            color = tertiary.copy(alpha = 0.3f),
            radius = w * 0.7f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}
