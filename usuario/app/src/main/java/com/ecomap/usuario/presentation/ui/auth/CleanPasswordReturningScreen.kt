package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.ForgotPasswordBottomSheet
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthUiState

/**
 * Wrapper para CleanPasswordScreen - Usuarios Registrados (modo oscuro)
 * Para usuarios que presionan "Ingresar con contraseña" desde NuBiometricLoginScreen
 * Navega al dashboard después de login exitoso
 */
@Composable
fun CleanPasswordReturningScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToWelcome: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordBottomSheet by remember { mutableStateOf(false) }

    // Manejar estados de signIn
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                isLoading = false
                passwordError = null
                println("✅ Login exitoso (usuario registrado)")
                // ✅ Navegar al dashboard directamente
                onNavigateToMain()
            }
            is AuthUiState.Error -> {
                isLoading = false
                val errorMessage = (authState as AuthUiState.Error).message.lowercase()

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
            is AuthUiState.Loading -> {
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
