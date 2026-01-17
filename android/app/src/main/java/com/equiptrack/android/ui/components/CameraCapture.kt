package com.equiptrack.android.ui.components

import android.Manifest
import android.net.Uri
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.equiptrack.android.utils.CameraUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    
    // Camera State
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isCapturing by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    
    // Temporary captured image for preview
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    
    // Executors
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    
    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }

    // If we have a captured image, show the preview screen
    if (capturedUri != null) {
        CameraPreviewScreen(
            uri = capturedUri!!,
            onRetake = {
                // Clean up temp file if needed? CameraUtils handles temp files.
                capturedUri = null 
            },
            onUse = {
                onImageCaptured(capturedUri!!)
            }
        )
        return
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Focus tap state
                var focusPoint by remember { mutableStateOf<Offset?>(null) }
                val scope = rememberCoroutineScope()
                var previewView by remember { mutableStateOf<PreviewView?>(null) }

                // Key the AndroidView to force recreation on camera switch
                key(cameraSelector) {
                    AndroidView(
                        factory = { ctx ->
                            val view = PreviewView(ctx)
                            previewView = view
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            view.scaleType = PreviewView.ScaleType.FILL_CENTER
                            
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(view.surfaceProvider)
                                }
                                imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setFlashMode(flashMode)
                                    .build()
                                try {
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    onError(exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    focusPoint = offset
                                    previewView?.meteringPointFactory?.let { factory ->
                                        val point = factory.createPoint(offset.x, offset.y)
                                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                            .build()
                                        camera?.cameraControl?.startFocusAndMetering(action)
                                    }
                                    // Reset focus point visualization after delay
                                    scope.launch {
                                        delay(1000)
                                        focusPoint = null
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
                                        val currentZoomRatio = zoomState.zoomRatio
                                        val maxZoomRatio = zoomState.maxZoomRatio
                                        val minZoomRatio = zoomState.minZoomRatio
                                        
                                        val newZoom = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                                        camera?.cameraControl?.setZoomRatio(newZoom)
                                        zoomRatio = newZoom
                                    }
                                }
                            }
                    )
                }

                // Grid Overlay
                CameraGridOverlay()
                
                // Focus Ring
                focusPoint?.let { offset ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = 40.dp.toPx(),
                            center = offset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                
                // Top Controls (Flash, Close)
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
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "关闭", tint = Color.White)
                        }
                        
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
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
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

                // Bottom Controls (Gallery, Shutter, Switch)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(bottom = 50.dp, top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Placeholder (Spacer)
                        Spacer(modifier = Modifier.size(48.dp))

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clickable(enabled = !isCapturing) {
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
                                            capturedUri = uri
                                        },
                                        onError = { exception ->
                                            isCapturing = false
                                            onError(exception)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer ring
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.White,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            // Inner circle (animates size)
                            val innerSize by animateFloatAsState(
                                targetValue = if (isCapturing) 40f else 64f,
                                label = "shutter"
                            )
                            Canvas(modifier = Modifier.size(innerSize.dp)) {
                                drawCircle(color = Color.White)
                            }
                        }

                        // Switch Camera Button
                        IconButton(
                            onClick = {
                                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = "切换相机",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            cameraPermissionState.status.shouldShowRationale -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("需要相机权限来拍照", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("授权")
                    }
                    TextButton(onClick = onClose) { Text("取消", color = Color.White) }
                }
            }
            
            else -> {
                LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }
            }
        }
    }
}

@Composable
fun CameraPreviewScreen(
    uri: Uri,
    onRetake: () -> Unit,
    onUse: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = uri,
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("重拍")
            }
            
            Button(
                onClick = onUse,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("使用照片")
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
        val color = Color.White.copy(alpha = 0.3f)

        // Vertical lines
        drawLine(color, Offset(width / 3, 0f), Offset(width / 3, height), strokeWidth)
        drawLine(color, Offset(2 * width / 3, 0f), Offset(2 * width / 3, height), strokeWidth)

        // Horizontal lines
        drawLine(color, Offset(0f, height / 3), Offset(width, height / 3), strokeWidth)
        drawLine(color, Offset(0f, 2 * height / 3), Offset(width, 2 * height / 3), strokeWidth)
    }
}
