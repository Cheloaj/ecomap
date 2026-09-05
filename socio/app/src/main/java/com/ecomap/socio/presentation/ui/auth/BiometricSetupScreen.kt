package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import kotlinx.coroutines.delay
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Pantalla de Configuración Biométrica
 * Se muestra la primera vez que un usuario aprobado inicia sesión
 * Ofrece activar el ingreso rápido con biometría
 */
@Composable
fun BiometricSetupScreen(
    email: String,
    password: String,
    onBiometricEnabled: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    var isBiometricAvailable by remember { mutableStateOf(false) }

    // Verificar si la biometría está disponible
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
        val biometricManager = BiometricManager.from(context)
        isBiometricAvailable = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Espacio superior
            Spacer(modifier = Modifier.height(60.dp))

            // Contenido central
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Ícono de huella animado
                    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "fingerprint_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometría",
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        tint = NuColors.Primary
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Título
                    Text(
                        text = "Activa el Ingreso Rápido",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción
                    Text(
                        text = if (isBiometricAvailable) {
                            "Ingresa a tu cuenta de forma más rápida y segura usando tus datos biométricos. No necesitarás escribir tu contraseña cada vez."
                        } else {
                            "La biometría no está disponible en este dispositivo. Puedes usar tu contraseña para iniciar sesión."
                        },
                        fontSize = 16.sp,
                        color = NuColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Botones inferiores
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Botón Principal - Activar
                    Button(
                        onClick = {
                            if (isBiometricAvailable) {
                                // Guardar credenciales para biometría
                                BiometricHelper.saveBiometricCredentials(context, email, password)
                                Toast.makeText(context, "Biometría habilitada correctamente", Toast.LENGTH_SHORT).show()
                                // Marcar como habilitado
                                onBiometricEnabled()
                            } else {
                                Toast.makeText(context, "Biometría no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
                                onSkip()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBiometricAvailable) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        enabled = isBiometricAvailable
                    ) {
                        Text(
                            text = "Activar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Secundario - Quizás más tarde
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Quizás más tarde",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NuColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}