@file:OptIn(ExperimentalMaterial3Api::class)

package com.equiptrack.android.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.equiptrack.android.ui.theme.LocalHapticFeedbackEnabled
import com.equiptrack.android.ui.theme.LocalHapticStrength

@Composable
private fun rememberHapticExecutor(): (HapticFeedbackType?) -> Unit {
    val context = LocalContext.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val strength = LocalHapticStrength.current
    val fallbackHaptic = LocalHapticFeedback.current

    return remember(context, hapticEnabled, strength, fallbackHaptic) {
        { type ->
            if (hapticEnabled && type != null) {
                // If strength is effectively 1.0 (default), use system haptics for best native feel
                if (strength in 0.95f..1.05f) {
                    fallbackHaptic.performHapticFeedback(type)
                } else {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }

                    if (vibrator?.hasVibrator() == true) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = when (type) {
                                HapticFeedbackType.LongPress -> {
                                    // Heavy click: Base amp ~255
                                    val amp = (255 * strength).toInt().coerceIn(1, 255)
                                    VibrationEffect.createOneShot(70, amp)
                                }
                                HapticFeedbackType.TextHandleMove -> {
                                    // Light tick: Base amp ~100
                                    val amp = (100 * strength).toInt().coerceIn(1, 255)
                                    VibrationEffect.createOneShot(20, amp)
                                }
                                else -> null
                            }
                            if (effect != null) {
                                vibrator.vibrate(effect)
                            } else {
                                fallbackHaptic.performHapticFeedback(type)
                            }
                        } else {
                            fallbackHaptic.performHapticFeedback(type)
                        }
                    } else {
                        fallbackHaptic.performHapticFeedback(type)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable RowScope.() -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    Button(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable RowScope.() -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "outlined_button_scale"
    )
    
    OutlinedButton(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable () -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_button_scale"
    )
    
    IconButton(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable RowScope.() -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "text_button_scale"
    )
    
    TextButton(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = FloatingActionButtonDefaults.shape,
    containerColor: androidx.compose.ui.graphics.Color = FloatingActionButtonDefaults.containerColor,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable () -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )
    
    FloatingActionButton(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedSmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = FloatingActionButtonDefaults.smallShape,
    containerColor: androidx.compose.ui.graphics.Color = FloatingActionButtonDefaults.containerColor,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable () -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "small_fab_scale"
    )
    
    SmallFloatingActionButton(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun AnimatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticFeedbackType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    content: @Composable ColumnScope.() -> Unit
) {
    val performHaptic = rememberHapticExecutor()
    val wrappedOnClick = {
        performHaptic(hapticFeedbackType)
        onClick()
    }

    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        onClick = wrappedOnClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}