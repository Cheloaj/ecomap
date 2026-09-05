package com.ecomap.usuario.presentation.ui.subscription

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ecomap.usuario.data.local.SubscriptionManager
import kotlinx.coroutines.delay

@Composable
fun ProcessingPaymentScreen(
    cardBrand: String,
    lastFour: String,
    onPaymentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val subscriptionManager = remember {
        dagger.hilt.EntryPoints.get(
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.ecomap.usuario.di.SubscriptionEntryPoint::class.java
            ),
            com.ecomap.usuario.di.SubscriptionEntryPoint::class.java
        ).subscriptionManager()
    }

    var processingState by remember { mutableStateOf(ProcessingState.VALIDATING) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Paso 1: Validando (0-30%)
        processingState = ProcessingState.VALIDATING
        for (i in 0..30) {
            progress = i / 100f
            delay(30)
        }
        delay(500)

        // Paso 2: Procesando pago (30-70%)
        processingState = ProcessingState.PROCESSING
        for (i in 31..70) {
            progress = i / 100f
            delay(30)
        }
        delay(500)

        // Paso 3: Activando suscripción (70-100%)
        processingState = ProcessingState.ACTIVATING
        for (i in 71..100) {
            progress = i / 100f
            delay(30)
        }
        delay(500)

        // Activar suscripción PRO
        val success = subscriptionManager.activateProSubscription()

        if (success) {
            processingState = ProcessingState.SUCCESS
            delay(2000)
            onPaymentSuccess()
        } else {
            processingState = ProcessingState.ERROR
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = processingState,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                        fadeOut(animationSpec = tween(600))
            },
            label = "state_transition"
        ) { state ->
            when (state) {
                ProcessingState.VALIDATING,
                ProcessingState.PROCESSING,
                ProcessingState.ACTIVATING -> {
                    MinimalProcessingContent(
                        state = state,
                        progress = progress
                    )
                }
                ProcessingState.SUCCESS -> {
                    MinimalSuccessContent(cardBrand, lastFour)
                }
                ProcessingState.ERROR -> {
                    MinimalErrorContent()
                }
            }
        }
    }
}

@Composable
private fun MinimalProcessingContent(
    state: ProcessingState,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Loader circular minimalista
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Círculo de fondo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )

            // Anillo de progreso
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .size(120.dp)
                    .rotate(rotation),
                color = Color(0xFF1F1F1F),
                strokeWidth = 3.dp,
                trackColor = Color(0xFFF5F5F5)
            )

            // Ícono central
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            ProcessingState.VALIDATING -> Color(0xFFE3F2FD)
                            ProcessingState.PROCESSING -> Color(0xFFFFF9E6)
                            ProcessingState.ACTIVATING -> Color(0xFFF3E5F5)
                            else -> Color(0xFFF5F5F5)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (state) {
                        ProcessingState.VALIDATING -> Icons.Default.Security
                        ProcessingState.PROCESSING -> Icons.Default.Payment
                        ProcessingState.ACTIVATING -> Icons.Default.Star
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = when (state) {
                        ProcessingState.VALIDATING -> Color(0xFF2196F3)
                        ProcessingState.PROCESSING -> Color(0xFFFFA000)
                        ProcessingState.ACTIVATING -> Color(0xFF9C27B0)
                        else -> Color(0xFF1F1F1F)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Título
        Text(
            text = when (state) {
                ProcessingState.VALIDATING -> "Validando datos"
                ProcessingState.PROCESSING -> "Procesando pago"
                ProcessingState.ACTIVATING -> "Activando PRO"
                else -> ""
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Descripción
        Text(
            text = when (state) {
                ProcessingState.VALIDATING -> "Verificando tu información de pago"
                ProcessingState.PROCESSING -> "Autorizando el cargo de $29.00 MXN"
                ProcessingState.ACTIVATING -> "Desbloqueando todas las funciones"
                else -> ""
            },
            fontSize = 15.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Barra de progreso minimalista
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFF1F1F1F))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF999999)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pasos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProcessStep(
                label = "Validar",
                isActive = state == ProcessingState.VALIDATING,
                isCompleted = progress > 0.3f
            )
            ProcessStep(
                label = "Procesar",
                isActive = state == ProcessingState.PROCESSING,
                isCompleted = progress > 0.7f
            )
            ProcessStep(
                label = "Activar",
                isActive = state == ProcessingState.ACTIVATING,
                isCompleted = progress >= 1f
            )
        }
    }
}

@Composable
private fun ProcessStep(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF4CAF50)
                        isActive -> Color(0xFF1F1F1F)
                        else -> Color(0xFFF0F0F0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }

        Text(
            text = label,
            fontSize = 12.sp,
            color = when {
                isActive || isCompleted -> Color(0xFF1F1F1F)
                else -> Color(0xFF999999)
            },
            fontWeight = if (isActive || isCompleted) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun MinimalSuccessContent(cardBrand: String, lastFour: String) {
    val scale = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(200)
        checkScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de éxito animado
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF66BB6A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(checkScale.value),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Título de éxito
        Text(
            text = "¡Pago exitoso!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bienvenido a EcoMap PRO",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta de resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con badge PRO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detalles del pago",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF9E6),
                                        Color(0xFFFFE8CC)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PRO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA000)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Detalles
                PaymentDetailRow(
                    label = "Plan",
                    value = "PRO Mensual"
                )

                PaymentDetailRow(
                    label = "Monto",
                    value = "$29.00 MXN"
                )

                PaymentDetailRow(
                    label = "Tarjeta",
                    value = "$cardBrand •••• $lastFour"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Info adicional
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tu suscripción está activa",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Beneficios desbloqueados
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ahora tienes acceso a:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )
                }

                BenefitItem("Análisis completo de compras")
                BenefitItem("Comparación de precios por tienda")
                BenefitItem("Rutas optimizadas")
                BenefitItem("Ahorro en transporte")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info final
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Redirigiendo...",
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun PaymentDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F1F1F)
        )
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun MinimalErrorContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de error
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFE53935)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Error al procesar",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No pudimos completar tu pago. Por favor, verifica tu información e intenta nuevamente.",
            fontSize = 15.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3F3)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Si el problema persiste, contacta a soporte",
                    fontSize = 13.sp,
                    color = Color(0xFFE53935),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private enum class ProcessingState {
    VALIDATING,
    PROCESSING,
    ACTIVATING,
    SUCCESS,
    ERROR
}