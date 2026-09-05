package com.ecomap.socio.presentation.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.ForgotPasswordBottomSheet
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.utils.UiState

/**
 * Wrapper para CleanPasswordScreen - Usuarios Registrados (modo oscuro)
 * Para usuarios que presionan "Ingresar con contraseña" desde QuickLoginScreen
 * Navega según estado del usuario
 */
@Composable
fun CleanPasswordReturningScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit = {},
    onNavigateToOnboarding: (String) -> Unit = {},
    onNavigateToDocumentVerification: (String) -> Unit = {},
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToWelcome: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val signInState by viewModel.signInState.collectAsState()
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordBottomSheet by remember { mutableStateOf(false) }

    // Manejar estados de signIn
    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                isLoading = false
                passwordError = null
                val user = (signInState as UiState.Success).data

                println("✅ Login exitoso (usuario registrado)")
                println("   - Estado: ${user.accountStatus}")
                println("   - Onboarding: ${user.onboardingStep}")

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
                        println("🧩 Onboarding incompleto → Onboarding")
                        onNavigateToOnboarding(user.id)
                    }
                    user.onboardingStep == "document_upload" -> {
                        println("📄 Falta documento → DocumentVerification")
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
                        println("✅ Usuario activo → Main")
                        onNavigateToMain()
                    }
                    else -> {
                        println("❌ Estado inesperado → Redirigir a VerificationPending por seguridad")
                        println("   user.onboardingStep: ${user.onboardingStep}")
                        println("   user.accountStatus: ${user.accountStatus}")
                        onNavigateToVerificationPending()
                    }
                }
            }
            is UiState.Error -> {
                isLoading = false
                val errorMessage = (signInState as UiState.Error).message.lowercase()

                // ✅ Detectar tipo de error automáticamente
                passwordError = when {
                    errorMessage.contains("email") || errorMessage.contains("correo") ->
                        "Este correo no está registrado"
                    errorMessage.contains("password") || errorMessage.contains("contraseña") || errorMessage.contains("credentials") ->
                        "Contraseña incorrecta. Inténtalo de nuevo"
                    else ->
                        "Error al iniciar sesión. Verifica tus datos"
                }
                println("❌ Error de login: $passwordError")
            }
            is UiState.Loading -> {
                isLoading = true
                passwordError = null
                println("⏳ Iniciando sesión...")
            }
            else -> {
                isLoading = false
            }
        }
    }

    // UI
    Box {
        CleanPasswordScreen(
            isDarkMode = true,
            onNavigateBack = onNavigateBack,
            onPasswordSubmit = { password ->
                passwordError = null // Reset error
                viewModel.signIn(email, password, true) // rememberMe = true para usuarios registrados
            },
            onForgotPassword = {
                showForgotPasswordBottomSheet = true
            },
            errorMessage = passwordError,
            isLoading = isLoading
        )

        // Bottom Sheet de "Olvidé la contraseña"
        if (showForgotPasswordBottomSheet) {
            ForgotPasswordBottomSheet(
                onDismiss = {
                    showForgotPasswordBottomSheet = false
                },
                onContinue = {
                    showForgotPasswordBottomSheet = false
                    // Navegar a Welcome (sin Face ID)
                    onNavigateToWelcome()
                }
            )
        }
    }
}
