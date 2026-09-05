package com.ecomap.socio.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecomap.socio.presentation.ui.auth.*
import com.ecomap.socio.presentation.ui.main.MainScreen
import com.ecomap.socio.presentation.ui.onboarding.DocumentVerificationScreen
import com.ecomap.socio.presentation.ui.onboarding.OnboardingScreen
import com.ecomap.socio.presentation.ui.splash.SplashScreen
import com.ecomap.socio.presentation.ui.verification.VerificationPendingScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecomap.socio.presentation.viewmodel.ProductViewModel
import com.ecomap.socio.presentation.viewmodel.ConnectivityViewModel
import com.ecomap.socio.utils.ConnectivityObserver
import com.ecomap.socio.utils.NetworkConnectivityObserver
import com.ecomap.socio.presentation.ui.components.NoInternetScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    onExitApp: () -> Unit  // ✅ Callback para salir de la app
) {
    val context = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsState(
        initial = ConnectivityObserver.Status.Available
    )

    // ViewModel de conectividad global
    val connectivityViewModel: ConnectivityViewModel = viewModel()
    val showNoInternetScreen by connectivityViewModel.showNoInternetScreen.collectAsState()

    // Actualizar estado de red en el ViewModel
    LaunchedEffect(networkStatus) {
        connectivityViewModel.updateNetworkStatus(networkStatus)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToQuickLogin = { email, password, userName, autoLaunch ->
                    navController.navigate(Screen.QuickLogin.createRoute(email, password, userName, autoLaunch = autoLaunch)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToEmailVerification = { email ->
                    navController.navigate(Screen.EmailVerification.createRoute(email)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Bienvenida - Primera vez en la app
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.LoginEmail.route)
                }
            )
        }

        // Login paso 1: Correo
        composable(Screen.LoginEmail.route) {
            LoginEmailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPassword = { email ->
                    navController.navigate(Screen.CleanPasswordNew.createRoute(email))
                }
            )
        }

        // Login paso 2: Contraseña (con animación slideInVertically)
        composable(
            route = Screen.LoginPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            LoginPasswordScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmailVerification = { userEmail ->
                    navController.navigate(Screen.EmailVerification.createRoute(userEmail)) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // ✅ BiometricSetup eliminado - ya no se usa

        // Pantalla de Acceso Rápido (QuickLogin)
        composable(
            route = Screen.QuickLogin.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("autoLaunch") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuario"
            val autoLaunch = backStackEntry.arguments?.getBoolean("autoLaunch") ?: false
            QuickLoginScreen(
                userName = userName,
                savedEmail = email,
                savedPassword = password,
                autoLaunchBiometric = autoLaunch,
                onNavigateToPasswordLogin = {
                    // NO eliminar QuickLogin del backstack para que el botón "Regresar" funcione
                    navController.navigate(Screen.CleanPasswordReturning.createRoute(email))
                },
                onNavigateToEmailVerification = { userEmail ->
                    navController.navigate(Screen.EmailVerification.createRoute(userEmail)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                }
            )
        }

        // CleanPasswordScreen - Nuevo Usuario (modo claro)
        composable(
            route = Screen.CleanPasswordNew.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(600))
            }
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            CleanPasswordNewScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmailVerification = { userEmail ->
                    navController.navigate(Screen.EmailVerification.createRoute(userEmail)) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = { userEmail ->
                    navController.navigate(Screen.ForgotPasswordEmail.createRoute(userEmail))
                }
            )
        }

        // CleanPasswordScreen - Usuario Registrado (modo oscuro)
        composable(
            route = Screen.CleanPasswordReturning.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(600))
            }
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            CleanPasswordReturningScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmailVerification = { userEmail ->
                    navController.navigate(Screen.EmailVerification.createRoute(userEmail)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    // Navegar a Welcome (sin Face ID)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Login antiguo (mantener por compatibilidad)
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToEmailVerification = { email ->
                    navController.navigate(Screen.EmailVerification.createRoute(email)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToDocumentVerification = { userId ->
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Registro
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToVerification = { email ->
                    navController.navigate(Screen.EmailVerification.createRoute(email)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Verificación de Email
        composable(
            route = Screen.EmailVerification.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailVerificationScreen(
                email = email,
                onNavigateToOnboarding = { userId ->
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = true)) {
                        popUpTo(Screen.EmailVerification.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("isNewUser") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val isNewUser = backStackEntry.arguments?.getBoolean("isNewUser") ?: true
            OnboardingScreen(
                userId = userId,
                isNewUser = isNewUser,
                onNavigateToDocumentVerification = {
                    // ✅ Pasar el mismo parámetro isNewUser a DocumentVerification
                    navController.navigate(Screen.DocumentVerification.createRoute(userId, isNewUser)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    // ✅ Para usuarios Pro: regresar a MainScreen
                    navController.popBackStack()
                }
            )
        }

        // Verificación de Documentos
        composable(
            route = Screen.DocumentVerification.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("isNewUser") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val isNewUser = backStackEntry.arguments?.getBoolean("isNewUser") ?: true

            DocumentVerificationScreen(
                userId = userId,
                onUploadComplete = {
                    if (isNewUser) {
                        // ✅ FLUJO 1 (REGISTRO): Usuario NUEVO → VerificationPending
                        println("✅ Usuario nuevo - navegando a VerificationPending")
                        navController.navigate(Screen.VerificationPending.route) {
                            popUpTo(Screen.DocumentVerification.route) { inclusive = true }
                        }
                    } else {
                        // ✅ FLUJO 2 (PRO): Usuario existente → Inicio
                        println("✅ Usuario Pro - navegando a Inicio")
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.DocumentVerification.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Verificación Pendiente
        composable(Screen.VerificationPending.route) {
            VerificationPendingScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.VerificationPending.route) { inclusive = true }
                    }
                },
                onNavigateToQuickLogin = { email, password, userName ->
                    // ✅ NO auto-lanzar Face ID hasta que el usuario use el botón manualmente
                    navController.navigate(Screen.QuickLogin.createRoute(email, password, userName, autoLaunch = false)) {
                        popUpTo(Screen.VerificationPending.route) { inclusive = true }
                    }
                },
                onSignOut = {
                    // Volver a Splash para determinar navegación correcta
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onExitApp = onExitApp  // ✅ Pasar callback para salir de la app
            )
        }

        // Pantalla Principal
        composable(Screen.Main.route) {
            // Main es el destino raíz una vez iniciada la sesión: el login
            // navega aquí con popUpTo(0) { inclusive = true }, así que es la
            // única entrada del back stack.
            //
            // Sin este BackHandler, el botón Atrás dejaba que el NavHost sacara
            // esa última entrada y se quedara SIN destino que componer: la app
            // seguía viva y en primer plano, pero no dibujaba nada — la pantalla
            // se veía completamente negra y solo se salía matando la app.
            //
            // Ahora, Atrás en la raíz cierra la app, que es lo que se espera.
            BackHandler { onExitApp() }

            MainScreen(
                onSignOut = {
                    // Ir directo a Welcome al cerrar sesión
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { userId ->
                    // ✅ Cuando se navega desde MainScreen es un usuario PRO agregando negocio
                    // por lo tanto NO es usuario nuevo (isNewUser = false)
                    navController.navigate(Screen.Onboarding.createRoute(userId, isNewUser = false))
                },
                onNavigateToDashboard = { businessId ->
                    navController.navigate(Screen.BusinessDashboard.createRoute(businessId))
                },
                onNavigateToForgotPassword = { email ->
                    navController.navigate(Screen.ForgotPasswordEmail.createRoute(email))
                },
                onNavigateToUpgrade = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        // Upgrade to Pro Screen
        composable(Screen.Upgrade.route) {
            com.ecomap.socio.presentation.ui.upgrade.UpgradeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUpgradeToPro = {
                    // TODO: Handle successful upgrade
                    navController.popBackStack()
                }
            )
        }

        // ============ SUBSCRIPTION FLOW ============

        // Subscription Plans Screen
        composable(Screen.Subscription.route) {
            com.ecomap.socio.presentation.ui.subscription.SubscriptionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPayment = {
                    navController.navigate(Screen.Payment.route)
                }
            )
        }

        // Payment Screen
        composable(Screen.Payment.route) {
            com.ecomap.socio.presentation.ui.subscription.PaymentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPaymentComplete = { paymentInfo ->
                    // Solo viajan la marca y los últimos 4 dígitos. El número
                    // completo y el CVV se quedan en la pantalla de pago y
                    // nunca entran al back stack de navegación.
                    navController.navigate(
                        Screen.ProcessingPayment.createRoute(
                            java.net.URLEncoder.encode(paymentInfo.getCardType(), "UTF-8"),
                            paymentInfo.getLastFourDigits()
                        )
                    ) {
                        popUpTo(Screen.Subscription.route) { inclusive = false }
                    }
                }
            )
        }

        // Processing Payment Screen
        composable(
            route = Screen.ProcessingPayment.route,
            arguments = listOf(
                navArgument("cardBrand") { type = NavType.StringType },
                navArgument("lastFour") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cardBrand = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("cardBrand") ?: "",
                "UTF-8"
            )
            val lastFour = backStackEntry.arguments?.getString("lastFour") ?: ""

            com.ecomap.socio.presentation.ui.subscription.ProcessingPaymentScreen(
                cardBrand = cardBrand,
                lastFour = lastFour,
                onPaymentSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Subscription.route) { inclusive = true }
                    }
                }
            )
        }

        // Business Dashboard
        composable(
            route = Screen.BusinessDashboard.route,
            arguments = listOf(
                navArgument("businessId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            com.ecomap.socio.presentation.ui.products.BusinessDashboardScreen(
                businessId = businessId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProductDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId, businessId))
                },
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct.createRoute(businessId))
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId, businessId))
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        // Product Detail
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("businessId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            com.ecomap.socio.presentation.ui.products.ProductDetailScreen(
                productId = productId,
                businessId = businessId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Add Product
        composable(
            route = Screen.AddProduct.route,
            arguments = listOf(
                navArgument("businessId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""

            // Obtener el ViewModel del BusinessDashboard para compartir estado
            val dashboardEntry = navController.getBackStackEntry(Screen.BusinessDashboard.createRoute(businessId))
            val productViewModel: ProductViewModel = hiltViewModel(dashboardEntry)

            com.ecomap.socio.presentation.ui.products.AddProductScreen(
                businessId = businessId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProductCreated = {
                    // Recargar productos en el ViewModel compartido
                    productViewModel.loadProducts(businessId)
                },
                viewModel = productViewModel
            )
        }

        // Edit Product
        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("businessId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val businessId = backStackEntry.arguments?.getString("businessId") ?: ""
            com.ecomap.socio.presentation.ui.products.EditProductScreen(
                productId = productId,
                businessId = businessId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ============ FORGOT PASSWORD FLOW ============

        // Paso 1: Validar identidad con email
        composable(
            route = Screen.ForgotPasswordEmail.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ForgotPasswordEmailScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCodeVerification = { userEmail ->
                    navController.navigate(Screen.ForgotPasswordCode.createRoute(userEmail))
                }
            )
        }

        // Paso 2: Verificar código de 6 dígitos
        composable(
            route = Screen.ForgotPasswordCode.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ForgotPasswordCodeScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResetPassword = { userEmail, code ->
                    navController.navigate(Screen.ResetPassword.createRoute(userEmail, code)) {
                        popUpTo(Screen.ForgotPasswordEmail.route) { inclusive = true }
                    }
                }
            )
        }

        // Paso 3: Crear nueva contraseña
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val code = backStackEntry.arguments?.getString("code") ?: ""
            ResetPasswordScreen(
                email = email,
                verificationCode = code,
                onNavigateToVerificationPending = {
                    navController.navigate(Screen.VerificationPending.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ============ REPORTS SCREEN ============
        composable(Screen.Reports.route) {
            com.ecomap.socio.presentation.ui.reports.ProductReportsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

        // Pantalla modal de Sin Internet (bloqueante en tiempo real)
        if (showNoInternetScreen) {
            NoInternetScreen(
                onDismiss = {
                    // Cerrar la pantalla y volver a donde estaba
                    connectivityViewModel.dismissNoInternetScreen()
                },
                onRetry = {
                    // Verificar si hay conexión y cerrar si hay internet
                    connectivityViewModel.retryConnection()
                }
            )
        }
    }
}
