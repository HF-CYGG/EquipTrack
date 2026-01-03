package com.equiptrack.android.ui.equipment.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.equiptrack.android.data.model.Category
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.ui.equipment.AddEditCategoryDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.AnimatedTextButton
import com.equiptrack.android.ui.components.AnimatedIconButton
import java.io.File
import java.util.*

import com.equiptrack.android.utils.UrlUtils
import com.equiptrack.android.data.settings.SettingsRepository
import androidx.hilt.navigation.compose.hiltViewModel
import com.equiptrack.android.ui.components.ToastMessage
import com.equiptrack.android.ui.components.rememberToastState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: EquipmentItem?,
    categories: List<Category>,
    departmentId: String,
    serverUrl: String,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (EquipmentItem) -> Unit,
    onAddCategory: ((Category) -> Unit)? = null,
    onDeleteCategory: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val toastState = rememberToastState()
    
    // Show toast when error message changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            toastState.showError(it)
        }
    }
    
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var selectedCategoryId by remember { mutableStateOf(item?.categoryId ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var availableQuantity by remember { mutableStateOf(item?.availableQuantity?.toString() ?: "1") }
    var requiresApproval by remember { mutableStateOf(item?.requiresApproval ?: true) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    var expandedCategory by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    
    val isEditing = item != null
    
    // 创建临时文件用于相机拍照
    val tempImageFile = remember {
        File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    }
    
    val tempImageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempImageFile
        )
    }
    
    // 相机拍照启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempImageUri
        }
        showImageOptions = false
    }
    
    // 相册选择启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it }
        showImageOptions = false
    }
    
    if (showAddCategoryDialog && onAddCategory != null) {
        AddEditCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { category ->
                onAddCategory(category)
                showAddCategoryDialog = false
                // Auto select new category if needed
            }
        )
    }
    
    if (showImageOptions) {
        Dialog(
            onDismissRequest = { showImageOptions = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showImageOptions = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "选择图片来源",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimatedTextButton(
                                onClick = { cameraLauncher.launch(tempImageUri) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("拍照")
                            }
                            AnimatedTextButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("从相册选择")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            AnimatedTextButton(onClick = { showImageOptions = false }) {
                                Text("取消")
                            }
                        }
                    }
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .padding(vertical = 16.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEditing) "编辑物资" else "添加新物资",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AnimatedIconButton(
                            onClick = onDismiss
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. 图片区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showImageOptions = true }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null || !item?.image.isNullOrEmpty()) {
                                val displayUrl = if (imageUri != null) imageUri else UrlUtils.resolveImageUrl(serverUrl, item?.image)
                                AsyncImage(
                                    model = displayUrl,
                                    contentDescription = "物资图片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // 遮罩 + 编辑图标
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "更换图片",
                                            modifier = Modifier.padding(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                // 删除按钮
                                AnimatedIconButton(
                                    onClick = { imageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除图片", tint = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("点击上传物资图片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        // 2. 基础信息
                        Text("基础信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; nameError = null },
                            label = { Text("物资名称") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("所属类别") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                isError = categoryError != null,
                                supportingText = categoryError?.let { { Text(it) } },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                 if (onAddCategory != null) {
                                    DropdownMenuItem(
                                        text = { Text("✨ 添加新类别", color = MaterialTheme.colorScheme.primary) },
                                        onClick = { expandedCategory = false; showAddCategoryDialog = true }
                                    )
                                    Divider()
                                }
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(category.name, modifier = Modifier.weight(1f))
                                        },
                                        onClick = { selectedCategoryId = category.id; categoryError = null; expandedCategory = false },
                                        trailingIcon = {
                                            if (onDeleteCategory != null) {
                                                IconButton(
                                                    onClick = {
                                                        onDeleteCategory(category.id)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除类别",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 3. 库存信息
                        Text("库存详情", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                                label = { Text("总数量") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = availableQuantity,
                                onValueChange = { if (it.all { c -> c.isDigit() }) availableQuantity = it },
                                label = { Text("可用数量") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("详细描述 (可选)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "需要审批",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = requiresApproval,
                                onCheckedChange = { requiresApproval = it }
                            )
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Footer Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("取消")
                        }
                        AnimatedButton(
                            onClick = {
                                if (name.isBlank()) {
                                    nameError = "请输入物资名称"
                                    return@AnimatedButton
                                }
                                if (selectedCategoryId.isBlank()) {
                                    categoryError = "请选择类别"
                                    return@AnimatedButton
                                }
                                
                                val newItem = EquipmentItem(
                                    id = item?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    description = description,
                                    categoryId = selectedCategoryId,
                                    departmentId = item?.departmentId ?: departmentId,
                                    quantity = quantity.toIntOrNull() ?: 0,
                                    availableQuantity = availableQuantity.toIntOrNull() ?: 0,
                                    requiresApproval = requiresApproval,
                                    image = imageUri?.toString() ?: item?.image ?: "",
                                    imageFull = if (imageUri != null) null else item?.imageFull
                                )
                                onConfirm(newItem)
                            },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(if (isEditing) "保存修改" else "确认添加")
                        }
                    }
                }
            }

            // Toast message overlay inside Dialog
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ToastMessage(
                    toastData = toastState.currentToast,
                    onDismiss = { toastState.dismiss() }
                )
            }
        }
    }
}
