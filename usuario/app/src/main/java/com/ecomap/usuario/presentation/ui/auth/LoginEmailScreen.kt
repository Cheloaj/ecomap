package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.UiState
import kotlinx.coroutines.delay

/**
 * Pantalla de Ingreso de Correo - Paso 1 del Login
 *
 * ESPECIFICACIONES:
 * - Título: "Escribe tu correo electrónico" (FontWeight.Bold, FontFamily.SansSerif)
 * - Campo minimalista: Solo línea inferior en gris claro (siempre), sin bordes, sin fondo
 * - Etiqueta transparente fija encima de la línea inferior
 * - SIN ícono de correo, SIN texto "Ingresa el correo de tu cuenta"
 * - FAB circular perfecto, sube con el teclado
 * - Espacio reducido entre título y campo (16.dp)
 *
 * NOTA: Asegúrate de configurar la actividad con `window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)`
 * en el `onCreate` para que `imePadding()` funcione correctamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginEmailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPassword: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var isFocused by remember { mutableStateOf(false) }

    var showContent by remember { mutableStateOf(false) }

    val emailCheckState by viewModel.emailCheckState.collectAsState()

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    // Validar formato cuando el usuario deja de escribir
    LaunchedEffect(email) {
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            delay(800) // Esperar 800ms después de que dejó de escribir
            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Formato de correo inválido"
            }
        } else {
            emailError = null
        }
    }

    // Manejar resultado de verificación de email
    LaunchedEffect(emailCheckState) {
        when (emailCheckState) {
            is UiState.Success -> {
                isCheckingEmail = false
                val emailExists = (emailCheckState as UiState.Success).data
                if (emailExists) {
                    println("✅ Email encontrado en BD → Avanzar a contraseña")
                    viewModel.resetEmailCheckState()
                    onNavigateToPassword(email)
                } else {
                    println("❌ Email NO encontrado en BD")
                    emailError = "Este correo no está registrado"
                    viewModel.resetEmailCheckState() // ✅ Resetear estado
                }
            }
            is UiState.Error -> {
                isCheckingEmail = false
                emailError = "Error al verificar el correo"
                println("❌ Error al verificar email: ${(emailCheckState as UiState.Error).message}")
                viewModel.resetEmailCheckState() // ✅ Resetear estado
            }
            is UiState.Loading -> {
                isCheckingEmail = true
                emailError = null
            }
            else -> {
                isCheckingEmail = false
            }
        }
    }

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
                    if (isEmailValid && !isCheckingEmail) {
                        // ✅ Verificar si el email existe en la BD antes de avanzar
                        println("🔍 Verificando email en BD: $email")
                        emailError = null
                        viewModel.checkEmailExists(email)
                    }
                },
                shape = CircleShape,
                containerColor = if (isEmailValid && !isCheckingEmail) NuColors.Primary else NuColors.Divider,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                if (isCheckingEmail) {
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
        floatingActionButtonPosition = FabPosition.End, // Posición estándar para el FAB
        containerColor = NuColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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

                // TÍTULO: "Escribe tu correo electrónico" - FontWeight.Bold
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                        initialOffsetY = { -50 },
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                ) {
                    Text(
                        text = "Escribe tu correo electrónico",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = NuColors.TextPrimary,
                        lineHeight = 38.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espacio reducido entre título y campo

                // CAMPO MINIMALISTA - Solo línea inferior, etiqueta transparente encima
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column {
                        MinimalistEmailField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null // Reset error al escribir
                            },
                            isFocused = isFocused,
                            onFocusChange = { isFocused = it },
                            isError = emailError != null
                        )

                        // Mensaje de error
                        AnimatedVisibility(
                            visible = emailError != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = emailError ?: "",
                                fontSize = 14.sp,
                                color = NuColors.Error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
            }
        }
    }
}

/**
 * Campo de email minimalista
 * Solo línea inferior en gris claro (siempre), sin bordes ni fondo
 * Etiqueta transparente fija encima de la línea
 */
@Composable
private fun MinimalistEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    isError: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val lineColor = if (isError) NuColors.Error else Color.Gray.copy(alpha = 0.5f) // Rojo si hay error

    Box(modifier = Modifier.fillMaxWidth()) {
        // Campo de texto
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                },
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = NuColors.TextPrimary,
                fontFamily = FontFamily.SansSerif
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            cursorBrush = SolidColor(lineColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Etiqueta transparente fija encima de la línea
                    Text(
                        text = "Correo electrónico",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Transparent, // Etiqueta invisible
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.offset(y = (-12).dp) // Posicionada justo encima de la línea
                    )
                    innerTextField()
                }
            }
        )

        // Línea inferior, siempre gris
        HorizontalDivider(
            thickness = if (isFocused) 2.dp else 1.dp,
            color = lineColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
