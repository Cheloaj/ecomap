package com.ecomap.socio.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors

@Composable
fun NuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false, // ✅ Nuevo parámetro
    icon: ImageVector? = null,
    backgroundColor: Color = NuColors.Primary,
    textColor: Color = Color.White
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) backgroundColor else NuColors.Surface,
        label = "button_background"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) textColor else NuColors.TextSecondary,
        label = "button_text"
    )

    Button(
        onClick = {
            if (!isLoading) {
                println("🚀 NuPrimaryButton clicado: $text")
                onClick()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedBackgroundColor,
            disabledContainerColor = NuColors.Surface,
            contentColor = animatedTextColor,
            disabledContentColor = NuColors.TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ✅ Mostrar CircularProgressIndicator o contenido normal
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NuSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = NuColors.Primary
) {
    TextButton(
        onClick = {
            println("🚀 NuSecondaryButton clicado: $text")
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor,
            disabledContentColor = NuColors.TextSecondary
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NuFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false, // ✅ Nuevo parámetro
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    backgroundColor: Color = NuColors.Primary
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) backgroundColor else NuColors.ButtonDisabled,
        label = "fab_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled && !isLoading) Color.White else NuColors.TextSecondary,
        label = "fab_content"
    )

    FloatingActionButton(
        onClick = {
            if (enabled && !isLoading) {
                println("🚀 NuFloatingActionButton clicado")
                onClick()
            } else {
                println("⚠️ NuFloatingActionButton deshabilitado o cargando")
            }
        },
        modifier = modifier.size(56.dp),
        containerColor = animatedBackgroundColor,
        contentColor = contentColor,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (enabled && !isLoading) 6.dp else 0.dp,
            pressedElevation = if (enabled && !isLoading) 12.dp else 0.dp
        )
    ) {
        // ✅ Mostrar CircularProgressIndicator o ícono
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "Continuar",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}