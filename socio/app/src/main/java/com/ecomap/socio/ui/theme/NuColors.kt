package com.ecomap.socio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores inspirada en Nu/Nubank
 * Diseño moderno y minimalista
 */
object NuColors {
    // Color principal - Morado Nu
    val Primary = Color(0xFF8A2BE2) // Morado claro para botones
    val PrimaryDark = Color(0xFF6A00A8) // Morado oscuro para fondos
    val PrimaryLight = Color(0xFFA835D4)

    // Backgrounds
    val Background = Color(0xFFFFFFFF)
    val BackgroundDark = PrimaryDark // Fondo morado oscuro
    val Surface = Color(0xFFFFFFFF)

    // Textos
    val TextPrimary = Color(0xFF000000)
    val TextSecondary = Color(0xFF757575)
    val TextOnPrimary = Color(0xFFFFFFFF)

    // Botones
    val ButtonEnabled = Primary
    val ButtonDisabled = Color(0xFFE0E0E0)
    val ButtonTextEnabled = Color(0xFFFFFFFF)
    val ButtonTextDisabled = Color(0xFF9E9E9E)

    // TextField
    val TextFieldLine = Color(0xFFE0E0E0)
    val TextFieldLineFocused = Primary
    val TextFieldText = TextPrimary
    val TextFieldPlaceholder = Color(0xFF9E9E9E)

    // Estados
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFE53935)
    val Warning = Color(0xFFFF9800)

    // Overlay
    val Overlay = Color(0x80FFFFFF) // 50% transparente
}
