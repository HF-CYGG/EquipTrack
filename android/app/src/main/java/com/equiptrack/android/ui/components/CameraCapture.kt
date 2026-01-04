package com.equiptrack.android.ui.components

import android.Manifest
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.equiptrack.android.utils.CameraUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isCapturing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Camera permission granted, show camera
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(flashMode)
                                .build()
                            
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (exc: Exception) {
                                onError(exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Grid Overlay
                CameraGridOverlay()
                
                // Top Gradient Scrim & Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                        
                        // Flash Toggle
                        IconButton(
                            onClick = {
                                val newMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                                flashMode = newMode
                                imageCapture?.flashMode = newMode
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = "闪光灯",
                                tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color.Yellow
                            )
                        }
                    }
                }
                
                // Center Reticle (Visual only)
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Canvas(modifier = Modifier.size(48.dp)) {
                        val stroke = Stroke(width = 2.dp.toPx())
                        val color = Color.White.copy(alpha = 0.5f)
                        val length = 12.dp.toPx()
                        
                        // Corners
                        // Top-Left
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, length), end = androidx.compose.ui.geometry.Offset(0f, 0f), strokeWidth = stroke.width)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(length, 0f), strokeWidth = stroke.width)
                        
                        // Top-Right
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - length, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = stroke.width)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, length), strokeWidth = stroke.width)
                        
                        // Bottom-Left
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height - length), end = androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = stroke.width)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(length, size.height), strokeWidth = stroke.width)
                        
                        // Bottom-Right
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width - length, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = stroke.width)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height - length), strokeWidth = stroke.width)
                    }
                }

                // Bottom Gradient Scrim & Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                        .padding(vertical = 40.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Shutter button
                    val scale by animateFloatAsState(if (isCapturing) 0.85f else 1f, label = "shutter")
                    
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable(enabled = !isCapturing, onClick = {
                                if (isCapturing) return@clickable
                                isCapturing = true
                                val capture = imageCapture ?: return@clickable
                                val outputFile = CameraUtils.createImageFile(context)
                                
                                CameraUtils.takePhoto(
                                    imageCapture = capture,
                                    outputFile = outputFile,
                                    context = context,
                                    onImageCaptured = { uri ->
                                        isCapturing = false
                                        onImageCaptured(uri)
                                    },
                                    onError = { exception ->
                                        isCapturing = false
                                        onError(exception)
                                    }
                                )
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) Color.LightGray else Color.White)
                        )
                    }
                }
            }
            
            cameraPermissionState.status.shouldShowRationale -> {
                // Show rationale for camera permission
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("需要相机权限来拍照")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("请求权限")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onClose) {
                        Text("取消")
                    }
                }
            }
            
            else -> {
                // First time request
                LaunchedEffect(Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.2f)

        // Vertical lines
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 3, 0f),
            end = androidx.compose.ui.geometry.Offset(width / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(2 * width / 3, 0f),
            end = androidx.compose.ui.geometry.Offset(2 * width / 3, height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, height / 3),
            end = androidx.compose.ui.geometry.Offset(width, height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 2 * height / 3),
            end = androidx.compose.ui.geometry.Offset(width, 2 * height / 3),
            strokeWidth = strokeWidth
        )
    }
}
