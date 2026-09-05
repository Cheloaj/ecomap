package com.ecomap.socio.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object BiometricHelper {

    private const val TAG = "BiometricHelper"
    private const val PREFS_NAME = "biometric_prefs"

    /**
     * Verifica si el dispositivo soporta autenticación biométrica
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)

        // Verificar autenticadores disponibles
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        return when (val result = biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "✅ Biometría disponible (STRONG, WEAK o DEVICE_CREDENTIAL)")
                true
            }
            else -> {
                Log.e(TAG, "❌ No hay biometría disponible - Resultado: $result")
                false
            }
        }
    }

    /**
     * Verifica si el dispositivo tiene biometría ENROLLADA y lista para usar
     * (Face ID o huella digital configurados)
     *
     * Esta función es más estricta que isBiometricAvailable():
     * - Solo retorna true si hay biometría STRONG o WEAK enrollada
     * - NO incluye DEVICE_CREDENTIAL (PIN/patrón)
     *
     * Uso: Determinar si mostrar QuickLogin (true) o auto-login directo (false)
     */
    fun isBiometricEnrolled(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)

        // Verificar solo biometría (Face ID, huella), NO credenciales de dispositivo
        val strongResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val weakResult = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)

        Log.d(TAG, "🔍 Verificando biometría enrollada:")
        Log.d(TAG, "   STRONG result: $strongResult")
        Log.d(TAG, "   WEAK result: $weakResult")

        // ✅ PRIMERO verificar si HAY biometría configurada (STRONG o WEAK)
        return when {
            strongResult == BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "✅ Biometría STRONG enrollada (huella digital)")
                true
            }
            weakResult == BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "✅ Biometría WEAK enrollada (reconocimiento facial)")
                true
            }
            // DESPUÉS verificar errores (sin biometría)
            strongResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED &&
            weakResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d(TAG, "❌ Hardware biométrico disponible pero NO enrollado (ni STRONG ni WEAK)")
                false
            }
            strongResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
            weakResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d(TAG, "❌ No hay hardware biométrico en este dispositivo")
                false
            }
            strongResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE &&
            weakResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.d(TAG, "❌ Hardware biométrico temporalmente no disponible")
                false
            }
            else -> {
                Log.d(TAG, "❌ Biometría no disponible - STRONG: $strongResult, WEAK: $weakResult")
                false
            }
        }
    }

    /**
     * Muestra el diálogo de autenticación biométrica
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación Biométrica",
        subtitle: String = "Use su huella digital o Face ID para continuar",
        negativeButtonText: String = "Cancelar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        Log.d(TAG, "Contexto: ${activity.javaClass.simpleName}, activity: $activity")

        val biometricManager = BiometricManager.from(activity)

        // Determinar qué autenticadores están disponibles (sin incluir DEVICE_CREDENTIAL para evitar el error)
        val authenticators = when {
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "📱 Usando BIOMETRIC_STRONG")
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            }
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "📱 Usando BIOMETRIC_WEAK")
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            }
            else -> {
                Log.e(TAG, "❌ Biometría no disponible")
                onError("Biometría no disponible")
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "✅ Biometría exitosa")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val errorMessage = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED -> "Autenticación cancelada"
                        else -> errString.toString()
                    }
                    Log.e(TAG, "❌ Error biométrico: $errorMessage (Código: $errorCode)")
                    onError(errorMessage)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "⚠️ Biometría fallida")
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText) // Ahora siempre es seguro agregarlo
            .setAllowedAuthenticators(authenticators)
            .build()

        Log.d(TAG, "🚀 Lanzando BiometricPrompt...")
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Guarda las credenciales cuando el usuario activa "Recordarme"
     */
    fun saveBiometricCredentials(context: Context, email: String, password: String, fullName: String? = null) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().apply {
                putString("biometric_email", email)
                putString("biometric_password", password)
                putString("biometric_full_name", fullName)
                putBoolean("biometric_enabled", true)
                apply()
            }
            Log.d(TAG, "✅ Credenciales biométricas guardadas (email: $email, fullName: $fullName)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar credenciales: ${e.message}")
        }
    }

    /**
     * Obtiene las credenciales guardadas
     * @return Triple con (email, password, fullName) o null si no hay credenciales
     */
    fun getSavedCredentials(context: Context): Triple<String, String, String?>? {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val enabled = sharedPreferences.getBoolean("biometric_enabled", false)
            if (!enabled) {
                Log.d(TAG, "⚠️ Biometría no habilitada")
                return null
            }

            val email = sharedPreferences.getString("biometric_email", null)
            val password = sharedPreferences.getString("biometric_password", null)
            val fullName = sharedPreferences.getString("biometric_full_name", null)

            return if (email != null && password != null) {
                Log.d(TAG, "✅ Credenciales obtenidas: $email, fullName: $fullName")
                Triple(email, password, fullName)
            } else {
                Log.d(TAG, "⚠️ No hay credenciales guardadas")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener credenciales (corrupto): ${e.message}")
            // ✅ Si las credenciales están corruptas, limpiarlas y empezar de cero
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                context.deleteSharedPreferences(PREFS_NAME)
                Log.d(TAG, "🧹 Credenciales corruptas eliminadas")
            } catch (clearError: Exception) {
                Log.e(TAG, "❌ Error al limpiar credenciales corruptas: ${clearError.message}")
            }
            return null
        }
    }

    /**
     * Elimina las credenciales guardadas (cuando usuario cierra sesión o desactiva biométrica)
     */
    fun clearBiometricCredentials(context: Context) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().apply {
                remove("biometric_email")
                remove("biometric_password")
                putBoolean("biometric_enabled", false)
                apply()
            }
            Log.d(TAG, "✅ Credenciales biométricas eliminadas")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar credenciales: ${e.message}")
        }
    }

    /**
     * Verifica si la autenticación biométrica está habilitada
     */
    fun isBiometricEnabled(context: Context): Boolean {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            return sharedPreferences.getBoolean("biometric_enabled", false)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar si biometría está habilitada: ${e.message}")
            return false
        }
    }

    /**
     * Marca que el usuario usó Face ID manualmente al menos una vez
     * Esto activa el auto-launch para futuros ingresos
     */
    fun markBiometricUsedOnce(context: Context) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().apply {
                putBoolean("biometric_used_once", true)
                apply()
            }
            Log.d(TAG, "✅ Marcado: Usuario usó Face ID - auto-launch activado para próximos ingresos")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al marcar biometric_used_once: ${e.message}")
        }
    }

    /**
     * Verifica si el usuario ya usó Face ID al menos una vez
     * Si es true, se debe auto-lanzar Face ID al entrar desde Splash
     */
    fun hasBiometricBeenUsedOnce(context: Context): Boolean {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val used = sharedPreferences.getBoolean("biometric_used_once", false)
            Log.d(TAG, "🔍 hasBiometricBeenUsedOnce: $used")
            return used
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar biometric_used_once (corrupto): ${e.message}")
            // ✅ Si las credenciales están corruptas, limpiarlas
            try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                context.deleteSharedPreferences(PREFS_NAME)
                Log.d(TAG, "🧹 Credenciales corruptas eliminadas")
            } catch (clearError: Exception) {
                Log.e(TAG, "❌ Error al limpiar: ${clearError.message}")
            }
            return false
        }
    }
}