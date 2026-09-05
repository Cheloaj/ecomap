package com.ecomap.socio.utils

/**
 * Convierte mensajes de error técnicos en mensajes profesionales y amigables
 */
object ErrorMessageHelper {
    fun getFriendlyMessage(technicalMessage: String?): String {
        if (technicalMessage == null) return "Ocurrió un error inesperado. Por favor, inténtalo nuevamente."

        val message = technicalMessage.lowercase()

        return when {
            // Errores de autenticación
            message.contains("invalid login credentials") ||
            message.contains("invalid credentials") ||
            message.contains("credenciales") && message.contains("incorrectas") ->
                "El correo electrónico o la contraseña son incorrectos. Por favor, verifica tus datos."

            message.contains("user not found") ||
            message.contains("usuario no encontrado") ->
                "No encontramos una cuenta con ese correo electrónico. ¿Deseas registrarte?"

            message.contains("email not confirmed") ||
            message.contains("email no verificado") ->
                "Por favor, verifica tu correo electrónico antes de continuar."

            message.contains("user already registered") ||
            message.contains("usuario ya registrado") ||
            message.contains("email already exists") ->
                "Ya existe una cuenta con este correo electrónico. Intenta iniciar sesión."

            message.contains("weak password") ||
            message.contains("contraseña débil") ->
                "La contraseña debe tener al menos 6 caracteres."

            message.contains("too many requests") ||
            message.contains("rate limit") ->
                "Has realizado demasiados intentos. Por favor, espera unos momentos."

            // Errores de código de verificación
            message.contains("código inválido") ||
            message.contains("invalid code") ||
            message.contains("incorrect code") ->
                "El código ingresado es incorrecto. Por favor, verifica e intenta nuevamente."

            message.contains("código expirado") ||
            message.contains("code expired") ->
                "El código ha expirado. Por favor, solicita uno nuevo."

            // Errores de red/conexión
            message.contains("network") ||
            message.contains("connection") ||
            message.contains("conexión") ||
            message.contains("timeout") ->
                "No pudimos conectar con el servidor. Verifica tu conexión a internet."

            message.contains("no internet") ||
            message.contains("sin internet") ->
                "No hay conexión a internet. Por favor, verifica tu red."

            // Errores de sesión
            message.contains("session expired") ||
            message.contains("sesión expirada") ->
                "Tu sesión ha expirado. Por favor, inicia sesión nuevamente."

            message.contains("not authenticated") ||
            message.contains("no autenticado") ->
                "Necesitas iniciar sesión para continuar."

            // Errores de datos
            message.contains("missing") && message.contains("email") ->
                "Por favor, ingresa tu correo electrónico."

            message.contains("missing") && message.contains("password") ->
                "Por favor, ingresa tu contraseña."

            message.contains("invalid email") ||
            message.contains("email inválido") ->
                "El formato del correo electrónico no es válido."

            message.contains("passwords") && message.contains("match") ||
            message.contains("contraseñas no coinciden") ->
                "Las contraseñas no coinciden. Por favor, verifica."

            // Errores de negocio
            message.contains("business not found") ||
            message.contains("negocio no encontrado") ->
                "No se encontró información del negocio."

            message.contains("ubicación") ->
                "Debe seleccionar una ubicación válida para continuar."

            // Errores de permisos
            message.contains("permission denied") ||
            message.contains("access denied") ||
            message.contains("forbidden") ->
                "No tienes permisos para realizar esta acción."

            // Errores de archivos
            message.contains("file") && message.contains("large") ||
            message.contains("archivo") && message.contains("grande") ->
                "El archivo es demasiado grande. Por favor, selecciona uno más pequeño."

            message.contains("upload failed") ||
            message.contains("error al subir") ->
                "No se pudo subir el archivo. Por favor, intenta nuevamente."

            // Error genérico con mensaje técnico
            message.length > 100 ->
                "Ocurrió un error técnico. Por favor, inténtalo nuevamente o contacta a soporte."

            // Default - retornar mensaje original si no contiene texto técnico
            else -> {
                // Si el mensaje original ya es amigable (no contiene caracteres técnicos), úsalo
                if (message.contains("error:") ||
                    message.contains("exception") ||
                    message.contains("failed") ||
                    message.contains("null") ||
                    message.contains("undefined") ||
                    message.contains("[") ||
                    message.contains("{") ||
                    message.contains("code:") ||
                    message.contains("status:")) {
                    "Ocurrió un error inesperado. Por favor, inténtalo nuevamente."
                } else {
                    technicalMessage // El mensaje ya es amigable
                }
            }
        }
    }
}
