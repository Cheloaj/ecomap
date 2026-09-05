package com.ecomap.socio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🎨 Theme estilo Apple con Dark Mode por defecto
private val AppleDarkColorScheme = darkColorScheme(
    // Primarios - Azul iOS
    primary = AppleColors.IOSBlue,
    onPrimary = Color.White,
    primaryContainer = AppleColors.IOSBlue.copy(alpha = 0.2f),
    onPrimaryContainer = AppleColors.IOSBlue,

    // Secundarios - Verde iOS
    secondary = AppleColors.IOSGreen,
    onSecondary = Color.White,
    secondaryContainer = AppleColors.IOSGreen.copy(alpha = 0.2f),
    onSecondaryContainer = AppleColors.IOSGreen,

    // Terciarios - Índigo iOS
    tertiary = AppleColors.IOSIndigo,
    onTertiary = Color.White,
    tertiaryContainer = AppleColors.IOSIndigo.copy(alpha = 0.2f),
    onTertiaryContainer = AppleColors.IOSIndigo,

    // Backgrounds - Negro profundo iOS
    background = AppleColors.DarkBackground,
    onBackground = Color.White,

    // Surfaces - Gris oscuro iOS
    surface = AppleColors.DarkSurface,
    onSurface = Color.White,
    surfaceVariant = AppleColors.DarkCard,
    onSurfaceVariant = AppleColors.Gray1,

    // Outline
    outline = AppleColors.Gray3,
    outlineVariant = AppleColors.Gray4,

    // Error
    error = AppleColors.IOSRed,
    onError = Color.White,
    errorContainer = AppleColors.IOSRed.copy(alpha = 0.2f),
    onErrorContainer = AppleColors.IOSRed,

    // Otros
    inverseSurface = Color.White,
    inverseOnSurface = AppleColors.DarkBackground,
    inversePrimary = AppleColors.IOSBlue.copy(alpha = 0.8f)
)

private val AppleLightColorScheme = lightColorScheme(
    // Primarios - Azul iOS
    primary = AppleColors.IOSBlue,
    onPrimary = Color.White,
    primaryContainer = AppleColors.IOSBlue.copy(alpha = 0.1f),
    onPrimaryContainer = AppleColors.IOSBlue,

    // Secundarios - Verde iOS
    secondary = AppleColors.IOSGreen,
    onSecondary = Color.White,
    secondaryContainer = AppleColors.IOSGreen.copy(alpha = 0.1f),
    onSecondaryContainer = AppleColors.IOSGreen,

    // Terciarios - Índigo iOS
    tertiary = AppleColors.IOSIndigo,
    onTertiary = Color.White,
    tertiaryContainer = AppleColors.IOSIndigo.copy(alpha = 0.1f),
    onTertiaryContainer = AppleColors.IOSIndigo,

    // Backgrounds - Gris muy claro iOS
    background = AppleColors.LightBackground,
    onBackground = Color.Black,

    // Surfaces - Blanco
    surface = AppleColors.LightSurface,
    onSurface = Color.Black,
    surfaceVariant = AppleColors.LightCard,
    onSurfaceVariant = AppleColors.LightGray1,

    // Outline
    outline = AppleColors.LightGray3,
    outlineVariant = AppleColors.LightGray4,

    // Error
    error = AppleColors.IOSRed,
    onError = Color.White,
    errorContainer = AppleColors.IOSRed.copy(alpha = 0.1f),
    onErrorContainer = AppleColors.IOSRed,

    // Otros
    inverseSurface = AppleColors.DarkSurface,
    inverseOnSurface = Color.White,
    inversePrimary = AppleColors.IOSBlue
)

@Composable
fun EcoMapSocioTheme(
    darkTheme: Boolean = true,  // 🌙 DARK MODE POR DEFECTO
    dynamicColor: Boolean = false,  // Desactivamos dynamic color para usar nuestra paleta
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AppleDarkColorScheme
        else -> AppleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleTypography,  // 📝 Tipografía Apple
        content = content
    )
}
