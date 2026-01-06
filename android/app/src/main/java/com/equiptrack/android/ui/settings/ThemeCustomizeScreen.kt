package com.equiptrack.android.ui.settings

import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.equiptrack.android.ui.components.AnimatedIconButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.rememberToastState
import com.equiptrack.android.ui.navigation.NavigationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val overrides by repo.themeOverridesFlow.collectAsState()

    var colorText by remember(overrides.primaryColorHex) { mutableStateOf(TextFieldValue(overrides.primaryColorHex ?: "#006493")) }
    var previewColor by remember(overrides.primaryColorHex) { mutableStateOf(parseHexColorOrNull(overrides.primaryColorHex ?: "#006493") ?: Color(0xFF006493)) }
    
    var accentText by remember(overrides.accentColorHex) { mutableStateOf(TextFieldValue(overrides.accentColorHex ?: "#006493")) }
    var previewAccent by remember(overrides.accentColorHex) { mutableStateOf(parseHexColorOrNull(overrides.accentColorHex ?: "#006493") ?: Color(0xFF006493)) }
    
    var bgUri by remember(overrides.backgroundUri) { mutableStateOf(overrides.backgroundUri) }
    var dimAlpha by remember(overrides.backgroundDimAlpha) { mutableStateOf(overrides.backgroundDimAlpha ?: 0.25f) }
    var contentScale by remember(overrides.backgroundContentScale) { mutableStateOf(overrides.backgroundContentScale ?: "Crop") }
    var blurRadius by remember(overrides.backgroundBlurRadius) { mutableStateOf((overrides.backgroundBlurRadius ?: 0).toFloat()) }
    var cardOpacity by remember(overrides.cardOpacity) { mutableStateOf(overrides.cardOpacity ?: 1f) }
    var isCompactList by remember { mutableStateOf(repo.isEquipmentListCompact()) }
    var cardMaterial by remember(overrides.cardMaterial) { mutableStateOf(overrides.cardMaterial ?: repo.getCardMaterial()) }
    var noiseEnabled by remember(overrides.noiseEnabled) { mutableStateOf(overrides.noiseEnabled ?: repo.isNoiseEnabled()) }
    var cornerRadius by remember(overrides.cornerRadius) { mutableStateOf(overrides.cornerRadius ?: repo.getCornerRadius()) }
    var dynamicColorEnabled by remember(overrides.dynamicColorEnabled) { mutableStateOf(overrides.dynamicColorEnabled ?: repo.isDynamicColorEnabled()) }
    var darkModeStrategy by remember(overrides.darkModeStrategy) { mutableStateOf(overrides.darkModeStrategy ?: repo.getDarkModeStrategy()) }
    var equipmentImageRatio by remember(overrides.equipmentImageRatio) { mutableStateOf(overrides.equipmentImageRatio ?: repo.getEquipmentImageRatio()) }
    var listAnimationType by remember(overrides.listAnimationType) { mutableStateOf(overrides.listAnimationType ?: repo.getListAnimationType()) }
    var hapticEnabled by remember(overrides.hapticEnabled) { mutableStateOf(overrides.hapticEnabled ?: repo.isHapticEnabled()) }
    var hapticIntensity by remember(overrides.hapticIntensity) { mutableStateOf(overrides.hapticIntensity ?: repo.getHapticIntensity()) }
    var confettiEnabled by remember(overrides.confettiEnabled) { mutableStateOf(overrides.confettiEnabled ?: repo.isConfettiEnabled()) }
    var tagStyle by remember(overrides.tagStyle) { mutableStateOf(overrides.tagStyle ?: repo.getTagStyle()) }
    var lowPerformanceMode by remember(overrides.lowPerformanceMode) { mutableStateOf(overrides.lowPerformanceMode ?: repo.isLowPerformanceMode()) }
    var themeMode by remember(overrides.themeMode) { mutableStateOf(overrides.themeMode ?: repo.getThemeMode()) }

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
            repo.setBackgroundUri(uri.toString())
        }
    }

    val toastState = rememberToastState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题与背景", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    AnimatedIconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    AnimatedTextButton(onClick = {
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
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Wallpaper Preview & Settings
            item {
                SettingsSection(title = "背景壁纸", icon = Icons.Outlined.Wallpaper) {
                    // Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { pickImage.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (bgUri != null) {
                            AsyncImage(
                                model = bgUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (blurRadius > 0) it.blur(blurRadius.dp) else it },
                                contentScale = when (contentScale) {
                                    "Fit" -> ContentScale.Fit
                                    "FillBounds" -> ContentScale.FillBounds
                                    else -> ContentScale.Crop
                                }
                            )
                            // Dim Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = dimAlpha))
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("点击选择图片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        // Edit Controls Overlay
                        if (bgUri != null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { bgUri = null; repo.setBackgroundUri(null) },
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                                SmallFloatingActionButton(
                                    onClick = { pickImage.launch(arrayOf("image/*")) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Change")
                                }
                            }
                        }
                    }

                    if (bgUri != null) {
                        SettingsSlider(
                            label = "遮罩浓度",
                            value = dimAlpha,
                            valueRange = 0f..0.9f,
                            displayValue = "${(dimAlpha * 100).toInt()}%",
                            onValueChange = { dimAlpha = it },
                            onValueChangeFinished = { repo.setBackgroundDimAlpha(dimAlpha) }
                        )
                        SettingsSlider(
                            label = "磨砂程度",
                            value = blurRadius,
                            valueRange = 0f..25f,
                            displayValue = "${blurRadius.toInt()}dp",
                            onValueChange = { blurRadius = it },
                            onValueChangeFinished = { repo.setBackgroundBlurRadius(blurRadius.toInt()) }
                        )
                        SettingsChoice(
                            label = "填充模式",
                            options = listOf("Crop" to "裁剪填充", "Fit" to "适应屏幕", "FillBounds" to "拉伸填充"),
                            selectedKey = contentScale,
                            onSelectionChanged = { 
                                contentScale = it
                                repo.setBackgroundContentScale(it)
                            }
                        )
                    }
                }
            }

            // 2. Color & Theme
            item {
                SettingsSection(title = "色彩与主题", icon = Icons.Outlined.Palette) {
                    Text("灵感预设", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                                repo.setPrimaryColorHex(preset.primary)
                                repo.setAccentColorHex(preset.accent)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsColorPicker(
                        title = "主色调",
                        colorText = colorText,
                        onColorChange = { 
                            colorText = it
                            parseHexColorOrNull(it.text)?.let { c -> previewColor = c }
                        },
                        previewColor = previewColor,
                        presets = listOf("#006493", "#006C4C", "#A33E00", "#6750A4", "#9C27B0", "#B71C1C")
                    )

                    SettingsColorPicker(
                        title = "强调色",
                        colorText = accentText,
                        onColorChange = { 
                            accentText = it
                            parseHexColorOrNull(it.text)?.let { c -> previewAccent = c }
                        },
                        previewColor = previewAccent,
                        presets = listOf("#006493", "#4CAF50", "#FF9800", "#EADDFF", "#FF4081", "#00BCD4")
                    )

                    SettingsChoice(
                        label = "外观模式",
                        options = listOf("System" to "跟随系统", "Light" to "浅色", "Dark" to "深色"),
                        selectedKey = themeMode,
                        onSelectionChanged = { 
                            themeMode = it
                            repo.setThemeMode(it)
                        }
                    )
                    
                    if (themeMode == "Dark" || (themeMode == "System")) {
                        SettingsChoice(
                            label = "深色风格",
                            options = listOf("DarkGrey" to "深灰", "TrueBlack" to "纯黑"),
                            selectedKey = darkModeStrategy,
                            onSelectionChanged = { 
                                darkModeStrategy = it
                                repo.setDarkModeStrategy(it)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("动态取色", style = MaterialTheme.typography.bodyLarge)
                            Text("跟随壁纸生成主题色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { 
                                dynamicColorEnabled = it
                                repo.setDynamicColorEnabled(it)
                            }
                        )
                    }
                }
            }

            // 3. UI Customization
            item {
                SettingsSection(title = "界面外观", icon = Icons.Outlined.DashboardCustomize) {
                    SettingsSlider(
                        label = "卡片透明度",
                        value = cardOpacity,
                        valueRange = 0.3f..1f,
                        displayValue = "${(cardOpacity * 100).toInt()}%",
                        onValueChange = { cardOpacity = it },
                        onValueChangeFinished = { repo.setCardOpacity(cardOpacity) }
                    )

                    SettingsChoice(
                        label = "卡片材质",
                        options = listOf("Solid" to "实色", "Glass" to "磨砂", "Outline" to "描边"),
                        selectedKey = cardMaterial,
                        onSelectionChanged = { 
                            cardMaterial = it
                            repo.setCardMaterial(it)
                        }
                    )

                    SettingsSlider(
                        label = "圆角大小",
                        value = cornerRadius,
                        valueRange = 0f..32f,
                        displayValue = "${cornerRadius.toInt()}dp",
                        onValueChange = { cornerRadius = it },
                        onValueChangeFinished = { repo.setCornerRadius(cornerRadius) }
                    )
                    
                    SettingsChoice(
                        label = "噪点纹理",
                        options = listOf("true" to "开启", "false" to "关闭"),
                        selectedKey = noiseEnabled.toString(),
                        onSelectionChanged = { 
                            noiseEnabled = it.toBoolean()
                            repo.setNoiseEnabled(it.toBoolean())
                        }
                    )
                }
            }

            // 4. Content Layout
            item {
                SettingsSection(title = "布局与展示", icon = Icons.Outlined.ViewQuilt) {
                    SettingsChoice(
                        label = "设备图片比例",
                        options = listOf("Square" to "方形", "Landscape" to "横版", "Portrait" to "竖版"),
                        selectedKey = equipmentImageRatio,
                        onSelectionChanged = { 
                            equipmentImageRatio = it
                            repo.setEquipmentImageRatio(it)
                        }
                    )

                    SettingsChoice(
                        label = "列表密度",
                        options = listOf("false" to "舒适", "true" to "紧凑"),
                        selectedKey = isCompactList.toString(),
                        onSelectionChanged = { 
                            isCompactList = it.toBoolean()
                            repo.setEquipmentListCompact(it.toBoolean())
                        }
                    )

                    SettingsChoice(
                        label = "标签样式",
                        options = listOf("Filled" to "实心", "Outlined" to "描边", "Light" to "浅色"),
                        selectedKey = tagStyle,
                        onSelectionChanged = { 
                            tagStyle = it
                            repo.setTagStyle(it)
                        }
                    )
                    
                     SettingsChoice(
                        label = "列表动画",
                        options = listOf("Scale" to "缩放", "Slide" to "滑动", "Fade" to "淡入"),
                        selectedKey = listAnimationType,
                        onSelectionChanged = { 
                            listAnimationType = it
                            repo.setListAnimationType(it)
                        }
                    )
                }
            }

            // 5. Interaction & Performance
            item {
                SettingsSection(title = "交互与性能", icon = Icons.Outlined.Speed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("震动反馈", style = MaterialTheme.typography.bodyLarge)
                            Text("借还操作时的触感反馈", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { 
                                hapticEnabled = it
                                repo.setHapticEnabled(it)
                            }
                        )
                    }

                    if (hapticEnabled) {
                        SettingsSlider(
                            label = "震动强度",
                            value = hapticIntensity,
                            valueRange = 0f..1f,
                            displayValue = "${(hapticIntensity * 100).toInt()}%",
                            onValueChange = { hapticIntensity = it },
                            onValueChangeFinished = { repo.setHapticIntensity(hapticIntensity) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("低性能模式", style = MaterialTheme.typography.bodyLarge)
                            Text("关闭模糊和复杂动画以提高流畅度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = lowPerformanceMode,
                            onCheckedChange = { 
                                lowPerformanceMode = it
                                repo.setLowPerformanceMode(it)
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// --- Helper Composables ---

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)), 
                    RoundedCornerShape(20.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(displayValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsChoice(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelectionChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (key, labelText) ->
                FilterChip(
                    selected = selectedKey == key,
                    onClick = { onSelectionChanged(key) },
                    label = { Text(labelText) },
                    leadingIcon = if (selectedKey == key) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f) // No border when selected, just fill
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsColorPicker(
    title: String,
    colorText: TextFieldValue,
    onColorChange: (TextFieldValue) -> Unit,
    previewColor: Color,
    presets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(previewColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { hex ->
                val color = parseHexColorOrNull(hex) ?: Color.Transparent
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorChange(TextFieldValue(hex)) }
                        .border(
                            if (colorText.text.equals(hex, ignoreCase = true)) 2.dp else 0.dp,
                            if (colorText.text.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        )
                )
            }
        }
        
        OutlinedTextField(
            value = colorText,
            onValueChange = onColorChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            label = { Text("HEX Color", style = MaterialTheme.typography.bodySmall) },
            isError = validHexOrNull(colorText.text) == null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePresetChip(preset: ThemePreset, onClick: () -> Unit) {
    val primaryColor = parseHexColorOrNull(preset.primary) ?: Color.Gray
    val accentColor = parseHexColorOrNull(preset.accent) ?: Color.Gray
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.horizontalGradient(listOf(primaryColor, accentColor))))
            
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(preset.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(preset.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

private fun validHexOrNull(text: String): String? {
    val regex = Regex("^#([0-9a-fA-F]{6})$")
    return if (regex.matches(text)) text else null
}

private fun parseHexColorOrNull(text: String): Color? {
    return try { Color(android.graphics.Color.parseColor(text)) } catch (_: Exception) { null }
}
