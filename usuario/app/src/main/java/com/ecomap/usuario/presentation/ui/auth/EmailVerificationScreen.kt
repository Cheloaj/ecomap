package com.ecomap.usuario.presentation.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.NuLoadingStateView
import com.ecomap.usuario.presentation.ui.components.OtpInput
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de Verificación de Email con código OTP de 6 dígitos
 * Adaptado para EcoMapUsuario - Navega a Main después de verificación exitosa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val verificationState by viewModel.verificationState.collectAsState()
    val resendCodeState by viewModel.resendCodeState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var showSuccessAnimation by remember { mutableStateOf(false) }
    var otpCompleted by remember { mutableStateOf(false) }

    // Cooldown de 60 segundos para reenviar código
    var resendCooldown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    // Timer para el cooldown
    LaunchedEffect(Unit) {
        while (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
        canResend = true
    }

    // Reiniciar cooldown cuando se reenvía el código
    LaunchedEffect(resendCodeState) {
        if (resendCodeState is UiState.Success) {
            resendCooldown = 60
            canResend = false
            delay(2000)
            viewModel.resetResendCodeState()
        } else if (resendCodeState is UiState.Error) {
            delay(3000)
            viewModel.resetResendCodeState()
        }
    }

    // Navegación después de verificación exitosa
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is UiState.Success -> {
                println("✅ EmailVerificationScreen: Verificación exitosa")
                // Esperar 2 segundos para mostrar animación de éxito
                delay(2000)
                showSuccessAnimation = true
                // Esperar otro segundo antes de navegar
                delay(1000)
                onNavigateToMain()
            }
            else -> {}
        }
    }

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Calcular si debemos mostrar la animación de carga
    val shouldShowLoading = verificationState is UiState.Loading ||
                           resendCodeState is UiState.Loading

    // Box para que la animación cubra toda la pantalla
    Box(modifier = Modifier.fillMaxSize()) {
        // Solo mostrar Scaffold si NO está en animación
        if (!shouldShowLoading && !showSuccessAnimation) {
            Scaffold(
                containerColor = AppleColors.Background
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(scrollState)
                            .imePadding(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Título
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                                initialOffsetY = { -50 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        ) {
                            Column {
                                Text(
                                    text = "Revisa tu correo",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleColors.Label,
                                    lineHeight = 38.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Enviamos un código a",
                                    fontSize = 16.sp,
                                    color = AppleColors.SecondaryLabel
                                )

                                Text(
                                    text = email,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleColors.IOSBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Campos de código OTP
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                            )
                        ) {
                            Column {
                                OtpInput(
                                    otpLength = 6,
                                    onOtpComplete = { otp ->
                                        // Cerrar teclado
                                        focusManager.clearFocus()
                                        otpCompleted = true
                                        viewModel.verifyEmail(email, otp)
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Mensaje de error
                                AnimatedVisibility(
                                    visible = verificationState is UiState.Error,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
                                ) {
                                    Text(
                                        text = (verificationState as? UiState.Error)?.message ?: "Error al verificar código",
                                        fontSize = 14.sp,
                                        color = AppleColors.Error,
                                        textAlign = TextAlign.Start
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Mensaje de éxito de reenvío
                                AnimatedVisibility(
                                    visible = resendCodeState is UiState.Success,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
                                ) {
                                    Text(
                                        text = "Código reenviado exitosamente",
                                        fontSize = 14.sp,
                                        color = AppleColors.Success,
                                        textAlign = TextAlign.Start
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Botón Reenviar código con cooldown
                                TextButton(
                                    onClick = {
                                        if (canResend) {
                                            viewModel.resendVerificationCode(email)
                                        }
                                    },
                                    enabled = canResend && resendCodeState !is UiState.Loading
                                ) {
                                    Text(
                                        text = if (!canResend) {
                                            "Reenviar código (${resendCooldown}s)"
                                        } else {
                                            "Reenviar código"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (canResend) AppleColors.IOSBlue else AppleColors.SecondaryLabel
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
                    }
                }
            }
        }

        // Bloquear retroceder durante la animación
        BackHandler(enabled = shouldShowLoading || showSuccessAnimation) {
            // No hacer nada - bloquear navegación hacia atrás
        }

        // Mostrar animación de carga
        if (shouldShowLoading) {
            NuLoadingStateView(
                isLoading = true,
                message = "Verificando código..."
            )
        }
    }
}
