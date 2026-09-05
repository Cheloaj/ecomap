package com.ecomap.socio.presentation.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.dp

/**
 * Sistema de animaciones premium estilo Apple
 */
object AppleAnimations {

    // Duraciones estándar (en ms)
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 200
    const val DURATION_NORMAL = 300
    const val DURATION_MEDIUM = 400
    const val DURATION_SLOW = 500
    const val DURATION_VERY_SLOW = 800

    // Spring configurations
    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val springLowBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val springNoBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Tween configurations
    val tweenFast = tween<Float>(
        durationMillis = DURATION_FAST,
        easing = FastOutSlowInEasing
    )

    val tweenNormal = tween<Float>(
        durationMillis = DURATION_NORMAL,
        easing = FastOutSlowInEasing
    )

    val tweenMedium = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = FastOutSlowInEasing
    )

    val tweenSlow = tween<Float>(
        durationMillis = DURATION_SLOW,
        easing = FastOutSlowInEasing
    )

    // Enter/Exit transitions
    fun fadeInSlideUp(duration: Int = DURATION_NORMAL) = fadeIn(
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    ) + slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    )

    fun fadeOutSlideDown(duration: Int = DURATION_NORMAL) = fadeOut(
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    ) + slideOutVertically(
        targetOffsetY = { it / 4 },
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    )

    // Stagger animation delays
    fun staggerDelay(index: Int, baseDelay: Int = 50): Int {
        return baseDelay * index
    }
}

/**
 * Sistema de espaciado premium estilo Apple
 */
object AppleSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val xxLarge = 48.dp
    val xxxLarge = 64.dp
}

/**
 * Sistema de elevaciones y sombras
 */
object AppleElevation {
    val none = 0.dp
    val small = 2.dp
    val medium = 4.dp
    val large = 8.dp
    val extraLarge = 16.dp
    val floating = 24.dp
}

/**
 * Sistema de bordes redondeados
 */
object AppleCornerRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 20.dp
    val xxLarge = 24.dp
    val circular = 999.dp
}

/**
 * Tamaños de iconos
 */
object AppleIconSize {
    val small = 16.dp
    val medium = 24.dp
    val large = 32.dp
    val extraLarge = 48.dp
    val xxLarge = 64.dp
    val hero = 96.dp
}

/**
 * Configuraciones de blur (desenfoque)
 */
object AppleBlur {
    const val LIGHT = 10f
    const val MEDIUM = 20f
    const val HEAVY = 40f
}
