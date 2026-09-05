package com.ecomap.socio.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors
import kotlinx.coroutines.delay

/**
 * Indicador de carga universal estilo Nu
 * CON PROGRESO DETERMINADO (0% a 100%)
 * Se muestra sobre toda la pantalla con un mensaje dinámico
 *
 * @param isLoading Muestra la animación de carga
 * @param message Mensaje que se muestra durante la carga
 * @param duration Duración de la animación de carga en milisegundos (por defecto 6000ms)
 * @param modifier Modificador opcional
 */
@Composable
fun NuLoadingStateView(
    isLoading: Boolean,
    message: String = "Cargando...",
    duration: Long = 6000L,
    modifier: Modifier = Modifier
) {
    // Progreso animado de 0% a 100%
    var targetProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            targetProgress = 0f
            // Animar de 0% a 100% en el tiempo especificado
            targetProgress = 1f
        } else {
            targetProgress = 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) targetProgress else 0f,
        animationSpec = tween(
            durationMillis = duration.toInt(),
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White), // ✅ FONDO BLANCO SÓLIDO
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Texto del mensaje con animación de fade
                AnimatedContent(
                    targetState = message,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith
                        fadeOut(animationSpec = tween(400))
                    },
                    label = "message_fade"
                ) { animatedMessage ->
                    Text(
                        text = animatedMessage,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = NuColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                // Barra de progreso DETERMINADA (0% a 100%)
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.width(200.dp),
                    color = NuColors.Primary,
                    trackColor = NuColors.TextFieldLine
                )
            }
        }
    }
}

/**
 * Vista de éxito simple - SOLO TEXTO, SIN ICONOS
 * Se muestra cuando una operación se completa exitosamente
 *
 * @param isVisible Controla si se muestra o no
 * @param message Mensaje de éxito a mostrar
 * @param duration Duración que el mensaje permanece visible antes de desaparecer
 * @param onDismiss Callback cuando la animación termina
 */
@Composable
fun NuSuccessView(
    isVisible: Boolean,
    message: String = "¡Completado!",
    duration: Long = 6000L,
    onDismiss: () -> Unit = {}
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(duration)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White), // ✅ FONDO BLANCO SÓLIDO
            contentAlignment = Alignment.Center
        ) {
            // SOLO TEXTO - SIN ICONOS, con animación de fade
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
                },
                label = "success_message_fade"
            ) { animatedMessage ->
                Text(
                    text = animatedMessage,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

/**
 * Componente todo-en-uno con FONDO BLANCO
 * 1. Muestra barra de progreso 0→100% con texto (2s)
 * 2. Desvanece barra y texto
 * 3. Aparece texto de éxito con fade (1.5s)
 *
 * @param isLoading Estado de carga
 * @param isSuccess Estado de éxito
 * @param isError Estado de error
 * @param loadingMessage Mensaje durante la carga
 * @param successMessage Mensaje de éxito
 * @param errorMessage Mensaje de error
 * @param loadingDuration Duración de la animación de carga (default 2000ms)
 * @param successDuration Duración del mensaje de éxito (default 1500ms)
 * @param onComplete Callback cuando termina exitosamente
 * @param onError Callback cuando hay error
 */
@Composable
fun NuLoadingSuccessView(
    isLoading: Boolean,
    isSuccess: Boolean,
    isError: Boolean = false,
    loadingMessage: String = "Guardando...",
    successMessage: String = "¡Guardado exitosamente!",
    errorMessage: String = "Ocurrió un error",
    loadingDuration: Long = 2000L,
    successDuration: Long = 1500L,
    onComplete: () -> Unit = {},
    onError: () -> Unit = {}
) {
    // Progreso animado de 0% a 100%
    var targetProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            targetProgress = 0f
            targetProgress = 1f
        } else {
            targetProgress = 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) targetProgress else 0f,
        animationSpec = tween(
            durationMillis = loadingDuration.toInt(),
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    // Auto-dismiss para success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(successDuration)
            onComplete()
        }
    }

    // Auto-dismiss para error
    LaunchedEffect(isError) {
        if (isError) {
            delay(3000) // 3 segundos para leer el error
            onError()
        }
    }

    // Contenedor con fondo blanco
    AnimatedVisibility(
        visible = isLoading || isSuccess || isError,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White), // ✅ FONDO BLANCO
            contentAlignment = Alignment.Center
        ) {
            // Estado de carga: Texto + Barra
            AnimatedVisibility(
                visible = isLoading && !isSuccess && !isError,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) // ✅ Más rápido: 200ms
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = loadingMessage,
                        fontSize = 30.sp, // ✅ 30sp
                        fontWeight = FontWeight.Medium,
                        color = NuColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.width(150.dp), // ✅ MÁS CORTA: 200dp → 150dp
                        color = NuColors.Primary,
                        trackColor = NuColors.TextFieldLine
                    )
                }
            }

            // Estado de éxito: Solo texto con fade
            AnimatedVisibility(
                visible = isSuccess,
                enter = fadeIn(animationSpec = tween(200)), // ✅ Más rápido: 200ms
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Text(
                    text = successMessage,
                    fontSize = 30.sp, // ✅ 30sp
                    fontWeight = FontWeight.Bold,
                    color = NuColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            // Estado de error: Solo texto con fade
            AnimatedVisibility(
                visible = isError,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "❌",
                        fontSize = 48.sp
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = NuColors.Error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
