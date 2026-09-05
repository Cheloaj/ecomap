package com.ecomap.socio.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ecomap.socio.MainActivity
import com.ecomap.socio.R

/**
 * Helper para notificaciones locales de Android
 * Sin Firebase - Solo Supabase Realtime
 */
object NotificationHelper {

    private const val CHANNEL_ID = "ecomap_notifications"
    private const val CHANNEL_NAME = "EcoMap Notificaciones"
    private const val CHANNEL_DESCRIPTION = "Notificaciones de EcoMap Socio"

    /**
     * Verificar si el permiso de notificaciones está otorgado
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // En versiones anteriores a Android 13, el permiso se otorga automáticamente
            true
        }
    }

    /**
     * Crear canal de notificaciones (requerido en Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            println("✅ Canal de notificaciones creado: $CHANNEL_NAME")
        }
    }

    /**
     * Mostrar notificación local de Android
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = System.currentTimeMillis().toInt(),
        notificationType: String = "general"
    ) {
        // Verificar permiso
        if (!hasNotificationPermission(context)) {
            println("⚠️ No hay permiso de notificaciones")
            return
        }

        // Crear canal si no existe
        createNotificationChannel(context)

        // Intent para abrir la app al hacer clic
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", notificationType)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Mostrar notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)

        println("✅ Notificación mostrada: $title")
    }
}
