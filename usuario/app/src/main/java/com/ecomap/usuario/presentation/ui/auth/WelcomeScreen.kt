package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.usuario.presentation.ui.components.NuPrimaryButton
import com.ecomap.usuario.presentation.ui.components.NuSecondaryButton
import kotlinx.coroutines.delay

// Colores minimalistas estilo iOS - Azul suave para usuarios
private val SoftBlue = Color(0xFF0A84FF)
private val SoftBlueDark = Color(0xFF0066CC)
private val SoftBlueLight = Color(0xFF64B5F6)
private val NuWhite = Color(0xFFFFFFFF)

/**
 * Pantalla de Bienvenida estilo Nu para Usuarios
 * Primera pantalla que ve el usuario al abrir la app por primera vez
 */
@Composable
fun WelcomeScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    // Animaciones avanzadas para las tarjetas
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    // Tarjeta 1 - Movimiento en Y con rebote suave
    val offset1 by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    // Tarjeta 2 - Movimiento en Y opuesto
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    // Tarjeta 3 - Movimiento en Y diferente
    val offset3 by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset3"
    )

    // Rotación suave para tarjeta 1
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation1"
    )

    // Rotación para tarjeta 2
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation2"
    )

    // Rotación para tarjeta 3
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation3"
    )

    // Escala pulsante sutil
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SoftBlueLight,
                        SoftBlue,
                        SoftBlueDark
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Contenido central - Tarjetas flotantes
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 200)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    // Stack de "pantallas" flotantes representando la app
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Tarjeta 3 (más atrás)
                        Box(
                            modifier = Modifier
                                .offset(x = 50.dp, y = offset3.dp)
                                .scale(scale2)
                                .rotate(rotation3)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF64B5F6),
                                            Color(0xFF42A5F5),
                                            Color(0xFF2196F3)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = NuWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(32.dp)
                                )

                                Text(
                                    text = "Favoritos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuWhite.copy(alpha = 0.9f)
                                )
                            }
                        }

                        // Tarjeta 2 (medio)
                        Box(
                            modifier = Modifier
                                .offset(x = (-30).dp, y = offset2.dp)
                                .scale(scale1)
                                .rotate(rotation2)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF42A5F5),
                                            Color(0xFF2196F3),
                                            Color(0xFF1976D2)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBasket,
                                    contentDescription = null,
                                    tint = NuWhite,
                                    modifier = Modifier.size(32.dp)
                                )

                                Text(
                                    text = "Mi Canasta",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuWhite
                                )
                            }
                        }

                        // Tarjeta 1 (frente)
                        Box(
                            modifier = Modifier
                                .offset(y = offset1.dp)
                                .scale(scale2)
                                .rotate(rotation1)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF90CAF9),
                                            Color(0xFF64B5F6),
                                            Color(0xFF42A5F5)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NuWhite,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = NuWhite.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Explorar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuWhite
                                    )
                                    Text(
                                        text = "Productos cerca",
                                        fontSize = 11.sp,
                                        color = NuWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    // Título principal
                    Text(
                        text = "Descubre productos\nlocales y sostenibles.",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuWhite,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )
                }
            }

            // Botones inferiores
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    // Botón primario - Registrarse
                    NuPrimaryButton(
                        text = "Continuar o empezar registro",
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        backgroundColor = NuWhite,
                        textColor = SoftBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón secundario - Iniciar sesión
                    NuSecondaryButton(
                        text = "Iniciar sesión",
                        onClick = onNavigateToLogin,
                        textColor = NuWhite,
                        modifier = Modifier.height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Texto legal pequeño
                    Text(
                        text = "EcoMap Usuario",
                        fontSize = 11.sp,
                        color = NuWhite.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
