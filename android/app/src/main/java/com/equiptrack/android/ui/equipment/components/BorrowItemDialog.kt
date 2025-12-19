package com.equiptrack.android.ui.equipment.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.equiptrack.android.data.model.Borrower
import com.equiptrack.android.data.model.BorrowRequest
import com.equiptrack.android.data.model.EquipmentItem
import com.equiptrack.android.data.model.User
import com.equiptrack.android.ui.components.AnimatedButton
import com.equiptrack.android.ui.components.AnimatedOutlinedButton
import com.equiptrack.android.ui.components.CameraCapture
import com.equiptrack.android.utils.CameraUtils
import com.equiptrack.android.utils.UrlUtils
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowItemDialog(
    item: EquipmentItem,
    serverUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (BorrowRequest) -> Unit,
    currentUser: User? = null
) {
    val context = LocalContext.current
    
    var isPersonalBorrow by remember { mutableStateOf(true) }
    var borrowerName by remember { mutableStateOf("") }
    var borrowerPhone by remember { mutableStateOf("") }
    var expectedReturnDate by remember { mutableStateOf<Date?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var borrowQuantity by remember { mutableStateOf(1) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    
    var showCamera by remember { mutableStateOf(false) }
    
    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()
    
    // Temporary date holder to combine date and time
    var tempSelectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    
    // Auto-fill logic
    LaunchedEffect(isPersonalBorrow, currentUser) {
        if (isPersonalBorrow && currentUser != null) {
            borrowerName = currentUser.name
            borrowerPhone = currentUser.contact
            nameError = null
            phoneError = null
        } else if (!isPersonalBorrow) {
            borrowerName = ""
            borrowerPhone = ""
        }
    }
    
    // Photo logic
    LaunchedEffect(capturedImageUri) {
        capturedImageUri?.let { uri ->
            val base64 = CameraUtils.imageUriToBase64(context, uri)
            photoBase64 = base64
            photoError = null
        }
    }
    
    if (showCamera) {
        Dialog(
            onDismissRequest = { showCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraCapture(
                onImageCaptured = { uri ->
                    capturedImageUri = uri
                    showCamera = false
                },
                onError = { exception ->
                    photoError = "拍照失败: ${exception.message}"
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        }
    }
    
    // Date Picker Dialog
    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("下一步")
            negativeButton("取消")
        }
    ) {
        datepicker(
            initialDate = java.time.LocalDate.now().plusDays(1),
            title = "选择预期归还日期",
            allowedDateValidator = { it.isAfter(java.time.LocalDate.now().minusDays(1)) }
        ) { date ->
            tempSelectedDate = date
            timeDialogState.show()
        }
    }
    
    // Time Picker Dialog
    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("确定")
            negativeButton("取消")
        }
    ) {
        timepicker(
            title = "选择预期归还时间",
            is24HourClock = true
        ) { time ->
            tempSelectedDate?.let { date ->
                val calendar = Calendar.getInstance()
                calendar.set(date.year, date.monthValue - 1, date.dayOfMonth, time.hour, time.minute)
                expectedReturnDate = calendar.time
                dateError = null
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
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f) // Reduced height from 0.85f
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(20.dp), // Reduced corner radius from 28.dp
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Reduced elevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp) // Reduced padding from 24.dp
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "借用物资",
                            style = MaterialTheme.typography.titleLarge, // Reduced typography
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                .size(28.dp) // Reduced size from 32.dp
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Close", 
                                modifier = Modifier.size(16.dp), // Reduced icon size
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(top = 16.dp, bottom = 0.dp), // Reduced padding
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp), // Reduced padding
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
                    ) {
                        // 1. Item Info Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp), // Reduced padding
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon or Image placeholder
                            Surface(
                                modifier = Modifier.size(48.dp), // Reduced size
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val previewUrl = remember(item.image, serverUrl) {
                                        UrlUtils.resolveImageUrl(serverUrl, item.image)
                                    }
                                    if (!previewUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = previewUrl,
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                                            placeholder = rememberVectorPainter(Icons.Outlined.Image)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp) // Reduced icon size
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "可用库存: ${item.availableQuantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // 2. Quantity Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "借用数量",
                                style = MaterialTheme.typography.labelMedium, // Reduced typography
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilledIconButton(
                                    onClick = { if (borrowQuantity > 1) borrowQuantity-- },
                                    enabled = borrowQuantity > 1,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.size(40.dp) // Reduced size
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
                                }
                                
                                Text(
                                    text = borrowQuantity.toString(),
                                    style = MaterialTheme.typography.headlineSmall, // Reduced typography
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp) // Reduced padding
                                        .widthIn(min = 32.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                FilledIconButton(
                                    onClick = { if (borrowQuantity < item.availableQuantity) borrowQuantity++ },
                                    enabled = borrowQuantity < item.availableQuantity,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.size(40.dp) // Reduced size
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. Borrower Info
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // Reduced spacing
                            // Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isPersonalBorrow = !isPersonalBorrow }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "借用人信息",
                                    style = MaterialTheme.typography.labelMedium, // Reduced typography
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isPersonalBorrow) MaterialTheme.colorScheme.surface else Color.Transparent)
                                            .clickable { isPersonalBorrow = true }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "本人",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPersonalBorrow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (!isPersonalBorrow) MaterialTheme.colorScheme.surface else Color.Transparent)
                                            .clickable { isPersonalBorrow = false }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "他人",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (!isPersonalBorrow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = borrowerName,
                                onValueChange = { borrowerName = it; nameError = null },
                                label = { Text("姓名", style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("借用人姓名", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                isError = nameError != null,
                                supportingText = nameError?.let { { Text(it) } },
                                enabled = !isPersonalBorrow || currentUser == null,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = borrowerPhone,
                                onValueChange = { borrowerPhone = it; phoneError = null },
                                label = { Text("联系电话", style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("手机号码", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = phoneError != null,
                                supportingText = phoneError?.let { { Text(it) } },
                                enabled = !isPersonalBorrow || currentUser == null,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            
                            OutlinedTextField(
                                value = expectedReturnDate?.let { 
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it) 
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("预期归还", style = MaterialTheme.typography.bodySmall) },
                                placeholder = { Text("点击选择日期和时间", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dateDialogState.show() },
                                shape = RoundedCornerShape(12.dp),
                                enabled = false, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                isError = dateError != null,
                                supportingText = dateError?.let { { Text(it) } },
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 4. Photo Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "现场拍照 (可选)",
                                style = MaterialTheme.typography.labelMedium, // Reduced typography
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (capturedImageUri != null && photoBase64 != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp) // Reduced height
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    val bitmap = CameraUtils.base64ToBitmap(photoBase64!!)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        // Delete button overlay
                                        IconButton(
                                            onClick = { capturedImageUri = null; photoBase64 = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Outlined.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else {
                                val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                                val color = MaterialTheme.colorScheme.outlineVariant
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp) // Reduced height
                                        .clip(RoundedCornerShape(12.dp))
                                        .drawBehind {
                                            drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(12.dp.toPx()))
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .clickable { showCamera = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp) // Reduced size
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "点击拍摄照片",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            if (photoError != null) {
                                Text(
                                    text = photoError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp), // Reduced height
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }
                        
                        AnimatedButton(
                            onClick = {
                                var hasError = false
                                if (borrowerName.isBlank()) {
                                    nameError = "请输入姓名"
                                    hasError = true
                                }
                                if (borrowerPhone.isBlank()) {
                                    phoneError = "请输入电话"
                                    hasError = true
                                }
                                if (expectedReturnDate == null) {
                                    dateError = "请选择日期"
                                    hasError = true
                                }
                                
                                if (!hasError) {
                                    val request = BorrowRequest(
                                        borrower = Borrower(
                                            name = borrowerName,
                                            phone = borrowerPhone
                                        ),
                                        expectedReturnDate = expectedReturnDate!!,
                                        photo = photoBase64,
                                        quantity = borrowQuantity
                                    )
                                    onConfirm(request)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp), // Reduced height
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("确认借用")
                        }
                    }
                }
            }
        }
    }
}
