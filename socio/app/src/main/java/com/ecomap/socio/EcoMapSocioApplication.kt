package com.ecomap.socio

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject
import android.util.Log

@HiltAndroidApp
class EcoMapSocioApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid
        try {
            org.osmdroid.config.Configuration.getInstance().apply {
                userAgentValue = "EcoMapSocio/1.0"
                osmdroidBasePath = File(cacheDir, "osmdroid").apply {
                    mkdirs() // Asegurar que el directorio base exista
                }
                osmdroidTileCache = File(osmdroidBasePath, "tiles").apply {
                    mkdirs() // Asegurar que el directorio de tiles exista
                }
            }
            Log.d("EcoMapSocioApplication", "✅ osmdroid configurado correctamente")
        } catch (e: Exception) {
            Log.e("EcoMapSocioApplication", "❌ Error al configurar osmdroid: ${e.message}")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}