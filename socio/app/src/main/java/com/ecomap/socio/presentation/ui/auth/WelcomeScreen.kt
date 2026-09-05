package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import kotlinx.coroutines.delay
import kotlin.math.sin

// Colores estilo Nu
private val NuPurple = Color(0xFF820AD1)

private val NuPurpleBlack= Color(0xFF7C26A8)

private val NuPurpleLight = Color(0xFFA855F7)
private val NuPurpleDark = Color(0xFF6B21A8)
private val NuWhite = Color(0xFFFFFFFF)
private val NuWhiteTransparent = Color(0xCCFFFFFF)

/**
 * Pantalla de Bienvenida estilo Nu
 * Primera pantalla que ve el usuario al abrir la app por primera vez
 */
@Composable
fun WelcomeScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    // Animación de flotación para las tarjetas
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val rotation1 by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation1"
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
                        NuPurpleLight,
                        NuPurple,
                        NuPurpleDark
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

            // Contenido central - "Dashboard/App cards" flotantes
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
                        // Tarjeta 3 (más atrás, más rotada)
                        Box(
                            modifier = Modifier
                                .offset(x = 50.dp, y = offset2.dp)
                                .rotate(-12f)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF9333EA),
                                            Color(0xFF7C3AED)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            // Contenido de la tarjeta
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = NuWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(32.dp)
                                )

                                Text(
                                    text = "Análisis",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuWhite.copy(alpha = 0.9f)
                                )
                            }
                        }

                        // Tarjeta 2 (medio)
                        Box(
                            modifier = Modifier
                                .offset(x = (-30).dp, y = offset1.dp)
                                .rotate(rotation1)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFA855F7),
                                            Color(0xFF9333EA)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = NuWhite,
                                    modifier = Modifier.size(32.dp)
                                )

                                Text(
                                    text = "Inventario",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuWhite
                                )
                            }
                        }

                        // Tarjeta 1 (frente - Dashboard principal)
                        Box(
                            modifier = Modifier
                                .offset(y = (-offset1).dp)
                                .rotate(5f)
                                .size(width = 160.dp, height = 220.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFc084fc),
                                            Color(0xFFA855F7)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
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
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = NuWhite,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = NuWhite.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Mi Negocio",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NuWhite
                                    )
                                    Text(
                                        text = "Control total",
                                        fontSize = 11.sp,
                                        color = NuWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp)) // Aumentamos el espacio para balancear

                    // Título principal (MODIFICADO)
                    Text(
                        text = "Gestión inteligente\npara tu negocio.",
                        fontSize = 32.sp, // Aumentado para más impacto
                        fontWeight = FontWeight.Bold,
                        color = NuWhite,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp // Ajustado para el nuevo tamaño
                    )

                    // Subtítulo (ELIMINADO)
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
                        textColor = NuPurple
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
                        text = "EcoMap Socio S.A. de C.V.",
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