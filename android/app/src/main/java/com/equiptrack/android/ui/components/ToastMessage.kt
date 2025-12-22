package com.equiptrack.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS,
    ERROR
}

data class ToastData(
    val message: String,
    val type: ToastType,
    val duration: Long = 3000L
)

@Composable
fun ToastMessage(
    toastData: ToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(toastData) { mutableStateOf(false) }
    
    LaunchedEffect(toastData) {
        if (toastData != null) {
            visible = true
            delay(toastData.duration)
            visible = false
            delay(300) // Wait for exit animation
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = visible && toastData != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        ),
        modifier = modifier
    ) {
        if (toastData != null) {
            ToastContent(toastData = toastData)
        }
    }
}

@Composable
private fun ToastContent(
    toastData: ToastData,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (toastData.type) {
        ToastType.SUCCESS -> Color(0xFF4CAF50)
        ToastType.ERROR -> Color(0xFFF44336)
    }
    
    val icon: ImageVector = when (toastData.type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle
        ToastType.ERROR -> Icons.Default.Error
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = toastData.message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}

class ToastState {
    private var _currentToast by mutableStateOf<ToastData?>(null)
    val currentToast: ToastData? get() = _currentToast
    
    fun showSuccess(message: String, duration: Long = 3000L) {
        _currentToast = ToastData(message, ToastType.SUCCESS, duration)
    }
    
    fun showError(message: String, duration: Long = 3000L) {
        _currentToast = ToastData(message, ToastType.ERROR, duration)
    }
    
    fun dismiss() {
        _currentToast = null
    }
}


