package com.ecomap.usuario.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores estilo Nubank
 * Mismo diseño que EcoMap Socio
 */
object NuColors {
    // Color principal - Morado Nu
    val Primary = Color(0xFF8A2BE2)        // Morado vibrante para botones
    val PrimaryDark = Color(0xFF6A00A8)    // Morado oscuro para fondos
    val PrimaryLight = Color(0xFFB968F5)   // Morado claro para hover

    // Backgrounds
    val Background = Color(0xFFFFFFFF)      // Blanco
    val Surface = Color(0xFFFFFFFF)         // Blanco
    val SurfaceVariant = Color(0xFFF5F5F5)  // Gris muy claro

    // Textos
    val TextPrimary = Color(0xFF000000)     // Negro
    val TextSecondary = Color(0xFF757575)   // Gris medio
    val TextTertiary = Color(0xFF9E9E9E)    // Gris claro
    val TextOnPrimary = Color(0xFFFFFFFF)   // Blanco sobre morado

    // Estados
    val Success = Color(0xFF4CAF50)         // Verde
    val Error = Color(0xFFE53935)           // Rojo
    val Warning = Color(0xFFFF9800)         // Naranja
    val Info = Color(0xFF2196F3)            // Azul

    // Divisores y bordes
    val Divider = Color(0xFFE0E0E0)         // Gris divisor
    val Border = Color(0xFFBDBDBD)          // Gris borde

    // Overlays
    val Scrim = Color(0x99000000)           // Negro semi-transparente
    val Shadow = Color(0x1A000000)          // Negro 10% para sombras
}
