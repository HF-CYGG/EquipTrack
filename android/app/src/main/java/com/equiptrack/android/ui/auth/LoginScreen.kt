package com.equiptrack.android.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equiptrack.android.R
import com.equiptrack.android.utils.NetworkResult
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToServerConfig: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        viewModel.loginResult.collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    onLoginSuccess()
                }
                is NetworkResult.Error -> {
                    // Error is handled in the UI state
                }
                is NetworkResult.Loading -> {
                    // Loading is handled in the UI state
                }
            }
        }
    }
    
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
        Box(modifier = Modifier.fillMaxSize()) {
            FluidLoginBackground(modifier = Modifier.fillMaxSize())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) { 50 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Logo and title
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                 painter = painterResource(id = R.drawable.ic_launcher_foreground), // Assuming this exists, or use default icon
                                 contentDescription = "Logo",
                                 modifier = Modifier.size(60.dp),
                                 tint = MaterialTheme.colorScheme.primary
                             )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "EquipTrack",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "物资管理系统",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Login form
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, delayMillis = 300)) + slideInVertically(tween(1000, delayMillis = 300)) { 100 }
                ) {
                    GlassCard {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "欢迎回来",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Contact field
                            OutlinedTextField(
                                value = uiState.contact,
                                onValueChange = {
                                    viewModel.updateContact(it)
                                    viewModel.clearErrors()
                                },
                                label = { Text("联系方式") },
                                placeholder = { Text("手机号或邮箱") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                isError = uiState.contactError != null,
                                supportingText = uiState.contactError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            
                            // Password field
                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = {
                                    viewModel.updatePassword(it)
                                    viewModel.clearErrors()
                                },
                                label = { Text("密码") },
                                placeholder = { Text("请输入密码") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                isError = uiState.passwordError != null,
                                supportingText = uiState.passwordError?.let { { Text(it) } },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.login()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Login button
                            Button(
                                onClick = { viewModel.login() },
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("登 录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
    
                            // Error message feedback
                            if (uiState.errorMessage != null) {
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            // Signup link
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
                                    Text("立即注册", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FluidLoginBackground(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "background_anim")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t"
    )

    Canvas(modifier = modifier.fillMaxSize().blur(80.dp)) {
        val w = size.width
        val h = size.height
        
        // Blob 1 (Top-Left, Primary)
        drawCircle(
            color = primary.copy(alpha = 0.3f),
            radius = w * 0.5f,
            center = Offset(w * 0.2f + (w * 0.1f * cos(t * 2 * Math.PI).toFloat()), h * 0.2f)
        )
        
        // Blob 2 (Bottom-Right, Tertiary)
        drawCircle(
            color = tertiary.copy(alpha = 0.3f),
            radius = w * 0.6f,
            center = Offset(w * 0.8f - (w * 0.1f * sin(t * 2 * Math.PI).toFloat()), h * 0.8f)
        )
        
        // Blob 3 (Center, Moving)
        drawCircle(
            color = primary.copy(alpha = 0.2f),
            radius = w * 0.4f,
            center = Offset(w * 0.5f + (w * 0.2f * sin(t * Math.PI).toFloat()), h * 0.5f + (h * 0.2f * cos(t * Math.PI).toFloat()))
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        content()
    }
}