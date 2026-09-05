package com.ecomap.socio

// 1. IMPORTA ESTOS (SON LOS NUEVOS)
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect

// (Tus otros imports están bien)
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat // (Para el Controller)
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ecomap.socio.data.workers.NotificationWorker
import com.ecomap.socio.navigation.NavGraph
import com.ecomap.socio.presentation.ui.components.NotificationPermissionRequest
import com.ecomap.socio.ui.theme.EcoMapSocioTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import android.util.Log

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 2. ¡ESTA ES LA NUEVA LÍNEA!
        // Reemplaza setDecorFitsSystemWindows y statusBarColor
        enableEdgeToEdge()

        // 🔹 Iniciar el worker de notificaciones en background
        scheduleNotificationWorker()

        setContent {
            // 🔹 3. DETECTAR EL TEMA DEL SISTEMA
            val isDarkTheme = isSystemInDarkTheme()

            // 🔹 4. CÓDIGO PARA CAMBIAR ICONOS (CLARO/OSCURO)
            // Esto se ejecuta automáticamente cuando el tema cambia
            DisposableEffect(isDarkTheme) {
                val window = this@MainActivity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                // Si NO es tema oscuro (es claro), queremos iconos oscuros.
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                // Hacemos lo mismo para la barra de navegación (botones/gestos)
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme

                onDispose {}
            }

            EcoMapSocioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()

                        // 🔹 Gráfico de navegación principal
                        NavGraph(
                            navController = navController,
                            onExitApp = {
                                Log.d("MainActivity", "🚪 Saliendo de la app...")
                                finish()
                            }
                        )

                        // 🔹 Solicitud de permiso de notificaciones
                        NotificationPermissionRequest(
                            onPermissionGranted = {
                                Log.d("MainActivity", "✅ Usuario otorgó permiso de notificaciones")
                            },
                            onPermissionDenied = {
                                Log.w("MainActivity", "⚠️ Usuario rechazó permiso de notificaciones")
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Programa un Worker para chequear notificaciones en background.
     * (Esta función se queda exactamente igual, está perfecta)
     */
    private fun scheduleNotificationWorker() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                NotificationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d("MainActivity", "✅ Worker programado - ejecución inmediata y luego cada 15 min")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error al programar el Worker: ${e.message}")
        }
    }
}