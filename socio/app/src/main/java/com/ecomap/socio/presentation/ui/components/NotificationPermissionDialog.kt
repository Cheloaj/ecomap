package com.ecomap.socio.presentation.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ecomap.socio.utils.NotificationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Solicitar permiso de notificaciones (nativo de Android como GPS/Cámara)
 * Sin dialog personalizado - usa el sistema de Android
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionRequest(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current

    // Solo solicitar permiso en Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )

        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                println("📱 Solicitando permiso de notificaciones (nativo)...")
                notificationPermissionState.launchPermissionRequest()
            }
        }

        LaunchedEffect(notificationPermissionState.status.isGranted) {
            if (notificationPermissionState.status.isGranted) {
                println("✅ Permiso de notificaciones otorgado")
                NotificationHelper.createNotificationChannel(context)
                onPermissionGranted()
            } else {
                println("⚠️ Permiso de notificaciones denegado")
                onPermissionDenied()
            }
        }
    } else {
        // En versiones anteriores a Android 13, crear canal directamente
        LaunchedEffect(Unit) {
            NotificationHelper.createNotificationChannel(context)
            onPermissionGranted()
        }
    }
}
