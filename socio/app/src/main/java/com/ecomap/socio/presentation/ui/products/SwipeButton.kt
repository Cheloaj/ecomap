package com.ecomap.socio.presentation.ui.products

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeButton(
    text: String = "Desliza para agendar",
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // --- Colores como los de la imagen ---
    val trackColor = Color(0xFF2E2E2E) // Pista gris oscura
    val thumbColor = Color(0xFFB4E051) // Pulgar verde lima
    val trailStartColor = thumbColor.copy(alpha = 0.2f) // Estela (inicio transparente)
    val trailEndColor = thumbColor.copy(alpha = 0.8f)   // Estela (fin opaco)
    val iconColor = Color.Black.copy(alpha = 0.8f)
    val textColor = Color.White.copy(alpha = 0.7f)

    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            onSwipeComplete()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor) // ✅ PISTA OSCURA (Capa 1)
    ) {
        val thumbSize = 56.dp // 64.dp (alto) - 8.dp (padding vertical)
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val paddingPx = with(density) { 4.dp.toPx() }

        // El pulgar se mueve de 0f a (ancho total - tamaño del pulgar - padding)
        val maxOffset = constraints.maxWidth.toFloat() - thumbSizePx - (paddingPx * 2)

        // ✅ ESTELA DE GRADIENTE (Capa 2)
        // El ancho de la estela es desde el inicio hasta el centro del pulgar
        val trailWidth = (offsetX + thumbSizePx / 2 + paddingPx).coerceAtMost(constraints.maxWidth.toFloat())
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { trailWidth.toDp() })
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(trailStartColor, trailEndColor),
                        endX = trailWidth // El gradiente se estira con la estela
                    )
                )
        )

        // ✅ TEXTO (Capa 3)
        Box(
            modifier = Modifier
                .matchParentSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompleted) "✓ Agendado" else text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) thumbColor else textColor
            )
        }

        // ✅ PULGAR (Capa 4 - Arriba de todo)
        val draggableState = rememberDraggableState { delta ->
            if (isCompleted) return@rememberDraggableState
            val newOffset = (offsetX + delta).coerceIn(0f, maxOffset)
            offsetX = newOffset
        }

        Box(
            modifier = Modifier
                .offset {
                    // El offset tiene en cuenta el padding inicial
                    IntOffset(
                        (offsetX + paddingPx).roundToInt(),
                        paddingPx.roundToInt()
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        scope.launch {
                            if (offsetX >= maxOffset * 0.75) {
                                // Snap al final (Completado)
                                val animatable = Animatable(offsetX)
                                animatable.animateTo(maxOffset, animationSpec = tween(150))
                                offsetX = maxOffset
                                isCompleted = true
                            } else {
                                // Snap al inicio (Regresar)
                                val animatable = Animatable(offsetX)
                                animatable.animateTo(0f, animationSpec = tween(300))
                                offsetX = 0f
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = iconColor, // Icono oscuro
                modifier = Modifier.size(24.dp)
            )
        }
    }
}