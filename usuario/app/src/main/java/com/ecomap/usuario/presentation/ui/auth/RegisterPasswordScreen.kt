package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.NuFloatingActionButton
import com.ecomap.usuario.presentation.ui.components.NuTextField
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthUiState
import com.ecomap.usuario.ui.theme.AppleColors
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay

/**
 * Pantalla de ingreso de contraseña estilo Nu
 * Tercera pantalla del flujo de registro
 */
@Composable
fun RegisterPasswordScreen(
    fullName: String,
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToOTP: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val isPasswordValid = password.length >= 6

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Manejar registro exitoso
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            onNavigateToOTP(email)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppleColors.Background
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
                        tint = AppleColors.Label
                    )
                }

                // Contenido principal
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(400)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))

                        // Título
                        Text(
                            text = "Crea una\ncontraseña segura",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleColors.Label,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subtítulo
                        Text(
                            text = "Mínimo 6 caracteres",
                            fontSize = 16.sp,
                            color = AppleColors.SecondaryLabel
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // TextField de contraseña
                        NuTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Contraseña",
                            leadingIcon = Icons.Default.Lock,
                            trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            onTrailingIconClick = { passwordVisible = !passwordVisible },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            singleLine = true
                        )

                        // Mensaje de error
                        if (authState is AuthUiState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (authState as AuthUiState.Error).message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // FAB para continuar
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(600, delayMillis = 200)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    NuFloatingActionButton(
                        onClick = {
                            if (isPasswordValid) {
                                // Registrar usuario
                                viewModel.signUp(email, password, fullName)
                            }
                        },
                        enabled = isPasswordValid && authState !is AuthUiState.Loading,
                        isLoading = authState is AuthUiState.Loading,
                        backgroundColor = AppleColors.IOSGreen
                    )
                }
            }
        }
    }
}
