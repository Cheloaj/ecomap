package com.ecomap.usuario.presentation.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.* // <-- IMPORTACIÓN DE ANIMACIÓN (slideInVertically, fadeOut, etc.)
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- IMPORTACIÓN DE CLICKABLE
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.ui.components.NuPrimaryButton
import com.ecomap.usuario.presentation.viewmodel.AuthUiState
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.utils.BiometricHelper
import com.ecomap.usuario.utils.findActivity

/**
 * Pantalla de login rápido con autenticación biométrica
 *
 * Flujo:
 * 1. Muestra "Hola, [Nombre]"
 * 2. Auto-lanza Face ID si autoLaunchBiometric = true (Prioridad)
 * 3. Si Face ID exitoso → Inicia sesión y navega a Main INMEDIATAMENTE.
 * 4. Usuario solo tiene opción de usar Face ID (o botón de ingresar con contraseña si falla).
 */
@Composable
fun QuickLoginScreen(
    email: String,
    password: String,
    displayName: String,
    autoLaunchBiometric: Boolean = false,
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit, // <-- Agregamos navegación a login manual si falla
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    var biometricLaunched by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showRetryButton by remember { mutableStateOf(false) }

    val isLoading = authState is AuthUiState.Loading
    val errorMessage = (authState as? AuthUiState.Error)?.message

    // Animación del ícono de huella
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fingerprint_scale"
    )

    // Función que se llama al tener éxito biométrico
    val handleBiometricSuccess = {
        Log.d("QuickLoginScreen", "✅ Biometría exitosa. Iniciando sesión y navegando.")
        BiometricHelper.markBiometricUsedOnce(context)
        isAuthenticating = true
        viewModel.signIn(email, password, saveBiometric = true)
        onNavigateToMain() // <-- NAVEGACIÓN INMEDIATA (SOLUCIÓN AL BUG DE CARGA)
    }

    // Función que se llama al fallar la biometría
    val handleBiometricError = { error: String ->
        Log.e("QuickLoginScreen", "❌ Error Face ID: $error")
        if (error != "Autenticación cancelada") {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        showRetryButton = true // Mostrar el botón de reintento/login manual
    }

    // Auto-lanzar biometría si corresponde
    LaunchedEffect(Unit) {
        if (autoLaunchBiometric && !biometricLaunched) {
            Log.d("QuickLoginScreen", "🔐 Auto-lanzando Face ID...")
            biometricLaunched = true

            val activity = context.findActivity()
            if (activity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Bienvenido de nuevo",
                    subtitle = "Use Face ID o huella digital para continuar",
                    negativeButtonText = "Cancelar",
                    onSuccess = handleBiometricSuccess,
                    onError = handleBiometricError,
                    onFailed = {
                        Log.w("QuickLoginScreen", "⚠️ Face ID fallido (intentar de nuevo)")
                        showRetryButton = true
                    }
                )
            } else {
                handleBiometricError("Error al inicializar autenticación")
            }
        } else {
            // Si no se autolanzó (ej: el usuario canceló en el splash)
            showRetryButton = true
        }
    }

    // Mostrar errores de Supabase
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error: String ->
            Log.e("QuickLoginScreen", "❌ Error de login: $error")
            Toast.makeText(context, "Error de sesión: $error. Ingrese contraseña.", Toast.LENGTH_LONG).show()
            viewModel.resetSignInState()
            isAuthenticating = false
            showRetryButton = true
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5E35B1), // Purple 700
                        Color(0xFF4527A0)  // Purple 800
                    )
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Saludo personalizado
            Text(
                text = "Hola,",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = displayName,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Icono Central: Animado o Carga
            if (isAuthenticating || isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .clickable(enabled = BiometricHelper.isBiometricEnrolled(context)) {
                            // Clic en el centro para lanzar el prompt de nuevo
                            val activity = context.findActivity()
                            if (activity != null) {
                                BiometricHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = "Autenticación Biométrica",
                                    subtitle = "Use Face ID o huella digital para continuar",
                                    negativeButtonText = "Cancelar",
                                    onSuccess = handleBiometricSuccess,
                                    onError = handleBiometricError,
                                    onFailed = { showRetryButton = true }
                                )
                            }
                        },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Huella digital",
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(48.dp))

            // Botón de Login Manual (Aparece si falla la biometría o el auto-lanzamiento)
            AnimatedVisibility(
                visible = showRetryButton && !isAuthenticating,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                NuPrimaryButton(
                    text = "Ingresar con Contraseña",
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !isAuthenticating,
                    backgroundColor = Color.White,
                    textColor = Color(0xFF5E35B1)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador de carga
            if (isLoading || isAuthenticating) {
                Text(
                    text = "Verificando credenciales...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            } else if (!BiometricHelper.isBiometricEnrolled(context)) {
                Text(
                    text = "Configura Face ID o Huella para acceso rápido.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}