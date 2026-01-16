package com.equiptrack.android.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.ui.components.*
import com.equiptrack.android.utils.NetworkResult
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToServerConfig: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    fromSplash: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    if (uiState.showServerConfigPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissServerConfigPrompt() },
            title = { Text("连接失败") },
            text = { Text("无法连接到服务器，请检查您的网络设置或服务器配置。") },
            confirmButton = {
                AnimatedTextButton(
                    onClick = {
                        viewModel.dismissServerConfigPrompt()
                        onNavigateToServerConfig()
                    }
                ) {
                    Text("去配置")
                }
            },
            dismissButton = {
                AnimatedTextButton(onClick = { viewModel.dismissServerConfigPrompt() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    AnimatedIconButton(onClick = onNavigateToServerConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "服务器设置", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val density = LocalDensity.current
            
            // --- Magic Move Calculation ---
            
            // 1. Splash Screen Metrics (Center Aligned)
            // Logo(120) + Spacer(24) + Text(headlineMedium ~36dp height)
            val splashLogoSize = 120.dp
            val splashLogoBottomPadding = 24.dp
            val splashTextApproxHeight = 36.dp 
            val splashContentHeight = splashLogoSize + splashLogoBottomPadding + splashTextApproxHeight
            
            val splashLogoTopY = (screenHeight - splashContentHeight) / 2
            val splashLogoCenterY = splashLogoTopY + (splashLogoSize / 2)
            
            val splashTextTopY = splashLogoTopY + splashLogoSize + splashLogoBottomPadding
            val splashTextCenterY = splashTextTopY + (splashTextApproxHeight / 2)

            // 2. Login Screen Metrics (Top Aligned)
            // Column Start Y (due to padding)
            val loginContentTopPadding = innerPadding.calculateTopPadding() + 24.dp
            
            // Logo Position: loginContentTopPadding + Spacer(32)
            val loginLogoTopY = loginContentTopPadding + 32.dp
            val loginLogoSize = 80.dp
            val loginLogoCenterY = loginLogoTopY + (loginLogoSize / 2)
            
            // Text Position: loginLogoTopY + Logo(80) + Spacer(24)
            val loginTextTopY = loginLogoTopY + loginLogoSize + 24.dp
            val loginTextApproxHeight = 48.dp // displaySmall ~48dp
            val loginTextCenterY = loginTextTopY + (loginTextApproxHeight / 2)

            // 3. Calculate Deltas (Start - End)
            val initialLogoOffsetY = with(density) { (splashLogoCenterY - loginLogoCenterY).toPx() }
            val initialTextOffsetY = with(density) { (splashTextCenterY - loginTextCenterY).toPx() }
            
            // 4. Animatable State
            val logoOffsetY = remember(fromSplash) { Animatable(if (fromSplash) initialLogoOffsetY else 0f) }
            val logoScale = remember(fromSplash) { Animatable(if (fromSplash) 1.5f else 1f) } // 120/80 = 1.5
            
            val textOffsetY = remember(fromSplash) { Animatable(if (fromSplash) initialTextOffsetY else 0f) }
            val textScale = remember(fromSplash) { Animatable(if (fromSplash) 0.75f else 1f) } // ~28/36 ≈ 0.77

            LaunchedEffect(Unit) {
                isVisible = true
                if (fromSplash) {
                    launch {
                        logoOffsetY.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
                    }
                    launch {
                        logoScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                    }
                    launch {
                        textOffsetY.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
                    }
                    launch {
                        textScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                    }
                }
                
                viewModel.loginResult.collect { result ->
                    when (result) {
                        is NetworkResult.Success -> onLoginSuccess()
                        else -> {}
                    }
                }
            }

            FluidLoginBackground(modifier = Modifier.fillMaxSize())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Logo and Title (Always visible, animated)
                Spacer(modifier = Modifier.height(32.dp))
                
                EquipTrackLogo(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(y = with(density) { logoOffsetY.value.toDp() })
                        .scale(logoScale.value)
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "EquipTrack",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset(y = with(density) { textOffsetY.value.toDp() })
                        .scale(textScale.value)
                )
                
                // Subtitle (Fade in)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800, delayMillis = 300)) + slideInVertically(tween(800, delayMillis = 300)) { 50 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "物资管理系统",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Login form (Fade in)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(tween(800, delayMillis = 400)) { 100 }
                ) {
                    GlassCard {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Contact
                            ModernTextField(
                                value = uiState.contact,
                                onValueChange = { viewModel.updateContact(it) },
                                label = "联系方式 (手机/邮箱)",
                                icon = Icons.Default.Person,
                                isError = uiState.contactError != null,
                                errorMessage = uiState.contactError,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            
                            // Password
                            ModernTextField(
                                value = uiState.password,
                                onValueChange = { viewModel.updatePassword(it) },
                                label = "密码",
                                icon = Icons.Default.Lock,
                                isPassword = true,
                                isPasswordVisible = passwordVisible,
                                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                isError = uiState.passwordError != null,
                                errorMessage = uiState.passwordError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login() })
                            )
                            
                            // General Error
                            if (uiState.errorMessage != null) {
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            
                            // Login Button
                            GradientButton(
                                text = "登 录",
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.login()
                                },
                                isLoading = uiState.isLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            )
                            
                            // Signup Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "还没有账号？",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onNavigateToSignup) {
                                    Text(
                                        text = "立即注册",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}






