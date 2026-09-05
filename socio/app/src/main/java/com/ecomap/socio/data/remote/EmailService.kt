package com.ecomap.socio.data.remote

import android.util.Log
import com.ecomap.socio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para enviar emails usando Supabase Edge Functions
 */
@Singleton
class EmailService @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "EmailService"
        private const val FUNCTION_NAME = "send-pro-activation-email-socio"
    }

    @Serializable
    data class EmailRequest(
        val email: String,
        val userName: String,
        val planName: String,
        val activationDate: String
    )

    /**
     * Envía un email de confirmación de activación PRO
     */
    suspend fun sendProActivationEmail(
        userEmail: String,
        userName: String,
        planName: String = "PRO Mensual",
        activationDate: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Enviando email de activación PRO a: $userEmail")

            val requestData = EmailRequest(
                email = userEmail,
                userName = userName,
                planName = planName,
                activationDate = activationDate
            )

            val jsonBody = Json.encodeToString(requestData)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val url = "${BuildConfig.SUPABASE_URL}/functions/v1/$FUNCTION_NAME"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "Email enviado exitosamente")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error al enviar email: ${response.code} - ${response.message}")
                Result.failure(Exception("Error al enviar email de confirmación"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al enviar email: ${e.message}", e)
            // No fallar el proceso si el email falla, solo registrar
            Result.failure(e)
        }
    }
}
