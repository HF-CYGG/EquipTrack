package com.equiptrack.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.components.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onNavigateBack: () -> Unit,
    onConfigSaved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    showFluidBackground: Boolean = true
) {
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf(viewModel.getServerUrl()) }
    var isLocalDebug by remember { mutableStateOf(viewModel.isLocalDebug()) }
    var showLogMenu by remember { mutableStateOf(false) }
    var logLevel by remember { mutableStateOf(viewModel.getHttpLogLevel()) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val showAutoStart = remember { viewModel.checkAutoStartPermission(context) }
    var isPollingEnabled by remember { mutableStateOf(viewModel.isNotificationServiceEnabled()) }

    // Observe Navigation Events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SettingsViewModel.NavigationEvent.NavigateToMain -> {
                    onConfigSaved()
                }
                is SettingsViewModel.NavigationEvent.NavigateToLogin -> {
                    onConfigSaved()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isPollingEnabled && isGranted) {
                viewModel.togglePollingService(context, true)
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showFluidBackground) Color.Transparent else MaterialTheme.colorScheme.surface)
    ) {
        if (showFluidBackground) {
            FluidLoginBackground(modifier = Modifier.fillMaxSize())
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "服务器配置", 
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Instruction
                Text(
                    text = "请配置远程服务器同步地址。局域网测试请使用本机IP (例如 192.168.x.x:3000)。\n开启本地调试模式将使用设备内建数据，不连接服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // 1. 服务设置
                ConfigSection(title = "服务设置") {
                    // Auto Start Permission Guide
                    if (showAutoStart) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "后台运行权限优化",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "为了确保通知功能在国产手机（小米/Vivo等）上正常工作，建议手动开启“自启动”权限。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedButton(
                                    onClick = { viewModel.requestAutoStartPermission(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("去设置自启动权限")
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Real-time Polling Service Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用实时通知服务",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "在通知栏显示常驻服务，确保及时收到通知",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPollingEnabled,
                            onCheckedChange = { checked ->
                                isPollingEnabled = checked
                                if (checked) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.togglePollingService(context, true)
                                        }
                                    } else {
                                        viewModel.togglePollingService(context, true)
                                    }
                                } else {
                                    viewModel.togglePollingService(context, false)
                                }
                            }
                        )
                    }
                }

                // 2. 服务器连接
                ConfigSection(title = "服务器连接") {
                    // Local Debug Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用本地调试模式",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "使用本地模拟数据，暂停服务器同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLocalDebug,
                            onCheckedChange = { 
                                isLocalDebug = it 
                                viewModel.setLocalDebug(it)
                            }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Server URL Input
                    ModernTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = if (isLocalDebug) "服务器地址 (本地模式已禁用)" else "服务器同步地址",
                        icon = Icons.Default.Dns,
                        placeholder = { Text("例如 http://192.168.2.119:3000") },
                        enabled = !isLocalDebug
                    )

                    // Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                if (serverUrl.isBlank()) {
                                    testMessage = "请先输入服务器地址"
                                    return@OutlinedButton
                                }
                                isTesting = true
                                testMessage = "正在测试连接..."
                                viewModel.testConnection(serverUrl) { ok, err ->
                                    isTesting = false
                                    testMessage = if (ok) "✓ 连接正常" else "✗ 连接失败" + (err?.let { ": $it" } ?: "")
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLocalDebug && serverUrl.isNotBlank() && !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("测试连接")
                            }
                        }

                        // Save Button
                        GradientButton(
                            text = "保存并继续",
                            onClick = {
                                viewModel.saveServerUrl(serverUrl)
                                onConfigSaved()
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        )
                    }

                    // Test Message Display
                    AnimatedVisibility(visible = testMessage != null) {
                        testMessage?.let { msg ->
                            val isSuccess = msg.contains("✓") || msg.contains("正常")
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) 
                                    else 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (isSuccess) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // 3. 高级选项
                ConfigSection(title = "高级选项") {
                    // Log Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HTTP日志级别", style = MaterialTheme.typography.bodyLarge)
                        Box {
                            AnimatedTextButton(onClick = { showLogMenu = true }) {
                                Text(logLevel.name)
                            }
                            DropdownMenu(
                                expanded = showLogMenu,
                                onDismissRequest = { showLogMenu = false }
                            ) {
                                okhttp3.logging.HttpLoggingInterceptor.Level.values().forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(level.name) },
                                        onClick = {
                                            logLevel = level
                                            viewModel.setHttpLogLevel(level)
                                            showLogMenu = false
                                        },
                                        trailingIcon = if (logLevel == level) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    OutlinedButton(
                        onClick = { viewModel.shareLogs(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("分享错误日志")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.triggerSessionExpiry() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("模拟会话过期 (Debug)")
                    }
                }


                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ConfigSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content
            )
        }
    }
}
