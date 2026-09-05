package com.ecomap.usuario.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.NuPrimaryButton
import com.ecomap.usuario.presentation.ui.components.NuSecondaryButton
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.NuColors
import com.ecomap.usuario.utils.BiometricHelper
import com.ecomap.usuario.utils.findActivity
import kotlinx.coroutines.delay
import android.util.Log
import android.widget.Toast
import com.ecomap.usuario.presentation.viewmodel.AuthUiState

/**
 * Pantalla de acceso biométrico estilo Nu para Usuarios
 * Se muestra después de crear cuenta cuando el usuario tiene biometría disponible
 */
@Composable
fun NuBiometricLoginScreen(
    userName: String = "Usuario",
    savedEmail: String,
    savedPassword: String,
    onNavigateToPasswordLogin: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val tag = "NuBiometricLoginScreen"
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    var biometricLaunched by remember { mutableStateOf(false) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    // Lanzar biometría automáticamente
    LaunchedEffect(Unit) {
        if (!biometricLaunched) {
            biometricLaunched = true
            delay(500) // Pequeño delay para que se vea la UI primero

            Log.d(tag, "🔍 Attempting to launch biometric prompt")
            Log.d(tag, "Context: ${context.javaClass.simpleName}")

            val activity = context.findActivity() as? FragmentActivity
            if (activity != null) {
                Log.d(tag, "✅ Activity found: ${activity.javaClass.simpleName}")
                if (BiometricHelper.isBiometricAvailable(context)) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Ingresa a EcoMap",
                        subtitle = "Usa tu huella o Face ID",
                        negativeButtonText = "Cancelar",
                        onSuccess = {
                            Log.d(tag, "✅ Biometría exitosa - iniciando sesión automática")
                            viewModel.signIn(savedEmail, savedPassword, true)
                        },
                        onError = { errorMessage: String ->
                            Log.e(tag, "❌ Error biométrico: $errorMessage")
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            onNavigateToPasswordLogin(savedEmail) // Fallback to password login
                        },
                        onFailed = {
                            Log.w(tag, "⚠️ Biometría fallida")
                            Toast.makeText(context, "Autenticación fallida, intenta de nuevo", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Log.w(tag, "⚠️ Biometría no disponible")
                    Toast.makeText(context, "Biometría no disponible, usa contraseña", Toast.LENGTH_SHORT).show()
                    onNavigateToPasswordLogin(savedEmail)
                }
            } else {
                Log.e(tag, "❌ No se pudo obtener la FragmentActivity para biometría")
                Toast.makeText(context, "No se pudo iniciar la autenticación biométrica", Toast.LENGTH_SHORT).show()
                onNavigateToPasswordLogin(savedEmail) // Fallback to password login
            }
        }
    }

    // Observar estado de login
    val authState by viewModel.authState.collectAsState()
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                Log.d(tag, "✅ Login exitoso - navegando a pantalla principal")
                onLoginSuccess()
            }
            is AuthUiState.Error -> {
                val errorMessage = (authState as AuthUiState.Error).message
                // ✅ Detectar fallo por credenciales incorrectas
                if (errorMessage.contains("Invalid login credentials", ignoreCase = true) ||
                    errorMessage.contains("Invalid email or password", ignoreCase = true)) {
                    Log.w(tag, "⚠️ Credenciales biométricas desactualizadas - limpiando y redirigiendo")
                    // Limpiar credenciales silenciosamente
                    BiometricHelper.clearBiometricCredentials(context)
                    // Redirigir a login con contraseña (email pre-llenado)
                    onNavigateToPasswordLogin(savedEmail)
                }
            }
            else -> { /* Idle o Loading */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Espacio superior
            Spacer(modifier = Modifier.height(60.dp))

            // Contenido central
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Ícono de usuario
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        modifier = Modifier.size(80.dp),
                        tint = NuColors.Primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mensaje de bienvenida personalizado
                    Text(
                        text = "Hola,",
                        fontSize = 24.sp,
                        color = NuColors.TextSecondary
                    )

                    Text(
                        text = userName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = NuColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Ícono de huella animado
                    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "fingerprint_scale"
                    )

                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Huella",
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        tint = NuColors.Primary.copy(alpha = 0.3f)
                    )
                }
            }

            // Botones inferiores
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    // Botón primario - Reintentar biometría
                    NuPrimaryButton(
                        text = "Ingresar con Face ID",
                        onClick = {
                            Log.d(tag, "🚀 Botón Face ID presionado")
                            val activity = context.findActivity() as? FragmentActivity
                            if (activity != null) {
                                Log.d(tag, "✅ Activity found: ${activity.javaClass.simpleName}")
                                if (BiometricHelper.isBiometricAvailable(context)) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = "Ingresa a EcoMap",
                                        subtitle = "Usa tu huella o Face ID",
                                        negativeButtonText = "Cancelar",
                                        onSuccess = {
                                            Log.d(tag, "✅ Biometría exitosa - iniciando sesión")
                                            viewModel.signIn(savedEmail, savedPassword, true)
                                        },
                                        onError = { errorMessage: String ->
                                            Log.e(tag, "❌ Error biométrico: $errorMessage")
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                            onNavigateToPasswordLogin(savedEmail)
                                        },
                                        onFailed = {
                                            Log.w(tag, "⚠️ Biometría fallida")
                                            Toast.makeText(context, "Autenticación fallida, intenta de nuevo", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Log.w(tag, "⚠️ Biometría no disponible")
                                    Toast.makeText(context, "Biometría no disponible, usa contraseña", Toast.LENGTH_SHORT).show()
                                    onNavigateToPasswordLogin(savedEmail)
                                }
                            } else {
                                Log.e(tag, "❌ No se pudo obtener la FragmentActivity para biometría")
                                Toast.makeText(context, "No se pudo iniciar la autenticación biométrica", Toast.LENGTH_SHORT).show()
                                onNavigateToPasswordLogin(savedEmail)
                            }
                        },
                        icon = Icons.Default.Fingerprint,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón secundario - Usar contraseña
                    NuSecondaryButton(
                        text = "Ingresar con contraseña",
                        onClick = {
                            Log.d(tag, "🚀 Botón Ingresar con contraseña presionado")
                            onNavigateToPasswordLogin(savedEmail)
                        },
                        textColor = NuColors.Primary
                    )
                }
            }
        }
    }
}
