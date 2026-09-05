package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecomap.socio.presentation.ui.components.NuFloatingActionButton
import com.ecomap.socio.presentation.ui.components.NuTextField
import com.ecomap.socio.presentation.ui.components.NuLoadingStateView
import com.ecomap.socio.ui.theme.NuColors
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.utils.UiState

/**
 * Pantalla de ingreso de correo estilo Nu
 * Primera pantalla del flujo de login
 */
@Composable
fun NuLoginEmailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPassword: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailCheckState by viewModel.emailCheckState.collectAsState()
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Manejar estados de verificación de email
    LaunchedEffect(emailCheckState) {
        when (emailCheckState) {
            is UiState.Success -> {
                val emailExists = (emailCheckState as UiState.Success<Boolean>).data
                if (emailExists) {
                    // Email existe, navegar a pantalla de contraseña
                    errorMessage = null
                    viewModel.resetEmailCheckState()
                    onNavigateToPassword(email)
                } else {
                    // Email NO existe
                    errorMessage = "Este correo no está registrado. Por favor, regístrate primero."
                }
            }
            is UiState.Error -> {
                errorMessage = (emailCheckState as UiState.Error).message
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = NuColors.Background
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
                            text = "Escribe tu correo\nelectrónico",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuColors.TextPrimary,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // TextField de correo
                        NuTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                // Limpiar mensaje de error al escribir
                                errorMessage = null
                            },
                            placeholder = "tu@correo.com",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            singleLine = true
                        )

                        // Mensaje de error
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage!!,
                                fontSize = 14.sp,
                                color = NuColors.Error
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
                            if (isEmailValid) {
                                // Verificar si el email existe antes de navegar
                                viewModel.checkEmailExists(email)
                            }
                        },
                        enabled = isEmailValid && emailCheckState !is UiState.Loading
                    )
                }
            }
        }

        // Loading overlay
        NuLoadingStateView(
            isLoading = emailCheckState is UiState.Loading,
            message = "Verificando correo..."
        )
    }
}
