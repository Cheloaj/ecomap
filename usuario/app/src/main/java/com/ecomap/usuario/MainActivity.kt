package com.ecomap.usuario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.ecomap.usuario.navigation.NavGraph
import com.ecomap.usuario.ui.theme.EcoMapUsuarioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var deepLinkData by mutableStateOf<DeepLinkData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Manejar deep link
        handleIntent(intent)

        setContent {
            val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

            DisposableEffect(isDarkTheme) {
                val window = this@MainActivity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)

                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme

                onDispose {}
            }

            EcoMapUsuarioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        deepLinkData = deepLinkData,
                        onDeepLinkHandled = { deepLinkData = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        Log.d("MainActivity", "Deep link received: $data")

        if (data != null && data.scheme == "ecomap" && data.host == "reset-password") {
            // Extraer el access_token del fragment (después del #)
            val fragment = data.fragment
            Log.d("MainActivity", "Fragment: $fragment")

            if (fragment != null) {
                // El fragment viene en formato: access_token=xxx&type=recovery
                val params = fragment.split("&").associate {
                    val (key, value) = it.split("=")
                    key to value
                }

                val accessToken = params["access_token"]
                Log.d("MainActivity", "Access token: $accessToken")

                if (accessToken != null) {
                    deepLinkData = DeepLinkData.ResetPassword(accessToken)
                }
            }
        }
    }
}

sealed class DeepLinkData {
    data class ResetPassword(val accessToken: String) : DeepLinkData()
}
