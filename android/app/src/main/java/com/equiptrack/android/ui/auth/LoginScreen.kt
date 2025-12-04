package com.equiptrack.android.ui.auth

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
    
    // Observe login result
    LaunchedEffect(Unit) {
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
                title = { Text("登录") },
                actions = {
                    AnimatedIconButton(onClick = onNavigateToServerConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "服务器设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DynamicBackground(modifier = Modifier.fillMaxSize())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo and title
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "EquipTrack",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "物资管理系统",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Login form
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Contact field
                OutlinedTextField(
                    value = uiState.contact,
                    onValueChange = {
                        viewModel.updateContact(it)
                        viewModel.clearErrors()
                    },
                    label = { Text("联系方式") },
                    placeholder = { Text("请输入手机号或邮箱") },
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
                    singleLine = true
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
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { { Text(it) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        AnimatedIconButton(onClick = { passwordVisible = !passwordVisible }) {
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
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Login button
                AnimatedButton(
                    onClick = { viewModel.login() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("登录")
                }

                // Error message feedback
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedTextButton(onClick = onNavigateToSignup) {
                        Text("申请注册")
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
fun DynamicBackground(modifier: Modifier = Modifier) {
    val color1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val color2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val color3 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    
    val infiniteTransition = rememberInfiniteTransition(label = "background_anim")
    
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )
    
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Circle 1
        drawCircle(
            color = color1,
            radius = width * 0.6f,
            center = Offset(width * 0.2f + (width * 0.6f * offset1), height * 0.2f + (height * 0.1f * offset2))
        )
        
        // Circle 2
        drawCircle(
            color = color2,
            radius = width * 0.5f,
            center = Offset(width * 0.8f - (width * 0.4f * offset2), height * 0.7f - (height * 0.2f * offset1))
        )
        
        // Circle 3
        drawCircle(
            color = color3,
            radius = width * 0.7f,
            center = Offset(width * 0.5f, height * 0.5f)
        )
        
        // Overlay gradient to smooth things out
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    surfaceColor.copy(alpha = 0.7f),
                    surfaceColor.copy(alpha = 0.3f),
                    surfaceColor.copy(alpha = 0.7f)
                )
            )
        )
    }
}