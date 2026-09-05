package com.ecomap.usuario.presentation.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.ui.theme.AppleColors
import com.ecomap.usuario.utils.BiometricHelper
import com.ecomap.usuario.utils.getFirstName
import kotlinx.coroutines.delay
import android.util.Log
import android.widget.Toast

/**
 * Splash Screen para EcoMapUsuario
 * LÓGICA CORREGIDA: Prioriza QuickLogin/Biometría si hay credenciales guardadas.
 */
@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToQuickLogin: (String, String, String, Boolean) -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    var navigationHandled by remember { mutableStateOf(false) }
    var isLoadingComplete by remember { mutableStateOf(false) }

    // Cargar usuario
    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "🚀 Iniciando...")
        delay(500)
        viewModel.checkUserLoggedIn()
        delay(1000)
        isLoadingComplete = true
        Log.d("SplashScreen", "✅ Carga completa, procesando navegación...")
    }

    // Manejar navegación SOLO cuando la carga esté completa
    LaunchedEffect(isLoadingComplete, currentUser) {
        if (!isLoadingComplete) return@LaunchedEffect
        if (navigationHandled) return@LaunchedEffect

        Log.d("SplashScreen", "✅ Revisando usuario...")
        val user = currentUser

        // 1. Obtener credenciales guardadas (Biometría / Auto-Login)
        val savedCredentials = try {
            BiometricHelper.getSavedCredentials(context)
        } catch (e: Exception) {
            Log.e("SplashScreen", "❌ Error al obtener credenciales: ${e.message}")
            null
        }

        val isBiometricEnrolled = BiometricHelper.isBiometricEnrolled(context)
        val shouldAutoLaunchBiometric = savedCredentials != null && isBiometricEnrolled

        Log.d("SplashScreen", "   currentUser: $user")
        Log.d("SplashScreen", "   savedCredentials: ${if (savedCredentials != null) "SÍ" else "NO"}")
        Log.d("SplashScreen", "   isBiometricEnrolled: $isBiometricEnrolled")

        when {
            // PRIORIDAD 1: QUICKLOGIN (Face ID / Auto-Login)
            savedCredentials != null -> {
                val (email, password, fullName) = savedCredentials
                val displayName = getFirstName(fullName)

                Log.d("SplashScreen", "🔐 Credenciales guardadas → QuickLogin")
                navigationHandled = true
                onNavigateToQuickLogin(email, password, displayName, shouldAutoLaunchBiometric)
            }

            // PRIORIDAD 2: SESIÓN ACTIVA (Si el usuario ya pasó el QuickLogin o usó login manual)
            user != null -> {
                Log.d("SplashScreen", "✅ Sesión activa → Main")
                navigationHandled = true
                onNavigateToMain()
            }

            // PRIORIDAD 3: SIN NADA → WELCOME
            else -> {
                Log.d("SplashScreen", "❌ Primera vez → Welcome")
                navigationHandled = true
                onNavigateToWelcome()
            }
        }
    }

    // UI (Pantalla de carga con logo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo de la app
            Image(
                painter = painterResource(id = com.ecomap.usuario.R.drawable.logo),
                contentDescription = "EcoMap Logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(color = AppleColors.IOSBlue)
        }
    }
}