package com.ecomap.usuario.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.ecomap.usuario.presentation.ui.about.AboutScreen
import com.ecomap.usuario.presentation.ui.auth.QuickLoginScreen
import com.ecomap.usuario.presentation.ui.auth.ForgotPasswordEmailScreen
import com.ecomap.usuario.presentation.ui.auth.ForgotPasswordCodeScreen
import com.ecomap.usuario.presentation.ui.splash.SplashScreen
import com.ecomap.usuario.presentation.ui.basket.BasketAnalysisScreen
import com.ecomap.usuario.presentation.ui.basket.BasketScreen
import com.ecomap.usuario.presentation.ui.business.BusinessDetailScreen
import com.ecomap.usuario.presentation.ui.community.CommunityProductsScreen
import com.ecomap.usuario.presentation.ui.favorites.FavoritesScreen
import com.ecomap.usuario.presentation.ui.help.HelpScreen
import com.ecomap.usuario.presentation.ui.map.MapScreen
import com.ecomap.usuario.presentation.ui.offers.OffersScreen
import com.ecomap.usuario.presentation.ui.products.ProductDetailScreen
import com.ecomap.usuario.presentation.ui.products.ProductReviewsScreen
import com.ecomap.usuario.presentation.ui.profile.ProfileScreen
import com.ecomap.usuario.presentation.ui.search.SearchScreen
import com.ecomap.usuario.presentation.viewmodel.AuthViewModel
import com.ecomap.usuario.presentation.viewmodel.BusinessViewModel
import com.ecomap.usuario.utils.ImageConfig
import com.ecomap.usuario.utils.ConnectivityObserver
import com.ecomap.usuario.utils.NetworkConnectivityObserver
import com.ecomap.usuario.presentation.ui.components.NoInternetScreen
import com.ecomap.usuario.presentation.viewmodel.ConnectivityViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    deepLinkData: com.ecomap.usuario.DeepLinkData? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val businessViewModel: BusinessViewModel = hiltViewModel()

    // TODO: Actualizar DeepLinkData para usar email y code en lugar de accessToken
    // LaunchedEffect(deepLinkData) {
    //     when (deepLinkData) {
    //         is com.ecomap.usuario.DeepLinkData.ResetPassword -> {
    //             navController.navigate(Screen.ResetPassword.createRoute(deepLinkData.email, deepLinkData.code)) {
    //                 popUpTo(0) { inclusive = false }
    //             }
    //             onDeepLinkHandled()
    //         }
    //         null -> { }
    //     }
    // }

    val isSignedIn by authViewModel.isSignedIn.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSignedIn) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = navController.currentBackStackEntry)
    val currentRouteName = currentRoute.value?.destination?.route
    val isMapScreen = currentRouteName == Screen.Map.route

    // ✅ Solo habilitar drawer si estamos logueados Y NO estamos en pantallas de auth
    val isAuthScreen = currentRouteName in listOf(
        Screen.Splash.route,
        Screen.Welcome.route,
        Screen.LoginEmail.route,
        Screen.RegisterNew.route,
        Screen.RegisterName.route
    ) || currentRouteName?.startsWith("quick_login") == true
      || currentRouteName?.startsWith("register_email") == true
      || currentRouteName?.startsWith("register_password") == true
      || currentRouteName?.startsWith("register_otp") == true
      || currentRouteName?.startsWith("nu_biometric_login") == true
      || currentRouteName?.startsWith("clean_password_returning") == true
      || currentRouteName?.startsWith("login_password") == true
      || currentRouteName?.startsWith("reset_password") == true
      || currentRouteName?.startsWith("forgot_password_email") == true
      || currentRouteName?.startsWith("forgot_password_code") == true
    val gesturesEnabled = isSignedIn && !isMapScreen && !isAuthScreen

    LaunchedEffect(currentRoute.value) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    val startDestination = Screen.Splash.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            // ✅ Solo mostrar drawer si NO estamos en pantallas de auth
            if (isSignedIn && !isAuthScreen) {
                NavigationDrawerContent(
                    navController = navController,
                    authViewModel = authViewModel,
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        NavGraphContent(
            navController = navController,
            authViewModel = authViewModel,
            businessViewModel = businessViewModel,
            startDestination = startDestination,
            onOpenDrawer = if (isSignedIn) {
                { scope.launch { drawerState.open() } }
            } else {
                {}
            }
        )
    }
}

