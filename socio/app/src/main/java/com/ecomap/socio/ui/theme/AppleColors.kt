package com.ecomap.socio.ui.theme

import androidx.compose.ui.graphics.Color

// 🎨 Paleta de colores estilo Apple - iOS Dark Mode
object AppleColors {
    // Backgrounds - Oscuros profundos
    val DarkBackground = Color(0xFF000000)           // Negro puro estilo iPhone
    val DarkSurface = Color(0xFF1C1C1E)             // Gris oscuro iOS
    val DarkCard = Color(0xFF2C2C2E)                // Cards elevados
    val DarkElevated = Color(0xFF3A3A3C)            // Elementos elevados

    // Backgrounds - Claros
    val LightBackground = Color(0xFFF2F2F7)         // Gris muy claro iOS
    val LightSurface = Color(0xFFFFFFFF)            // Blanco puro
    val LightCard = Color(0xFFFFFFFF)               // Cards en modo claro
    val LightElevated = Color(0xFFF2F2F7)           // Elementos elevados

    // iOS System Colors
    val IOSBlue = Color(0xFF007AFF)                 // Azul iOS
    val IOSGreen = Color(0xFF34C759)                // Verde iOS (Success)
    val IOSIndigo = Color(0xFF5856D6)               // Índigo iOS
    val IOSPurple = Color(0xFFAF52DE)               // Morado iOS
    val IOSPink = Color(0xFFFF2D55)                 // Rosa iOS
    val IOSRed = Color(0xFFFF3B30)                  // Rojo iOS (Error)
    val IOSOrange = Color(0xFFFF9500)               // Naranja iOS (Warning)
    val IOSYellow = Color(0xFFFFCC00)               // Amarillo iOS
    val IOSTeal = Color(0xFF5AC8FA)                 // Teal iOS

    // Grays - Dark Mode
    val Gray1 = Color(0xFF8E8E93)                   // Text secundario oscuro
    val Gray2 = Color(0xFF636366)                   // Terciario oscuro
    val Gray3 = Color(0xFF48484A)                   // Cuaternario oscuro
    val Gray4 = Color(0xFF3A3A3C)                   // Quinario oscuro
    val Gray5 = Color(0xFF2C2C2E)                   // Sexto nivel oscuro
    val Gray6 = Color(0xFF1C1C1E)                   // Más oscuro

    // Grays - Light Mode
    val LightGray1 = Color(0xFF8E8E93)              // Text secundario claro
    val LightGray2 = Color(0xFFAEAEB2)              // Terciario claro
    val LightGray3 = Color(0xFFC7C7CC)              // Cuaternario claro
    val LightGray4 = Color(0xFFD1D1D6)              // Quinario claro
    val LightGray5 = Color(0xFFE5E5EA)              // Sexto nivel claro
    val LightGray6 = Color(0xFFF2F2F7)              // Más claro

    // Semantic Colors
    val Success = IOSGreen
    val Error = IOSRed
    val Warning = IOSOrange
    val Info = IOSBlue

    // Special Effects
    val BlurredGlass = Color(0x33FFFFFF)            // Efecto glassmorphism
    val ShadowColor = Color(0x40000000)             // Sombras sutiles

    // Gradient Colors
    val GradientStart = Color(0xFF007AFF)
    val GradientEnd = Color(0xFF5856D6)
}
