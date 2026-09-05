package com.ecomap.socio.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ecomap.socio.data.model.Subscription
import com.ecomap.socio.data.model.SubscriptionPlan
import com.ecomap.socio.data.remote.EmailService
import com.ecomap.socio.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de suscripciones para Socios (ficticio para demo)
 * Guarda el estado PRO/Free del negocio localmente
 */
@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emailService: EmailService,
    private val authRepository: AuthRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "subscription_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "SubscriptionManager"
        private const val KEY_IS_PRO = "is_pro_user"
        private const val KEY_SUBSCRIPTION_DATE = "subscription_date"
        private const val KEY_PLAN_NAME = "plan_name"
    }

    /**
     * Verifica si el socio tiene suscripción PRO activa
     */
    fun isProUser(): Boolean {
        return prefs.getBoolean(KEY_IS_PRO, false)
    }

    /**
     * Obtiene el plan actual del socio
     */
    fun getCurrentPlan(): SubscriptionPlan {
        return if (isProUser()) {
            SubscriptionPlan.PRO
        } else {
            SubscriptionPlan.FREE
        }
    }

    /**
     * Obtiene la suscripción actual
     */
    fun getCurrentSubscription(): Subscription {
        val plan = getCurrentPlan()
        val startDate = prefs.getLong(KEY_SUBSCRIPTION_DATE, 0L)

        return Subscription(
            plan = plan,
            isActive = plan == SubscriptionPlan.PRO,
            startDate = if (startDate > 0) startDate else System.currentTimeMillis(),
            autoRenew = true
        )
    }

    /**
     * Activa la suscripción PRO (ficticio - sin cobro real)
     * Actualiza la base de datos y envía email de confirmación
     */
    suspend fun activateProSubscription(): Boolean {
        return try {
            val activationTime = System.currentTimeMillis()

            // Obtener usuario actual
            val currentUser = authRepository.getCurrentUser().getOrNull()
            if (currentUser == null) {
                Log.e(TAG, "No se pudo obtener el usuario actual")
                return false
            }

            // 1. Actualizar estado PRO en la base de datos primero
            Log.d(TAG, "Actualizando estado PRO en la base de datos para usuario: ${currentUser.id}")
            val updateResult = authRepository.updateProStatus(currentUser.id, true)
            if (updateResult.isFailure) {
                Log.e(TAG, "Error al actualizar estado PRO en BD: ${updateResult.exceptionOrNull()?.message}")
                return false
            }
            Log.d(TAG, "✅ Estado PRO actualizado en la base de datos")

            // 2. Guardar en SharedPreferences local
            prefs.edit().apply {
                putBoolean(KEY_IS_PRO, true)
                putLong(KEY_SUBSCRIPTION_DATE, activationTime)
                putString(KEY_PLAN_NAME, SubscriptionPlan.PRO.displayName)
                apply()
            }
            Log.d(TAG, "✅ Estado PRO guardado localmente")

            // 3. Enviar email de confirmación en segundo plano
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(activationTime))

                    emailService.sendProActivationEmail(
                        userEmail = currentUser.email,
                        userName = currentUser.fullName,
                        planName = "PRO Mensual",
                        activationDate = formattedDate
                    )
                    Log.d(TAG, "Email de activación enviado a ${currentUser.email}")
                } catch (e: Exception) {
                    // No fallar la activación si el email falla
                    Log.e(TAG, "Error al enviar email de confirmación: ${e.message}", e)
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar suscripción: ${e.message}", e)
            false
        }
    }

    /**
     * Cancela la suscripción PRO (vuelve a Free)
     */
    fun cancelSubscription(): Boolean {
        return try {
            prefs.edit().apply {
                putBoolean(KEY_IS_PRO, false)
                remove(KEY_SUBSCRIPTION_DATE)
                remove(KEY_PLAN_NAME)
                apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene la fecha de inicio de la suscripción
     */
    fun getSubscriptionStartDate(): Long {
        return prefs.getLong(KEY_SUBSCRIPTION_DATE, 0L)
    }
}
