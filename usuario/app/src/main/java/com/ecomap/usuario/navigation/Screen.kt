package com.ecomap.usuario.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object QuickLogin : Screen("quick_login/{email}/{password}/{displayName}/{autoLaunchBiometric}") {
        fun createRoute(email: String, password: String, displayName: String, autoLaunchBiometric: Boolean) =
            "quick_login/$email/$password/$displayName/$autoLaunchBiometric"
    }
    object Login : Screen("login")
    object Register : Screen("register")

    // Nueva pantalla única de registro
    object RegisterNew : Screen("register_new")

    // Pantalla de login biométrico estilo Nu (después de crear cuenta)
    object NuBiometricLogin : Screen("nu_biometric_login/{email}/{password}/{displayName}") {
        fun createRoute(email: String, password: String, displayName: String) =
            "nu_biometric_login/$email/$password/$displayName"
    }

    // Pantalla de contraseña limpia - Usuario Registrado (modo oscuro)
    object CleanPasswordReturning : Screen("clean_password_returning/{email}") {
        fun createRoute(email: String) = "clean_password_returning/$email"
    }

    // Flujo de login paso a paso
    object LoginEmail : Screen("login_email")
    object LoginPassword : Screen("login_password/{email}") {
        fun createRoute(email: String) = "login_password/$email"
    }

    // Flujo de registro paso a paso
    object RegisterName : Screen("register_name")
    object RegisterEmail : Screen("register_email/{fullName}") {
        fun createRoute(fullName: String) = "register_email/$fullName"
    }
    object RegisterPassword : Screen("register_password/{fullName}/{email}") {
        fun createRoute(fullName: String, email: String) = "register_password/$fullName/$email"
    }
    object RegisterOTP : Screen("register_otp/{email}/{password}/{displayName}") {
        fun createRoute(email: String, password: String, displayName: String) = "register_otp/$email/$password/$displayName"
    }
    object Map : Screen("map")
    object Search : Screen("search")
    object Offers : Screen("offers")
    object Favorites : Screen("favorites")
    object Basket : Screen("basket")
    object BasketAnalysis : Screen("basket_analysis")
    object Profile : Screen("profile")
    object ReportProduct : Screen("report_product")
    object MyComplaints : Screen("my_complaints")
    object Help : Screen("help")
    object About : Screen("about")
    object History : Screen("history")
    object Notifications : Screen("notifications")
    object EditProfile : Screen("edit_profile")
    object CommunityProducts : Screen("community_products")

    // Forgot Password Flow
    object ForgotPasswordEmail : Screen("forgot_password_email/{email}") {
        fun createRoute(email: String) = "forgot_password_email/$email"
    }
    object ForgotPasswordCode : Screen("forgot_password_code/{email}") {
        fun createRoute(email: String) = "forgot_password_code/$email"
    }
    object ResetPassword : Screen("reset_password/{email}/{code}") {
        fun createRoute(email: String, code: String) = "reset_password/$email/$code"
    }

    object BusinessDetail : Screen("business_detail/{businessId}") {
        fun createRoute(businessId: String) = "business_detail/$businessId"
    }
    object ProductDetail : Screen("product_detail/{productId}/{businessName}/{businessAddress}/{businessLatitude}/{businessLongitude}") {
        fun createRoute(
            productId: String,
            businessName: String,
            businessAddress: String,
            businessLatitude: Double,
            businessLongitude: Double
        ) = "product_detail/$productId/$businessName/$businessAddress/$businessLatitude/$businessLongitude"
    }
    object ProductReviews : Screen("product_reviews/{productId}/{productName}") {
        fun createRoute(productId: String, productName: String) = "product_reviews/$productId/$productName"
    }
    object Subscription : Screen("subscription")
    object Payment : Screen("payment")
    /**
     * Pantalla de procesamiento.
     *
     * Antes la ruta incluía {cardNumber} y {cvv}, con lo que el número de
     * tarjeta y el CVV quedaban escritos en el back stack del NavController y
     * en el SavedStateHandle. La pantalla solo necesita mostrar
     * "Visa •••• 4242", así que ahora recibe únicamente eso.
     */
    object ProcessingPayment : Screen("processing_payment/{cardBrand}/{lastFour}") {
        fun createRoute(cardBrand: String, lastFour: String) =
            "processing_payment/$cardBrand/$lastFour"
    }
}
