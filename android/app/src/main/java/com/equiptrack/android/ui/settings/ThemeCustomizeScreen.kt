package com.equiptrack.android.ui.settings

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.equiptrack.android.data.settings.ThemeOverrides
import com.equiptrack.android.ui.navigation.NavigationViewModel
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.rememberToastState
import kotlinx.coroutines.launch
import android.content.Intent
import kotlinx.coroutines.delay

data class ThemePreset(
    val name: String,
    val primary: String,
    val accent: String,
    val description: String
)

val themePresets = listOf(
    ThemePreset("默认蓝", "#006493", "#006493", "官方默认配色"),
    ThemePreset("翡翠绿", "#006C4C", "#006C4C", "清新自然"),
    ThemePreset("活力橙", "#A33E00", "#FF8A50", "热情洋溢"),
    ThemePreset("暗夜紫", "#6750A4", "#EADDFF", "神秘优雅"),
    ThemePreset("极客黑", "#1B1B1B", "#4CAF50", "简约极客"),
    ThemePreset("赛博粉", "#D81B60", "#FF4081", "前卫潮流")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizeScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val navVm: NavigationViewModel = hiltViewModel()
    val repo = navVm.settingsRepository
    val overrides by repo.themeOverridesFlow.collectAsState(
        initial = ThemeOverrides(
            primaryColorHex = repo.getPrimaryColorHex(),
            accentColorHex = repo.getAccentColorHex(),
            backgroundUri = repo.getBackgroundUri(),
            backgroundDimAlpha = repo.getBackgroundDimAlpha(),
            backgroundContentScale = repo.getBackgroundContentScale()
        )
    )

    var colorText by remember(overrides.primaryColorHex) { mutableStateOf(TextFieldValue(overrides.primaryColorHex ?: "#006493")) }
    var previewColor by remember(overrides.primaryColorHex) { mutableStateOf(parseHexColorOrNull(overrides.primaryColorHex ?: "#006493") ?: Color(0xFF006493)) }
    
    var accentText by remember(overrides.accentColorHex) { mutableStateOf(TextFieldValue(overrides.accentColorHex ?: "#006493")) }
    var previewAccent by remember(overrides.accentColorHex) { mutableStateOf(parseHexColorOrNull(overrides.accentColorHex ?: "#006493") ?: Color(0xFF006493)) }
    
    var bgUri by remember(overrides.backgroundUri) { mutableStateOf(overrides.backgroundUri) }
    var dimAlpha by remember(overrides.backgroundDimAlpha) { mutableStateOf(overrides.backgroundDimAlpha ?: 0.25f) }
    var contentScale by remember(overrides.backgroundContentScale) { mutableStateOf(overrides.backgroundContentScale ?: "Crop") }
    var blurRadius by remember(overrides.backgroundBlurRadius) { mutableStateOf((overrides.backgroundBlurRadius ?: 0).toFloat()) }

    // Update overrides on change to allow preview
    LaunchedEffect(colorText, accentText, bgUri, dimAlpha, contentScale, blurRadius) {
        // Only apply if valid hex
        val primary = validHexOrNull(colorText.text)
        val accent = validHexOrNull(accentText.text)
        
        if (primary != null) repo.setPrimaryColorHex(primary)
        if (accent != null) repo.setAccentColorHex(accent)
        
        repo.setBackgroundUri(bgUri)
        repo.setBackgroundDimAlpha(dimAlpha)
        repo.setBackgroundContentScale(contentScale)
        repo.setBackgroundBlurRadius(blurRadius.toInt())
    }

    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bgUri = uri.toString()
        }
    }

    val toastState = rememberToastState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("主题与背景", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        AnimatedIconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                    },
                    actions = {
                        AnimatedTextButton(onClick = {
                            // Saving is now implicit/real-time, but we can keep this for confirmation
                            scope.launch {
                                toastState.showSuccess("主题已应用")
                                delay(500) 
                                onSaveSuccess()
                            }
                        }) {
                            Text("完成", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            // Remove snackbarHost
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. 预览区域 (Removed as requested)
                
                // 2. 快速预设
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(" 灵感预设", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(themePresets) { preset ->
                            ThemePresetChip(preset) {
                                colorText = TextFieldValue(preset.primary)
                                previewColor = parseHexColorOrNull(preset.primary)!!
                                accentText = TextFieldValue(preset.accent)
                                previewAccent = parseHexColorOrNull(preset.accent)!!
                            }
                        }
                    }
                }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. 颜色自定义
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(" 色彩定制", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // 主色
                ColorPickerSection(
                    title = "主色调",
                    colorText = colorText,
                    onColorChange = { 
                        colorText = it
                        parseHexColorOrNull(it.text)?.let { c -> previewColor = c }
                    },
                    presets = listOf("#006493", "#006C4C", "#A33E00", "#6750A4", "#9C27B0", "#B71C1C")
                )

                // 强调色
                ColorPickerSection(
                    title = "强调色",
                    colorText = accentText,
                    onColorChange = { 
                        accentText = it
                        parseHexColorOrNull(it.text)?.let { c -> previewAccent = c }
                    },
                    presets = listOf("#006493", "#4CAF50", "#FF9800", "#EADDFF", "#FF4081", "#00BCD4")
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4. 背景设置
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(" 背景壁纸", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (bgUri != null) {
                        AnimatedTextButton(onClick = { bgUri = null }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("清除背景")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnimatedButton(
                            onClick = { pickImage.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (bgUri == null) "选择图片" else "更换图片")
                        }

                        AnimatedVisibility(
                            visible = bgUri != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // 遮罩浓度
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("遮罩浓度", style = MaterialTheme.typography.bodyMedium)
                                        Text("${(dimAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = dimAlpha,
                                        onValueChange = { dimAlpha = it },
                                        valueRange = 0f..0.9f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                // 磨砂效果
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("磨砂程度", style = MaterialTheme.typography.bodyMedium)
                                        Text("${blurRadius.toInt()}dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = blurRadius,
                                        onValueChange = { blurRadius = it },
                                        valueRange = 0f..25f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                // 填充模式
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("填充模式", style = MaterialTheme.typography.bodyMedium)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Crop" to "裁剪填充", "Fit" to "适应屏幕", "FillBounds" to "拉伸填充").forEach { (mode, label) ->
                                            FilterChip(
                                                selected = contentScale == mode,
                                                onClick = { contentScale = mode },
                                                label = { Text(label) },
                                                leadingIcon = if (contentScale == mode) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    ToastMessage(
        toastData = toastState.currentToast,
        onDismiss = { toastState.dismiss() },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)
    )
    }
}

@Composable
fun ThemePresetChip(preset: ThemePreset, onClick: () -> Unit) {
    val primary = parseHexColorOrNull(preset.primary) ?: Color.Gray
    val accent = parseHexColorOrNull(preset.accent) ?: Color.Gray
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            Text(
                preset.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ColorPickerSection(
    title: String,
    colorText: TextFieldValue,
    onColorChange: (TextFieldValue) -> Unit,
    presets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        // 颜色预设
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(presets) { hex ->
                val color = parseHexColorOrNull(hex) ?: Color.Transparent
                val isSelected = colorText.text.equals(hex, ignoreCase = true)
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(TextFieldValue(hex)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 自定义输入
        OutlinedTextField(
            value = colorText,
            onValueChange = onColorChange,
            label = { Text("自定义 HEX 代码") },
            leadingIcon = { Icon(Icons.Default.Colorize, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            singleLine = true,
            isError = validHexOrNull(colorText.text) == null
        )
    }
}

private fun validHexOrNull(text: String): String? {
    val regex = Regex("^#([0-9a-fA-F]{6})$")
    return if (regex.matches(text)) text else null
}

private fun parseHexColorOrNull(text: String): Color? {
    return try { Color(android.graphics.Color.parseColor(text)) } catch (_: Exception) { null }
}