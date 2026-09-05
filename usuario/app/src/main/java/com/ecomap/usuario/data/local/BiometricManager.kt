package com.ecomap.usuario.data.local

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val context: Context) {

    /**
     * Verifica si el dispositivo tiene capacidades biométricas
     */
    fun canAuthenticate(): BiometricCapability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricCapability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricCapability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricCapability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricCapability.NoneEnrolled
            else -> BiometricCapability.Unknown
        }
    }

    /**
     * Muestra el prompt de autenticación biométrica
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Autenticación Biométrica",
        subtitle: String = "Usa tu huella dactilar para iniciar sesión",
        negativeButtonText: String = "Cancelar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

sealed class BiometricCapability {
    object Available : BiometricCapability()
    object NoHardware : BiometricCapability()
    object HardwareUnavailable : BiometricCapability()
    object NoneEnrolled : BiometricCapability()
    object Unknown : BiometricCapability()
}
