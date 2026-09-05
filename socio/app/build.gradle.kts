import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.google.services) // ❌ No usamos Firebase, solo Supabase
}

// Lee credenciales desde local.properties (NO se sube a Git)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// Obtener credenciales de forma segura
fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key) ?: defaultValue
}

android {
    namespace = "com.ecomap.socio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ecomap.socio"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Campos BuildConfig para Supabase - Leídos desde local.properties
        buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("supabase.key")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/DEPENDENCIES")
            excludes.add("/META-INF/LICENSE")
        }
    }
}

dependencies {

  // AÑADE ESTAS DOS LÍNEAS
    implementation("de.charlex.compose:revealswipe:1.2.0")
    implementation("androidx.compose.material:material:1.6.8") // (La librería la necesita)

    implementation(libs.androidx.compose.animation)

    // --- Core Android ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.5") // Para FragmentActivity (requerido por BiometricPrompt)

    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // --- Navegación ---
    implementation(libs.androidx.navigation.compose)

    // --- Supabase ---
    implementation(libs.supabase.functions)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)

    // --- Ktor ---
    implementation(libs.ktor.client.okhttp) // ✅ OkHttp soporta WebSockets (necesario para Supabase Realtime)
    implementation(libs.ktor.client.core)

    // --- Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // --- osmdroid ---
    implementation(libs.osmdroid.android)

    // --- Google Play Services (Ubicación) ---
    implementation(libs.play.services.location)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // --- Coil (Imágenes) ---
    implementation(libs.coil.compose)

    // --- Retrofit ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // --- kotlinx-datetime ---
    implementation(libs.kotlinx.datetime)

    // --- Accompanist (Permisos) ---
    implementation(libs.accompanist.permissions)

    // --- WorkManager (Notificaciones en background) ---
    implementation("androidx.work:work-runtime-ktx:2.9.1") // Actualizado a 2.9.1
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // --- Biometric ---
    implementation("androidx.biometric:biometric:1.1.0") // ✅ Correcto
    implementation("androidx.security:security-crypto:1.0.0") // Añadido para EncryptedSharedPreferences

    // --- Vico Charts ---
    implementation(libs.vico.core)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // --- Serialización ---
    implementation(libs.kotlinx.serialization.json)

    // --- Firebase --- ❌ COMENTADO - Usamos solo Supabase
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.messaging)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}