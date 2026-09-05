package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.ErrorMessageHelper
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de Ingreso de Contraseña - Paso 2 del Login
 * Con animación slideInVertically desde abajo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPasswordScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToDocumentVerification: (String) -> Unit,
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val signInState by viewModel.signInState.collectAsState()
    val scrollState = rememberScrollState()

    // Resetear el estado de error cuando el usuario cambia la contraseña
    LaunchedEffect(password) {
        if (signInState is UiState.Error && password.isNotEmpty()) {
            println("🔄 Reseteando signInState porque el usuario está escribiendo")
            viewModel.resetSignInState()
        }
    }

    // Navegación según estado del usuario
    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                println("✅ SignIn exitoso")
                val user = (signInState as UiState.Success).data

                // ✅ Auto-actualizar credenciales si:
                // 1. Usuario tiene "Recordarme" activado AHORA, o
                // 2. Usuario tenía biometría habilitada previamente (credenciales desactualizadas)
                val hadBiometricEnabled = BiometricHelper.isBiometricEnabled(context)

                if ((rememberMe || hadBiometricEnabled) && email.isNotBlank() && password.isNotBlank()) {
                    println("✅ Actualizando credenciales biométricas (rememberMe: $rememberMe, hadBiometric: $hadBiometricEnabled)")
                    BiometricHelper.saveBiometricCredentials(context, email, password, user.fullName)
                } else if (!rememberMe && !hadBiometricEnabled) {
                    BiometricHelper.clearBiometricCredentials(context)
                }

                // Navegar según el estado del usuario
                // PRIORIDAD: Onboarding PRIMERO, luego accountStatus
                when {
                    !user.emailVerified -> {
                        println("⚠️ Email no verificado → EmailVerification")
                        onNavigateToEmailVerification(user.email)
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
                        println("❌ Estado inesperado → Redirigir a Onboarding por seguridad")
                        println("   user.onboardingStep: ${user.onboardingStep}")
                        println("   user.accountStatus: ${user.accountStatus}")
                        onNavigateToOnboarding(user.id)
                    }
                }
            }
            is UiState.Error -> {
                val errorMsg = (signInState as UiState.Error).message
                println("❌ SignIn ERROR: $errorMsg")
                println("📍 El mensaje de error debería mostrarse ahora en la UI")
            }
            is UiState.Loading -> {
                println("⏳ SignIn LOADING...")
            }
            else -> {}
        }
    }

    var showContent by remember { mutableStateOf(false) }

    // Animación slideInVertically desde abajo
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val isPasswordValid = password.isNotBlank() && password.length >= 8

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = NuColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // FAB CIRCULAR PERFECTO
            FloatingActionButton(
                onClick = {
                    if (isPasswordValid && signInState !is UiState.Loading) {
                        println("🔑 Intentando iniciar sesión...")
                        viewModel.signIn(email, password, rememberMe)
                    }
                },
                shape = CircleShape,
                containerColor = if (isPasswordValid && signInState !is UiState.Loading) NuColors.Primary else NuColors.ButtonDisabled,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Iniciar sesión"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End, // Posición estándar para el FAB
        containerColor = NuColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = "Ahora ingresa tu contraseña",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary,
                        lineHeight = 38.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Contraseña para $email",
                        fontSize = 16.sp,
                        color = NuColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Campo de contraseña minimalista
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                )
            ) {
                MinimalistPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    passwordVisible = passwordVisible,
                    onVisibilityToggle = { passwordVisible = !passwordVisible }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox "Recordarme"
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NuColors.Primary,
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    "Recordarme",
                    fontSize = 14.sp,
                    color = NuColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enlace "Olvidé la contraseña"
            TextButton(
                onClick = { /* TODO: Implementar recuperación de contraseña */ }
            ) {
                Text(
                    text = "Olvidé la contraseña",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = NuColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de error con animación
            AnimatedVisibility(
                visible = signInState is UiState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NuColors.Error.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NuColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (signInState is UiState.Error) {
                                ErrorMessageHelper.getFriendlyMessage(
                                    (signInState as UiState.Error).message
                                )
                            } else "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = NuColors.Error,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
        }

        // Loading overlay
        NuLoadingStateView(
            isLoading = signInState is UiState.Loading,
            message = "Iniciando sesión..."
        )
    }
}

/**
 * Campo de contraseña minimalista
 * Solo línea inferior en gris claro, sin bordes ni fondo
 */
@Composable
private fun MinimalistPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val lineColor = Color.Gray.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Campo de texto
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = NuColors.TextPrimary,
                    fontFamily = FontFamily.SansSerif
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                cursorBrush = SolidColor(lineColor),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            )

            // Ícono de visibilidad
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = NuColors.TextSecondary
                )
            }
        }

        // Línea inferior, siempre gris
        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = lineColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