@Composable
private fun NavGraphContent(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    businessViewModel: BusinessViewModel,
    startDestination: String,
    onOpenDrawer: (() -> Unit)?
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
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToQuickLogin = { email, password, displayName, autoLaunchBiometric ->
                    navController.navigate(
                        Screen.QuickLogin.createRoute(email, password, displayName, autoLaunchBiometric)
                    ) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            com.ecomap.usuario.presentation.ui.auth.WelcomeScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterNew.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.LoginEmail.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.RegisterNew.route) {
            com.ecomap.usuario.presentation.ui.auth.RegisterScreenNew(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBiometric = { email, password, displayName ->
                    val encodedEmail = java.net.URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                    val encodedPassword = java.net.URLEncoder.encode(password, StandardCharsets.UTF_8.toString())
                    val encodedDisplayName = java.net.URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.RegisterOTP.createRoute(encodedEmail, encodedPassword, encodedDisplayName)) {
                        popUpTo(Screen.RegisterNew.route) { inclusive = false }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(
            route = Screen.RegisterOTP.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
            val encodedPassword = backStackEntry.arguments?.getString("password") ?: ""
            val encodedDisplayName = backStackEntry.arguments?.getString("displayName") ?: ""

            val email = java.net.URLDecoder.decode(encodedEmail, StandardCharsets.UTF_8.toString())
            val password = java.net.URLDecoder.decode(encodedPassword, StandardCharsets.UTF_8.toString())
            val displayName = java.net.URLDecoder.decode(encodedDisplayName, StandardCharsets.UTF_8.toString())

            com.ecomap.usuario.presentation.ui.auth.RegisterOTPScreen(
                email = email,
                password = password,
                displayName = displayName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNuBiometric = {
                    val encodedEmail = java.net.URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                    val encodedPassword = java.net.URLEncoder.encode(password, StandardCharsets.UTF_8.toString())
                    val encodedDisplayName = java.net.URLEncoder.encode(displayName, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.NuBiometricLogin.createRoute(encodedEmail, encodedPassword, encodedDisplayName)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // ✅ Nueva pantalla de login biométrico estilo Nu (después de crear cuenta)
        composable(
            route = Screen.NuBiometricLogin.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email") ?: ""
            val encodedPassword = backStackEntry.arguments?.getString("password") ?: ""
            val encodedDisplayName = backStackEntry.arguments?.getString("displayName") ?: ""

            val email = java.net.URLDecoder.decode(encodedEmail, StandardCharsets.UTF_8.toString())
            val password = java.net.URLDecoder.decode(encodedPassword, StandardCharsets.UTF_8.toString())
            val displayName = java.net.URLDecoder.decode(encodedDisplayName, StandardCharsets.UTF_8.toString())

            com.ecomap.usuario.presentation.ui.auth.NuBiometricLoginScreen(
                userName = displayName,
                savedEmail = email,
                savedPassword = password,
                onNavigateToPasswordLogin = { email ->
                    navController.navigate(Screen.CleanPasswordReturning.createRoute(email)) {
                        popUpTo(Screen.NuBiometricLogin.route) { inclusive = false }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        // ✅ Pantalla de contraseña limpia - Usuario registrado (modo oscuro)
        composable(
            route = Screen.CleanPasswordReturning.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            com.ecomap.usuario.presentation.ui.auth.CleanPasswordReturningScreen(
                email = email,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.RegisterName.route) {
            com.ecomap.usuario.presentation.ui.auth.RegisterNameScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmail = { fullName ->
                    navController.navigate(Screen.RegisterEmail.createRoute(fullName))
                }
            )
        }

        composable(
            route = Screen.RegisterEmail.route,
            arguments = listOf(
                navArgument("fullName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fullName = backStackEntry.arguments?.getString("fullName") ?: ""

            com.ecomap.usuario.presentation.ui.auth.RegisterEmailScreen(
                fullName = fullName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPassword = { name, email ->
                    navController.navigate(Screen.RegisterPassword.createRoute(name, email))
                }
            )
        }

        composable(
            route = Screen.RegisterPassword.route,
            arguments = listOf(
                navArgument("fullName") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fullName = backStackEntry.arguments?.getString("fullName") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""

            com.ecomap.usuario.presentation.ui.auth.RegisterPasswordScreen(
                fullName = fullName,
                email = email,
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToOTP = { userEmail ->
                    val encodedEmail = java.net.URLEncoder.encode(userEmail, StandardCharsets.UTF_8.toString())
                    val encodedPassword = java.net.URLEncoder.encode("", StandardCharsets.UTF_8.toString())
                    val encodedDisplayName = java.net.URLEncoder.encode(fullName, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.RegisterOTP.createRoute(encodedEmail, encodedPassword, encodedDisplayName)) {
                        popUpTo(Screen.RegisterName.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LoginEmail.route) {
            com.ecomap.usuario.presentation.ui.auth.LoginEmailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPassword = { email ->
                    navController.navigate(Screen.LoginPassword.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.LoginPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            com.ecomap.usuario.presentation.ui.auth.LoginPasswordScreen(
                email = email,
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEmailVerification = { email ->
                    // Navigate to email verification if user is not verified
                    val encodedEmail = java.net.URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                    val encodedPassword = java.net.URLEncoder.encode("", StandardCharsets.UTF_8.toString())
                    val encodedDisplayName = java.net.URLEncoder.encode("Usuario", StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.RegisterOTP.createRoute(encodedEmail, encodedPassword, encodedDisplayName)) {
                        popUpTo(Screen.LoginEmail.route) { inclusive = false }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = { email ->
                    navController.navigate(Screen.ForgotPasswordEmail.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.ForgotPasswordEmail.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            ForgotPasswordEmailScreen(
                email = email,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCodeVerification = { email ->
                    navController.navigate(Screen.ForgotPasswordCode.createRoute(email))
                },
                viewModel = authViewModel
            )
        }

        composable(
            route = Screen.ForgotPasswordCode.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            ForgotPasswordCodeScreen(
                email = email,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResetPassword = { email, code ->
                    navController.navigate(Screen.ResetPassword.createRoute(email, code)) {
                        popUpTo(Screen.LoginEmail.route) { inclusive = false }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(
            route = Screen.QuickLogin.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType },
                navArgument("autoLaunchBiometric") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val displayName = backStackEntry.arguments?.getString("displayName") ?: "Usuario"
            val autoLaunchBiometric = backStackEntry.arguments?.getBoolean("autoLaunchBiometric") ?: false

// DENTRO DE NavGraph.kt de EcoMapUsuario
// composable(Screen.QuickLogin.route) { ... }

// Líneas que necesitas corregir:
            QuickLoginScreen(
                email = email,
                password = password,
                displayName = displayName,
                autoLaunchBiometric = autoLaunchBiometric,
                viewModel = authViewModel,
                // ✅ ¡AGREGA ESTO!
                onNavigateToLogin = {
                    navController.navigate(Screen.LoginEmail.route) {
                        // Limpiar el quicklogin del backstack
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                },
                // El onNavigateToMain ya lo tienes correcto:
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.QuickLogin.route) { inclusive = true }
                    }
                }
            )
}

        composable(Screen.Map.route) {
            MapScreen(
                onBusinessClick = { business: com.ecomap.usuario.data.model.Business ->
                    businessViewModel.selectBusiness(business)
                    navController.navigate(Screen.BusinessDetail.createRoute(business.id))
                },
                onOpenDrawer = onOpenDrawer ?: {},
                onNavigateToReportProduct = {
                    navController.navigate(Screen.ReportProduct.route)
                },
                onNavigateToCommunityProducts = {
                    navController.navigate(Screen.CommunityProducts.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                viewModel = businessViewModel
            )
        }

        composable(
            route = Screen.BusinessDetail.route,
            arguments = listOf(
                navArgument("businessId") { type = NavType.StringType }
            )
        ) {
            val selectedBusiness by businessViewModel.selectedBusiness.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                selectedBusiness?.let { business ->
                    BusinessDetailScreen(
                        business = business,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBasket = { navController.navigate(Screen.Basket.route) },
                        onProductClick = { product ->
                            val encodedBusinessName = URLEncoder.encode(business.businessName, StandardCharsets.UTF_8.toString())
                            val encodedAddress = URLEncoder.encode(business.address, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                Screen.ProductDetail.createRoute(
                                    productId = product.id,
                                    businessName = encodedBusinessName,
                                    businessAddress = encodedAddress,
                                    businessLatitude = business.latitude,
                                    businessLongitude = business.longitude
                                )
                            )
                        }
                    )
                } ?: run {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onBusinessClick = { business ->
                    businessViewModel.selectBusiness(business)
                    navController.navigate(Screen.BusinessDetail.createRoute(business.id))
                },
                onProductClick = { product ->
                    val encodedBusinessName = URLEncoder.encode(product.ownerName ?: "Negocio", StandardCharsets.UTF_8.toString())
                    val encodedAddress = URLEncoder.encode(product.locationAddress ?: "Ubicación", StandardCharsets.UTF_8.toString())
                    navController.navigate(
                        Screen.ProductDetail.createRoute(
                            productId = product.id,
                            businessName = encodedBusinessName,
                            businessAddress = encodedAddress,
                            businessLatitude = product.latitude ?: 0.0,
                            businessLongitude = product.longitude ?: 0.0
                        )
                    )
                },
                userLatitude = null, // TODO: Obtener ubicación del usuario desde MapViewModel
                userLongitude = null
            )
        }

        composable(Screen.Offers.route) {
            OffersScreen(
                viewModel = businessViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOfferClick = { business ->
                    businessViewModel.selectBusiness(business)
                    navController.navigate(Screen.BusinessDetail.createRoute(business.id))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onBusinessClick = { business ->
                    businessViewModel.selectBusiness(business)
                    navController.navigate(Screen.BusinessDetail.createRoute(business.id))
                }
            )
        }

        composable(Screen.Basket.route) {
            BasketScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { navController.navigate(Screen.BasketAnalysis.route) },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }
            )
        }

        composable(Screen.BasketAnalysis.route) {
            BasketAnalysisScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Subscription.route) {
            com.ecomap.usuario.presentation.ui.subscription.SubscriptionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPayment = { navController.navigate(Screen.Payment.route) }
            )
        }

        composable(Screen.Payment.route) {
            com.ecomap.usuario.presentation.ui.subscription.PaymentScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentComplete = { paymentInfo ->
                    navController.navigate(
                        // Solo la marca y los últimos 4 dígitos entran a la ruta
                        Screen.ProcessingPayment.createRoute(
                            cardBrand = java.net.URLEncoder.encode(paymentInfo.getCardType(), "UTF-8"),
                            lastFour = paymentInfo.getLastFourDigits()
                        )
                    ) {
                        popUpTo(Screen.Subscription.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ProcessingPayment.route,
            arguments = listOf(
                navArgument("cardBrand") { type = NavType.StringType },
                navArgument("lastFour") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cardBrand = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("cardBrand") ?: "", "UTF-8"
            )
            val lastFour = backStackEntry.arguments?.getString("lastFour") ?: ""

            com.ecomap.usuario.presentation.ui.subscription.ProcessingPaymentScreen(
                cardBrand = cardBrand,
                lastFour = lastFour,
                onPaymentSuccess = {
                    navController.navigate(Screen.Basket.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToHelp = {
                    navController.navigate(Screen.Help.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToMyComplaints = {
                    navController.navigate(Screen.MyComplaints.route)
                }
            )
        }

        composable(Screen.ReportProduct.route) {
            com.ecomap.usuario.presentation.ui.report.ReportProductScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyComplaints.route) {
            com.ecomap.usuario.presentation.ui.complaints.MyComplaintsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            com.ecomap.usuario.presentation.ui.history.HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onBusinessClick = { business ->
                    businessViewModel.selectBusiness(business)
                    navController.navigate(Screen.BusinessDetail.createRoute(business.id))
                }
            )
        }

        composable(Screen.Notifications.route) {
            com.ecomap.usuario.presentation.ui.notifications.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            com.ecomap.usuario.presentation.ui.profile.EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CommunityProducts.route) {
            CommunityProductsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("code") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val code = backStackEntry.arguments?.getString("code") ?: ""

            com.ecomap.usuario.presentation.ui.auth.ResetPasswordScreen(
                email = email,
                verificationCode = code,
                onNavigateToEmailVerification = { email ->
                    // En Usuario, no hay verificación de email después del reset
                    // Este parámetro se mantiene por compatibilidad pero no se usa
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("businessName") { type = NavType.StringType },
                navArgument("businessAddress") { type = NavType.StringType },
                navArgument("businessLatitude") { type = NavType.StringType },
                navArgument("businessLongitude") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val encodedBusinessName = backStackEntry.arguments?.getString("businessName") ?: ""
            val encodedAddress = backStackEntry.arguments?.getString("businessAddress") ?: ""
            val latitudeStr = backStackEntry.arguments?.getString("businessLatitude") ?: "0.0"
            val longitudeStr = backStackEntry.arguments?.getString("businessLongitude") ?: "0.0"

            val businessName = URLDecoder.decode(encodedBusinessName, StandardCharsets.UTF_8.toString())
            val businessAddress = URLDecoder.decode(encodedAddress, StandardCharsets.UTF_8.toString())
            val latitude = latitudeStr.toDoubleOrNull() ?: 0.0
            val longitude = longitudeStr.toDoubleOrNull() ?: 0.0

            val productViewModel: com.ecomap.usuario.presentation.viewmodel.ProductViewModel = hiltViewModel()
            val product by productViewModel.getProductById(productId).collectAsState(initial = null)

            product?.let {
                ProductDetailScreen(
                    product = it,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    businessLatitude = latitude,
                    businessLongitude = longitude,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAllReviews = { prodId ->
                        val encodedProductName = URLEncoder.encode(it.name, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screen.ProductReviews.createRoute(prodId, encodedProductName))
                    }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = com.ecomap.usuario.ui.theme.NuColors.Primary)
                }
            }
        }

        composable(
            route = Screen.ProductReviews.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("productName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val encodedProductName = backStackEntry.arguments?.getString("productName") ?: ""
            val productName = URLDecoder.decode(encodedProductName, StandardCharsets.UTF_8.toString())

            ProductReviewsScreen(
                productId = productId,
                productName = productName,
                onNavigateBack = { navController.popBackStack() }
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

@Composable
private fun NavigationDrawerContent(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    onCloseDrawer: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userDataViewModel: com.ecomap.usuario.presentation.viewmodel.UserDataViewModel = hiltViewModel()
    val userPreferences by userDataViewModel.userPreferences.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        userDataViewModel.loadUserPreferences()
    }

    // Recargar preferencias cuando el drawer se vuelve visible
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                userDataViewModel.loadUserPreferences()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFFFAFAFA),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header moderno con foto de perfil
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Avatar con foto de perfil
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                    ) {
                        key(userPreferences?.avatarUrl) {
                            if (!userPreferences?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPreferences?.avatarUrl,
                                    imageLoader = ImageConfig.getImageLoader(context),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (userPreferences?.displayName ?: currentUser?.email?.firstOrNull()?.toString() ?: "U").firstOrNull()?.uppercase() ?: "U",
                                        fontSize = 32.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = userPreferences?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Usuario",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )

                    Text(
                        text = currentUser?.email ?: "",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menú moderno
            DrawerMenuItem(
                icon = Icons.Default.Map,
                title = "Mapa",
                iconColor = Color(0xFF2196F3),
                onClick = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                    }
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Favorite,
                title = "Favoritos",
                iconColor = Color(0xFFE91E63),
                onClick = {
                    navController.navigate(Screen.Favorites.route)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.ShoppingCart,
                title = "Mi Canasta",
                iconColor = Color(0xFFFF9800),
                onClick = {
                    navController.navigate(Screen.Basket.route)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Person,
                title = "Mi Perfil",
                iconColor = Color(0xFF4CAF50),
                onClick = {
                    navController.navigate(Screen.Profile.route)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Star,
                title = "Suscripción PRO",
                iconColor = Color(0xFFFFC107),
                onClick = {
                    navController.navigate(Screen.Subscription.route)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.AddBox,
                title = "Publicar Producto",
                iconColor = Color(0xFF9C27B0),
                onClick = {
                    navController.navigate(Screen.ReportProduct.route)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.People,
                title = "Productos Comunitarios",
                iconColor = Color(0xFF00BCD4),
                onClick = {
                    navController.navigate(Screen.CommunityProducts.route)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Separador sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE0E0E0))
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cerrar sesión
            DrawerMenuItem(
                icon = Icons.Default.Logout,
                title = "Cerrar Sesión",
                iconColor = Color(0xFFE53935),
                onClick = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Versión
            Text(
                text = "EcoMap Usuario v1.0.0",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F1F1F)
            )
        }
    }
}