package com.equiptrack.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    autofillTypes: List<AutofillType> = emptyList()
) {
    val autofill = LocalAutofill.current
    val autofillNode = remember(autofillTypes) {
        AutofillNode(
            autofillTypes = autofillTypes,
            onFill = onValueChange
        )
    }
    
    // Note: To fully support autofill, the node should be added to LocalAutofillTree,
    // but it is internal in some Compose versions. 
    // We rely on requestAutofillForNode and keyboardOptions heuristics here.

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder,
            leadingIcon = if (icon != null) {
                {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            } else null,
            trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "隐藏密码" else "显示密码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    autofillNode.boundingBox = coordinates.boundsInWindow()
                }
                .onFocusChanged { focusState ->
                    autofill?.apply {
                        if (focusState.isFocused) {
                            requestAutofillForNode(autofillNode)
                        } else {
                            cancelAutofillForNode(autofillNode)
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FluidLoginBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_bg")

    // Independent animations for organic, non-repeating movement
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t3"
    )

    // Use stronger base colors for better visibility through blur, but with transparency
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.background(surfaceColor).blur(100.dp)) {
        val w = size.width
        val h = size.height

        // Blob 1: Top-Leftish (Drifting)
        drawCircle(
            color = primaryColor.copy(alpha = 0.3f),
            radius = w * 0.6f,
            center = Offset(
                x = w * 0.2f + (w * 0.2f * cos(t1 * Math.PI).toFloat()),
                y = h * 0.3f + (h * 0.1f * sin(t1 * Math.PI).toFloat())
            )
        )

        // Blob 2: Bottom-Rightish (Drifting)
        drawCircle(
            color = secondaryColor.copy(alpha = 0.3f),
            radius = w * 0.6f,
            center = Offset(
                x = w * 0.8f - (w * 0.2f * sin(t2 * Math.PI).toFloat()),
                y = h * 0.7f - (h * 0.1f * cos(t2 * Math.PI).toFloat())
            )
        )

        // Blob 3: Center (Breathing/Floating)
        drawCircle(
            color = tertiaryColor.copy(alpha = 0.25f),
            radius = w * 0.7f,
            center = Offset(
                x = w * 0.5f + (w * 0.15f * cos(t3 * Math.PI * 2).toFloat()),
                y = h * 0.5f + (h * 0.15f * sin(t3 * Math.PI * 2).toFloat())
            )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}
