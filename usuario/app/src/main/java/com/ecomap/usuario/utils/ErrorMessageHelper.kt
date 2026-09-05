package com.ecomap.usuario.utils

/**
 * Helper para convertir mensajes de error técnicos en mensajes amigables para el usuario.
 */
object ErrorMessageHelper {
    fun getFriendlyMessage(error: String?): String {
        return when {
            error == null -> "Error desconocido"
            error.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
            error.contains("Email not confirmed", ignoreCase = true) -> "Por favor verifica tu correo electrónico"
            error.contains("Invalid email", ignoreCase = true) -> "Correo electrónico inválido"
            error.contains("network", ignoreCase = true) -> "Error de conexión. Verifica tu internet"
            error.contains("timeout", ignoreCase = true) -> "La solicitud tardó demasiado. Intenta de nuevo"
            else -> error
        }
    }
}
