package com.ecomap.socio.presentation.ui.splash

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
import com.ecomap.socio.presentation.viewmodel.AuthViewModel
import com.ecomap.socio.presentation.viewmodel.BusinessViewModel
import com.ecomap.socio.ui.theme.NuColors
import com.ecomap.socio.utils.BiometricHelper
import com.ecomap.socio.utils.getFirstName
import kotlinx.coroutines.delay
import android.util.Log
import android.widget.Toast

/**
 * Splash Screen sin permisos iniciales
 * Los permisos se solicitarán contextualmente cuando sean necesarios
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToQuickLogin: (String, String, String, Boolean) -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToDocumentVerification: (String) -> Unit,
    onNavigateToVerificationPending: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    businessViewModel: BusinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val businesses by businessViewModel.businesses.collectAsState()
    var navigationHandled by remember { mutableStateOf(false) }
    var businessesLoaded by remember { mutableStateOf(false) }

    // Cargar usuario al inicio
    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "🚀 Iniciando...")
        viewModel.checkUserLoggedIn()
        delay(1000)
    }

    // Cargar negocios cuando haya usuario logueado
    LaunchedEffect(currentUser) {
        if (currentUser != null && !businessesLoaded) {
            Log.d("SplashScreen", "📦 Cargando negocios del usuario...")
            businessViewModel.loadAllUserBusinesses()
            businessesLoaded = true
        }
    }

    // Manejar navegación cuando los datos estén listos
    LaunchedEffect(currentUser, businesses) {
        if (navigationHandled) return@LaunchedEffect

        // Si hay usuario pero aún no cargamos negocios, esperar
        if (currentUser != null && !businessesLoaded) {
            Log.d("SplashScreen", "⏳ Esperando a que carguen los negocios...")
            return@LaunchedEffect
        }

        delay(300)
        Log.d("SplashScreen", "✅ Revisando usuario...")
        Log.d("SplashScreen", "   currentUser: $currentUser")
        Log.d("SplashScreen", "   businesses: ${businesses.size} negocios")

        val user = currentUser

        // ✅ SIEMPRE verificar credenciales primero (aunque no haya sesión)
        val savedCredentials = try {
            BiometricHelper.getSavedCredentials(context)
        } catch (e: Exception) {
            Log.e("SplashScreen", "❌ Error al obtener credenciales: ${e.message}")
            Toast.makeText(context, "Error al cargar credenciales, inicia sesión manualmente", Toast.LENGTH_SHORT).show()
            null
        }
        Log.d("SplashScreen", "   savedCredentials: ${if (savedCredentials != null) "SÍ (${savedCredentials.first}, ${savedCredentials.third})" else "NO"}")

        // ✅ Verificar si el dispositivo tiene biometría enrollada (Face ID o huella)
        val isBiometricEnrolled = BiometricHelper.isBiometricEnrolled(context)
        Log.d("SplashScreen", "   isBiometricEnrolled: $isBiometricEnrolled")

        // ✅ Verificar si el usuario ya usó Face ID manualmente al menos una vez
        val hasBiometricBeenUsed = BiometricHelper.hasBiometricBeenUsedOnce(context)
        Log.d("SplashScreen", "   hasBiometricBeenUsedOnce: $hasBiometricBeenUsed")

        // ✅ Solo auto-lanzar Face ID si el usuario ya lo usó manualmente antes
        val shouldAutoLaunchBiometric = savedCredentials != null && hasBiometricBeenUsed
        Log.d("SplashScreen", "   shouldAutoLaunchBiometric: $shouldAutoLaunchBiometric")

        when {
            // NO HAY SESIÓN ACTIVA
            user == null -> {
                if (savedCredentials != null) {
                    val (email, password, fullName) = savedCredentials

                    // 🎯 LÓGICA CORRECTA:
                    // - Sin biometría → AUTO-LOGIN directo
                    // - Con biometría → QuickLogin (porque ya cerraron sesión)
                    if (!isBiometricEnrolled) {
                        // ✅ AUTO-LOGIN: Sin biometría → Login automático
                        Log.d("SplashScreen", "🚀 AUTO-LOGIN: Sin biometría → Login automático")
                        Log.d("SplashScreen", "   Email: $email")
                        Log.d("SplashScreen", "   ⏳ Iniciando login y esperando...")
                        viewModel.signIn(email, password, rememberMe = true)
                        // NO marcar navigationHandled, esperar a que currentUser se actualice
                    } else {
                        // ✅ QUICKLOGIN: Con biometría (usuario cerró sesión)
                        Log.d("SplashScreen", "🔐 QUICKLOGIN: Biometría configurada (sesión cerrada)")
                        Log.d("SplashScreen", "   Email: $email")
                        Log.d("SplashScreen", "   autoLaunch=$shouldAutoLaunchBiometric")
                        navigationHandled = true
                        // Obtener el primer nombre del fullName guardado
                        val displayName = getFirstName(fullName)
                        onNavigateToQuickLogin(email, password, displayName, shouldAutoLaunchBiometric)
                    }
                } else {
                    // Primera vez → Welcome
                    Log.d("SplashScreen", "❌ Primera vez → Welcome")
                    navigationHandled = true
                    onNavigateToLogin()
                }
            }

            // Email no verificado → Ir directo
            user.emailVerified != true -> {
                Log.d("SplashScreen", "⚠️ Email no verificado → EmailVerification")
                navigationHandled = true
                onNavigateToEmailVerification(user.email ?: "")
            }

            // ✅ ACTIVE - Ir DIRECTAMENTE a Main (prioridad absoluta)
            user.accountStatus == "active" -> {
                Log.d("SplashScreen", "✅ ACTIVE → Main (cuenta activa)")
                navigationHandled = true
                onNavigateToMain()
            }

            // REJECTED → Validar si tiene al menos un negocio aprobado
            user.accountStatus == "rejected" -> {
                val hasApprovedBusiness = businesses.any { it.verificationStatus == "approved" }
                Log.d("SplashScreen", "❌ REJECTED → Validando negocios...")
                Log.d("SplashScreen", "   Total negocios: ${businesses.size}")
                Log.d("SplashScreen", "   Negocios aprobados: ${businesses.count { it.verificationStatus == "approved" }}")
                Log.d("SplashScreen", "   ¿Tiene al menos uno aprobado?: $hasApprovedBusiness")

                if (hasApprovedBusiness) {
                    Log.d("SplashScreen", "✅ Tiene negocios aprobados → Main (puede usar la app)")
                    navigationHandled = true
                    onNavigateToMain()
                } else {
                    Log.d("SplashScreen", "❌ NO tiene negocios aprobados → VerificationPending")
                    navigationHandled = true
                    onNavigateToVerificationPending()
                }
            }

            // Onboarding incompleto → Ir directo
            user.onboardingStep == "business_setup" -> {
                Log.d("SplashScreen", "🧩 Onboarding incompleto → Onboarding")
                navigationHandled = true
                onNavigateToOnboarding(user.id ?: "")
            }

            // Falta documento → Ir directo
            user.onboardingStep == "document_upload" -> {
                Log.d("SplashScreen", "📄 Falta documento → DocumentVerification")
                navigationHandled = true
                onNavigateToDocumentVerification(user.id ?: "")
            }

            // PENDING_VERIFICATION → Verificar si tiene negocios aprobados
            user.accountStatus == "pending_verification" || user.onboardingStep == "pending_verification" -> {
                val hasApprovedBusiness = businesses.any { it.verificationStatus == "approved" }
                Log.d("SplashScreen", "⏳ PENDING_VERIFICATION → Validando negocios...")
                Log.d("SplashScreen", "   accountStatus: ${user.accountStatus}")
                Log.d("SplashScreen", "   onboardingStep: ${user.onboardingStep}")
                Log.d("SplashScreen", "   Total negocios: ${businesses.size}")
                Log.d("SplashScreen", "   Negocios aprobados: ${businesses.count { it.verificationStatus == "approved" }}")
                Log.d("SplashScreen", "   ¿Tiene al menos uno aprobado?: $hasApprovedBusiness")

                if (hasApprovedBusiness) {
                    Log.d("SplashScreen", "✅ Tiene negocios aprobados → Main (puede usar la app)")
                    navigationHandled = true
                    onNavigateToMain()
                } else {
                    Log.d("SplashScreen", "❌ NO tiene negocios aprobados → VerificationPending")
                    navigationHandled = true
                    onNavigateToVerificationPending()
                }
            }

            // COMPLETED → Ir directo a Main
            user.onboardingStep == "completed" -> {
                Log.d("SplashScreen", "✅ COMPLETED → Main")
                navigationHandled = true
                onNavigateToMain()
            }

            // Estado desconocido
            else -> {
                Log.w("SplashScreen", "❓ Estado desconocido → Welcome")
                viewModel.signOut()
                delay(300)
                navigationHandled = true
                onNavigateToLogin()
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
                painter = painterResource(id = com.ecomap.socio.R.drawable.logo),
                contentDescription = "EcoMap Logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(color = NuColors.Primary)
        }
    }
}