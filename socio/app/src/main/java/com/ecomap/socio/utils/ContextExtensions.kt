package com.ecomap.socio.utils

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.fragment.app.FragmentActivity

/**
 * Encuentra la FragmentActivity desde un Context de Compose.
 *
 * En Jetpack Compose, LocalContext.current no retorna directamente una Activity,
 * sino un ContextWrapper. Esta función "desenvuelve" el contexto hasta encontrar
 * la FragmentActivity subyacente.
 *
 * IMPORTANTE: BiometricPrompt requiere FragmentActivity, no solo Activity.
 * Por eso MainActivity debe extender FragmentActivity en lugar de ComponentActivity.
 *
 * @return FragmentActivity si la encuentra, null en caso contrario
 */
fun Context.findActivity(): FragmentActivity? {
    val tag = "ContextExtensions"
    var currentContext: Context? = this
    var iteration = 0

    Log.d(tag, "🔍 Starting findActivity() for context: ${this.javaClass.simpleName}")

    while (currentContext is ContextWrapper) {
        Log.d(tag, "🔄 Iteration $iteration: Context is ${currentContext.javaClass.simpleName}")
        if (currentContext is FragmentActivity) {
            Log.d(tag, "✅ Found FragmentActivity: ${currentContext.javaClass.simpleName}")
            return currentContext
        }
        currentContext = currentContext.baseContext
        iteration++
        if (currentContext == null) {
            Log.e(tag, "❌ Context chain ended with null baseContext")
            break
        }
    }
    Log.e(tag, "❌ No FragmentActivity found in context chain")
    return null
}

/**
 * Extrae el primer nombre de un nombre completo.
 *
 * Ejemplos:
 * - "Victor Manuel Hernandez" → "Victor"
 * - "Ana María" → "Ana"
 * - "José" → "José"
 * - null o "" → "Usuario"
 *
 * @param fullName El nombre completo del usuario
 * @return El primer nombre o "Usuario" si no hay nombre
 */
fun getFirstName(fullName: String?): String {
    return fullName
        ?.trim()
        ?.split(" ")
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "Usuario"
}