package com.equiptrack.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onNavigateBack: () -> Unit,
    onConfigSaved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var serverUrl by remember { mutableStateOf(viewModel.getServerUrl()) }
    var isLocalDebug by remember { mutableStateOf(viewModel.isLocalDebug()) }
    var showLogMenu by remember { mutableStateOf(false) }
    var logLevel by remember { mutableStateOf(viewModel.getHttpLogLevel()) }
    var testMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器配置") },
                navigationIcon = {
                    AnimatedTextButton(onClick = onNavigateBack) { Text("返回") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instruction
            Text(
                text = "请配置远程服务器同步地址。局域网测试请使用本机IP (例如 192.168.x.x:3000)。\n开启本地调试模式将使用设备内建数据，不连接服务器。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        text = "开启后将使用本地模拟数据，暂停服务器同步",
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
            
            Divider()

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(if (isLocalDebug) "服务器地址 (本地模式已禁用此项)" else "服务器同步地址") },
                placeholder = { Text("例如 http://192.168.2.119:3000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLocalDebug
            )

            // HTTP Log Level Selection
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedButton(
                    onClick = {
                        viewModel.saveServerUrl(serverUrl)
                        onConfigSaved()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存并继续")
                }

                AnimatedOutlinedButton(
                    onClick = {
                        if (serverUrl.isBlank()) {
                            testMessage = "请先输入服务器地址"
                            return@AnimatedOutlinedButton
                        }
                        testMessage = "正在测试连接..."
                        viewModel.testConnection(serverUrl) { ok, err ->
                            testMessage = if (ok) "✓ 连接正常" else "✗ 连接失败" + (err?.let { ": $it" } ?: "")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLocalDebug && serverUrl.isNotBlank()
                ) {
                    Text("测试连接")
                }
            }
            
            testMessage?.let {
                Text(
                    text = it,
                    color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}