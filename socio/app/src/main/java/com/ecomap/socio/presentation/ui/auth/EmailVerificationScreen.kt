package com.ecomap.socio.presentation.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.ui.components.NuLoadingSuccessView
import com.ecomap.socio.presentation.ui.components.OtpInput
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de Verificación de Código con diseño minimalista
 * 6 campos para código OTP
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    onNavigateToOnboarding: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val verificationState by viewModel.verificationState.collectAsState()
    val resendCodeState by viewModel.resendCodeState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var showSuccessAnimation by remember { mutableStateOf(false) }
    var otpCompleted by remember { mutableStateOf(false) }

    // ✅ Cooldown de 60 segundos para reenviar código
    var resendCooldown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    // ✅ Timer para el cooldown
    LaunchedEffect(Unit) {
        while (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
        canResend = true
    }

    // ✅ Reiniciar cooldown cuando se reenvía el código
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
                if ((verificationState as UiState.Success).data) {
                    println("✅ EmailVerificationScreen: Verificación exitosa")
                    // ✅ Esperar 3 segundos para que la barra llegue al 100%
                    delay(3000)
                    showSuccessAnimation = true
                }
            }
            else -> {}
        }
    }


    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // ✅ Calcular si debemos mostrar la animación
    val shouldShowLoading = verificationState is UiState.Loading ||
                           resendCodeState is UiState.Loading ||
                           (verificationState is UiState.Success && !showSuccessAnimation)

    // ✅ Box para que la animación cubra TODA la pantalla incluyendo TopAppBar
    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Solo mostrar Scaffold si NO está en animación
        if (!shouldShowLoading && !showSuccessAnimation) {
            Scaffold(
                containerColor = NuColors.Background
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
                            color = NuColors.TextPrimary,
                            lineHeight = 38.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enviamos un código a",
                            fontSize = 16.sp,
                            color = NuColors.TextSecondary
                        )

                        Text(
                            text = email,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.Primary
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
                                // ✅ CERRAR TECLADO
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
                                text = ErrorMessageHelper.getFriendlyMessage(
                                    (verificationState as? UiState.Error)?.message
                                ),
                                fontSize = 14.sp,
                                color = NuColors.Error,
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
                                color = NuColors.Success,
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
                                color = if (canResend) NuColors.Primary else NuColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
            } // Cierra Column
        } // Cierra Box interno
        } // Cierra Scaffold
        } // Cierra if

        // ✅ Animación FUERA del Scaffold para cubrir TODO incluyendo TopAppBar

        // ✅ Bloquear retroceder durante la animación
        BackHandler(enabled = shouldShowLoading || showSuccessAnimation) {
            // No hacer nada - bloquear navegación hacia atrás
        }

        // ✅ Solo mostrar animación de carga, sin texto de éxito
        if (shouldShowLoading) {
            com.ecomap.socio.presentation.ui.components.NuLoadingStateView(
                isLoading = true,
                message = "Verificando código...",
                duration = 6000L // ✅ 6 segundos para la barra
            )
        }

        // ✅ Navegación directa después de completar barra
        LaunchedEffect(showSuccessAnimation) {
            if (showSuccessAnimation) {
                currentUser?.let { user ->
                    println("✅ EmailVerificationScreen: Navegando a Onboarding")
                    onNavigateToOnboarding(user.id)
                }
            }
        }
    } // Cierra Box
} // Cierra función
