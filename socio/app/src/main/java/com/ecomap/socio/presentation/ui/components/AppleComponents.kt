package com.ecomap.socio.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ecomap.socio.ui.theme.AppleColors

// ✨ TextField estilo iOS con auto-advance
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions.copy(
                imeAction = if (keyboardOptions.imeAction == ImeAction.Default)
                    ImeAction.Next else keyboardOptions.imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    focusManager.moveFocus(FocusDirection.Down)
                },
                onDone = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppleColors.IOSBlue,
                unfocusedBorderColor = AppleColors.Gray3,
                errorBorderColor = AppleColors.IOSRed,
                focusedContainerColor = AppleColors.DarkCard,
                unfocusedContainerColor = AppleColors.DarkSurface,
                cursorColor = AppleColors.IOSBlue
            )
        )

        // Error message con animación
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
        ) {
            Text(
                text = errorMessage ?: "",
                color = AppleColors.IOSRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// 🔘 Botón estilo iOS con animaciones
@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    style: AppleButtonStyle = AppleButtonStyle.Primary,
    icon: ImageVector? = null
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    Button(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = when (style) {
            AppleButtonStyle.Primary -> ButtonDefaults.buttonColors(
                containerColor = AppleColors.IOSBlue,
                contentColor = Color.White,
                disabledContainerColor = AppleColors.IOSBlue.copy(alpha = 0.5f)
            )
            AppleButtonStyle.Secondary -> ButtonDefaults.buttonColors(
                containerColor = AppleColors.DarkCard,
                contentColor = AppleColors.IOSBlue
            )
            AppleButtonStyle.Destructive -> ButtonDefaults.buttonColors(
                containerColor = AppleColors.IOSRed,
                contentColor = Color.White
            )
            AppleButtonStyle.Success -> ButtonDefaults.buttonColors(
                containerColor = AppleColors.IOSGreen,
                contentColor = Color.White
            )
        },
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = remember { MutableInteractionSource() }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

enum class AppleButtonStyle {
    Primary, Secondary, Destructive, Success
}

// 📦 Card estilo iOS
@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppleColors.DarkCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// 🔔 Alert/Notification estilo iOS
@Composable
fun AppleAlert(
    title: String,
    message: String,
    type: AppleAlertType = AppleAlertType.Info,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (type) {
                AppleAlertType.Success -> AppleColors.IOSGreen.copy(alpha = 0.15f)
                AppleAlertType.Error -> AppleColors.IOSRed.copy(alpha = 0.15f)
                AppleAlertType.Warning -> AppleColors.IOSOrange.copy(alpha = 0.15f)
                AppleAlertType.Info -> AppleColors.IOSBlue.copy(alpha = 0.15f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = when (type) {
                        AppleAlertType.Success -> AppleColors.IOSGreen
                        AppleAlertType.Error -> AppleColors.IOSRed
                        AppleAlertType.Warning -> AppleColors.IOSOrange
                        AppleAlertType.Info -> AppleColors.IOSBlue
                    }
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

enum class AppleAlertType {
    Success, Error, Warning, Info
}

// 💬 Toast estilo iOS
@Composable
fun AppleToast(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -100 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -100 }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppleColors.DarkCard.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
