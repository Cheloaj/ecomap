package com.ecomap.socio.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object LoginEmail : Screen("login_email")
    data object LoginPassword : Screen("login_password/{email}") {
        fun createRoute(email: String) = "login_password/$email"
    }
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object EmailVerification : Screen("email_verification/{email}") {
        fun createRoute(email: String) = "email_verification/$email"
    }
    data object BiometricSetup : Screen("biometric_setup/{email}/{password}") {
        fun createRoute(email: String, password: String) = "biometric_setup/$email/$password"
    }
    data object QuickLogin : Screen("quick_login/{email}/{password}/{userName}?autoLaunch={autoLaunch}") {
        fun createRoute(email: String, password: String, userName: String, autoLaunch: Boolean = false) =
            "quick_login/$email/$password/$userName?autoLaunch=$autoLaunch"
    }
    data object CleanPasswordNew : Screen("clean_password_new/{email}") {
        fun createRoute(email: String) = "clean_password_new/$email"
    }
    data object CleanPasswordReturning : Screen("clean_password_returning/{email}") {
        fun createRoute(email: String) = "clean_password_returning/$email"
    }
    data object Onboarding : Screen("onboarding/{userId}/{isNewUser}") {
        fun createRoute(userId: String, isNewUser: Boolean) =
            "onboarding/$userId/$isNewUser"
    }
    data object DocumentVerification : Screen("document_verification/{userId}/{isNewUser}") {
        fun createRoute(userId: String, isNewUser: Boolean) =
            "document_verification/$userId/$isNewUser"
    }
    data object VerificationPending : Screen("verification_pending")
    data object Main : Screen("main")
    data object BusinessDashboard : Screen("business_dashboard/{businessId}") {
        fun createRoute(businessId: String) = "business_dashboard/$businessId"
    }
    data object ProductDetail : Screen("product_detail/{productId}/{businessId}") {
        fun createRoute(productId: String, businessId: String) = "product_detail/$productId/$businessId"
    }
    data object AddProduct : Screen("add_product/{businessId}") {
        fun createRoute(businessId: String) = "add_product/$businessId"
    }
    data object EditProduct : Screen("edit_product/{productId}/{businessId}") {
        fun createRoute(productId: String, businessId: String) = "edit_product/$productId/$businessId"
    }

    // Forgot Password Flow
    data object ForgotPasswordEmail : Screen("forgot_password_email/{email}") {
        fun createRoute(email: String) = "forgot_password_email/$email"
    }
    data object ForgotPasswordCode : Screen("forgot_password_code/{email}") {
        fun createRoute(email: String) = "forgot_password_code/$email"
    }
    data object ResetPassword : Screen("reset_password/{email}/{code}") {
        fun createRoute(email: String, code: String) = "reset_password/$email/$code"
    }

    // Upgrade to Pro
    data object Upgrade : Screen("upgrade")

    // Subscription Flow
    data object Subscription : Screen("subscription")
    data object Payment : Screen("payment")
    /**
     * Pantalla de procesamiento.
     *
     * Antes la ruta era:
     *   processing_payment/{cardNumber}/{expiryMonth}/{expiryYear}/{cvv}/{cardHolderName}
     *
     * Es decir, el número de tarjeta y el CVV quedaban escritos dentro de la
     * ruta: en el back stack del NavController, en el SavedStateHandle y en
     * cualquier volcado de estado o reporte de fallo.
     *
     * La pantalla solo necesita mostrar "Visa •••• 4242", así que ahora se le
     * pasa únicamente eso. Los datos sensibles no salen de la pantalla de pago.
     */
    data object ProcessingPayment : Screen("processing_payment/{cardBrand}/{lastFour}") {
        fun createRoute(cardBrand: String, lastFour: String) =
            "processing_payment/$cardBrand/$lastFour"
    }

    // Reports
    data object Reports : Screen("reports")
}
