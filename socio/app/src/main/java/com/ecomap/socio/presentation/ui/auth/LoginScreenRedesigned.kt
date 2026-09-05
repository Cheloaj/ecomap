package com.ecomap.socio.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.presentation.ui.theme.*
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.UiState
import kotlinx.coroutines.delay

@Composable
fun LoginScreenRedesigned(
    onNavigateToRegister: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val signInState by viewModel.signInState.collectAsState()
    val scrollState = rememberScrollState()

    // Animaciones de entrada
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true

        // Cargar credenciales guardadas
        val savedCredentials = BiometricHelper.getSavedCredentials(context)
        if (savedCredentials != null) {
            email = savedCredentials.first
            password = savedCredentials.second
            rememberMe = true
        }
    }

    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                val user = (signInState as UiState.Success).data
                if (user.emailVerified) {
                    if (rememberMe && email.isNotBlank() && password.isNotBlank()) {
                        BiometricHelper.saveBiometricCredentials(context, email, password, user.fullName)
                    } else if (!rememberMe) {
                        BiometricHelper.clearBiometricCredentials(context)
                    }
                    onNavigateToMain()
                }
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppleSpacing.large)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(AppleSpacing.xxLarge))

            // Logo animado
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM) +
                        scaleIn(
                            initialScale = 0.8f,
                            animationSpec = AppleAnimations.springBouncy
                        )
            ) {
                Surface(
                    modifier = Modifier.size(AppleIconSize.hero),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = AppleElevation.large
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(AppleIconSize.extraLarge),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.large))

            // Título
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 50)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(AppleSpacing.small))

                    Text(
                        text = "Inicia sesión para continuar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xxLarge))

            // Campo Email
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 100)
            ) {
                AppleTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    error = signInState is UiState.Error && email.isBlank()
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.medium))

            // Campo Password
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 150)
            ) {
                AppleTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    onTrailingIconClick = { passwordVisible = !passwordVisible },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    error = signInState is UiState.Error && password.isBlank()
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.medium))

            // Recordarme
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 200)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleCheckbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                    Spacer(modifier = Modifier.width(AppleSpacing.small))
                    Text(
                        text = "Recordarme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.large))

            // Error message
            AnimatedVisibility(
                visible = signInState is UiState.Error,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_FAST),
                exit = AppleAnimations.fadeOutSlideDown(AppleAnimations.DURATION_FAST)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(AppleSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(AppleIconSize.medium)
                        )
                        Spacer(modifier = Modifier.width(AppleSpacing.small))
                        Text(
                            text = (signInState as? UiState.Error)?.message ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.medium))

            // Botón Iniciar Sesión
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 250)
            ) {
                AppleButton(
                    text = "Iniciar Sesión",
                    onClick = {
                        viewModel.signIn(email, password, rememberMe)
                    },
                    isLoading = signInState is UiState.Loading,
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(AppleSpacing.large))

            // Link Registrarse
            AnimatedVisibility(
                visible = showContent,
                enter = AppleAnimations.fadeInSlideUp(AppleAnimations.DURATION_MEDIUM + 300)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿No tienes cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Regístrate",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppleSpacing.xxLarge))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    error: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = AppleAnimations.springBouncy,
        label = "textfield_scale"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingIcon = trailingIcon?.let {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(
                        imageVector = it,
                        contentDescription = null
                    )
                }
            }
        },
        visualTransformation = visualTransformation,
        isError = error,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
    )
}

@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = AppleAnimations.springBouncy,
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = AppleElevation.small,
            pressedElevation = AppleElevation.none
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AppleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = AppleAnimations.springBouncy,
        label = "checkbox_scale"
    )

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.scale(scale)
    )
}
