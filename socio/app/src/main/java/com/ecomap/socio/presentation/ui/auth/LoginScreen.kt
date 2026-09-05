package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.ui.components.NuPrimaryButton
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import com.ecomap.socio.presentation.ui.components.NuTextField
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import com.ecomap.socio.utils.findActivity
import com.ecomap.socio.utils.rememberSingleExecution
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToDocumentVerification: (String) -> Unit,
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var biometricPromptShown by remember { mutableStateOf(false) }

    val signInState by viewModel.signInState.collectAsState()
    val scrollState = rememberScrollState()

    // 🛡️ FASE 1: Protección contra clics rápidos (500ms cooldown)
    val executeSafely = rememberSingleExecution()

    // Intentar login biométrico automático si hay credenciales guardadas
    LaunchedEffect(Unit) {
        println("\n🔐 === INICIO DE SESIÓN BIOMÉTRICO ===")

        // Verificar si la biometría está disponible
        val isBiometricAvailable = BiometricHelper.isBiometricAvailable(context)
        println("  isBiometricAvailable: $isBiometricAvailable")

        // Verificar si hay credenciales guardadas
        val savedCredentials = BiometricHelper.getSavedCredentials(context)
        println("  savedCredentials: ${if (savedCredentials != null) "✅ Encontradas" else "❌ No hay"}")

        // Verificar el tipo de contexto y activity
        println("  Tipo de contexto: ${context.javaClass.name}")
        println("  Activity encontrada: ${if (activity != null) "✅ ${activity.javaClass.simpleName}" else "❌ NULL"}")

        if (isBiometricAvailable && savedCredentials != null && activity != null && !biometricPromptShown) {
            biometricPromptShown = true
            println("  🚀 Mostrando prompt biométrico...")

            // Cargar email y password en el estado
            email = savedCredentials.first
            password = savedCredentials.second
            rememberMe = true

            // Mostrar prompt biométrico
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Inicio de Sesión",
                subtitle = "Use su huella digital para continuar",
                negativeButtonText = "Cancelar",
                onSuccess = {
                    println("  ✅ Biometría exitosa → Iniciando sesión")
                    viewModel.signIn(email, password, rememberMe)
                },
                onError = { error ->
                    println("  ❌ Error biométrico: $error")
                },
                onFailed = {
                    println("  ❌ Autenticación biométrica falló")
                }
            )
        } else {
            println("  ℹ️ No se muestra biometría:")
            if (!isBiometricAvailable) println("    - Biometría no disponible")
            if (savedCredentials == null) println("    - No hay credenciales guardadas")
            if (activity == null) println("    - Activity es null")
            if (biometricPromptShown) println("    - Ya se mostró el prompt")
        }

        println("=== FIN INICIO DE SESIÓN BIOMÉTRICO ===\n")
    }

    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                val user = (signInState as UiState.Success).data
                // Guardar credenciales si "Recordarme" está activado
                if (rememberMe && email.isNotBlank() && password.isNotBlank()) {
                    BiometricHelper.saveBiometricCredentials(context, email, password, user.fullName)
                } else if (!rememberMe) {
                    BiometricHelper.clearBiometricCredentials(context)
                }

                // Navegar directamente según el estado del usuario
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
                        println("✅ Login exitoso → Main")
                        onNavigateToMain()
                    }
                    else -> {
                        println("❌ Estado inválido → VerificationPending")
                        println("   - accountStatus: ${user.accountStatus}")
                        println("   - onboardingStep: ${user.onboardingStep}")
                        onNavigateToVerificationPending()
                    }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título con animación de entrada estilo Nu
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { -50 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bienvenido",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Inicia sesión en tu cuenta",
                        fontSize = 16.sp,
                        color = NuColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Formulario con animación
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                )
            ) {
                Column {
                    // TextField de correo estilo Nu
                    NuTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "tu@correo.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // TextField de contraseña estilo Nu
                    NuTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "8 dígitos o más",
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = NuColors.TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkbox "Recordarme"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NuColors.Primary,
                                checkmarkColor = NuColors.TextOnPrimary
                            )
                        )
                        Text(
                            "Recordarme",
                            fontSize = 14.sp,
                            color = NuColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Mensaje de error
                    if (signInState is UiState.Error) {
                        Text(
                            text = ErrorMessageHelper.getFriendlyMessage(
                                (signInState as UiState.Error).message
                            ),
                            fontSize = 14.sp,
                            color = NuColors.Error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Botón de inicio de sesión estilo Nu
                    NuPrimaryButton(
                        text = "Iniciar Sesión",
                        onClick = {
                            // 🛡️ FASE 1: Protección contra doble clic (evita múltiples logins)
                            executeSafely {
                                viewModel.signIn(email, password, rememberMe)
                            }
                        },
                        enabled = email.isNotBlank() && password.isNotBlank() && signInState !is UiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de registro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "¿No tienes cuenta? ",
                            fontSize = 14.sp,
                            color = NuColors.TextSecondary
                        )
                        NuSecondaryButton(
                            text = "Regístrate",
                            onClick = onNavigateToRegister,
                            textColor = NuColors.Primary
                        )
                    }
                }
            }
        }

        // Loading overlay estilo Nu
        NuLoadingStateView(
            isLoading = signInState is UiState.Loading,
            message = "Iniciando sesión..."
        )
    }
}
