package com.ecomap.socio.presentation.ui.welcome

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.R
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import com.ecomap.socio.ui.theme.NuColors
import kotlinx.coroutines.delay

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

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo y contenido superior
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { -100 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 60.dp)
                ) {
                    // Logo
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Logo",
                        modifier = Modifier.size(80.dp),
                        tint = NuColors.Primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nombre de la app
                    Text(
                        text = "EcoMap Socio",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )
                }
            }

            // Visual central
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Elemento visual (puedes reemplazar con una imagen)
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Visual",
                        modifier = Modifier.size(200.dp),
                        tint = NuColors.Primary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Eslogan
                    Text(
                        text = "Tu negocio en el mapa\nde forma sostenible",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = NuColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción
                    Text(
                        text = "Conecta con clientes locales y\ncrea ofertas del día",
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
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Botón primario - Registrarse
                    NuPrimaryButton(
                        text = "Registrarse",
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón secundario - Iniciar sesión
                    NuSecondaryButton(
                        text = "Iniciar sesión",
                        onClick = onNavigateToLogin,
                        textColor = NuColors.Primary
                    )
                }
            }
        }
    }
}
