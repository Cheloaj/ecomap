package com.ecomap.socio.presentation.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.ScamWarningBottomSheet
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.UiState

/**
 * Wrapper para CleanPasswordScreen - Nuevos Usuarios (modo claro)
 * Incluye lógica de signIn y navegación
 */
@Composable
fun CleanPasswordNewScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToDocumentVerification: (String) -> Unit,
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToForgotPassword: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val signInState by viewModel.signInState.collectAsState()
    var submittedPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showScamWarningBottomSheet by remember { mutableStateOf(false) }

    // Navegación según estado del usuario
    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                isLoading = false
                passwordError = null
                val user = (signInState as UiState.Success).data

                // ✅ GUARDAR CREDENCIALES AUTOMÁTICAMENTE para nuevos usuarios
                if (email.isNotBlank() && submittedPassword.isNotBlank()) {
                    println("🔐 ================================")
                    println("🔐 GUARDANDO CREDENCIALES AUTOMÁTICAMENTE")
                    println("🔐 Email: $email")
                    println("🔐 Password length: ${submittedPassword.length}")
                    println("🔐 FullName: ${user.fullName}")
                    println("🔐 ================================")
                    BiometricHelper.saveBiometricCredentials(context, email, submittedPassword, user.fullName)

                    // Verificar que se guardaron correctamente
                    val saved = BiometricHelper.getSavedCredentials(context)
                    if (saved != null) {
                        println("✅ Credenciales guardadas y verificadas exitosamente")
                        println("✅ Email guardado: ${saved.first}")
                        println("✅ FullName guardado: ${saved.third}")
                    } else {
                        println("❌ ERROR: Las credenciales NO se guardaron correctamente")
                    }
                } else {
                    println("⚠️ NO se guardaron credenciales - email o password vacíos")
                    println("⚠️ Email: '$email', Password length: ${submittedPassword.length}")
                }

                // Navegar según el estado del usuario
                // ✅ PRIORIDAD: Email → Cuenta rechazada → Onboarding → accountStatus
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
                        println("✅ Login exitoso → Main")
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
            isDarkMode = false,
            onNavigateBack = onNavigateBack,
            onPasswordSubmit = { password ->
                submittedPassword = password
                passwordError = null // Reset error
                println("🔐 Nuevo usuario - guardando credenciales automáticamente")
                viewModel.signIn(email, password, true) // ✅ rememberMe = TRUE para guardar credenciales
            },
            onForgotPassword = {
                showScamWarningBottomSheet = true
            },
            errorMessage = passwordError,
            isLoading = isLoading
        )

        // Bottom Sheet de advertencia de estafas
        if (showScamWarningBottomSheet) {
            ScamWarningBottomSheet(
                onDismiss = {
                    showScamWarningBottomSheet = false
                },
                onContinue = {
                    showScamWarningBottomSheet = false
                    // Navegar a ForgotPasswordEmailScreen con el email
                    onNavigateToForgotPassword(email)
                }
            )
        }
    }
}
