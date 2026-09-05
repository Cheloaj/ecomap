package com.ecomap.usuario.ui.theme

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

// 🎨 Theme estilo Nubank (igual que EcoMapSocio)
private val NuLightColorScheme = lightColorScheme(
    // Primarios - Morado Nubank
    primary = NuColors.Primary,
    onPrimary = NuColors.TextOnPrimary,
    primaryContainer = NuColors.PrimaryLight.copy(alpha = 0.2f),
    onPrimaryContainer = NuColors.Primary,

    // Secundarios - Verde Success
    secondary = NuColors.Success,
    onSecondary = Color.White,
    secondaryContainer = NuColors.Success.copy(alpha = 0.2f),
    onSecondaryContainer = NuColors.Success,

    // Terciarios - Azul Info
    tertiary = NuColors.Info,
    onTertiary = Color.White,
    tertiaryContainer = NuColors.Info.copy(alpha = 0.2f),
    onTertiaryContainer = NuColors.Info,

    // Backgrounds - Blanco
    background = NuColors.Background,
    onBackground = NuColors.TextPrimary,

    // Surfaces - Blanco
    surface = NuColors.Surface,
    onSurface = NuColors.TextPrimary,
    surfaceVariant = NuColors.SurfaceVariant,
    onSurfaceVariant = NuColors.TextSecondary,

    // Outline
    outline = NuColors.Border,
    outlineVariant = NuColors.Divider,

    // Error
    error = NuColors.Error,
    onError = Color.White,
    errorContainer = NuColors.Error.copy(alpha = 0.1f),
    onErrorContainer = NuColors.Error
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
fun EcoMapUsuarioTheme(
    darkTheme: Boolean = false, // Por defecto modo claro como EcoMapSocio
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> NuLightColorScheme // Siempre usar tema Nubank
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
