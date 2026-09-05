package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.ui.components.NuSecondaryButton
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de ingreso de contraseña estilo Nu
 * Se desliza desde abajo después de ingresar el correo
 *
 * ESPECIFICACIONES:
 * - Título: "Ahora ingresa tu contraseña" (FontWeight.Bold, FontFamily.SansSerif, 32.sp)
 * - Campo minimalista: Solo línea inferior en gris claro (siempre), sin bordes, sin fondo
 * - Placeholder: "8 dígitos o más" (transparente, FontFamily.SansSerif, 32.sp, fijo encima de la línea)
 * - Ícono de candado (leadingIcon) e ícono de visibilidad (trailingIcon)
 * - FAB circular perfecto, sube con el teclado
 * - Espacio reducido entre título y campo (16.dp)
 *
 * NOTA: Asegúrate de configurar la actividad con `window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)`
 * en el `onCreate` para que `imePadding()` funcione correctamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuLoginPasswordScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val signInState by viewModel.signInState.collectAsState()
    val isPasswordValid = password.length >= 8
    val scrollState = rememberScrollState()

    // Animación de entrada deslizante
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Manejar estados de login
    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                errorMessage = null
                onLoginSuccess()
            }
            is UiState.Error -> {
                errorMessage = (signInState as UiState.Error).message
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NuColors.Background,
            floatingActionButton = {
                // FAB CIRCULAR PERFECTO
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(600, delayMillis = 200)
                    ),
                    modifier = Modifier.padding(24.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (isPasswordValid) {
                                viewModel.signIn(email, password, false)
                            }
                        },
                        shape = CircleShape,
                        containerColor = if (isPasswordValid && signInState !is UiState.Loading)
                            NuColors.Primary else NuColors.ButtonDisabled,
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continuar"
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End // Posición estándar para el FAB
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Botón atrás
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

                // Contenido principal con animación deslizante
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(500)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(scrollState)
                            .imePadding(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))

                        // Título
                        Text(
                            text = "Ahora ingresa tu\ncontraseña",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = NuColors.TextPrimary,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp)) // Espacio reducido

                        // Campo de contraseña
                        MinimalistPasswordField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "8 dígitos o más",
                            passwordVisible = passwordVisible,
                            onVisibilityToggle = { passwordVisible = !passwordVisible }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Opción "Olvidé la contraseña"
                        NuSecondaryButton(
                            text = "Olvidé la contraseña",
                            onClick = { /* TODO */ },
                            textColor = NuColors.Primary
                        )

                        // Mensaje de error
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = errorMessage!!,
                                fontSize = 14.sp,
                                color = NuColors.Error,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
                    }
                }
            }
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
 * Solo línea inferior en gris claro (siempre), sin bordes ni fondo
 * Placeholder transparente (32.sp), fijo encima de la línea
 * Ícono de candado (leadingIcon) e ícono de visibilidad (trailingIcon)
 */
@Composable
private fun MinimalistPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val lineColor = Color.Gray.copy(alpha = 0.5f) // Gris claro, siempre

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono de candado
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Contraseña",
                tint = NuColors.TextSecondary,
                modifier = Modifier.padding(end = 8.dp)
            )

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
                        // Placeholder transparente fijo encima de la línea
                        Text(
                            text = placeholder,
                            fontSize = 32.sp, // Mismo tamaño que el título
                            fontWeight = FontWeight.Bold,
                            color = Color.Transparent, // Invisible
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.offset(y = (-12).dp) // Encima de la línea
                        )
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