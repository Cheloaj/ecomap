package com.ecomap.usuario.presentation.ui.auth

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
import com.ecomap.usuario.presentation.ui.components.NuFloatingActionButton
import com.ecomap.usuario.presentation.ui.components.NuTextField
import com.ecomap.usuario.ui.theme.AppleColors
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay

/**
 * Pantalla de ingreso de email estilo Nu
 * Segunda pantalla del flujo de registro
 */
@Composable
fun RegisterEmailScreen(
    fullName: String,
    onNavigateBack: () -> Unit,
    onNavigateToPassword: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
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
                            text = "Escribe tu correo\nelectrónico",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleColors.Label,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // TextField de correo
                        NuTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            singleLine = true
                        )
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
                                onNavigateToPassword(fullName, email.trim())
                            }
                        },
                        enabled = isEmailValid,
                        backgroundColor = AppleColors.IOSGreen
                    )
                }
            }
        }
    }
}
