package com.ecomap.usuario

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import java.io.File

@HiltAndroidApp
class EcoMapUsuarioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid
        try {
            org.osmdroid.config.Configuration.getInstance().apply {
                userAgentValue = "EcoMapUsuario/1.0"
                osmdroidBasePath = File(cacheDir, "osmdroid").apply {
                    mkdirs()
                }
                osmdroidTileCache = File(osmdroidBasePath, "tiles").apply {
                    mkdirs()
                }
            }
            Log.d("EcoMapUsuarioApp", "✅ osmdroid configurado correctamente")
        } catch (e: Exception) {
            Log.e("EcoMapUsuarioApp", "❌ Error al configurar osmdroid: ${e.message}")
        }
    }
}
