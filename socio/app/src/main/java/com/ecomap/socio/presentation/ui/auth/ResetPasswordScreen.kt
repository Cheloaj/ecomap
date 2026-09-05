package com.ecomap.socio.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState

/**
 * Pantalla para crear una nueva contraseña
 * Incluye validaciones de seguridad y confirmación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    email: String,
    verificationCode: String,
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    businessViewModel: com.ecomap.socio.presentation.viewmodel.BusinessViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") } // Guardar la nueva contraseña para hacer login

    // Estado para el reset de contraseña (usaremos signInState temporalmente)
    val signInState by viewModel.signInState.collectAsState()
    val businesses by businessViewModel.businesses.collectAsState()

    // Observar estado de reset/login
    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                val user = (signInState as UiState.Success).data

                if (newPassword.isNotEmpty()) {
                    // Acabamos de hacer login después de resetear contraseña
                    println("✅ ResetPassword: Login exitoso después de resetear contraseña")
                    println("   - accountStatus: ${user.accountStatus}")
                    println("   - onboardingStep: ${user.onboardingStep}")

                    // ✅ ACTUALIZAR credenciales guardadas con la NUEVA contraseña
                    println("🔐 Actualizando credenciales guardadas con nueva contraseña...")
                    com.ecomap.socio.utils.BiometricHelper.saveBiometricCredentials(
                        context,
                        email,
                        newPassword,
                        user.fullName
                    )
                    println("✅ Credenciales actualizadas exitosamente (fullName: ${user.fullName})")

                    // Cargar negocios del usuario
                    businessViewModel.loadAllUserBusinesses()

                    // Esperar un momento para que se carguen los negocios
                    kotlinx.coroutines.delay(500)

                    // ✅ Navegar según el estado del usuario Y sus negocios
                    val hasApprovedBusiness = businesses.any { it.verificationStatus == "approved" }
                    println("   Total negocios: ${businesses.size}")
                    println("   Negocios aprobados: ${businesses.count { it.verificationStatus == "approved" }}")
                    println("   ¿Tiene al menos uno aprobado?: $hasApprovedBusiness")

                    when {
                        // Si es ACTIVE y tiene negocios aprobados → Main
                        user.accountStatus == "active" && hasApprovedBusiness -> {
                            println("✅ Usuario activo con negocios aprobados → Main")
                            onNavigateToMain()
                        }
                        // Si es ACTIVE pero NO tiene negocios aprobados → VerificationPending
                        user.accountStatus == "active" && !hasApprovedBusiness -> {
                            println("⏳ Usuario activo SIN negocios aprobados → VerificationPending")
                            onNavigateToVerificationPending()
                        }
                        // Si está pending → VerificationPending
                        user.accountStatus == "pending_verification" || user.accountStatus == "rejected" -> {
                            println("⏳ Usuario pendiente/rechazado → VerificationPending")
                            onNavigateToVerificationPending()
                        }
                        else -> {
                            println("⚠️ Estado desconocido → VerificationPending")
                            onNavigateToVerificationPending()
                        }
                    }
                    viewModel.resetSignInState()
                }
            }
            is UiState.Error -> {
                val errorMessage = (signInState as UiState.Error).message
                println("❌ ResetPassword: Error: $errorMessage")
                passwordError = errorMessage
                newPassword = "" // Limpiar para poder reintentar
            }
            else -> {}
        }
    }

    // Validaciones en tiempo real
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        // No navegar hacia atrás, usuario debe completar el reset
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = NuColors.TextSecondary.copy(alpha = 0.3f) // Deshabilitado
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Validar contraseñas
                    if (!hasMinLength || !hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecialChar) {
                        passwordError = "La contraseña no cumple con los requisitos de seguridad"
                        return@FloatingActionButton
                    }

                    if (password != confirmPassword) {
                        confirmPasswordError = "Las contraseñas no coinciden"
                        return@FloatingActionButton
                    }

                    // Guardar la contraseña para saber que el reset fue iniciado
                    newPassword = password

                    // Resetear contraseña (el ViewModel hará login automático)
                    println("🔐 ResetPassword: Actualizando contraseña para $email")
                    println("   Código de verificación: $verificationCode")
                    viewModel.resetPassword(email, verificationCode, password)
                },
                shape = CircleShape,
                containerColor = if (signInState !is UiState.Loading && hasMinLength && hasUpperCase &&
                    hasLowerCase && hasDigit && hasSpecialChar && passwordsMatch)
                    NuColors.Primary else NuColors.ButtonDisabled,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                // ✅ Mostrar CircularProgressIndicator o flecha
                if (signInState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Continuar"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Contenido superior
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Título
                Text(
                    text = "Crear nueva contraseña",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NuColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtítulo
                Text(
                    text = "Tu nueva contraseña debe cumplir con los siguientes requisitos de seguridad",
                    fontSize = 16.sp,
                    color = NuColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Campo de Nueva Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = NuColors.Primary,
                        unfocusedBorderColor = NuColors.TextSecondary.copy(alpha = 0.3f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (passwordError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = passwordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Confirmar Contraseña
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmPasswordError = null
                    },
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPasswordError != null,
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showConfirmPassword) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = NuColors.Primary,
                        unfocusedBorderColor = NuColors.TextSecondary.copy(alpha = 0.3f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (confirmPasswordError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = confirmPasswordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Requisitos de contraseña
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NuColors.Primary.copy(alpha = 0.05f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Requisitos de seguridad:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NuColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PasswordRequirement(text = "Mínimo 8 caracteres", isMet = hasMinLength)
                        PasswordRequirement(text = "Al menos una mayúscula", isMet = hasUpperCase)
                        PasswordRequirement(text = "Al menos una minúscula", isMet = hasLowerCase)
                        PasswordRequirement(text = "Al menos un número", isMet = hasDigit)
                        PasswordRequirement(text = "Al menos un carácter especial", isMet = hasSpecialChar)
                        PasswordRequirement(text = "Las contraseñas coinciden", isMet = passwordsMatch)
                    }
                }
            }

            // ✅ Espaciador inferior (botón eliminado - ahora es FAB)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ✅ Loading indicator ahora está en el FAB, no necesitamos overlay
}

/**
 * Componente para mostrar un requisito de contraseña
 */
@Composable
fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isMet) "✓" else "○",
            fontSize = 16.sp,
            color = if (isMet) NuColors.Primary else NuColors.TextSecondary.copy(alpha = 0.5f),
            fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isMet) NuColors.TextPrimary else NuColors.TextSecondary,
            fontWeight = if (isMet) FontWeight.Medium else FontWeight.Normal
        )
    }
}
