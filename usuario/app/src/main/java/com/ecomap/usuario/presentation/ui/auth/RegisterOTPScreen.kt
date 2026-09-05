package com.ecomap.usuario.presentation.ui.auth

import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.OtpInput
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthUiState
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.BiometricHelper
import kotlinx.coroutines.delay

/**
 * Pantalla de verificación OTP estilo Nu
 * Verifica el código de 6 dígitos enviado por email
 */
@Composable
fun RegisterOTPScreen(
    email: String,
    password: String,
    displayName: String,
    onNavigateBack: () -> Unit,
    onNavigateToNuBiometric: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Observar estado de verificación
    val authState by viewModel.authState.collectAsState()

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Manejar resultado de verificación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                if (isVerifying) {
                    showSuccess = true
                    delay(1500) // Mostrar animación de éxito

                    // Delay adicional para asegurar que NavGraph se recomponga con isSignedIn = true
                    delay(500) // Aumentado de 200ms a 500ms

                    // Detectar si tiene biométrico disponible
                    val biometricManager = BiometricManager.from(context)
                    val hasBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

                    if (hasBiometric) {
                        // ✅ Guardar credenciales biométricas antes de navegar
                        BiometricHelper.saveBiometricCredentials(context, email, password)
                        // ✅ Navegar a NuBiometricLoginScreen en lugar de BiometricSetup
                        onNavigateToNuBiometric()
                    } else {
                        onNavigateToMain()
                    }
                }
            }
            is AuthUiState.Error -> {
                isVerifying = false
                errorMessage = (authState as AuthUiState.Error).message
            }
            else -> { /* Loading or Initial */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = NuColors.Background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Botón atrás
                if (!isVerifying && !showSuccess) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NuColors.TextPrimary
                        )
                    }
                }

                // Contenido principal
                AnimatedVisibility(
                    visible = showContent && !showSuccess,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(400)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))

                        // Título
                        Text(
                            text = "Revisa tu correo",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subtítulo
                        Text(
                            text = "Enviamos un código a",
                            fontSize = 16.sp,
                            color = NuColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = email,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // Input de OTP
                        if (!isVerifying) {
                            OtpInput(
                                otpLength = 6,
                                onOtpComplete = { code ->
                                    // Cuando se complete el código, verificar con el backend
                                    isVerifying = true
                                    errorMessage = null
                                    viewModel.verifyCode(email, code)
                                }
                            )
                        }

                        // Mensaje de error
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Indicador de carga al verificar
                        if (isVerifying && !showSuccess) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NuColors.Success
                                )
                            }
                        }
                    }
                }

                // Animación de éxito
                AnimatedVisibility(
                    visible = showSuccess,
                    enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = NuColors.Success
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "¡Email verificado!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Bienvenido a EcoMap",
                            fontSize = 18.sp,
                            color = NuColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
