package com.ecomap.socio.presentation.ui.verification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import com.ecomap.socio.presentation.viewmodel.ProfileViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.getFirstName
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationPendingScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToQuickLogin: (String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onExitApp: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val business by viewModel.business.collectAsState()
    val businessState by viewModel.businessState.collectAsState()
    val signOutState by viewModel.signOutState.collectAsState()

    var animationScale by remember { mutableStateOf(0f) }
    val savedCredentials = remember { BiometricHelper.getSavedCredentials(context) }
    val isBiometricEnrolled = remember { BiometricHelper.isBiometricEnrolled(context) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        animationScale = 1f
    }

    // ✅ NAVEGAR AUTOMÁTICAMENTE cuando el negocio sea aprobado
    LaunchedEffect(business?.verificationStatus) {
        if (business?.verificationStatus == "approved") {
            println("✅ Negocio aprobado! Navegando a pantalla principal...")
            delay(500)
            onNavigateToMain()
        }
    }

    LaunchedEffect(signOutState) {
        when (signOutState) {
            is UiState.Success -> {
                // Si la cuenta está rechazada, siempre salir de la app
                if (user?.accountStatus == "rejected") {
                    onExitApp()
                } else if (isBiometricEnrolled) {
                    onSignOut()
                } else {
                    onExitApp()
                }
            }
            else -> {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = animationScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuColors.Background)
            .systemBarsPadding()
    ) {
        when {
            // Cuenta rechazada (accountStatus)
            user?.accountStatus == "rejected" -> {
                AccountRejectedContent(
                    scale = scale,
                    onExitApp = {
                        BiometricHelper.clearBiometricCredentials(context)
                        viewModel.signOut()
                        // El LaunchedEffect(signOutState) manejará la salida
                    }
                )
            }
            // Documento rechazado (business.verificationStatus)
            business?.verificationStatus == "rejected" -> {
                RejectedVerificationContent(
                    scale = scale,
                    isBiometricEnrolled = isBiometricEnrolled,
                    savedCredentials = savedCredentials,
                    onAction = { withBiometric ->
                        val user = viewModel.user.value
                        if (!withBiometric) {
                            viewModel.signOut()
                        } else if (user != null && savedCredentials != null) {
                            val firstName = getFirstName(user.fullName)
                            onNavigateToQuickLogin(
                                savedCredentials.first,
                                savedCredentials.second,
                                firstName
                            )
                        } else {
                            viewModel.signOut()
                            onSignOut()
                        }
                    }
                )
            }
            else -> {
                PendingVerificationContent(
                    scale = scale,
                    isBiometricEnrolled = isBiometricEnrolled,
                    savedCredentials = savedCredentials,
                    onAction = { withBiometric ->
                        val user = viewModel.user.value
                        if (!withBiometric) {
                            viewModel.signOut()
                        } else if (user != null && savedCredentials != null) {
                            val firstName = getFirstName(user.fullName)
                            onNavigateToQuickLogin(
                                savedCredentials.first,
                                savedCredentials.second,
                                firstName
                            )
                        } else {
                            viewModel.signOut()
                            onSignOut()
                        }
                    }
                )
            }
        }

        NuLoadingStateView(
            isLoading = businessState is UiState.Loading,
            message = "Cargando información..."
        )
    }
}

@Composable
fun PendingVerificationContent(
    scale: Float,
    isBiometricEnrolled: Boolean,
    savedCredentials: Triple<String, String, String?>?,
    onAction: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono animado con efecto pulsante
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Círculo de fondo con degradado
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NuColors.Primary.copy(alpha = 0.2f),
                                NuColors.Primary.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Ícono principal
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = "En proceso",
                modifier = Modifier.size(64.dp),
                tint = NuColors.Primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Título con animación de entrada
        Text(
            text = "Verificación en Proceso",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Estamos revisando tu información",
            fontSize = 16.sp,
            color = NuColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card moderna con información
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Documento recibido",
                    description = "Tu información está siendo verificada"
                )

                InfoRow(
                    icon = Icons.Default.AccessTime,
                    title = "Tiempo estimado",
                    description = "24 a 48 horas hábiles"
                )

                InfoRow(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Próximos pasos",
                    description = "Te notificaremos cuando esté lista"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info adicional
        Card(
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Primary.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = NuColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Mientras tanto, puedes cerrar sesión y volver más tarde",
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón Cerrar Sesión
        OutlinedButton(
            onClick = { onAction(isBiometricEnrolled) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NuColors.Primary
            )
        ) {
            Text(
                text = if (isBiometricEnrolled) "Cerrar Sesión" else "Salir",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RejectedVerificationContent(
    scale: Float,
    isBiometricEnrolled: Boolean,
    savedCredentials: Triple<String, String, String?>?,
    onAction: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de error con animación
        val shakeOffset by rememberInfiniteTransition(label = "shake").animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shake_offset"
        )

        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Círculo de fondo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NuColors.Error.copy(alpha = 0.2f),
                                NuColors.Error.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Rechazado",
                modifier = Modifier.size(64.dp),
                tint = NuColors.Error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verificación Rechazada",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.Error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No pudimos aprobar tu documento",
            fontSize = 16.sp,
            color = NuColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card de error moderna
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Error.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NuColors.Error.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Motivos posibles:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.Error
                )

                Text(
                    text = "• Documento ilegible o borroso\n• Información incompleta\n• Documento no válido",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card de contacto
        Card(
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NuColors.Primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 20.sp
                    )
                }
                Text(
                    text = "Contacta a soporte para más información sobre tu caso",
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón de acción
        Button(
            onClick = { onAction(isBiometricEnrolled) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NuColors.Error
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = if (isBiometricEnrolled) "Cerrar Sesión" else "Salir",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun AccountRejectedContent(
    scale: Float,
    onExitApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono de error
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NuColors.Error.copy(alpha = 0.2f),
                                NuColors.Error.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Cuenta Rechazada",
                modifier = Modifier.size(64.dp),
                tint = NuColors.Error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Cuenta Rechazada",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NuColors.Error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tu solicitud de registro no fue aprobada",
            fontSize = 16.sp,
            color = NuColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card de error
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Error.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NuColors.Error.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tu cuenta ha sido rechazada",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NuColors.Error,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Esto puede deberse a:\n\n• Información incompleta o incorrecta\n• Documentos no válidos\n• Incumplimiento de requisitos",
                    fontSize = 14.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card de contacto
        Card(
            colors = CardDefaults.cardColors(
                containerColor = NuColors.Surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NuColors.Primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ⓘ",
                        fontSize = 20.sp
                    )
                }
                Text(
                    text = "Para más información, contacta al equipo de soporte",
                    fontSize = 13.sp,
                    color = NuColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón de salir
        Button(
            onClick = onExitApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NuColors.Error
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "Salir",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NuColors.Primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NuColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = NuColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = NuColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}