package com.ecomap.socio.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ecomap.socio.R
import com.ecomap.socio.domain.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que chequea notificaciones en BACKGROUND cada 15 minutos
 * Funciona incluso cuando la app está completamente cerrada
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            println("🔔 NotificationWorker: Chequeando notificaciones en background...")

            // Obtener notificaciones no leídas
            val result = notificationRepository.getUnreadNotifications()

            result.fold(
                onSuccess = { notifications ->
                    println("✅ NotificationWorker: ${notifications.size} notificaciones no leídas encontradas")

                    // Mostrar cada notificación
                    notifications.forEach { notification ->
                        showNotification(
                            id = notification.id.hashCode(),
                            title = notification.title,
                            body = notification.body
                        )
                        println("📬 NotificationWorker: Notificación mostrada: ${notification.title}")
                    }
                },
                onFailure = { error ->
                    println("❌ NotificationWorker: Error al obtener notificaciones: ${error.message}")
                }
            )

            Result.success()
        } catch (e: Exception) {
            println("❌ NotificationWorker: Error crítico: ${e.message}")
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(id: Int, title: String, body: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificaciones (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones EcoMap",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de estado de cuenta"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crear notificación
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "ecomap_notifications"
        const val WORK_NAME = "notification_worker"
    }
}
