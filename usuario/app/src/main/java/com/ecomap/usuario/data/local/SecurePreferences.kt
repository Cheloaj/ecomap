package com.ecomap.usuario.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * Almacén cifrado. Es NULO si el cifrado no está disponible.
     *
     * Antes, este bloque caía a `getSharedPreferences(...)` sin cifrar cuando
     * `EncryptedSharedPreferences.create` fallaba, y luego guardaba ahí la
     * contraseña del usuario EN TEXTO PLANO, sin avisar a nadie. Y ese fallo no
     * es hipotético: restaurar la app desde un respaldo en otro dispositivo
     * invalida la llave del Keystore y hace fallar exactamente esta llamada.
     *
     * Ahora, si no hay cifrado disponible, simplemente NO se guarda nada: el
     * login biométrico queda deshabilitado y el usuario entra con su
     * contraseña. Perder una comodidad es preferible a filtrar la credencial.
     */
    private val sharedPreferences: SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Almacenamiento cifrado no disponible; el acceso biométrico queda deshabilitado")
        // Se limpia cualquier resto de una versión anterior que sí pudo
        // haber escrito credenciales sin cifrar en este mismo archivo.
        runCatching { context.deleteSharedPreferences(PREFS_NAME) }
        null
    }

    /** true si el dispositivo puede almacenar credenciales de forma segura. */
    fun isSecureStorageAvailable(): Boolean = sharedPreferences != null

    fun saveCredentials(email: String, password: String) {
        val prefs = sharedPreferences
        if (prefs == null) {
            Log.w(TAG, "No se guardan credenciales: sin almacenamiento cifrado")
            return
        }
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
    }

    fun getEmail(): String? = sharedPreferences?.getString(KEY_EMAIL, null)

    fun getPassword(): String? = sharedPreferences?.getString(KEY_PASSWORD, null)

    fun isBiometricEnabled(): Boolean =
        sharedPreferences?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false

    fun clearCredentials() {
        sharedPreferences?.edit()
            ?.remove(KEY_EMAIL)
            ?.remove(KEY_PASSWORD)
            ?.putBoolean(KEY_BIOMETRIC_ENABLED, false)
            ?.apply()
    }

    fun disableBiometric() {
        sharedPreferences?.edit()
            ?.putBoolean(KEY_BIOMETRIC_ENABLED, false)
            ?.apply()
    }

    companion object {
        private const val TAG = "SecurePreferences"
        private const val PREFS_NAME = "ecomap_secure_prefs"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
