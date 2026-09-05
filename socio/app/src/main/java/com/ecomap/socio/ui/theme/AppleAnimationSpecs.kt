package com.ecomap.socio.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// Animaciones estilo Apple - Fluidas y naturales
object AppleAnimationSpecs {
    // Spring Animations - Principales de iOS
    val springDefault = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val springSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Durations
    const val DURATION_FAST = 200
    const val DURATION_MEDIUM = 300
    const val DURATION_SLOW = 500

    // Enter/Exit Animations
    val fadeInAnimation = fadeIn(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = FastOutSlowInEasing
        )
    )

    val fadeOutAnimation = fadeOut(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = FastOutSlowInEasing
        )
    )

    val scaleInAnimation = scaleIn(
        initialScale = 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val scaleOutAnimation = scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(
            durationMillis = DURATION_FAST,
            easing = FastOutSlowInEasing
        )
    )

    val slideInFromBottomAnimation = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val slideOutToBottomAnimation = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = FastOutSlowInEasing
        )
    )

    val slideInFromTopAnimation = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val slideOutToTopAnimation = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = FastOutSlowInEasing
        )
    )

    // Combined animations (fade + scale) - Muy Apple
    fun fadeWithScale() = fadeInAnimation + scaleInAnimation
    fun fadeOutWithScale() = fadeOutAnimation + scaleOutAnimation

    // Page transitions
    fun pageEnterTransition() = fadeInAnimation + slideInFromBottomAnimation
    fun pageExitTransition() = fadeOutAnimation + slideOutToBottomAnimation
}
