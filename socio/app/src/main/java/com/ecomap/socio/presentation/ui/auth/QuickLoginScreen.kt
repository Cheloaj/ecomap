package com.ecomap.socio.presentation.ui.auth

import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Text
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.findActivity
import kotlinx.coroutines.delay

/**
 * Pantalla de Acceso Rápido - Usuario registrado
 * Fondo morado oscuro, auto-lanza biometría
 * Edge-to-Edge con systemBarsPadding
 */
@Composable
fun QuickLoginScreen(
    userName: String = "Usuario",
    savedEmail: String,
    savedPassword: String,
    autoLaunchBiometric: Boolean = false,
    onNavigateToPasswordLogin: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit = {},
    onNavigateToOnboarding: (String) -> Unit = {},
    onNavigateToDocumentVerification: (String) -> Unit = {},
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    var showContent by remember { mutableStateOf(false) }

    // Resetear estado del ViewModel al entrar
    LaunchedEffect(Unit) {
        println("🔄 QuickLogin: Pantalla montada - autoLaunch=$autoLaunchBiometric")
        viewModel.resetSignInState()
    }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    // Auto-lanzar biometría SOLO si autoLaunchBiometric es true (viene desde Splash)
    LaunchedEffect(autoLaunchBiometric) {
        if (!autoLaunchBiometric) {
            println("🚫 QuickLogin: autoLaunchBiometric=false - NO se lanza automáticamente")
            return@LaunchedEffect
        }

        println("🔄 QuickLogin: Auto-lanzando biometría (autoLaunch=$autoLaunchBiometric)")

        if (activity != null) {
            // Esperar un poco más para asegurar que la UI esté lista
            delay(800)

            if (BiometricHelper.isBiometricAvailable(context)) {
                println("🔐 Auto-lanzando biometría...")
                showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        println("✅ Biometría exitosa - iniciando sesión automática")
                        viewModel.signIn(savedEmail, savedPassword, true)
                    }
                )
            } else {
                println("⚠️ Biometría no disponible")
                println("   isBiometricAvailable: ${BiometricHelper.isBiometricAvailable(context)}")
            }
        } else {
            println("❌ QuickLogin: Activity es null - no se puede mostrar biometría")
            println("   Tipo de contexto: ${context.javaClass.name}")
            // Navegar a login con contraseña si no hay activity
            onNavigateToPasswordLogin()
        }
    }

    // Observar estado de login
    val signInState by viewModel.signInState.collectAsState()
    LaunchedEffect(signInState) {
        if (signInState is com.ecomap.socio.utils.UiState.Success) {
            val user = (signInState as com.ecomap.socio.utils.UiState.Success).data

            println("✅ QuickLogin: Login biométrico exitoso")
            println("   - Estado: ${user.accountStatus}")
            println("   - Onboarding: ${user.onboardingStep}")
            println("   - Email verificado: ${user.emailVerified}")

            // ✅ Navegar según estado del usuario
            // PRIORIDAD: Email → Cuenta rechazada → Onboarding → accountStatus
            when {
                !user.emailVerified -> {
                    println("⚠️ Email no verificado → EmailVerification")
                    onNavigateToEmailVerification(user.email)
                }
                user.accountStatus == "rejected" -> {
                    println("❌ Cuenta rechazada → VerificationPending")
                    onNavigateToVerificationPending()
                }
                user.onboardingStep == "business_setup" -> {
                    println("🧩 Onboarding incompleto (business_setup) → Onboarding")
                    onNavigateToOnboarding(user.id)
                }
                user.onboardingStep == "document_upload" -> {
                    println("📄 Falta subir documento → DocumentVerification")
                    onNavigateToDocumentVerification(user.id)
                }
                user.onboardingStep == "pending_verification" -> {
                    println("⏳ Documento pendiente de verificación → VerificationPending")
                    onNavigateToVerificationPending()
                }
                user.onboardingStep == "completed" && user.accountStatus == "pending_verification" -> {
                    println("⏳ Onboarding completo, cuenta pendiente de verificación → VerificationPending")
                    onNavigateToVerificationPending()
                }
                user.onboardingStep == "completed" && user.accountStatus == "active" -> {
                    println("✅ Usuario aprobado y completo → Main")
                    onNavigateToMain()
                }
                else -> {
                    println("❌ Estado inesperado → Redirigir a VerificationPending por seguridad")
                    println("   user.onboardingStep: ${user.onboardingStep}")
                    println("   user.accountStatus: ${user.accountStatus}")
                    println("   user.emailVerified: ${user.emailVerified}")
                    onNavigateToVerificationPending()
                }
            }
        }
    }

    // UI con Edge-to-Edge
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuColors.PrimaryDark) // Fondo morado oscuro
            .systemBarsPadding(), // Edge-to-Edge
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
                    Spacer(modifier = Modifier.height(48.dp))

                    // Mensaje de bienvenida
                    Text(
                        text = "Hola,",
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = userName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mensaje de seguridad
                    Text(
                        text = "Por tu seguridad cerramos la sesión cuando hay 5 minutos de inactividad.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

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

                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometría",
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        tint = Color.White.copy(alpha = 0.4f)
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
                    // Botón Principal - Biometría
                    NuPrimaryButton(
                        text = "Ingresar con Face ID",
                        onClick = {
                            println("🔐 Botón Face ID presionado MANUALMENTE")
                            println("   activity: $activity")
                            println("   context: ${context::class.simpleName}")
                            println("   isBiometricAvailable: ${BiometricHelper.isBiometricAvailable(context)}")

                            if (activity != null) {
                                if (BiometricHelper.isBiometricAvailable(context)) {
                                    println("✅ Mostrando prompt biométrico...")
                                    showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = {
                                            println("✅ Biometría exitosa desde botón MANUAL")
                                            // ✅ MARCAR que el usuario usó Face ID manualmente
                                            BiometricHelper.markBiometricUsedOnce(context)
                                            println("✅ Flag activado: auto-launch Face ID en próximos ingresos")
                                            viewModel.signIn(savedEmail, savedPassword, true)
                                        }
                                    )
                                } else {
                                    println("❌ Biometría no disponible")
                                }
                            } else {
                                println("❌ Activity es null - no se puede mostrar biometría")
                                println("   Tipo de contexto: ${context.javaClass.name}")
                                onNavigateToPasswordLogin()
                            }
                        },
                        icon = Icons.Default.Fingerprint,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White,
                        textColor = NuColors.PrimaryDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Secundario - Contraseña
                    NuSecondaryButton(
                        text = "Ingresar con contraseña",
                        onClick = onNavigateToPasswordLogin,
                        textColor = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Mostrar prompt biométrico
 */
private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    BiometricHelper.showBiometricPrompt(
        activity = activity,
        title = "Ingresa a EcoMap Socio",
        subtitle = "Usa tu huella o Face ID para continuar",
        negativeButtonText = "Cancelar",
        onSuccess = {
            activity.runOnUiThread {
                onSuccess()
            }
        },
        onError = { errorMessage: String ->
            println("❌ Error biométrico: $errorMessage")
        },
        onFailed = {
            println("⚠️ Biometría fallida - huella no reconocida")
        }
    )
}
